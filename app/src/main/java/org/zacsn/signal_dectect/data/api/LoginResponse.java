package org.zacsn.signal_dectect.data.api;

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

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }

        public String getValidUntil() { return validUntil; }
        public void setValidUntil(String validUntil) { this.validUntil = validUntil; }
    }
}
