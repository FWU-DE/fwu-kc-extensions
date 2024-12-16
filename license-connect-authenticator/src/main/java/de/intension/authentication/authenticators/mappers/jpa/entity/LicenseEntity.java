package de.intension.authentication.authenticators.mappers.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "LICENSE")
@NamedQueries({
    @NamedQuery(name = LicenseEntity.GET_LICENCE_BY_HMAC_ID,
                query = "SELECT w.content FROM MappingEntity w WHERE w.hmacId = :hmacId")
})
public class LicenseEntity {
    public static final String GET_LICENCE_BY_HMAC_ID = "getLicenceByHmacId";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "HMAC_ID", nullable = false)
    private String hmacId;

    @Column(name = "CONTENT", nullable = false)
    private String content;

    public LicenseEntity(String hmacId, String licence) {
        this.hmacId = hmacId;
        this.content = licence;
    }

    public LicenseEntity() {
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setHmacId(String hmacId) {
        this.hmacId = hmacId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getId() {
        return id;
    }

    public String getHmacId() {
        return hmacId;
    }

    public String getContent() {
        return content;
    }
}
