package org.zacsn.signal_dectect.domain.model;

import com.google.gson.annotations.SerializedName;

public enum ManufacturerVerdict {
    @SerializedName("CONFIRMED")
    CONFIRMED,
    @SerializedName("LIKELY")
    LIKELY,
    @SerializedName("POSSIBLE")
    POSSIBLE,
    @SerializedName("UNKNOWN")
    UNKNOWN
}
