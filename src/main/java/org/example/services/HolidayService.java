package org.example.services;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🗓 Service Jours Fériés — GoVibe Hotel Module
 * Checks if a date falls on a public holiday.
 * Uses built-in Tunisian + French public holidays calendar.
 * Applies surcharge automatically when reservation is on a holiday.
 */
public class HolidayService {

    /** Surcharge percentage for holiday bookings */
    public static final double SURCHARGE_POURCENTAGE = 20.0; // +20%

    public static class Holiday {
        public String name;
        public LocalDate date;
        public Holiday(String name, LocalDate date) {
            this.name = name;
            this.date = date;
        }
    }

    /** Built-in holiday calendar */
    private static final Map<String, List<Holiday>> HOLIDAYS_BY_YEAR = new HashMap<>();

    static {
        int year = 2026;
        List<Holiday> h2026 = new ArrayList<>();
        // Tunisian public holidays
        h2026.add(new Holiday("Nouvel An",           LocalDate.of(year, Month.JANUARY, 1)));
        h2026.add(new Holiday("Fête du Travail",     LocalDate.of(year, Month.MAY, 1)));
        h2026.add(new Holiday("Fête de la République", LocalDate.of(year, Month.JULY, 25)));
        h2026.add(new Holiday("Fête de la Femme",    LocalDate.of(year, Month.AUGUST, 13)));
        h2026.add(new Holiday("Fête Nationale",      LocalDate.of(year, Month.MARCH, 20)));
        h2026.add(new Holiday("Fête des Martyrs",    LocalDate.of(year, Month.APRIL, 9)));
        h2026.add(new Holiday("Evacuation",          LocalDate.of(year, Month.OCTOBER, 15)));
        h2026.add(new Holiday("Révolution 14 Jan",   LocalDate.of(year, Month.JANUARY, 14)));
        // Islamic holidays (approximate 2026)
        h2026.add(new Holiday("Aïd al-Fitr",         LocalDate.of(year, Month.MARCH, 30)));
        h2026.add(new Holiday("Aïd al-Adha",         LocalDate.of(year, Month.JUNE, 6)));
        h2026.add(new Holiday("Mouled (Eid Mawlid)", LocalDate.of(year, Month.SEPTEMBER, 4)));
        h2026.add(new Holiday("Nouvel An Hégirien",  LocalDate.of(year, Month.JUNE, 26)));
        HOLIDAYS_BY_YEAR.put(String.valueOf(year), h2026);

        int year2025 = 2025;
        List<Holiday> h2025 = new ArrayList<>();
        h2025.add(new Holiday("Nouvel An",           LocalDate.of(year2025, Month.JANUARY, 1)));
        h2025.add(new Holiday("Fête du Travail",     LocalDate.of(year2025, Month.MAY, 1)));
        h2025.add(new Holiday("Fête de la République", LocalDate.of(year2025, Month.JULY, 25)));
        h2025.add(new Holiday("Aïd al-Fitr",         LocalDate.of(year2025, Month.MARCH, 30)));
        h2025.add(new Holiday("Aïd al-Adha",         LocalDate.of(year2025, Month.JUNE, 6)));
        HOLIDAYS_BY_YEAR.put(String.valueOf(year2025), h2025);
    }

    /**
     * Check if a given date is a public holiday.
     * @return Holiday object if yes, null otherwise
     */
    public Holiday isJourFerie(LocalDate date) {
        String year = String.valueOf(date.getYear());
        List<Holiday> holidays = HOLIDAYS_BY_YEAR.getOrDefault(year, new ArrayList<>());
        for (Holiday h : holidays) {
            if (h.date.equals(date)) return h;
        }
        return null;
    }

    /**
     * Check each day in a range and return all holidays found.
     */
    public List<Holiday> getJoursFeriesDansPeriode(LocalDate debut, LocalDate fin) {
        List<Holiday> found = new ArrayList<>();
        LocalDate current = debut;
        while (!current.isAfter(fin)) {
            Holiday h = isJourFerie(current);
            if (h != null) found.add(h);
            current = current.plusDays(1);
        }
        return found;
    }

    /**
     * Calculate the surcharge for a period.
     * Each holiday day in the period adds SURCHARGE_POURCENTAGE on the night price.
     *
     * @param prixParNuit  Price per night
     * @param debut        Start date
     * @param fin          End date
     * @return Surcharge amount (DT)
     */
    public double calculerSurcharge(double prixParNuit, LocalDate debut, LocalDate fin) {
        List<Holiday> joursFeriers = getJoursFeriesDansPeriode(debut, fin);
        if (joursFeriers.isEmpty()) return 0;
        // Each holiday night is charged an extra SURCHARGE_POURCENTAGE %
        return joursFeriers.size() * prixParNuit * (SURCHARGE_POURCENTAGE / 100.0);
    }

    /**
     * Build a summary message about holidays in the reservation period.
     */
    public String buildHolidaySummary(LocalDate debut, LocalDate fin) {
        List<Holiday> joursFeriers = getJoursFeriesDansPeriode(debut, fin);
        if (joursFeriers.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        sb.append("🗓 Jours fériés détectés (+").append((int) SURCHARGE_POURCENTAGE).append("% par nuit):\n");
        for (Holiday h : joursFeriers) {
            sb.append("  • ").append(h.date).append(" — ").append(h.name).append("\n");
        }
        return sb.toString().trim();
    }

    public List<Holiday> getAllHolidaysForYear(int year) {
        return HOLIDAYS_BY_YEAR.getOrDefault(String.valueOf(year), new ArrayList<>());
    }
}
