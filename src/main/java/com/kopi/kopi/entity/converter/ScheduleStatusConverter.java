package com.kopi.kopi.entity.converter;

import com.kopi.kopi.entity.enums.ScheduleStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ScheduleStatusConverter implements AttributeConverter<ScheduleStatus, String> {
    @Override
    public String convertToDatabaseColumn(ScheduleStatus attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }

    @Override
    public ScheduleStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return ScheduleStatus.valueOf(dbData.toUpperCase());
    }
} 