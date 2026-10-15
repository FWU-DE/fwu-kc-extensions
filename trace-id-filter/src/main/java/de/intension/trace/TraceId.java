package de.intension.trace;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * Constants and helpers for handling trace IDs.
 */
public final class TraceId
{

    /** Name of the HTTP header and of the query parameter carrying the trace ID. */
    public static final String TRACE_ID_PARAMETER = "x-trace-id";
    /** Key used in the logging MDC and as auth note in the authentication session. */
    public static final String TRACE_ID_KEY       = "trace-id";
    public static final String GENERATED_PREFIX   = "generated_";

    static final int             GENERATED_LENGTH = 10;
    private static final String  LETTERS          = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    /**
     * Trace IDs are supplied by the client and end up in the logs, so only a safe set of characters
     * is accepted (prevents log injection, e.g. via line breaks).
     */
    private static final Pattern VALID_TRACE_ID   = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final SecureRandom RANDOM      = new SecureRandom();

    private TraceId()
    {
    }

    /**
     * @return a new trace ID in the format {@code generated_<10 random letters>}
     */
    public static String generate()
    {
        var sb = new StringBuilder(GENERATED_PREFIX.length() + GENERATED_LENGTH).append(GENERATED_PREFIX);
        for (int i = 0; i < GENERATED_LENGTH; i++) {
            sb.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        }
        return sb.toString();
    }

    /**
     * @return {@code true} if the given value can be used as trace ID
     */
    public static boolean isValid(String traceId)
    {
        return traceId != null && VALID_TRACE_ID.matcher(traceId).matches();
    }
}
