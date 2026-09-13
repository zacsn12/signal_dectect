package org.zacsn.signal_dectect.data.scanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrength;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.zacsn.signal_dectect.domain.model.FakeBaseStationCell;
import org.zacsn.signal_dectect.domain.model.FakeCellRiskLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Dedicated real-time detection engine for Rogue / Fake Base Stations.
 */
@Singleton
public class FakeBaseStationDetector {
    private static final String TAG = "FakeBaseStationDetector";
    private static final long SCAN_INTERVAL_MS = 2500L;

    private final Context context;
    private final TelephonyManager telephonyManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Listener listener;
    private boolean isScanning = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyCallback telephonyCallback;

    // History tracking for forced 2G downgrades & rapid LAC ping-pong
    private long lastHighGenTimestamp = 0;
    private String lastServingLac = null;
    private final LinkedList<Long> lacChangeTimestamps = new LinkedList<>();

    public interface Listener {
        void onCellsEvaluated(List<FakeBaseStationCell> cells, FakeCellRiskLevel overallRisk, boolean hasNewThreat);
        void onStatus(String status);
        void onError(String error);
    }

    @Inject
    public FakeBaseStationDetector(@ApplicationContext Context context) {
        this.context = context;
        this.telephonyManager = context != null ? (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE) : null;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public synchronized void start() {
        if (isScanning) return;
        if (telephonyManager == null) {
            if (listener != null) listener.onError("设备不支持蜂窝通信服务");
            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (listener != null) listener.onError("未授予位置信息权限，无法获取小区基站数据");
            return;
        }

        isScanning = true;
        lacChangeTimestamps.clear();
        lastServingLac = null;
        lastHighGenTimestamp = System.currentTimeMillis(); // assume initial high-gen baseline

        setupSignalListener();
        scheduleNextPoll();
        if (listener != null) listener.onStatus("正在扫描基站并分析安全特征...");
    }

    public synchronized void stop() {
        if (!isScanning) return;
        isScanning = false;
        mainHandler.removeCallbacks(pollRunnable);

        if (phoneStateListener != null && telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            phoneStateListener = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephonyCallback != null && telephonyManager != null) {
            telephonyManager.unregisterTelephonyCallback(telephonyCallback);
            telephonyCallback = null;
        }

        if (listener != null) listener.onStatus("伪基站扫描已停止");
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isScanning) return;
            pollAndAnalyzeCells();
            scheduleNextPoll();
        }
    };

    private void scheduleNextPoll() {
        mainHandler.postDelayed(pollRunnable, SCAN_INTERVAL_MS);
    }

    private void setupSignalListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = new TelephonyCallback() {};
            try {
                telephonyManager.registerTelephonyCallback(context.getMainExecutor(), telephonyCallback);
            } catch (Exception ignored) {}
            return;
        }

        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                if (isScanning) {
                    pollAndAnalyzeCells();
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    public synchronized void pollAndAnalyzeCells() {
        if (!isScanning) return;

        List<CellInfo> cellInfos = null;
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                cellInfos = telephonyManager.getAllCellInfo();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to read cell info: " + e.getMessage());
        }

        long now = System.currentTimeMillis();
        String operatorName = safeText(telephonyManager.getNetworkOperatorName(), "未知运营商");
        String networkOperator = safeText(telephonyManager.getNetworkOperator(), "UNKNOWN");

        List<FakeBaseStationCell> analyzedCells = new ArrayList<>();
        boolean hasThreat = false;
        FakeCellRiskLevel maxRisk = FakeCellRiskLevel.SAFE;

        if (cellInfos != null && !cellInfos.isEmpty()) {
            int totalCells = cellInfos.size();
            int neighborCount = totalCells - 1;

            // Check if any serving cell is 4G/5G
            for (CellInfo info : cellInfos) {
                if (info.isRegistered() && (info instanceof CellInfoLte || info instanceof CellInfoNr)) {
                    lastHighGenTimestamp = now;
                    break;
                }
            }

            for (CellInfo info : cellInfos) {
                RawCellRawData raw = extractRawCellData(info, operatorName, networkOperator);
                if (raw == null) continue;

                // Update LAC history if serving
                if (raw.isServing && !raw.lacTac.equals("未知")) {
                    if (lastServingLac != null && !lastServingLac.equals(raw.lacTac)) {
                        lacChangeTimestamps.add(now);
                    }
                    lastServingLac = raw.lacTac;
                }

                FakeBaseStationCell evaluated = evaluateCell(raw, neighborCount, now);
                analyzedCells.add(evaluated);

                if (evaluated.getRiskLevel() == FakeCellRiskLevel.DANGEROUS) {
                    hasThreat = true;
                    maxRisk = FakeCellRiskLevel.DANGEROUS;
                } else if (evaluated.getRiskLevel() == FakeCellRiskLevel.SUSPICIOUS && maxRisk != FakeCellRiskLevel.DANGEROUS) {
                    maxRisk = FakeCellRiskLevel.SUSPICIOUS;
                }
            }
        } else {
            // Fallback cell from operator level
            RawCellRawData fallback = new RawCellRawData();
            fallback.operator = operatorName;
            fallback.plmn = networkOperator;
            fallback.radioType = "蜂窝网络";
            fallback.isServing = true;
            fallback.signalDbm = -90;

            FakeBaseStationCell evaluated = evaluateCell(fallback, 0, now);
            analyzedCells.add(evaluated);
        }

        // Clean up old LAC timestamps (older than 15s)
        while (!lacChangeTimestamps.isEmpty() && now - lacChangeTimestamps.peekFirst() > 15000) {
            lacChangeTimestamps.pollFirst();
        }

        if (listener != null) {
            listener.onCellsEvaluated(analyzedCells, maxRisk, hasThreat);
        }
    }

    /**
     * Evaluates a raw cell against the multi-dimensional heuristic threat rule matrix.
     */
    public FakeBaseStationCell evaluateCell(RawCellRawData raw, int neighborCount, long now) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        // 1. Abnormal LAC/TAC inspection (位置区码异常)
        if (isAbnormalLac(raw.lacTac)) {
            score += 45;
            reasons.add("位置区码异常 (LAC=" + raw.lacTac + "，伪基站常用保留码诱发位置更新)");
        }

        // 2. 2G / GSM Forced Downgrade and Isolated High Power (孤立强信号2G与强制降网)
        boolean isGsm = raw.radioType.contains("2G") || raw.radioType.contains("GSM");
        if (isGsm) {
            if (raw.signalDbm >= -75) {
                if (neighborCount <= 1) {
                    score += 40;
                    reasons.add("孤立极强2G信号且无正常邻区 (电平 " + raw.signalDbm + " dBm，典型伪基站压制特征)");
                } else {
                    score += 15;
                    reasons.add("2G GSM 接收电平异常偏高 (" + raw.signalDbm + " dBm)");
                }
            }

            if (lastHighGenTimestamp > 0 && (now - lastHighGenTimestamp < 15000)) {
                score += 50;
                reasons.add("突发从4G/5G强制回退至2G GSM (疑似中间人降频劫持攻击)");
            }
        }

        // 3. Invalid / Suspicious Operator PLMN (非法或未知运营商代码)
        if (isInvalidPlmn(raw.plmn)) {
            score += 35;
            reasons.add("运营商代码异常 (PLMN=" + raw.plmn + "，非国内合规公共移动网)");
        }

        // 4. Invalid Cell ID (小区标识异常)
        if (isInvalidCellId(raw.cellId)) {
            score += 25;
            reasons.add("小区身份标识缺失或非法 (Cell ID=" + raw.cellId + ")");
        }

        // 5. Rapid LAC Reselection / Flapping (位置区码高频闪跳)
        if (raw.isServing && lacChangeTimestamps.size() >= 3) {
            score += 30;
            reasons.add("位置区码频繁跳变 (15秒内切换" + lacChangeTimestamps.size() + "次，疑似伪基站移动诱捕)");
        }

        // Risk Level determination
        FakeCellRiskLevel level;
        if (score >= 55) {
            level = FakeCellRiskLevel.DANGEROUS;
        } else if (score >= 25) {
            level = FakeCellRiskLevel.SUSPICIOUS;
        } else {
            level = FakeCellRiskLevel.SAFE;
        }

        String id = "CELL_" + raw.plmn + "_" + raw.radioType + "_" + raw.lacTac + "_" + raw.cellId + "_" + raw.pciPsc;

        return new FakeBaseStationCell(
                id,
                raw.operator,
                raw.plmn,
                raw.radioType,
                raw.cellId,
                raw.lacTac,
                raw.pciPsc,
                raw.channel,
                raw.signalDbm,
                raw.isServing,
                score,
                level,
                reasons,
                now
        );
    }

    private boolean isAbnormalLac(String lac) {
        if (lac == null || lac.trim().isEmpty() || "未知".equals(lac)) return false;
        String trimmed = lac.trim();
        return "0".equals(trimmed)
                || "65535".equals(trimmed)
                || "65534".equals(trimmed)
                || "0x0000".equalsIgnoreCase(trimmed)
                || "0xFFFF".equalsIgnoreCase(trimmed)
                || "0xFFFE".equalsIgnoreCase(trimmed);
    }

    private boolean isInvalidPlmn(String plmn) {
        if (plmn == null || plmn.trim().isEmpty() || "未知".equals(plmn) || "UNKNOWN".equalsIgnoreCase(plmn)) {
            return false; // missing is handled separately
        }
        String trimmed = plmn.trim();
        if ("00000".equals(trimmed) || "99999".equals(trimmed)) return true;
        // Check standard Chinese carrier MCC 460
        return !trimmed.startsWith("460") && trimmed.length() >= 5;
    }

    private boolean isInvalidCellId(String cellId) {
        if (cellId == null || cellId.trim().isEmpty() || "未知".equals(cellId)) return false;
        String trimmed = cellId.trim();
        return "0".equals(trimmed) || "65535".equals(trimmed) || "-1".equals(trimmed);
    }

    private RawCellRawData extractRawCellData(CellInfo info, String fallbackOp, String fallbackPlmn) {
        RawCellRawData data = new RawCellRawData();
        data.isServing = info.isRegistered();
        data.operator = fallbackOp;
        data.plmn = fallbackPlmn;

        if (info instanceof CellInfoLte) {
            CellInfoLte lte = (CellInfoLte) info;
            CellIdentityLte id = lte.getCellIdentity();
            data.radioType = "4G LTE";
            if (id != null) {
                if (id.getCi() != Integer.MAX_VALUE && id.getCi() > 0) data.cellId = String.valueOf(id.getCi());
                if (id.getTac() != Integer.MAX_VALUE && id.getTac() > 0) data.lacTac = String.valueOf(id.getTac());
                if (id.getPci() != Integer.MAX_VALUE && id.getPci() >= 0) data.pciPsc = String.valueOf(id.getPci());
                if (id.getEarfcn() != Integer.MAX_VALUE && id.getEarfcn() > 0) data.channel = String.valueOf(id.getEarfcn());
                if (id.getMccString() != null && id.getMncString() != null) data.plmn = id.getMccString() + id.getMncString();
            }
            CellSignalStrength sig = lte.getCellSignalStrength();
            if (sig != null) data.signalDbm = sig.getDbm();
        } else if (info instanceof CellInfoNr) {
            CellInfoNr nr = (CellInfoNr) info;
            data.radioType = "5G NR";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && nr.getCellIdentity() instanceof CellIdentityNr) {
                CellIdentityNr id = (CellIdentityNr) nr.getCellIdentity();
                if (id.getNci() != Long.MAX_VALUE && id.getNci() > 0) data.cellId = String.valueOf(id.getNci());
                if (id.getTac() != Integer.MAX_VALUE && id.getTac() > 0) data.lacTac = String.valueOf(id.getTac());
                if (id.getPci() != Integer.MAX_VALUE && id.getPci() >= 0) data.pciPsc = String.valueOf(id.getPci());
                if (id.getNrarfcn() != Integer.MAX_VALUE && id.getNrarfcn() > 0) data.channel = String.valueOf(id.getNrarfcn());
                if (id.getMccString() != null && id.getMncString() != null) data.plmn = id.getMccString() + id.getMncString();
            }
            CellSignalStrength sig = nr.getCellSignalStrength();
            if (sig != null) data.signalDbm = sig.getDbm();
        } else if (info instanceof CellInfoGsm) {
            CellInfoGsm gsm = (CellInfoGsm) info;
            CellIdentityGsm id = gsm.getCellIdentity();
            data.radioType = "2G GSM";
            if (id != null) {
                if (id.getCid() != Integer.MAX_VALUE && id.getCid() > 0) data.cellId = String.valueOf(id.getCid());
                if (id.getLac() != Integer.MAX_VALUE && id.getLac() >= 0) data.lacTac = String.valueOf(id.getLac());
                if (id.getPsc() != Integer.MAX_VALUE && id.getPsc() >= 0) data.pciPsc = String.valueOf(id.getPsc());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && id.getArfcn() != Integer.MAX_VALUE && id.getArfcn() > 0) {
                    data.channel = String.valueOf(id.getArfcn());
                }
                if (id.getMccString() != null && id.getMncString() != null) data.plmn = id.getMccString() + id.getMncString();
            }
            CellSignalStrength sig = gsm.getCellSignalStrength();
            if (sig != null) data.signalDbm = sig.getDbm();
        } else if (info instanceof CellInfoWcdma) {
            CellInfoWcdma wcdma = (CellInfoWcdma) info;
            CellIdentityWcdma id = wcdma.getCellIdentity();
            data.radioType = "3G WCDMA";
            if (id != null) {
                if (id.getCid() != Integer.MAX_VALUE && id.getCid() > 0) data.cellId = String.valueOf(id.getCid());
                if (id.getLac() != Integer.MAX_VALUE && id.getLac() >= 0) data.lacTac = String.valueOf(id.getLac());
                if (id.getPsc() != Integer.MAX_VALUE && id.getPsc() >= 0) data.pciPsc = String.valueOf(id.getPsc());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && id.getUarfcn() != Integer.MAX_VALUE && id.getUarfcn() > 0) {
                    data.channel = String.valueOf(id.getUarfcn());
                }
                if (id.getMccString() != null && id.getMncString() != null) data.plmn = id.getMccString() + id.getMncString();
            }
            CellSignalStrength sig = wcdma.getCellSignalStrength();
            if (sig != null) data.signalDbm = sig.getDbm();
        } else if (info instanceof CellInfoCdma) {
            CellInfoCdma cdma = (CellInfoCdma) info;
            CellIdentityCdma id = cdma.getCellIdentity();
            data.radioType = "CDMA";
            if (id != null) {
                if (id.getBasestationId() != Integer.MAX_VALUE && id.getBasestationId() >= 0) data.cellId = String.valueOf(id.getBasestationId());
                if (id.getNetworkId() != Integer.MAX_VALUE && id.getNetworkId() >= 0) data.lacTac = String.valueOf(id.getNetworkId());
                if (id.getSystemId() != Integer.MAX_VALUE && id.getSystemId() >= 0) data.pciPsc = String.valueOf(id.getSystemId());
            }
            CellSignalStrength sig = cdma.getCellSignalStrength();
            if (sig != null) data.signalDbm = sig.getDbm();
        }

        // Sanitize signal dbm
        if (data.signalDbm == 0 || data.signalDbm == Integer.MAX_VALUE) {
            data.signalDbm = -95;
        }

        // Format operator from PLMN
        data.operator = formatOperatorName(data.operator, data.plmn);

        return data;
    }

    private String formatOperatorName(String operator, String plmn) {
        if (plmn != null && plmn.startsWith("460")) {
            if (plmn.startsWith("46000") || plmn.startsWith("46002") || plmn.startsWith("46007") || plmn.startsWith("46008")) {
                return "中国移动";
            }
            if (plmn.startsWith("46001") || plmn.startsWith("46006") || plmn.startsWith("46009")) {
                return "中国联通";
            }
            if (plmn.startsWith("46003") || plmn.startsWith("46005") || plmn.startsWith("46011")) {
                return "中国电信";
            }
            if (plmn.startsWith("46015")) {
                return "中国广电";
            }
        }
        return safeText(operator, "未知运营商");
    }

    private String safeText(String value, String def) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : def;
    }

    public static class RawCellRawData {
        public String operator = "未知运营商";
        public String plmn = "未知";
        public String radioType = "蜂窝网络";
        public String cellId = "未知";
        public String lacTac = "未知";
        public String pciPsc = "未知";
        public String channel = "未知";
        public int signalDbm = -95;
        public boolean isServing = false;
    }
}
