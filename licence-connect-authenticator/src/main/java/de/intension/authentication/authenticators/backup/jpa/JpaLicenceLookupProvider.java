package de.intension.authentication.authenticators.backup.jpa;

import de.intension.authentication.authenticators.backup.LicenceLookupProvider;
import de.intension.authentication.authenticators.backup.jpa.entity.LicenceEntity;
import jakarta.persistence.EntityManager;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

import java.util.List;

public class JpaLicenceLookupProvider implements LicenceLookupProvider {
    private final KeycloakSession session;

    public JpaLicenceLookupProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
        //Nothing to do
    }

    @Override
    public List<String> getLicenceByHmacId(String hmacId) {
        //todo: change to only string!? -> check in user attributes
        List<String> licences = getEntityManager()
                .createNamedQuery(LicenceEntity.GET_LICENCE_BY_HMAC_ID)
                .setParameter("hmacId", hmacId)
                .getResultList();
        if (licences.isEmpty()){
            return null;
        }
        return licences;
    }

    @Override
    public LicenceEntity createLicence(LicenceEntity licenceEntity) {
        getEntityManager().persist(licenceEntity);
        return licenceEntity;
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }
}
