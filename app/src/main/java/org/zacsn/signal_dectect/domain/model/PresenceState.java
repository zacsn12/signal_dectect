package org.zacsn.signal_dectect.domain.model;

import com.google.gson.annotations.SerializedName;

/** Runtime presence state of a device in the current scan session. */
public enum PresenceState {
    @SerializedName("ACTIVE")
    ACTIVE,
    @SerializedName("MISSING")
    MISSING,
    @SerializedName("STALE")
    STALE
}
