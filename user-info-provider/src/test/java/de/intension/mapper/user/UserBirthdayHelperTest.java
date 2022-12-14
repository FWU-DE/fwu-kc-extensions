package de.intension.mapper.user;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserBirthdayHelperTest
{

    private final UserBirthdayHelper userBirthdayHelper = new UserBirthdayHelper();

    @ParameterizedTest
    @ValueSource(ints = {18, 7, 1, 0})
    void should_return_correct_age_for_calculated_dates_of_birth(int age)
    {
        LocalDateTime ldt = LocalDateTime.now().minusYears(age);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (age == 0) {
            Assertions.assertNull(userBirthdayHelper.calculateAge(formatter.format(ldt)));
        }
        else {
            Assertions.assertEquals(age, userBirthdayHelper.calculateAge(formatter.format(ldt)));
        }
    }

}
