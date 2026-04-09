package de.intension.authentication.authenticators.jpa.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "LICENCE")
@NamedQueries({
        @NamedQuery(name = LicenceEntity.GET_LICENCE_BY_HMAC_ID,
                query = "SELECT w.content FROM LicenceEntity w WHERE w.hmacId = :hmacId"),
        @NamedQuery(
                name = LicenceEntity.REMOVE_LICENCE_BY_HMAC_ID,
                query = "DELETE FROM LicenceEntity w WHERE w.hmacId = :hmacId")
})
@Getter
@Setter
@NoArgsConstructor
public class LicenceEntity {
    public static final String GET_LICENCE_BY_HMAC_ID = "getLicenceByHmacId";
    public static final String REMOVE_LICENCE_BY_HMAC_ID = "removeLicenceByHmacId";
    public static final String HMAC_ID = "hmacId";

    @Id
    @Column(name = "HMAC_ID", nullable = false)
    private String hmacId;

    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Setter(AccessLevel.NONE)
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public LicenceEntity(String hmacId, String licence) {
        this.hmacId = hmacId;
        this.content = licence;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void anonymize()
    {
        if (content == null || content.isEmpty()) {
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(content);
            JsonNode userNode = root.path("user");
            if (userNode.isObject() && userNode.has("id")) {
                ((ObjectNode) userNode).put("id", hmacId);
                content = mapper.writeValueAsString(root);
            }
        } catch (Exception e) {
            // Optionally log or handle error
        }
    }
    
    @PreUpdate
    private void beforeUpdate() {
        setUpdatedAt(LocalDateTime.now());
    }
}