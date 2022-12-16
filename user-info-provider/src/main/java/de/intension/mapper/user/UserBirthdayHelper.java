package de.intension.mapper.user;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.jboss.logging.Logger;
import org.keycloak.utils.StringUtil;

import de.intension.api.UserInfoAttribute;

public class UserBirthdayHelper
{

    protected static final Logger logger       = Logger.getLogger(UserBirthdayHelper.class);

    private static final String   DATE_PATTERN = "YYYY-MM-DD";

    /**
     * Checks, whether the given date of birth is a valid ISO-8601 date
     */
    public boolean isValidBirthdayFormat(String geburtsdatum)
    {
        boolean valid = false;
        if (StringUtil.isNotBlank(geburtsdatum)) {
            try {
                LocalDate.parse(geburtsdatum, DateTimeFormatter.ISO_LOCAL_DATE);
                valid = true;
            } catch (DateTimeParseException e) {
                logger.warnf("invalid date format for %s - please use %s as a pattern", UserInfoAttribute.PERSON_GEBURTSDATUM.getAttributeName(), DATE_PATTERN);
            }
        }
        return valid;
    }

    /**
     * Calculate age based on the date of birth.
     */
    public Integer calculateAge(String geburtsdatum)
    {
        Integer age = null;
        try {
            LocalDate birthday = LocalDate.parse(geburtsdatum, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalDate now = LocalDate.now();
            if (birthday.isBefore(now)) {
                age = Period.between(birthday, now).getYears();
            }
        } catch (DateTimeParseException e) {
            logger.warnf("invalid date format for %s - please use %s as a pattern", UserInfoAttribute.PERSON_GEBURTSDATUM.getAttributeName(), DATE_PATTERN);
        }
        return age;
    }

}
