package de.intension.mapper.user;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.hash.Hashing;

import de.intension.api.enumerations.*;
import de.intension.api.json.Fach;
import de.intension.api.json.Geburt;
import de.intension.api.json.Gruppe;
import de.intension.api.json.GruppeWithZugehoerigkeit;
import de.intension.api.json.GruppenId;
import de.intension.api.json.GruppenZugehoerigkeit;
import de.intension.api.json.HeimatOrganisation;
import de.intension.api.json.Organisation;
import de.intension.api.json.Person;
import de.intension.api.json.PersonName;
import de.intension.api.json.Personenkontext;
import de.intension.api.json.UserInfo;

/**
 * Fills whatever is missing on a {@link UserInfo} built from real user attributes with plausible,
 * deterministically randomized test data. Existing (real) values are never overwritten.
 */
public class RandomUserInfoFiller
{

    private static final UserBirthdayHelper        birthdayHelper        = new UserBirthdayHelper();
    private static final UserVolljaehrigkeitHelper volljaehrigkeitHelper = new UserVolljaehrigkeitHelper();

    /**
     * Build a complete {@link UserInfo} with exactly one {@link Personenkontext} holding the given role.
     * Real data already present on {@code realUserInfo} is kept as-is; anything missing is fabricated.
     * The fabricated data is deterministic for a given user as long as its underlying attributes don't change.
     */
    public UserInfo fill(UserInfo realUserInfo, Rolle rolle, UserModel user)
    {
        RandomDataGenerator generator = new RandomDataGenerator(seed(realUserInfo, user));

        UserInfo userInfo = new UserInfo();
        userInfo.setPid(realUserInfo.getPid());
        HeimatOrganisation heimatOrganisation = fillHeimatOrganisation(realUserInfo.getHeimatOrganisation(), generator);
        userInfo.setHeimatOrganisation(heimatOrganisation);
        userInfo.setPerson(fillPerson(realUserInfo.getPerson(), generator));

        Personenkontext realKontext = realUserInfo.getPersonenKontexte().stream()
            .filter(k -> rolle.equals(k.getRolle()))
            .findFirst()
            .orElse(null);
        userInfo.getPersonenKontexte().add(fillPersonenkontext(realKontext, rolle, heimatOrganisation, generator));
        return userInfo;
    }

    private long seed(UserInfo realUserInfo, UserModel user)
    {
        String seedSource = user.getId();
        try {
            seedSource += "|" + realUserInfo.getJsonRepresentation();
        } catch (JsonProcessingException e) {
            // fall back to seeding by user id alone
        }
        return Hashing.sha256().hashString(seedSource, StandardCharsets.UTF_8).asLong();
    }

    private HeimatOrganisation fillHeimatOrganisation(HeimatOrganisation real, RandomDataGenerator generator)
    {
        HeimatOrganisation heimatOrganisation = real != null ? real : new HeimatOrganisation();
        if (StringUtil.isBlank(heimatOrganisation.getId())) {
            heimatOrganisation.setId("test-idp-" + generator.kennung().toLowerCase());
        }
        if (StringUtil.isBlank(heimatOrganisation.getName())) {
            heimatOrganisation.setName(generator.schulname());
        }
        if (StringUtil.isBlank(heimatOrganisation.getBundesland())) {
            heimatOrganisation.setBundesland(generator.bundesland());
        }
        return heimatOrganisation;
    }

    private Person fillPerson(Person real, RandomDataGenerator generator)
    {
        Person person = real != null ? real : new Person();
        person.setPersonName(fillPersonName(person.getPersonName(), generator));
        if (person.getGeburt() == null || person.getGeburt().isEmpty()) {
            person.setGeburt(fillGeburt(generator));
        }
        if (person.getGeschlecht() == null) {
            person.setGeschlecht(generator.pick(Geschlecht.class));
        }
        if (StringUtil.isBlank(person.getLokalisierung())) {
            person.setLokalisierung("de-DE");
        }
        if (person.getVertrauensstufe() == null) {
            person.setVertrauensstufe(generator.vertrauensstufe());
        }
        return person;
    }

