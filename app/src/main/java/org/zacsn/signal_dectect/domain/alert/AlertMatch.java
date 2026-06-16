package org.zacsn.signal_dectect.domain.alert;

public class AlertMatch {
    private final String alertType;
    private final String alertReason;
    private final String normalizedMac;

    public AlertMatch(String alertType, String alertReason, String normalizedMac) {
        this.alertType = alertType;
        this.alertReason = alertReason;
        this.normalizedMac = normalizedMac;
    }

    public String getAlertType() {
        return alertType;
    }

    public String getAlertReason() {
        return alertReason;
    }

    public String getNormalizedMac() {
        return normalizedMac;
    }

    public String getDedupKey() {
        return alertType + ":" + normalizedMac;
    }
}
