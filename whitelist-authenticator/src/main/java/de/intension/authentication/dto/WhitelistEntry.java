package de.intension.authentication.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object for whitelist entries.
 */
public class WhitelistEntry
{

    private String       clientId;
    private List<String> listOfIdPs;

    public String getClientId()
    {
        return clientId;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }

    public List<String> getListOfIdPs()
    {
        if (listOfIdPs == null) {
            listOfIdPs = new ArrayList<>();
        }
        return listOfIdPs;
    }

    public void setListOfIdPs(List<String> listOfIdPs)
    {
        this.listOfIdPs = listOfIdPs;
    }
}
