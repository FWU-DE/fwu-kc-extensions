package de.intension.events;

import static de.intension.events.DetailedLoginEventFactory.SCHOOLIDS_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.events.Event;
import org.keycloak.models.KeycloakSession;

import de.intension.events.publishers.dto.DetailedLoginEvent;
import de.intension.events.testhelper.*;

class DetailedLoginEventFactoryTest
{

    DetailedLoginEventFactory classUnderTest;

    @BeforeEach
    void setup()
    {
        MockScope config = MockScope.create();
        config.put(SCHOOLIDS_ATTRIBUTE, "schulkennung");
        classUnderTest = new DetailedLoginEventFactory(config);
    }

    @Test
    void shouldCreateMatchingDetailedLoginEvent()
    {

        String realm = "test-realm";
        String clientId = "account-console";
        Event event = TestEventFactory.create();
        RealmModelMock realmModelMock = RealmModelMock.create(realm);
        UserModelMock userModelMock = UserModelMock.create("idp-user", Arrays.asList("DE_BY-1234", "DE_BY-4321"));
        KeycloakSession session = KeycloakSessionMock.create(realmModelMock, userModelMock);
        DetailedLoginEvent actual = classUnderTest.create(event, session);
        assertThat(actual).isNotNull();
        assertThat(actual).extracting(DetailedLoginEvent::getType).isEqualTo("LOGIN");
        assertThat(actual).extracting(DetailedLoginEvent::getRealmId).isEqualTo(TestEventFactory.REALM_ID);
        assertThat(actual).extracting(DetailedLoginEvent::getClientId).isEqualTo(TestEventFactory.CLIENT_ID);
        assertThat(actual).extracting(DetailedLoginEvent::getTimeStamp).isEqualTo(TestEventFactory.TIMESTAMP);
        assertThat(actual).extracting(DetailedLoginEvent::getIdpName).isEqualTo(TestEventFactory.IDP_NAME);

        List<String> actualSchoolIds = actual.getSchoolIds();
        assertThat(actualSchoolIds).isNotEmpty();
    }
}
