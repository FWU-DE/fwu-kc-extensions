package de.intension.mapper.oidc;

import org.skyscreamer.jsonassert.ValueMatcher;

import de.intension.mapper.user.UserBirthdayHelper;

public class AgeValueMatcher
    implements ValueMatcher
{

    public static final  String             DATE_OF_BIRTH  = "2010-01-01";
    private static final UserBirthdayHelper birthdayHelper = new UserBirthdayHelper();

    @Override
    public boolean equal(Object o1, Object o2)
    {
        boolean isEqual = false;
        if (o1 != null) {
            int i1 = Integer.parseInt(o1.toString());
            if (i1 == birthdayHelper.calculateAge(DATE_OF_BIRTH)) {
                isEqual = true;
            }
        }
        return isEqual;
    }
}
