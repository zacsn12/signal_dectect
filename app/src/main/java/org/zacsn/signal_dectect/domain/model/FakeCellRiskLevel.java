package org.zacsn.signal_dectect.domain.model;

/**
 * Risk evaluation levels for scanned cellular base stations.
 */
public enum FakeCellRiskLevel {
    /**
     * Normal, compliant carrier base station (Risk score: 0 - 25).
     */
    SAFE,

    /**
     * Mild anomaly detected, requires attention (Risk score: 26 - 55).
     */
    SUSPICIOUS,

    /**
     * High confidence rogue / fake base station threat (Risk score: > 55).
     */
    DANGEROUS
}
