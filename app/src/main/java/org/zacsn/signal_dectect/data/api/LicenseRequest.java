package org.zacsn.signal_dectect.data.api;

public class LicenseRequest {
    private String licenseKey;
    private String machineCode;

    public LicenseRequest(String licenseKey, String machineCode) {
        this.licenseKey = licenseKey;
        this.machineCode = machineCode;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
    }
}
