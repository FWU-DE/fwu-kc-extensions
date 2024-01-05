package de.intension.events.testhelper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.models.UserModel;
import org.mockito.Mockito;

public abstract class UserModelMock
    implements UserModel
{

    private String                      id;
    private Map<String, Stream<String>> attributes;

    public static UserModelMock create(String userID, List<String> schoolIds)
    {
        UserModelMock inst = Mockito.mock(UserModelMock.class, Mockito.CALLS_REAL_METHODS);
        inst.id = userID;
        inst.attributes = Map.of("schulkennung", schoolIds.stream());
        return init(inst);
    }

    private static UserModelMock init(UserModelMock inst)
    {
        return inst;
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public Stream<String> getAttributeStream(String name)
    {
        return Optional.of(attributes.get(name)).orElse(Stream.empty());
    }
}
