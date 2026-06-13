package org.zacsn.signal_dectect.data.scanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityCdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoCdma;
import android.telephony.CellSignalStrength;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import androidx.core.app.ActivityCompat;

import org.zacsn.signal_dectect.domain.model.DeviceType;
import org.zacsn.signal_dectect.domain.model.ManufacturerVerdict;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Controller for cellular network signal monitoring.
 */
@Singleton
public class CellularScanController {
    
    private final Context context;
    private final TelephonyManager telephonyManager;
    
    private PhoneStateListener phoneStateListener;
    private TelephonyCallback telephonyCallback;
    private boolean isScanning = false;
    private ScanListener scanListener;
    private final java.util.Map<String, SignalDevice> cellularDevices = new java.util.HashMap<>();
    
    public interface ScanListener {
        void onSignalUpdate(SignalDevice device);
        void onScanError(String error);
    }
    
    @Inject
    public CellularScanController(@ApplicationContext Context context) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }
    
    public void setScanListener(ScanListener listener) {
        this.scanListener = listener;
    }

    
    /**
     * Start cellular signal monitoring.
     */
    public void startScan() {
        if (telephonyManager == null) {
            if (scanListener != null) {
                scanListener.onScanError("Telephony service not available");
            }
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
            if (scanListener != null) {
                scanListener.onScanError("Phone state permission not granted");
            }
            return;
        }
        
        isScanning = true;
        cellularDevices.clear();
        setupSignalListener();
        publishLatestCellInfo(null);
    }
    
    private void setupSignalListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = new SignalStrengthCallback(this);
            try {
                telephonyManager.registerTelephonyCallback(context.getMainExecutor(), telephonyCallback);
            } catch (SecurityException e) {
                if (scanListener != null) {
                    scanListener.onScanError("Cellular callback permission error: " + e.getMessage());
                }
            }
            return;
        }

        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                publishSignal(signalStrength);
            }
        };
        
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    
    /**
     * Stop cellular signal monitoring.
     */
    public void stopScan() {
        if (!isScanning) return;
        
        isScanning = false;
        
        if (phoneStateListener != null && telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            phoneStateListener = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && telephonyCallback != null
                && telephonyManager != null) {
            telephonyManager.unregisterTelephonyCallback(telephonyCallback);
            telephonyCallback = null;
        }
    }

    private void publishSignal(SignalStrength signalStrength) {
        if (!isScanning || signalStrength == null) {
            return;
        }
        publishLatestCellInfo(signalStrength);
    }

    private void publishLatestCellInfo(SignalStrength signalStrength) {
        if (!isScanning) {
            return;
        }

        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                java.util.List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();
                if (cellInfos != null && !cellInfos.isEmpty()) {
                    publishCellInfoDevices(cellInfos, signalStrength);
                    return;
                }
            }
        } catch (SecurityException e) {
            emitError("读取小区信息权限不足: " + e.getMessage());
        } catch (Exception e) {
            emitError("读取小区信息失败: " + e.getMessage());
        }

        publishFallbackOperatorDevice(signalStrength);
    }

    private void publishCellInfoDevices(java.util.List<CellInfo> cellInfos, SignalStrength signalStrength) {
        String operatorName = safeTelephonyText(() -> telephonyManager.getNetworkOperatorName(), "未知运营商");
        String networkOperator = safeTelephonyText(() -> telephonyManager.getNetworkOperator(), "UNKNOWN");
        String dataNetworkType = safeNetworkType();
        long now = System.currentTimeMillis();
        boolean publishedRegistered = false;

        for (CellInfo cellInfo : cellInfos) {
            CellularIdentity identity = parseCellInfo(cellInfo);
            if (identity == null || !identity.hasUsableIdentity()) {
                continue;
            }

            boolean registered = cellInfo.isRegistered();
            int rawDbm = identity.dbm;
            if (!isValidDbm(rawDbm) && signalStrength != null) {
                rawDbm = getSignalStrengthDbm(signalStrength);
            }
            String deviceKey = identity.buildKey(networkOperator);
            SignalDevice previous = cellularDevices.get(deviceKey);
            int dbm = SignalDeviceStabilizer.smoothSignalStrength(previous, rawDbm);
            String evidence = "运营商=" + operatorName
                    + ", PLMN=" + firstUseful(identity.plmn, networkOperator)
                    + ", 小区=" + identity.identitySummary()
                    + ", 制式=" + identity.radioType
                    + ", 当前服务小区=" + (registered ? "是" : "否")
                    + ", 原始信号=" + rawDbm + " dBm";

            SignalDevice device = new SignalDevice(
                    deviceKey,
                    (registered ? "当前服务小区 " : "邻区 ") + identity.radioType,
                    DeviceType.CELLULAR,
                    "未确认",
                    operatorName,
                    "cellular_cell_identity",
                    registered ? 80 : 65,
                    registered ? ManufacturerVerdict.LIKELY : ManufacturerVerdict.POSSIBLE,
                    evidence,
                    dbm,
                    null,
                    0.0,
                    previous != null ? previous.getFirstSeen() : now,
                    now,
                    false,
                    false,
                    false
            );
            cellularDevices.put(deviceKey, device);

            if (scanListener != null) {
                scanListener.onSignalUpdate(device);
            }
            publishedRegistered = publishedRegistered || registered;
        }

        if (!publishedRegistered && signalStrength != null) {
            publishFallbackOperatorDevice(signalStrength);
        }
    }

    private void publishFallbackOperatorDevice(SignalStrength signalStrength) {
        if (!isScanning) {
            return;
        }
        int rawDbm = getSignalStrengthDbm(signalStrength);
        String operatorName = safeTelephonyText(() -> telephonyManager.getNetworkOperatorName(), "未知运营商");
        String networkOperator = safeTelephonyText(() -> telephonyManager.getNetworkOperator(), "UNKNOWN");
        String networkType = safeNetworkType();
        String deviceKey = "CELLULAR_OPERATOR_" + networkOperator + "_" + networkType;
        SignalDevice previous = cellularDevices.get(deviceKey);
        int dbm = SignalDeviceStabilizer.smoothSignalStrength(previous, rawDbm);
        int level = signalStrength != null ? signalStrength.getLevel() : -1;
        long now = System.currentTimeMillis();
        String evidence = "运营商=" + operatorName
                + ", PLMN=" + networkOperator
                + ", 网络制式=" + networkType
                + ", 未获取到小区身份信息"
                + ", 系统等级=" + level + "/4"
                + ", 原始信号=" + rawDbm + " dBm";

        SignalDevice device = new SignalDevice(
                deviceKey,
                "蜂窝网络 " + networkType,
                DeviceType.CELLULAR,
                "未确认",
                operatorName,
                "cellular_operator",
                operatorName.equals("未知运营商") ? 0 : 55,
                operatorName.equals("未知运营商") ? ManufacturerVerdict.UNKNOWN : ManufacturerVerdict.POSSIBLE,
                evidence,
                dbm,
                null,
                0.0,
                previous != null ? previous.getFirstSeen() : now,
                now,
                false,
                false,
                false
        );
        cellularDevices.put(deviceKey, device);

        if (scanListener != null) {
            scanListener.onSignalUpdate(device);
        }
    }
    
    /**
     * Get signal strength in dBm from SignalStrength object.
     */
    private int getSignalStrengthDbm(SignalStrength signalStrength) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+
            java.util.List<CellSignalStrength> cellSignalStrengths = 
                signalStrength.getCellSignalStrengths();
            if (!cellSignalStrengths.isEmpty()) {
                int bestDbm = Integer.MIN_VALUE;
                for (CellSignalStrength cellSignalStrength : cellSignalStrengths) {
                    int dbm = cellSignalStrength.getDbm();
                    if (dbm > -150 && dbm < 0 && dbm > bestDbm) {
                        bestDbm = dbm;
                    }
                }
                if (bestDbm != Integer.MIN_VALUE) {
                    return bestDbm;
                }
            }
        }
        
        // Fallback for older versions
        // Approximate conversion: level 0-4 maps to -113 to -51 dBm
        int level = signalStrength != null ? signalStrength.getLevel() : 0;
        return -113 + (level * 28);
    }

    private CellularIdentity parseCellInfo(CellInfo cellInfo) {
        if (cellInfo instanceof CellInfoLte) {
            CellInfoLte lte = (CellInfoLte) cellInfo;
            CellIdentityLte identity = lte.getCellIdentity();
            return new CellularIdentity(
                    "4G LTE",
                    getPlmn(identity),
                    valueOrUnknown(identity.getTac()),
                    valueOrUnknown(identity.getCi()),
                    valueOrUnknown(identity.getPci()),
                    getLteEarfcn(identity),
                    lte.getCellSignalStrength().getDbm()
            );
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo instanceof CellInfoNr) {
            CellInfoNr nr = (CellInfoNr) cellInfo;
            CellIdentityNr identity = (CellIdentityNr) nr.getCellIdentity();
            return new CellularIdentity(
                    "5G NR",
                    getPlmn(identity),
                    valueOrUnknown(identity.getTac()),
                    valueOrUnknown(identity.getNci()),
                    valueOrUnknown(identity.getPci()),
                    valueOrUnknown(identity.getNrarfcn()),
                    nr.getCellSignalStrength().getDbm()
            );
        }
        if (cellInfo instanceof CellInfoWcdma) {
            CellInfoWcdma wcdma = (CellInfoWcdma) cellInfo;
            CellIdentityWcdma identity = wcdma.getCellIdentity();
            return new CellularIdentity(
                    "3G WCDMA",
                    getPlmn(identity),
                    valueOrUnknown(identity.getLac()),
                    valueOrUnknown(identity.getCid()),
                    valueOrUnknown(identity.getPsc()),
                    valueOrUnknown(identity.getUarfcn()),
                    wcdma.getCellSignalStrength().getDbm()
            );
        }
        if (cellInfo instanceof CellInfoGsm) {
            CellInfoGsm gsm = (CellInfoGsm) cellInfo;
            CellIdentityGsm identity = gsm.getCellIdentity();
            return new CellularIdentity(
                    "2G GSM",
                    getPlmn(identity),
                    valueOrUnknown(identity.getLac()),
                    valueOrUnknown(identity.getCid()),
                    valueOrUnknown(identity.getPsc()),
                    valueOrUnknown(identity.getArfcn()),
                    gsm.getCellSignalStrength().getDbm()
            );
        }
        if (cellInfo instanceof CellInfoCdma) {
            CellInfoCdma cdma = (CellInfoCdma) cellInfo;
            CellIdentityCdma identity = cdma.getCellIdentity();
            return new CellularIdentity(
                    "CDMA",
                    "",
                    valueOrUnknown(identity.getNetworkId()),
                    valueOrUnknown(identity.getBasestationId()),
                    valueOrUnknown(identity.getSystemId()),
                    "unknown",
                    cdma.getCellSignalStrength().getDbm()
            );
        }
        return null;
    }

    private String getPlmn(CellIdentityLte identity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && identity != null) {
            return combinePlmn(identity.getMccString(), identity.getMncString());
        }
        return "";
    }

    private String getPlmn(CellIdentityNr identity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && identity != null) {
            return combinePlmn(identity.getMccString(), identity.getMncString());
        }
        return "";
    }

    private String getPlmn(CellIdentityWcdma identity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && identity != null) {
            return combinePlmn(identity.getMccString(), identity.getMncString());
        }
        return "";
    }

    private String getPlmn(CellIdentityGsm identity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && identity != null) {
            return combinePlmn(identity.getMccString(), identity.getMncString());
        }
        return "";
    }

    private String combinePlmn(String mcc, String mnc) {
        if (mcc != null && mnc != null) {
            return mcc + mnc;
        }
        return "";
    }

    private String getLteEarfcn(CellIdentityLte identity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return valueOrUnknown(identity.getEarfcn());
        }
        return "unknown";
    }

    private String getNetworkTypeLabel(int type) {
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G NR";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G LTE";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "2G";
            default:
                return "蜂窝网络";
        }
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String safeNetworkType() {
        try {
            return getNetworkTypeLabel(telephonyManager.getDataNetworkType());
        } catch (SecurityException e) {
            emitError("读取网络制式权限不足: " + e.getMessage());
            return "蜂窝网络";
        } catch (Exception e) {
            return "蜂窝网络";
        }
    }

    private String safeTelephonyText(TelephonyTextProvider provider, String fallback) {
        try {
            return safeText(provider.get(), fallback);
        } catch (SecurityException e) {
            emitError("读取蜂窝信息权限不足: " + e.getMessage());
            return fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private String valueOrUnknown(int value) {
        return value == Integer.MAX_VALUE || value < 0 ? "unknown" : String.valueOf(value);
    }

    private String valueOrUnknown(long value) {
        return value == Long.MAX_VALUE || value < 0 ? "unknown" : String.valueOf(value);
    }

    private boolean isValidDbm(int dbm) {
        return dbm > -150 && dbm < 0;
    }

    private String firstUseful(String first, String fallback) {
        return first != null && !first.trim().isEmpty() ? first.trim() : fallback;
    }

    private void emitError(String message) {
        if (scanListener != null && message != null && !message.trim().isEmpty()) {
            scanListener.onScanError(message);
        }
    }
    
    public boolean isScanning() {
        return isScanning;
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.S)
    private static final class SignalStrengthCallback extends TelephonyCallback
            implements TelephonyCallback.SignalStrengthsListener {
        private final CellularScanController controller;

        private SignalStrengthCallback(CellularScanController controller) {
            this.controller = controller;
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            controller.publishSignal(signalStrength);
        }
    }

    private interface TelephonyTextProvider {
        String get();
    }

    private static final class CellularIdentity {
        private final String radioType;
        private final String plmn;
        private final String areaCode;
        private final String cellId;
        private final String physicalId;
        private final String channel;
        private final int dbm;

        private CellularIdentity(
                String radioType,
                String plmn,
                String areaCode,
                String cellId,
                String physicalId,
                String channel,
                int dbm
        ) {
            this.radioType = radioType;
            this.plmn = plmn;
            this.areaCode = areaCode;
            this.cellId = cellId;
            this.physicalId = physicalId;
            this.channel = channel;
            this.dbm = dbm;
        }

        private boolean hasUsableIdentity() {
            return isUseful(areaCode) || isUseful(cellId) || isUseful(physicalId);
        }

        private String buildKey(String fallbackPlmn) {
            return "CELLULAR_CELL_"
                    + normalize(firstUseful(plmn, fallbackPlmn)) + "_"
                    + normalize(radioType) + "_"
                    + normalize(areaCode) + "_"
                    + normalize(cellId) + "_"
                    + normalize(physicalId);
        }

        private String identitySummary() {
            return "区域=" + areaCode
                    + ", 小区ID=" + cellId
                    + ", PCI/PSC=" + physicalId
                    + ", 频点=" + channel;
        }

        private static boolean isUseful(String value) {
            return value != null && !value.trim().isEmpty() && !"unknown".equalsIgnoreCase(value.trim());
        }

        private static String firstUseful(String value, String fallback) {
            return isUseful(value) ? value.trim() : fallback;
        }

        private static String normalize(String value) {
            if (value == null || value.trim().isEmpty()) {
                return "UNKNOWN";
            }
            return value.trim().replaceAll("[^A-Za-z0-9]+", "_");
        }
    }
}
