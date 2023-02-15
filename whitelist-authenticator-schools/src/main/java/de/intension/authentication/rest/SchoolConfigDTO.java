package de.intension.authentication.rest;

import java.util.List;

public class SchoolConfigDTO {

    boolean allowAll;
    List<String> vidisSchoolIdentifiers;

    public boolean isAllowAll()
    {
        return allowAll;
    }

    public void setAllowAll(boolean allowAll)
    {
        this.allowAll = allowAll;
    }

    public List<String> getVidisSchoolIdentifiers()
    {
        return vidisSchoolIdentifiers;
    }

    public void setVidisSchoolIdentifiers(List<String> vidisSchoolIdentifiers)
    {
        this.vidisSchoolIdentifiers = vidisSchoolIdentifiers;
    }
}
