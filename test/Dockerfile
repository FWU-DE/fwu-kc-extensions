ARG keycloak_version=26.4.6
FROM quay.io/keycloak/keycloak:${keycloak_version} AS builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

COPY target/. /opt/keycloak/providers/
COPY test/lib/keycloak-custom-attribute-idp-linking-2.1.0.jar /opt/keycloak/providers/keycloak-custom-attribute-idp-linking-2.1.0.jar

WORKDIR /opt/keycloak

RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:${keycloak_version}

USER keycloak

ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

COPY --from=builder /opt/keycloak/ /opt/keycloak/

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
