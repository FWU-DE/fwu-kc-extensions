package de.intension.mapper.oidc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.adapter.InMemoryUserAdapter;

public class TestUserModel extends InMemoryUserAdapter
{

    private final List<GroupModel> groupModels = new ArrayList<>();

    public TestUserModel(KeycloakSession session, RealmModel realm, String id)
    {
        super(session, realm, id);
    }

    @Override
    public Stream<GroupModel> getGroupsStream()
    {
        return groupModels.stream();
    }
}
