package org.zacsn.signal_dectect.data.api;

public class ChangePasswordRequest {
    private final String oldPassword;
    private final String newPassword;

    public ChangePasswordRequest(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}
