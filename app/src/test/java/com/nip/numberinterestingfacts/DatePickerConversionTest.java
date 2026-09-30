package com.nip.numberinterestingfacts;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneOffset;
import java.util.TimeZone;

public class DatePickerConversionTest {
    @Test
    public void pickedDayDoesNotShiftWithTheDeviceTimeZone() {
        TimeZone original = TimeZone.getDefault();
        try {
            long march14Utc = LocalDate.of(2026, 3, 14).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
            long feb29Utc = LocalDate.of(2028, 2, 29).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
            for (String zone : new String[]{"Pacific/Honolulu", "America/New_York", "UTC", "Europe/Sarajevo",
                    "Asia/Kolkata", "Pacific/Kiritimati"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                assertEquals(zone, MonthDay.of(3, 14), RandomActivity.monthDayFromPickerSelection(march14Utc));
                assertEquals(zone, MonthDay.of(2, 29), RandomActivity.monthDayFromPickerSelection(feb29Utc));
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
