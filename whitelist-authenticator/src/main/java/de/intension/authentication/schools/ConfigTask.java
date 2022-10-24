package de.intension.authentication.schools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intension.authentication.dto.SchoolWhitelistEntry;

/**
 * Tasks, which loads a json config from a HTTP-Server and updates the @{@link WhiteListCache}
 */
public class ConfigTask
    implements Runnable
{

    private static final Logger logger = Logger.getLogger(ConfigTask.class);

    private final URI           config;

    public ConfigTask(URI config)
    {
        this.config = config;
    }

    @Override
    public void run()
    {
        String jsonString = "";
        try {
            jsonString = readConfigJson();
            ObjectMapper objectMapper = new ObjectMapper();
            List<SchoolWhitelistEntry> entries = objectMapper.readValue(jsonString, new TypeReference<>() {});
            WhiteListCache.getInstance().updateCache(entries);
            logger.info("School whitelist cache updated.");
        } catch (JsonProcessingException e) {
            logger.errorf(e, "Invalid whitelist format for schools configuration: %s", jsonString);
        } catch (IOException e) {
            logger.errorf(e, "School whitelist json could not be accessed from: %s", config.toString());
        }
    }

    /**
     * Read json config file from HTTP-Server.
     */
    private String readConfigJson()
        throws IOException
    {
        URL url = config.toURL();
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(30 * 1000); //30 seconds
        connection.setReadTimeout(60 * 1000); //60 seconds
        InputStream input = connection.getInputStream();
        InputStreamReader isr = new InputStreamReader(input);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder json = new StringBuilder();
        int c;
        while ((c = br.read()) != -1) {
            json.append((char)c);
        }
        return json.toString();
    }

}
