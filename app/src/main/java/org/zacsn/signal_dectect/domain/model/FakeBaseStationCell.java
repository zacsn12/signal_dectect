package org.zacsn.signal_dectect.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Model representing a cellular cell analyzed for fake base station threats.
 */
public class FakeBaseStationCell {
    private final String id;
    private final String operator;
    private final String plmn;
    private final String radioType;
    private final String cellId;
    private final String lacTac;
    private final String pciPsc;
    private final String channel;
    private final int signalDbm;
    private final boolean isServing;
    private final int riskScore;
    private final FakeCellRiskLevel riskLevel;
    private final List<String> anomalyReasons;
    private final long timestamp;

    public FakeBaseStationCell(
            String id,
            String operator,
            String plmn,
            String radioType,
            String cellId,
            String lacTac,
            String pciPsc,
            String channel,
            int signalDbm,
            boolean isServing,
            int riskScore,
            FakeCellRiskLevel riskLevel,
            List<String> anomalyReasons,
            long timestamp
    ) {
        this.id = id != null ? id : (operator + "_" + radioType + "_" + cellId + "_" + lacTac);
        this.operator = operator != null ? operator : "未知运营商";
        this.plmn = plmn != null ? plmn : "未知";
        this.radioType = radioType != null ? radioType : "蜂窝网络";
        this.cellId = cellId != null ? cellId : "未知";
        this.lacTac = lacTac != null ? lacTac : "未知";
        this.pciPsc = pciPsc != null ? pciPsc : "未知";
        this.channel = channel != null ? channel : "未知";
        this.signalDbm = signalDbm;
        this.isServing = isServing;
        this.riskScore = Math.max(0, Math.min(100, riskScore));
        this.riskLevel = riskLevel != null ? riskLevel : FakeCellRiskLevel.SAFE;
        this.anomalyReasons = anomalyReasons != null ? new ArrayList<>(anomalyReasons) : Collections.emptyList();
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getOperator() {
        return operator;
    }

    public String getPlmn() {
        return plmn;
    }

    public String getRadioType() {
        return radioType;
    }

    public String getCellId() {
        return cellId;
    }

    public String getLacTac() {
        return lacTac;
    }

    public String getPciPsc() {
        return pciPsc;
    }

    public String getChannel() {
        return channel;
    }

    public int getSignalDbm() {
        return signalDbm;
    }

    public boolean isServing() {
        return isServing;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public FakeCellRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public List<String> getAnomalyReasons() {
        return Collections.unmodifiableList(anomalyReasons);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isThreat() {
        return riskLevel == FakeCellRiskLevel.DANGEROUS;
    }

    public boolean isSuspicious() {
        return riskLevel == FakeCellRiskLevel.SUSPICIOUS || riskLevel == FakeCellRiskLevel.DANGEROUS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FakeBaseStationCell)) return false;
        FakeBaseStationCell that = (FakeBaseStationCell) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
