package de.intension.authenticator;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.UserModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ConditionalUserAttributeTest {

    private static final String CONF_ATTR_KEY = "attributeToCheck";

    /**
     * GIVEN Keycloak user with a specific attribute
     * WHEN authenticator is called and looks for this attribute
     * THEN it should be found and true must be returned
     */
    @Test
    void should_return_true_because_user_attribut_exists(){
        UserModel user = mock(UserModel.class);
        Map<String, List<String>> userAttributes = new HashMap<>();
        userAttributes.put(CONF_ATTR_KEY, List.of("aValue"));
        when(user.getAttributes()).thenReturn(userAttributes);
        ConditionalUserAttribute cua = ConditionalUserAttribute.SINGLETON;
        Assertions.assertTrue(cua.matchCondition(mockContext(user, false)));
    }

    /**
     * GIVEN Keycloak user with a specific attribute
     * WHEN authenticator is called and looks for this attribute
     * THEN it should be found and false must be returned because of negation
     */
    @Test
    void should_return_false_because_user_attribut_exists_but_negated(){
        UserModel user = mock(UserModel.class);
        Map<String, List<String>> userAttributes = new HashMap<>();
        userAttributes.put(CONF_ATTR_KEY, List.of("aValue"));
        when(user.getAttributes()).thenReturn(userAttributes);
        ConditionalUserAttribute cua = ConditionalUserAttribute.SINGLETON;
        Assertions.assertFalse(cua.matchCondition(mockContext(user, true)));
    }

    /**
     * GIVEN Keycloak user without attributes
     * WHEN authenticator is called and looks for this attribute
     * THEN it should be not found and false must be returned
     */
    @Test
    void should_return_false_because_user_attribute_not_exists(){
        UserModel user = mock(UserModel.class);
        Map<String, List<String>> userAttributes = new HashMap<>();
        when(user.getAttributes()).thenReturn(userAttributes);
        ConditionalUserAttribute cua = ConditionalUserAttribute.SINGLETON;
        Assertions.assertFalse(cua.matchCondition(mockContext(user, false)));
    }

    /**
     * GIVEN Keycloak user without attributes
     * WHEN authenticator is called and looks for this attribute
     * THEN it should be not found and true must be returned because of negation
     */
    @Test
    void should_return_false_because_user_attribute_not_exists_but_negated(){
        UserModel user = mock(UserModel.class);
        Map<String, List<String>> userAttributes = new HashMap<>();
        when(user.getAttributes()).thenReturn(userAttributes);
        ConditionalUserAttribute cua = ConditionalUserAttribute.SINGLETON;
        Assertions.assertTrue(cua.matchCondition(mockContext(user, true)));
    }

    private AuthenticationFlowContext mockContext(UserModel user, boolean negateOutput){
        AuthenticationFlowContext context = mock(AuthenticationFlowContext.class);
        when(context.getUser()).thenReturn(user);
        //mock authenticator config
        AuthenticatorConfigModel authConfig = mock(AuthenticatorConfigModel.class);
        when(context.getAuthenticatorConfig()).thenReturn(authConfig);
        HashMap<String, String> config = new HashMap<>();
        config.put(ConditionalUserAttributeFactory.CONF_ATTRIBUTE_NAME, CONF_ATTR_KEY);
        config.put(ConditionalUserAttributeFactory.CONF_NOT, Boolean.toString(negateOutput));
        when(authConfig.getConfig()).thenReturn(config);
        return context;
    }

}
