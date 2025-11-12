package de.intension.keycloak;

import dasniko.testcontainers.keycloak.ExtendableKeycloakContainer;

public class IntensionKeycloakContainer extends ExtendableKeycloakContainer<IntensionKeycloakContainer> {

    private static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version", "latest");

    public IntensionKeycloakContainer() {
        super("quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION);
    }
}
