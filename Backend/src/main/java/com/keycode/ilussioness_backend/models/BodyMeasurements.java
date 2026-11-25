package com.keycode.ilussioness_backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Body measurements (cm) captured as free text for a custom-made dress. */
@Embeddable
public class BodyMeasurements {

    public static final int MAX_LENGTH = 20;

    @Column(name = "measure_shoulder", length = MAX_LENGTH)
    private String shoulder;

    @Column(name = "measure_bust", length = MAX_LENGTH)
    private String bust;

    @Column(name = "measure_hip", length = MAX_LENGTH)
    private String hip;

    @Column(name = "measure_waist", length = MAX_LENGTH)
    private String waist;

    @Column(name = "measure_skirt_length", length = MAX_LENGTH)
    private String skirtLength;

    @Column(name = "measure_blouse_length", length = MAX_LENGTH)
    private String blouseLength;

    @Column(name = "measure_neckline", length = MAX_LENGTH)
    private String neckline;

    public BodyMeasurements() {
    }

    public String[] values() {
        return new String[] {shoulder, bust, hip, waist, skirtLength, blouseLength, neckline};
    }

    public String getShoulder() { return shoulder; }
    public void setShoulder(String shoulder) { this.shoulder = shoulder; }

    public String getBust() { return bust; }
    public void setBust(String bust) { this.bust = bust; }

    public String getHip() { return hip; }
    public void setHip(String hip) { this.hip = hip; }

    public String getWaist() { return waist; }
    public void setWaist(String waist) { this.waist = waist; }

    public String getSkirtLength() { return skirtLength; }
    public void setSkirtLength(String skirtLength) { this.skirtLength = skirtLength; }

    public String getBlouseLength() { return blouseLength; }
    public void setBlouseLength(String blouseLength) { this.blouseLength = blouseLength; }

    public String getNeckline() { return neckline; }
    public void setNeckline(String neckline) { this.neckline = neckline; }
}
