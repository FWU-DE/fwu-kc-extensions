package de.intension.authentication.authenticators.backup;

import de.intension.authentication.authenticators.backup.jpa.entity.LicenseEntity;
import org.keycloak.provider.Provider;

import java.util.List;

public interface LicenseLookupProvider extends Provider {
    List<String> getLicenceByPseudonym(String pseudonym);

    LicenseEntity createMapping(LicenseEntity licenseEntity);

    //todo: add function that removed the old entries (once the session is over) -> see github project
}
