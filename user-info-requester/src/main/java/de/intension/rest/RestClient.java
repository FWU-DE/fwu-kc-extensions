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

    /**
     * Performs a HTTP-GET request with the given bearer token.
     */
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

        if(isConnected(con)){
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
        } else {
            return null;
        }
    }

    private static boolean isConnected(HttpURLConnection con)
        throws IOException
    {
        int httpCode = con.getResponseCode();
        boolean connected = true;
        if(httpCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            connected = false;
            StringBuilder builder = new StringBuilder();
            try{
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    builder.append(line);
                }
                in.close();
            } catch (IOException e){
                //do nothing
            }
            logger.errorf("Connection failed: %s - response code: %s with message %s", con.getURL().toString(), httpCode, builder.toString());
        } else {
            logger.infof("Connection established: %s - response code: %s", con.getURL().toString(), httpCode);
        }
        return connected;
    }

}
