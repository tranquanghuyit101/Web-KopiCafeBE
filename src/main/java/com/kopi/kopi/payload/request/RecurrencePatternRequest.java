package com.kopi.kopi.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class RecurrencePatternRequest {
    private String name;
    private Integer shiftId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String recurrenceType; // DAILY, WEEKLY, MONTHLY, YEARLY
    // primary internal representation used by service
    private Integer interval;
    private List<String> dayOfWeek; // for WEEKLY as list
    private List<LocalDate> excludeDates;
    private Boolean active;

    // Accept frontend payload 'intervalDays' and map to internal 'interval'
    @JsonProperty("intervalDays")
    public void setIntervalDays(Integer v) {
        this.interval = v;
    }

    // Accept frontend payload 'dayOfWeek' which may be a CSV string and map to list
    @JsonProperty("dayOfWeek")
    public void setDayOfWeekFromJson(Object v) {
        if (v == null) {
            this.dayOfWeek = null;
            return;
        }
        if (v instanceof List<?>) {
            this.dayOfWeek = ((List<?>) v).stream().map(Object::toString).map(String::toUpperCase)
                    .collect(Collectors.toList());
            return;
        }
        String s = v.toString();
        this.dayOfWeek = Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }
}
