package de.intension.authentication.authenticators.jpa;

import de.intension.authentication.authenticators.jpa.entity.LicenceEntity;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;

@AllArgsConstructor
public class LicenceJpaProvider implements Provider {
    private final KeycloakSession session;

    @Override
    public void close() {
        //Nothing to do
    }

    public String getLicenceByHmacId(String hmacId) {
        return getEntityManager()
                .createNamedQuery(LicenceEntity.GET_LICENCE_BY_HMAC_ID, String.class)
                .setParameter(LicenceEntity.HMAC_ID, hmacId)
                .getResultStream().findFirst().orElse(null);
    }

    public void persistLicence(LicenceEntity licenceEntity) {
        getEntityManager().persist(licenceEntity);
    }

    public void deleteLicence(String hmacId) {
        getEntityManager()
                .createNamedQuery(LicenceEntity.REMOVE_LICENCE_BY_HMAC_ID)
                .setParameter(LicenceEntity.HMAC_ID, hmacId)
                .executeUpdate();
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }
}
