package de.intension.authentication.authenticators.mappers;

import de.intension.authentication.authenticators.mappers.jpa.entity.MappingEntity;
import org.keycloak.provider.Provider;

import java.util.List;

public interface LicenceLookupProvider extends Provider {
    List<String> getLicenceByPseudonym(String pseudonym);

    MappingEntity createMapping(MappingEntity mappingEntity);

    //todo: add function that removed the old entries (once the session is over) -> see github project
}
