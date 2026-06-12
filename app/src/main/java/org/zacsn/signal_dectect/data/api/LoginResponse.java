package org.zacsn.signal_dectect.data.api;

import java.util.List;

public class LoginResponse {
    private int code;
    private String message;
    private Data data;

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public static class Data {
        private String token;
        private String userId;
        private String nickname;
        private String validUntil;
        private String machineCode;
        private String machineBoundAt;
        private int maxMachineBindings;
        private List<MachineBinding> machineBindings;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public String getValidUntil() { return validUntil; }
        public void setValidUntil(String validUntil) { this.validUntil = validUntil; }

        public String getMachineCode() { return machineCode; }
        public void setMachineCode(String machineCode) { this.machineCode = machineCode; }

        public String getMachineBoundAt() { return machineBoundAt; }
        public void setMachineBoundAt(String machineBoundAt) { this.machineBoundAt = machineBoundAt; }

        public int getMaxMachineBindings() { return maxMachineBindings; }
        public void setMaxMachineBindings(int maxMachineBindings) { this.maxMachineBindings = maxMachineBindings; }

        public List<MachineBinding> getMachineBindings() { return machineBindings; }
        public void setMachineBindings(List<MachineBinding> machineBindings) { this.machineBindings = machineBindings; }
    }

    public static class MachineBinding {
        private String machineCode;
        private String boundAt;

        public String getMachineCode() { return machineCode; }
        public void setMachineCode(String machineCode) { this.machineCode = machineCode; }

        public String getBoundAt() { return boundAt; }
        public void setBoundAt(String boundAt) { this.boundAt = boundAt; }
    }
}
