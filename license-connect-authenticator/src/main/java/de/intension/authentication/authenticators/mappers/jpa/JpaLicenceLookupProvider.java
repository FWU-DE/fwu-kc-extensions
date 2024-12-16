package de.intension.authentication.authenticators.mappers.jpa;

import de.intension.authentication.authenticators.mappers.LicenceLookupProvider;
import de.intension.authentication.authenticators.mappers.jpa.entity.MappingEntity;

import java.util.stream.Stream;

public class JpaLicenceLookupProvider implements LicenceLookupProvider {
    @Override
    public void close() {

    }

    @Override
    public Stream<String> getLicenceByPseudonym(String pseudonym) {
        return Stream.empty();
    }

    @Override
    public MappingEntity createMapping(MappingEntity mappingEntity) {
        return null;
    }

    //todo: implementation of all the methods I need
}
