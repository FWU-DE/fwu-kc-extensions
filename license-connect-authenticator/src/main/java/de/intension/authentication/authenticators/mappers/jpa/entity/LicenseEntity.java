package de.intension.authentication.authenticators.mappers.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "HMAC_PSEUDONYM_LICENCE")
@NamedQueries({
    @NamedQuery(name = MappingEntity.GET_LICENCE_BY_PSEUDONYM,
                query = "SELECT w.licence FROM MappingEntity w WHERE w.pseudonym = :pseudonym")
})
public class MappingEntity {
    public static final String GET_LICENCE_BY_PSEUDONYM = "getLicenceByPseudonym";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "HMAC_PSEUDONYM", nullable = false)
    private String pseudonym;

    @Column(name = "LICENCE", nullable = false)
    private String licence;

    public MappingEntity(String pseudonym, String licence) {
        this.pseudonym = pseudonym;
        this.licence = licence;
    }

    public MappingEntity() {
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setPseudonym(String pseudonym) {
        this.pseudonym = pseudonym;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    public Integer getId() {
        return id;
    }

    public String getPseudonym() {
        return pseudonym;
    }

    public String getLicence() {
        return licence;
    }
}
