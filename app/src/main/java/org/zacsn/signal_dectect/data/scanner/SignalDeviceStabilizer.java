package org.zacsn.signal_dectect.data.scanner;

import org.zacsn.signal_dectect.domain.model.SignalDevice;

final class SignalDeviceStabilizer {
    private static final double NEW_SAMPLE_WEIGHT = 0.35;

    private SignalDeviceStabilizer() {
    }

    static int smoothSignalStrength(SignalDevice previous, int latestSignalStrength) {
        if (previous == null) {
            return latestSignalStrength;
        }
        return (int) Math.round(
            previous.getSignalStrength() * (1.0 - NEW_SAMPLE_WEIGHT)
                + latestSignalStrength * NEW_SAMPLE_WEIGHT
        );
    }

    static SignalDevice merge(SignalDevice previous, SignalDevice latest) {
        if (previous == null) {
            return latest;
        }

        SignalDevice identitySource = latest.getManufacturerConfidence() >= previous.getManufacturerConfidence()
            ? latest
            : previous;

        return new SignalDevice(
            latest.getMacAddress(),
            betterText(latest.getDeviceName(), previous.getDeviceName()),
            latest.getDeviceType(),
            identitySource.getManufacturer(),
            betterText(latest.getCandidateManufacturer(), previous.getCandidateManufacturer()),
            identitySource.getManufacturerSource(),
            identitySource.getManufacturerConfidence(),
            identitySource.getManufacturerVerdict(),
            identitySource.getManufacturerEvidence(),
            latest.getSignalStrength(),
            latest.getFrequency(),
            latest.getDistance(),
            previous.getFirstSeen(),
            latest.getLastSeen(),
            previous.isFocused(),
            previous.isBlacklisted(),
            previous.isWhitelisted()
        );
    }

    private static String betterText(String latest, String previous) {
        if (isUseful(latest)) {
            return latest;
        }
        return previous;
    }

    private static boolean isUseful(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String normalized = value.trim().toLowerCase();
        return !normalized.equals("unknown")
            && !normalized.equals("unknown device")
            && !normalized.equals("unknown network")
            && !normalized.equals("hidden network")
            && !normalized.equals("未知")
            && !normalized.equals("未知厂商")
            && !normalized.equals("未确认")
            && !normalized.equals("随机地址");
    }
}
