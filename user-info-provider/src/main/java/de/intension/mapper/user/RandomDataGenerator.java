package de.intension.mapper.user;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

import de.intension.api.enumerations.OrganisationsTyp;
import de.intension.api.enumerations.PersonenStatus;
import de.intension.api.enumerations.Vertrauensstufe;

/**
 * Deterministic pseudo-random generator for plausible test data. Reused across a single
 * {@link RandomUserInfoFiller#fill} call so that all generated values derive from the same seed.
 */
class RandomDataGenerator
{

    private static final List<String>           VORNAMEN             = List.of("Max", "Anna", "Leon", "Mia", "Paul", "Emma", "Ben", "Lea", "Finn", "Lena",
            "Noah", "Mila");
    private static final List<String>           FAMILIENNAMEN        = List.of("Muster", "Schmidt", "Mueller", "Weber", "Fischer", "Becker", "Hoffmann",
            "Wagner", "Koch", "Richter");
    private static final List<String>           ORTE                 = List.of("Berlin", "Hamburg", "Muenchen", "Koeln", "Frankfurt", "Stuttgart",
            "Duesseldorf", "Leipzig", "Dresden", "Hannover");
    private static final List<String>           SCHULNAMEN_PRAEFIX   = List.of("Grundschule", "Gymnasium", "Realschule", "Gesamtschule", "Oberschule");
    private static final List<String>           SCHULNAMEN_SUFFIX    = List.of("am Stadtpark", "an der Aue", "Nord", "Sued", "am Wald", "an der Muehle",
            "West", "Ost");
    private static final List<String>           BUNDESLAENDER        = List.of("DE-BW", "DE-BY", "DE-BE", "DE-BB", "DE-HB", "DE-HH", "DE-HE", "DE-MV",
            "DE-NI", "DE-NW", "DE-RP", "DE-SL", "DE-SN", "DE-ST", "DE-SH", "DE-TH");
    private static final List<String>           FAECHER              = List.of("Mathematik", "Deutsch", "Englisch", "Sport", "Biologie", "Geschichte",
            "Kunst", "Musik");
    private static final List<String>           FACH_CODES           = List.of("MA", "DE", "EN", "SP", "BI", "GE", "KU", "MU");
    private static final List<OrganisationsTyp>  ORGANISATIONS_TYPEN  = List.of(OrganisationsTyp.SCHULE, OrganisationsTyp.ANBIETER,
            OrganisationsTyp.SONSTIGE);
    private static final List<Vertrauensstufe>   VERTRAUENSSTUFEN     = List.of(Vertrauensstufe.TEIL, Vertrauensstufe.VOLL);
    private static final List<PersonenStatus>    PERSONENSTATUS_WERTE = List.of(PersonenStatus.AKTIV, PersonenStatus.INAKTIV);
    private static final DateTimeFormatter       ISO_DATE             = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Random random;

    RandomDataGenerator(long seed)
    {
        this.random = new Random(seed);
    }

    String vorname()
    {
        return pick(VORNAMEN);
    }

    String familienname()
    {
        return pick(FAMILIENNAMEN);
    }

    String ort()
    {
        return pick(ORTE);
    }

    String schulname()
    {
        return pick(SCHULNAMEN_PRAEFIX) + " " + pick(SCHULNAMEN_SUFFIX);
    }

    String fachname()
    {
        return pick(FAECHER);
    }

    String fachCode()
    {
        return pick(FACH_CODES);
    }

    String bundesland()
    {
        return pick(BUNDESLAENDER);
    }

    OrganisationsTyp organisationsTyp()
    {
        return pick(ORGANISATIONS_TYPEN);
    }

    Vertrauensstufe vertrauensstufe()
    {
        return pick(VERTRAUENSSTUFEN);
    }

    PersonenStatus personenstatus()
    {
        return pick(PERSONENSTATUS_WERTE);
    }

    /**
     * Plausible identifier, e.g. for a Kennung.
     */
    String kennung()
    {
        return String.format("TEST_%05d", random.nextInt(100000));
    }

    /**
     * Birthdate somewhere between 6 and 60 years ago, formatted as ISO-8601 (yyyy-MM-dd).
     */
    String geburtsdatum()
    {
        LocalDate date = LocalDate.now().minusYears(6 + random.nextInt(55)).minusDays(random.nextInt(365));
        return date.format(ISO_DATE);
    }

    <T extends Enum<T>> T pick(Class<T> enumType)
    {
        T[] values = enumType.getEnumConstants();
        return values[random.nextInt(values.length)];
    }

    private <T> T pick(List<T> values)
    {
        return values.get(random.nextInt(values.size()));
    }
}
