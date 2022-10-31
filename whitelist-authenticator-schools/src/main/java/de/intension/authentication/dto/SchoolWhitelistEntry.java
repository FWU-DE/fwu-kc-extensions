package de.intension.authentication.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Data transfer object for school whitelist entries.
 */
public class SchoolWhitelistEntry
{

    private String       spAlias;
    private List<String> listOfSchools;

    public String getSpAlias()
    {
        return spAlias;
    }

    public void setSpAlias(String spAlias)
    {
        this.spAlias = spAlias;
    }

    public List<String> getListOfSchools()
    {
        if (listOfSchools == null) {
            listOfSchools = new ArrayList<>();
        }
        return listOfSchools;
    }

    public void setListOfSchools(List<String> listOfSchools)
    {
        this.listOfSchools = listOfSchools;
    }

    public String prettyPrint()
    {
        return String.format("%s -> %s", spAlias, Arrays.toString(listOfSchools.toArray()));
    }
}
