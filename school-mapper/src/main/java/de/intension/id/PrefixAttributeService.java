package de.intension.id;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Prefixes a value or list of values with a given prefix.
 */
public class PrefixAttributeService
{

    private final String  prefix;
    private final boolean toLowerCase;

    public PrefixAttributeService(String prefix)
    {
        this(prefix, false);
    }

    public PrefixAttributeService(String prefix, boolean toLowerCase)
    {
        this.prefix = prefix;
        this.toLowerCase = toLowerCase;
    }

    /**
     * Prefix each value of a given list.
     */
    public List<String> prefix(List<String> values)
    {
        return values.stream()
            .filter(v -> v != null && !v.isBlank())
            .map(this::prefix)
            .collect(Collectors.toList());
    }

    /**
     * Prefix a given value if it is non-empty.
     */
    public String prefix(String value)
    {
        if (prefix == null || prefix.isBlank()) {
            throw new UnsupportedOperationException("Prefix cannot be empty");
        }
        if (value == null || value.isBlank()) {
            return value;
        }
        String prefixed = prefix + value;
        return toLowerCase ? prefixed.toLowerCase() : prefixed;
    }
}
