package org.zacsn.signal_dectect.data.api;

public class LoginRequest {
    private String username;
    private String password;
    private String machineCode;

    public LoginRequest(String username, String password) {
        this(username, password, "");
    }

    public LoginRequest(String username, String password, String machineCode) {
        this.username = username;
        this.password = password;
        this.machineCode = machineCode;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMachineCode() { return machineCode; }
    public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
}
