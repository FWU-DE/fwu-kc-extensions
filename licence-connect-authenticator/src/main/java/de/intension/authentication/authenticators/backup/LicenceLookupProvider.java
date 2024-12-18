package de.intension.authentication.authenticators.backup;

import de.intension.authentication.authenticators.backup.jpa.entity.LicenceEntity;
import org.keycloak.provider.Provider;

import java.util.List;

public interface LicenceLookupProvider extends Provider {
    List<String> getLicenceByHmacId(String pseudonym);

    LicenceEntity createLicence(LicenceEntity licenceEntity);

    //todo: add function that removed the old entries (once the session is over) -> see github project
}
