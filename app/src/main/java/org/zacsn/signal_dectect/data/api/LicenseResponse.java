package org.zacsn.signal_dectect.data.api;

import java.util.List;

public class LicenseResponse {
    private int code;
    private String message;
    private Data data;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Data getData() {
        return data;
    }

    public static class Data {
        private String licenseKey;
        private String customerName;
        private String validUntil;
        private String machineCode;
        private int maxActivations;
        private List<MachineBinding> machineBindings;
        private List<String> features;
        private Payload payload;
        private String payloadJson;
        private String signature;

        public String getLicenseKey() { return licenseKey; }
        public String getCustomerName() { return customerName; }
        public String getValidUntil() { return validUntil; }
        public String getMachineCode() { return machineCode; }
        public int getMaxActivations() { return maxActivations; }
        public List<MachineBinding> getMachineBindings() { return machineBindings; }
        public List<String> getFeatures() { return features; }
        public Payload getPayload() { return payload; }
        public String getPayloadJson() { return payloadJson; }
        public String getSignature() { return signature; }
    }

    public static class Payload {
        private String licenseKey;
        private String customerName;
        private String productCode;
        private String edition;
        private String machineCode;
        private String validUntil;
        private String issuedAt;
        private List<String> features;

        public String getLicenseKey() { return licenseKey; }
        public String getCustomerName() { return customerName; }
        public String getProductCode() { return productCode; }
        public String getEdition() { return edition; }
        public String getMachineCode() { return machineCode; }
        public String getValidUntil() { return validUntil; }
        public String getIssuedAt() { return issuedAt; }
        public List<String> getFeatures() { return features; }
    }

    public static class MachineBinding {
        private String machineCode;
        private String boundAt;

        public String getMachineCode() { return machineCode; }
        public String getBoundAt() { return boundAt; }
    }
}
