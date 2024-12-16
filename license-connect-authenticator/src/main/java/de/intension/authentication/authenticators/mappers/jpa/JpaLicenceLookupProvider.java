package de.intension.authentication.authenticators.mappers.jpa;

import de.intension.authentication.authenticators.mappers.LicenceLookupProvider;
import de.intension.authentication.authenticators.mappers.jpa.entity.LicenseEntity;
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
    public List<String> getLicenceByPseudonym(String pseudonym) {
        List<String> licences = getEntityManager()
                .createNamedQuery(LicenseEntity.GET_LICENCE_BY_HMAC_ID)
                .setParameter("pseudonym", pseudonym)
                .getResultList();
        if (licences.isEmpty()){
            return null;
        }
        return licences;
    }

    @Override
    public LicenseEntity createMapping(LicenseEntity licenseEntity) {
        getEntityManager().persist(licenseEntity);
        return licenseEntity;
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }
}
