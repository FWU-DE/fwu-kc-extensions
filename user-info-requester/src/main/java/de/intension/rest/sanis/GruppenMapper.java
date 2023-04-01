package de.intension.rest.sanis;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import de.intension.api.json.GruppeWithZugehoerigkeit;
import de.intension.rest.IValueMapper;

public class GruppenMapper
    implements IValueMapper
{

    private final String jsonPath;
    private Logger       logger = Logger.getLogger(GruppenMapper.class);

    public GruppenMapper(String jsonPath)
    {
        this.jsonPath = jsonPath;
    }

    @Override
    public String getJsonPath()
    {
        return jsonPath;
    }

    @Override
    public List<String> mapValue(Object document, String jsonPath)
    {
        Configuration config = Configuration.builder().mappingProvider(new JacksonMappingProvider()).build();
        Integer count = JsonPath.read(document, jsonPath + ".length()");
        List<String> gruppen = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GruppeWithZugehoerigkeit gruppe = JsonPath.using(config).parse(document).read(String.format("%s[%d]", jsonPath, i),
                                                                                          new TypeRef<GruppeWithZugehoerigkeit>() {});
            try {
                gruppen.add(gruppe.getJsonRepresentation());
            } catch (JsonProcessingException e) {
                logger.error("Could not map Gruppen from SANIS Response");
            }
        }
        return gruppen;
    }
}
