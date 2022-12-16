package de.intension.mapper.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.intension.api.enumerations.OrganisationsTyp;
import de.intension.api.json.*;

class UserInfoHelperTest
{

    @Test
    void should_mark_as_empty_userInfo_if_pid_is_null()
    {
        UserInfo userInfo = new UserInfo();
        Person person = new Person();
        person.setPerson(new PersonName("Muster", "Hans", "M", "H", "hamu"));
        userInfo.setPerson(person);
        Assertions.assertTrue(userInfo.isEmpty());
    }

    @Test
    void should_mark_as_not_empty_userInfo_if_pid_and_heimatorganisation_are_not_null()
    {
        UserInfo userInfo = new UserInfo();
        userInfo.setPid("2757c7a9-bb12-44d8-adf4-32e8d1afd3a0");
        userInfo.setHeimatOrganisation(getHeimatorganisation(false));
        Assertions.assertFalse(userInfo.isEmpty());
    }

    @Test
    void should_mark_as_not_empty_userInfo_if_pid_and_person_are_not_null()
    {
        UserInfo userInfo = new UserInfo();
        userInfo.setPid("2757c7a9-bb12-44d8-adf4-32e8d1afd3a0");
        Person person = new Person();
        person.setPerson(new PersonName("Muster", "Hans", "M", "H", "hamu"));
        userInfo.setPerson(person);
        Assertions.assertFalse(userInfo.isEmpty());
    }

    @ParameterizedTest
    @CsvSource(value = {"'','',''", "null,null,null"}, nullValues = {"null"})
    void should_mark_as_empty_heimatorganisation(String id, String name, String bundesland)
    {
        HeimatOrganisation org = new HeimatOrganisation(id, name, bundesland);
        Assertions.assertTrue(org.isEmpty());
    }

    @ParameterizedTest
    @CsvSource(value = {"'','','','',''",
            "null,null,null,null,null"}, nullValues = {"null"})
    void should_mark_as_empty_person_Name(String familienname, String vorname, String initialenFamilienname, String initialenVorname, String akronym)
    {
        PersonName personName = new PersonName(familienname, vorname, initialenFamilienname, initialenVorname, akronym);
        Assertions.assertTrue(personName.isEmpty());
    }

    @ParameterizedTest
    @CsvSource(value = {"Muster,null,null,null,null",
            "null,Max,null,null,null",
            "null,null,M,null,null",
            "null,null,null,M,null",
            "null,null,null,null,mamu"}, nullValues = {"null"})
    void should_mark_as_not_empty_person_Name(String familienname, String vorname, String initialenFamilienname, String initialenVorname, String akronym)
    {
        PersonName personName = new PersonName(familienname, vorname, initialenFamilienname, initialenVorname, akronym);
        Assertions.assertFalse(personName.isEmpty());
    }

    @ParameterizedTest
    @CsvSource(value = {"'',null,",
            "null,null"}, nullValues = {"null"})
    void should_mark_as_empty_geburt(String datum, Integer alter)
    {
        Geburt geburt = new Geburt(datum, alter);
        Assertions.assertTrue(geburt.isEmpty());
    }

    @ParameterizedTest
    @CsvSource(value = {"1980-01-01,null,",
            "null,18",
            "1980-01-01,18",}, nullValues = {"null"})
    void should_mark_as_not_empty_geburt(String datum, Integer alter)
    {
        Geburt geburt = new Geburt(datum, alter);
        Assertions.assertFalse(geburt.isEmpty());
    }

    @ParameterizedTest
    @CsvSource(value = {"null,null,null,null,null",
            "'','','',null,''",
            "1242,null,null,null,null",
            "null,Test,null,null,null",
            "null,null,School,null,null",
            "null,null,null,SCHULE,null",
            "null,null,null,null,0815",
            "1242,Schule,null,null,0815",
            "1242,Schule,Musterschule,null,0815",
            "1242,null,Musterschule,SCHULE,null",
            "1242,'',Musterschule,SCHULE,null",
            "1242,null,Musterschule,SCHULE,''"}, nullValues = {"null"})
    void should_mark_as_empty_organisation(String orgid, String kennung, String name, OrganisationsTyp typ, String vidisSchulidentifikator)
    {
        Organisation org = new Organisation(orgid, kennung, name, typ, vidisSchulidentifikator);
        Assertions.assertTrue(org.isEmpty());
    }

    @ParameterizedTest
    @CsvSource(value = {"1242,Test,School,SCHULE,0815",
            "1242,Test,null,SCHULE,0815",
            "1242,Test,'',SCHULE,0815",
            "1242,null,School,SCHULE,0815",
            "1242,'',School,SCHULE,0815",
            "1242,Test,School,SCHULE,null",
            "1242,Test,School,SCHULE,''",
            "1242,Test,null,SCHULE,''",
            "1242,Test,null,SCHULE,null",
            "1242,null,null,SCHULE,0815",
            "1242,'','',SCHULE,0815",}, nullValues = {"null"})
    void should_mark_as_not_empty_organisation(String orgid, String kennung, String name, OrganisationsTyp typ, String vidisSchulidentifikator)
    {
        Organisation org = new Organisation(orgid, kennung, name, typ, vidisSchulidentifikator);
        Assertions.assertFalse(org.isEmpty());
    }

    private HeimatOrganisation getHeimatorganisation(boolean empty)
    {
        if (!empty) {
            return new HeimatOrganisation("111", "Musterschule", "DE-BY");
        }
        else {
            return new HeimatOrganisation();
        }
    }

}
