package de.intension.mapper.oidc;

import java.util.*;
import java.util.stream.Stream;

import org.keycloak.models.*;

public class TestUserModel
    implements UserModel
{

    private final List<GroupModel>              groupModels = new ArrayList<>();
    private final HashMap<String, List<String>> attributes  = new HashMap<>();
    private       String                        firstName;
    private String                        lastName;

    public TestUserModel(KeycloakSession session, RealmModel realm, String id)
    {
    }

    @Override
    public String getId()
    {
        return null;
    }

    @Override
    public String getUsername()
    {
        return null;
    }

    @Override
    public void setUsername(String username)
    {

    }

    @Override
    public Long getCreatedTimestamp()
    {
        return null;
    }

    @Override
    public void setCreatedTimestamp(Long timestamp)
    {

    }

    @Override
    public boolean isEnabled()
    {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled)
    {

    }

    @Override
    public void setSingleAttribute(String name, String value)
    {
        attributes.put(name, Collections.singletonList(value));
    }

    @Override
    public void setAttribute(String name, List<String> values)
    {

    }

    @Override
    public void removeAttribute(String name)
    {

    }

    @Override
    public String getFirstAttribute(String name)
    {
        return null;
    }

    @Override
    public Stream<String> getAttributeStream(String name)
    {
        List<String> value = attributes.get(name);
        if (value != null) {
            return value.stream();
        }
        return new ArrayList<String>().stream();
    }

    @Override
    public Map<String, List<String>> getAttributes()
    {
        return attributes;
    }

    @Override
    public Stream<String> getRequiredActionsStream()
    {
        return null;
    }

    @Override
    public void addRequiredAction(String action)
    {

    }

    @Override
    public void removeRequiredAction(String action)
    {

    }

    @Override
    public String getFirstName()
    {
        return firstName;
    }

    @Override
    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    @Override
    public String getLastName()
    {
        return lastName;
    }

    @Override
    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    @Override
    public String getEmail()
    {
        return null;
    }

    @Override
    public void setEmail(String email)
    {

    }

    @Override
    public boolean isEmailVerified()
    {
        return false;
    }

    @Override
    public void setEmailVerified(boolean verified)
    {

    }

    @Override
    public Stream<GroupModel> getGroupsStream()
    {
        return groupModels.stream();
    }

    @Override
    public void joinGroup(GroupModel group)
    {

    }

    @Override
    public void leaveGroup(GroupModel group)
    {

    }

    @Override
    public boolean isMemberOf(GroupModel group)
    {
        return false;
    }

    @Override
    public String getFederationLink()
    {
        return null;
    }

    @Override
    public void setFederationLink(String link)
    {

    }

    @Override
    public String getServiceAccountClientLink()
    {
        return null;
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId)
    {

    }

    @Override
    public SubjectCredentialManager credentialManager()
    {
        return null;
    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream()
    {
        return new ArrayList<RoleModel>().stream();
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app)
    {
        return new ArrayList<RoleModel>().stream();
    }

    @Override
    public boolean hasRole(RoleModel role)
    {
        return false;
    }

    @Override
    public void grantRole(RoleModel role)
    {

    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream()
    {
        return new ArrayList<RoleModel>().stream();
    }

    @Override
    public void deleteRoleMapping(RoleModel role)
    {

    }
}
