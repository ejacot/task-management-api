package com.ejacot.taskmanagement.hotel;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

@Service
public class GermanHolidayService {
    public boolean isGermanHoliday(LocalDate date) {
        return holidays(date.getYear()).contains(date);
    }

    public Set<LocalDate> holidays(int year) {
        LocalDate easter = easterSunday(year);
        return Set.of(
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 1, 6),
                easter.minusDays(2),
                easter.plusDays(1),
                LocalDate.of(year, 5, 1),
                easter.plusDays(39),
                easter.plusDays(50),
                easter.plusDays(60),
                LocalDate.of(year, 10, 3),
                LocalDate.of(year, 11, 1),
                LocalDate.of(year, 12, 25),
                LocalDate.of(year, 12, 26)
        );
    }

    private LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
