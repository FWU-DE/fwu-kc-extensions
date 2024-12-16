package de.intension.authentication.authenticators.mappers.jpa;

import de.intension.authentication.authenticators.mappers.LicenceLookupProvider;
import de.intension.authentication.authenticators.mappers.jpa.entity.MappingEntity;
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
                .createNamedQuery(MappingEntity.GET_LICENCE_BY_PSEUDONYM)
                .setParameter("pseudonym", pseudonym)
                .getResultList();
        if (licences.isEmpty()){
            return null;
        }
        return licences;
    }

    @Override
    public MappingEntity createMapping(MappingEntity mappingEntity) {
        getEntityManager().persist(mappingEntity);
        return mappingEntity;
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }
}
