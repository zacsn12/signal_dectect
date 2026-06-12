package org.zacsn.signal_dectect.data.scanner;

final class ScanSourceSelection {
    enum Source {
        EXTERNAL_ADAPTER,
        ANDROID_FRAMEWORK,
        UNAVAILABLE
    }

    private final Source source;
    private final boolean externalAdapterDetected;
    private final String message;

    ScanSourceSelection(Source source, boolean externalAdapterDetected, String message) {
        this.source = source;
        this.externalAdapterDetected = externalAdapterDetected;
        this.message = message;
    }

    Source getSource() {
        return source;
    }

    boolean isExternalAdapterDetected() {
        return externalAdapterDetected;
    }

    String getMessage() {
        return message;
    }

    boolean canUseAndroidFramework() {
        return source == Source.ANDROID_FRAMEWORK;
    }
}