    private PersonName fillPersonName(PersonName real, RandomDataGenerator generator)
    {
        PersonName personName = real != null ? real : new PersonName();
        if (StringUtil.isBlank(personName.getVorname())) {
            personName.setVorname(generator.vorname());
        }
        if (StringUtil.isBlank(personName.getFamilienname())) {
            personName.setFamilienname(generator.familienname());
        }
        if (StringUtil.isBlank(personName.getInitialenVorname())) {
            personName.setInitialenVorname(personName.getVorname().substring(0, 1));
        }
        if (StringUtil.isBlank(personName.getInitialenFamilienname())) {
            personName.setInitialenFamilienname(personName.getFamilienname().substring(0, 1));
        }
        if (StringUtil.isBlank(personName.getAkronym()) && personName.getVorname().length() >= 2 && personName.getFamilienname().length() >= 2) {
            personName.setAkronym((personName.getVorname().substring(0, 2) + personName.getFamilienname().substring(0, 2)).toLowerCase());
        }
        return personName;
    }

    private Geburt fillGeburt(RandomDataGenerator generator)
    {
        Geburt geburt = new Geburt();
        String datum = generator.geburtsdatum();
        geburt.setDatum(datum);
        geburt.setGeburtsort(generator.ort() + ", Deutschland");
        Integer age = birthdayHelper.calculateAge(datum);
        geburt.setAlter(age);
        geburt.setVolljaehrig(volljaehrigkeitHelper.isVolljaehrig(age));
        return geburt;
    }

    private Personenkontext fillPersonenkontext(Personenkontext real, Rolle rolle, HeimatOrganisation heimatOrganisation, RandomDataGenerator generator)
    {
        Personenkontext kontext = real != null ? real : new Personenkontext();
        kontext.setRolle(rolle);
        Organisation organisation = fillOrganisation(kontext.getOrganisation(), rolle, heimatOrganisation, generator);
        kontext.setOrganisation(organisation);
        if (kontext.getPersonenstatus() == null) {
            kontext.setPersonenstatus(generator.personenstatus());
        }
        if (StringUtil.isBlank(kontext.getId())) {
            String builder = rolle.name() + heimatOrganisation.getId() + organisation.getKennung();
            kontext.setId(Hashing.sha256().hashString(builder, StandardCharsets.UTF_8).toString());
        }
        if (kontext.getGruppen() == null || kontext.getGruppen().isEmpty()) {
            kontext.setGruppen(Collections.singletonList(fillGruppe(rolle, generator)));
        }
        return kontext;
    }

    private Organisation fillOrganisation(Organisation real, Rolle rolle, HeimatOrganisation heimatOrganisation, RandomDataGenerator generator)
    {
        Organisation organisation = real != null ? real : new Organisation();
        if (StringUtil.isBlank(organisation.getKennung())) {
            organisation.setKennung(generator.kennung());
        }
        if (StringUtil.isBlank(organisation.getName())) {
            organisation.setName(generator.schulname());
        }
        if (organisation.getTyp() == null) {
            organisation.setTyp(generator.organisationsTyp());
        }
        if (StringUtil.isBlank(organisation.getOrgid())) {
            String builder = rolle.name() + heimatOrganisation.getId() + organisation.getKennung();
            organisation.setOrgid(Hashing.sha256().hashString(builder, StandardCharsets.UTF_8).toString());
        }
        if (StringUtil.isBlank(organisation.getVidisSchulidentifikator())) {
            organisation.setVidisSchulidentifikator(String.format("%s.%s", heimatOrganisation.getId(), organisation.getKennung()).toLowerCase());
        }
        return organisation;
    }

    private GruppeWithZugehoerigkeit fillGruppe(Rolle rolle, RandomDataGenerator generator)
    {
        Gruppe gruppe = new Gruppe();
        gruppe.setId(new GruppenId(generator.kennung()));
        gruppe.setBezeichnung(generator.fachname());
        gruppe.setTyp(generator.pick(Gruppentyp.class));
        gruppe.setBereich(generator.pick(Gruppenbereich.class));
        gruppe.setDifferenzierung(generator.pick(GruppenDifferenzierung.class));
        gruppe.setBildungsziel(List.of(generator.pick(Bildungsziel.class)));
        gruppe.setJahrgangstufen(List.of(generator.pick(Jahrgangsstufe.class)));
        Fach fach = new Fach();
        fach.setCode(generator.fachCode());
        gruppe.setFaecher(List.of(fach));

        GruppenZugehoerigkeit zugehoerigkeit = new GruppenZugehoerigkeit();
        zugehoerigkeit.setRollen(List.of(rolle));

        GruppeWithZugehoerigkeit gruppeWithZugehoerigkeit = new GruppeWithZugehoerigkeit();
        gruppeWithZugehoerigkeit.setGruppe(gruppe);
        gruppeWithZugehoerigkeit.setGruppenZugehoerigkeit(zugehoerigkeit);
        return gruppeWithZugehoerigkeit;
    }
}
