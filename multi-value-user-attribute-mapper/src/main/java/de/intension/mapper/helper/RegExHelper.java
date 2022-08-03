package de.intension.mapper.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExHelper
{

    private static final Pattern isRegexPattern  = Pattern.compile("^REGEX\\(.*\\)$");
    private static final Pattern getRegexPattern = Pattern.compile("^REGEX\\((.*)\\)$");

    private RegExHelper()
    {
    }

    /**
     * Checks, whether the expression contains any wildcard characters (* or ?).
     */
    public static boolean isWildcardExpression(String expression)
    {
        return expression != null && (expression.contains("?") || expression.contains("*"));
    }

    /**
     * Translate a simple wildcard expression into a regular expression.
     */
    public static String wildcardToJavaRegex(String wildcardExpression)
    {
        if (wildcardExpression == null) {
            throw new IllegalArgumentException("Wildcard expression must not be null");
        }
        String regularExpr = wildcardExpression.replace("?", ".");
        regularExpr = regularExpr.replace("*", ".*");
        return regularExpr;
    }

    /**
     * Checks, whether the expression is a regular expression starting with "REGEX(" and ends with ")".
     */
    public static boolean isRegularExpression(String expression)
    {
        boolean isRegEx = false;
        if (expression != null && !expression.isEmpty()) {
            Matcher matcher = isRegexPattern.matcher(expression);
            if (matcher.matches()) {
                isRegEx = true;
            }
        }
        return isRegEx;
    }

    /**
     * Get regular expression by removing prefix and suffix.
     */
    public static String getRegularExpressionFromString(String expression)
    {
        String regEx = expression;
        if (expression != null && !expression.isEmpty()) {
            Matcher matcher = getRegexPattern.matcher(expression);
            if (matcher.find()) {
                regEx = matcher.group(1);
            }
        }
        return regEx;
    }

    /**
     * Tries to match a regular expression against a given value.
     */
    public static boolean matches(String regEx, String value)
    {
        boolean matches = false;
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            matches = true;
        }
        return matches;
    }
}
