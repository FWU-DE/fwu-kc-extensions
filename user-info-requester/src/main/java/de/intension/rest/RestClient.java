package de.intension.rest;

import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RestClient
{
    protected static final Logger logger = Logger.getLogger(RestClient.class);

    private RestClient()
    {
    }

    public static String get(URL url, String accessToken)
        throws IOException
    {
        String authString = "Bearer " + accessToken;
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", authString);
        con.setRequestProperty("Content-Type", "application/json");
        con.setConnectTimeout(30 * 1000);
        con.setReadTimeout(30 * 1000);
        con.setInstanceFollowRedirects(false);

        logger.infof("Connection - response code: %s with message %s", con.getResponseCode(), con.getResponseMessage());

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder builder = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            builder.append(inputLine);
        }
        in.close();
        con.disconnect();

        return builder.toString();
    }

}
