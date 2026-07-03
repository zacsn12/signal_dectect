package org.zacsn.signal_dectect.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.zacsn.signal_dectect.data.api.LicenseResponse;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LicenseManager {
    private static final String PREF_NAME = "SignalDetectLicense";
    private static final String KEY_LICENSE_KEY = "license_key";
    private static final String KEY_PAYLOAD_JSON = "payload_json";
    private static final String KEY_SIGNATURE = "signature";
    private static final String KEY_LAST_ACTIVATED_AT = "last_activated_at";
    private static final String PRODUCT_CODE = "signal_detect";
    private static final String PUBLIC_KEY_PEM =
            "-----BEGIN PUBLIC KEY-----\n"
                    + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs0xl0HmGsQXxPja5QLe1\n"
                    + "XycGggLFBB4QjodrQzwurEO/LvlWWuP+nn1zai/2nUBk4sb0FsYy3PbhXyyTkeug\n"
                    + "J5o4xm0FCD/If5VpWF6B2USKd3ieE2+Lvx9gT81WUn5O8u3kqVRmz+NITUPvATem\n"
                    + "Drt9SnpA37aGFHrbX7MXRA3JPYTJ7qdq+a/0WIKuj1xh+/pSGaj2IJgzGrdsHSgx\n"
                    + "JN0RstQPa/9nMRSZYmBii8ptvdcfnsqHqyXlKS5OgZbfhIN6+/sYNn95UtB2oRWj\n"
                    + "1opH6nPc7oaYbEnp44dNrBAz1smQK5/tQARUJDun8OC/IBPrWveH3KnIcNf8FZYA\n"
                    + "jQIDAQAB\n"
                    + "-----END PUBLIC KEY-----";

    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public LicenseManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean saveLicense(LicenseResponse.Data data) {
        if (data == null || data.getSignature() == null) {
            return false;
        }
        String payloadJson = data.getPayloadJson();
        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            if (data.getPayload() == null) {
                return false;
            }
            payloadJson = gson.toJson(data.getPayload());
        }
        if (!verifyPayload(payloadJson, data.getSignature())) {
            return false;
        }
        prefs.edit()
                .putString(KEY_LICENSE_KEY, data.getLicenseKey())
                .putString(KEY_PAYLOAD_JSON, payloadJson)
                .putString(KEY_SIGNATURE, data.getSignature())
                .putLong(KEY_LAST_ACTIVATED_AT, System.currentTimeMillis())
                .apply();
        return true;
    }

    public boolean hasValidLicense() {
        return getValidationError().isEmpty();
    }

    public String getValidationError() {
        String payloadJson = getPayloadJson();
        String signature = prefs.getString(KEY_SIGNATURE, "");
        if (payloadJson.isEmpty() || signature.isEmpty()) {
            return "未激活许可证";
        }
        if (!verifyPayload(payloadJson, signature)) {
            return "许可证签名无效";
        }

        JsonObject payload = parseJsonObject(payloadJson);
        if (!PRODUCT_CODE.equals(getString(payload, "productCode"))) {
            return "许可证产品不匹配";
        }
        String currentMachineCode = getMachineCode();
        if (!currentMachineCode.equalsIgnoreCase(getString(payload, "machineCode"))) {
            return "许可证不属于当前设备";
        }
        String validUntil = getString(payload, "validUntil");
        if (isExpired(validUntil)) {
            return "许可证已过期";
        }
        return "";
    }

    public String getLicenseKey() {
        String key = prefs.getString(KEY_LICENSE_KEY, "");
        if (key != null && !key.trim().isEmpty()) {
            return key;
        }
        return getPayloadValue("licenseKey", "");
    }

    public String getCustomerName() {
        return getPayloadValue("customerName", "授权客户");
    }

    public String getValidUntil() {
        return getPayloadValue("validUntil", "");
    }

    public String getMachineCode() {
        return MachineCodeUtils.getMachineCode(context);
    }

    public String getPayloadJson() {
        return prefs.getString(KEY_PAYLOAD_JSON, "");
    }

    public String getSignature() {
        return prefs.getString(KEY_SIGNATURE, "");
    }

    public long getLastActivatedAt() {
        return prefs.getLong(KEY_LAST_ACTIVATED_AT, 0L);
    }

    public String getFeaturesText() {
        try {
            JsonObject payload = parseJsonObject(getPayloadJson());
            if (!payload.has("features") || !payload.get("features").isJsonArray()) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < payload.getAsJsonArray("features").size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(payload.getAsJsonArray("features").get(i).getAsString());
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public void clearLicense() {
        prefs.edit().clear().apply();
    }

    private String getPayloadValue(String key, String fallback) {
        try {
            JsonObject payload = parseJsonObject(getPayloadJson());
            return getString(payload, key, fallback);
        } catch (Exception e) {
            return fallback;
        }
    }

    private boolean verifyPayload(String payloadJson, String signatureText) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(loadPublicKey());
            verifier.update(payloadJson.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = android.util.Base64.decode(signatureText, android.util.Base64.DEFAULT);
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    private JsonObject parseJsonObject(String json) {
        return new JsonParser().parse(json).getAsJsonObject();
    }

    private PublicKey loadPublicKey() throws Exception {
        String key = PUBLIC_KEY_PEM
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decoded = android.util.Base64.decode(key, android.util.Base64.DEFAULT);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private boolean isExpired(String validUntil) {
        if (validUntil == null || !validUntil.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            return true;
        }
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        return validUntil.compareTo(today) < 0;
    }

    private String getString(JsonObject object, String key) {
        return getString(object, key, "");
    }

    private String getString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        return object.get(key).getAsString();
    }
}
