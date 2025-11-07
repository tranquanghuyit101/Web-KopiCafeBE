package com.kopi.kopi.service;

import com.kopi.kopi.dto.ShiftDto;

import java.util.List;

public interface IShiftService {
    ShiftDto createShift(ShiftDto dto, Integer adminUserId);

    ShiftDto updateShift(Integer id, ShiftDto dto, Integer adminUserId);

    List<ShiftDto> listActiveShifts();

    ShiftDto getShiftById(Integer id);
}
