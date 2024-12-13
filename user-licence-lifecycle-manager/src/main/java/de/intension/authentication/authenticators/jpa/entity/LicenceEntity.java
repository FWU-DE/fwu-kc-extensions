package de.intension.authentication.authenticators.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "LICENCE")
@NamedQueries({
        @NamedQuery(name = LicenceEntity.GET_LICENCE_BY_HMAC_ID,
                query = "SELECT w.content FROM LicenceEntity w WHERE w.hmacId = :hmacId"),
        @NamedQuery(
                name = LicenceEntity.REMOVE_LICENCE_BY_HMAC_ID,
                query = "DELETE FROM LicenceEntity w WHERE w.hmacId = :hmacId")
})
public class LicenceEntity {
    public static final String GET_LICENCE_BY_HMAC_ID = "getLicenceByHmacId";
    public static final String REMOVE_LICENCE_BY_HMAC_ID = "removeLicenceByHmacId";

    @Id
    @Column(name = "HMAC_ID", nullable = false)
    private String hmacId;

    @Column(name = "CONTENT", nullable = false)
    private String content;

    public LicenceEntity(String hmacId, String licence) {
        this.hmacId = hmacId;
        this.content = licence;
    }

    public LicenceEntity() {
    }

    public void setHmacId(String hmacId) {
        this.hmacId = hmacId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHmacId() {
        return hmacId;
    }

    public String getContent() {
        return content;
    }
}
