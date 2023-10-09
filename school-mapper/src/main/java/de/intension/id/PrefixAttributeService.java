package de.intension.id;

import org.jboss.logging.Logger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Prefixes a value or list of values with a given prefix.
 */
public class PrefixAttributeService {

    private static final Logger LOG = Logger.getLogger(PrefixAttributeService.class);
    private final        String prefix;
    private final boolean toLowerCase;

    private Pattern regExPattern = null;

    public PrefixAttributeService(String prefix) {
        this(prefix, false, null);
    }

    public PrefixAttributeService(String prefix, boolean toLowerCase, String extractValueRegEx) {
        this.prefix = prefix;
        this.toLowerCase = toLowerCase;
        initRegExPattern(extractValueRegEx);
    }

    private void initRegExPattern(String extractValueRegEx) throws PatternSyntaxException{
        if(extractValueRegEx != null && !extractValueRegEx.isBlank()){
            regExPattern = Pattern.compile(extractValueRegEx);
        }
    }

    /**
     * Prefix each value of a given list.
     */
    public List<String> prefix(List<String> values) {
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(this::prefix)
                .collect(Collectors.toList());
    }

    /**
     * Prefix a given value if it is non-empty.
     */
    public String prefix(String value) {
        if (prefix == null || prefix.isBlank()) {
            throw new UnsupportedOperationException("Prefix cannot be empty");
        }
        if (value == null || value.isBlank()) {
            return value;
        }
        if (regExPattern != null) {
            Matcher matcher = regExPattern.matcher(value);
            if(matcher.matches() && matcher.groupCount() > 0){
                //extract first group only
                value = matcher.group(1);
            } else {
                LOG.warnf("Regular expression '%s' does not match value '%s'", regExPattern.pattern(), value);
                return null;
            }
        }
        String prefixed = prefix + value;
        return toLowerCase ? prefixed.toLowerCase() : prefixed;
    }
}
