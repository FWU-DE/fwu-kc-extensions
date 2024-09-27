package de.intension.authentication.authenticators.rest.model;

public class LicenseRequest
{

    private String userId;
    private String clientId;
    private String schulkennung;
    private String bundesland;

    public LicenseRequest(String userId, String clientId, String schulkennung, String bundesland)
    {
        this.userId = userId;
        this.clientId = clientId;
        this.schulkennung = schulkennung;
        this.bundesland = bundesland;
    }

    public String getClientId()
    {
        return clientId;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }

    public String getSchulkennung()
    {
        return schulkennung;
    }

    public void setSchulkennung(String schulkennung)
    {
        this.schulkennung = schulkennung;
    }

    public String getBundesland()
    {
        return bundesland;
    }

    public void setBundesland(String bundesland)
    {
        this.bundesland = bundesland;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }
}
