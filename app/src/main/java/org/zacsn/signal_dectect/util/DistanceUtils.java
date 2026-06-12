package org.zacsn.signal_dectect.util;

import java.util.Locale;

public final class DistanceUtils {
    public static final double UNKNOWN_DISTANCE = -1.0;
    public static final double MAX_RELIABLE_DISTANCE_METERS = 100.0;

    private DistanceUtils() {
    }

    public static double sanitize(double distanceMeters) {
        if (Double.isNaN(distanceMeters) || Double.isInfinite(distanceMeters)
                || distanceMeters < 0.0 || distanceMeters > MAX_RELIABLE_DISTANCE_METERS) {
            return UNKNOWN_DISTANCE;
        }
        return distanceMeters;
    }

    public static boolean isReliable(double distanceMeters) {
        return distanceMeters >= 0.0
                && distanceMeters <= MAX_RELIABLE_DISTANCE_METERS
                && !Double.isNaN(distanceMeters)
                && !Double.isInfinite(distanceMeters);
    }

    public static String formatMeters(double distanceMeters) {
        if (!isReliable(distanceMeters)) {
            return "超出估算范围";
        }

        if (distanceMeters < 1.0) {
            return String.format(Locale.getDefault(), "%.1f m", distanceMeters);
        }
        return String.format(Locale.getDefault(), "%.1f m", distanceMeters);
    }

    public static String formatMetersChinese(double distanceMeters) {
        if (!isReliable(distanceMeters)) {
            return "超出估算范围";
        }
        return String.format(Locale.getDefault(), "~%.1f米", distanceMeters);
    }
}
