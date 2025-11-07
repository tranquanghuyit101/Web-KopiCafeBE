package com.kopi.kopi.service.impl;

import com.kopi.kopi.dto.ShiftDto;
import com.kopi.kopi.entity.Position;
import com.kopi.kopi.entity.PositionShiftRule;
import com.kopi.kopi.entity.Shift;
import com.kopi.kopi.repository.PositionRepository;
import com.kopi.kopi.repository.PositionShiftRuleRepository;
import com.kopi.kopi.repository.ShiftRepository;
import com.kopi.kopi.service.IShiftService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShiftServiceImpl implements IShiftService {
    private final ShiftRepository shiftRepository;
    private final PositionShiftRuleRepository ruleRepository;
    private final PositionRepository positionRepository;
    private final com.kopi.kopi.repository.UserRepository userRepository;

    public ShiftServiceImpl(ShiftRepository shiftRepository, PositionShiftRuleRepository ruleRepository,
            PositionRepository positionRepository, com.kopi.kopi.repository.UserRepository userRepository) {
        this.shiftRepository = shiftRepository;
        this.ruleRepository = ruleRepository;
        this.positionRepository = positionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ShiftDto createShift(ShiftDto dto, Integer adminUserId) {
        // If position rules provided at create time, ensure total required_count >= 1
        if (dto.getPositionRules() != null) {
            int totalReq = dto.getPositionRules().stream()
                    .mapToInt(r -> r.getRequiredCount() == null ? 0 : r.getRequiredCount()).sum();
            if (totalReq < 1)
                throw new IllegalArgumentException("Total required_count across positions must be >= 1");
        }

        Boolean dtoActive = dto.getActive();
        boolean activeVal = dtoActive == null ? true : dtoActive;

        Shift s = Shift.builder()
                .shiftName(dto.getShiftName())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .description(dto.getDescription())
                .isActive(activeVal)
                .createdAt(LocalDateTime.now())
                .build();
        // attach createdBy user if provided
        if (adminUserId != null) {
            var uopt = userRepository.findById(adminUserId);
            uopt.ifPresent(s::setCreatedByUser);
        }

        Shift saved = shiftRepository.save(s);

        if (dto.getPositionRules() != null) {
            dto.getPositionRules().forEach(r -> {
                Position pos = positionRepository.findById(r.getPositionId()).orElse(null);
                if (pos != null) {
                    Integer req = r.getRequiredCount() == null ? 0 : r.getRequiredCount();
                    boolean allowed = req >= 1;
                    PositionShiftRule rule = PositionShiftRule.builder()
                            .position(pos)
                            .shift(saved)
                            .isAllowed(allowed)
                            .requiredCount(req)
                            .build();
                    ruleRepository.save(rule);
                }
            });
        }

        // map back
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ShiftDto updateShift(Integer id, ShiftDto dto, Integer adminUserId) {
        var opt = shiftRepository.findById(id);
        if (opt.isEmpty())
            throw new IllegalArgumentException("Shift not found");
        Shift s = opt.get();
        // only update fields that are present in DTO to avoid accidentally
        // nulling existing non-nullable columns when client only sends
        // partial payloads (e.g. only positionRules)
        if (dto.getShiftName() != null)
            s.setShiftName(dto.getShiftName());
        if (dto.getStartTime() != null)
            s.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null)
            s.setEndTime(dto.getEndTime());
        if (dto.getDescription() != null)
            s.setDescription(dto.getDescription());
        Boolean dtoActiveUpd = dto.getActive();
        if (dtoActiveUpd != null) {
            s.setActive(dtoActiveUpd);
        }
        // attach updated_by if provided
        if (adminUserId != null) {
            var uopt = userRepository.findById(adminUserId);
            uopt.ifPresent(s::setUpdatedByUser);
        }
        s.setUpdatedAt(LocalDateTime.now());
        Shift saved = shiftRepository.save(s);

        if (dto.getPositionRules() != null) {
            // Deduplicate incoming position rules by positionId to avoid unique
            // constraint violations if the client accidentally sends duplicates.
            java.util.Map<Integer, com.kopi.kopi.dto.ShiftDto.PositionRuleDto> dedup = new java.util.LinkedHashMap<>();
            for (var r : dto.getPositionRules()) {
                if (r == null || r.getPositionId() == null)
                    continue;
                if (dedup.containsKey(r.getPositionId())) {
                    // prefer last occurrence but log duplicate for diagnostics
                    System.out.println("Duplicate positionRule for positionId=" + r.getPositionId()
                            + " when updating shift " + saved.getShiftId() + ", using last occurrence");
                }
                dedup.put(r.getPositionId(), r);
            }

            int totalReq = dedup.values().stream()
                    .mapToInt(r -> r.getRequiredCount() == null ? 0 : r.getRequiredCount()).sum();
            if (totalReq < 1)
                throw new IllegalArgumentException("Total required_count across positions must be >= 1");

            // Upsert approach: update existing rules when possible, insert new ones,
            // and delete any existing rules that are no longer present in the
            // incoming payload. This avoids delete-then-insert races that can
            // trigger UNIQUE constraint violations.
            var existing = ruleRepository.findByShiftShiftId(saved.getShiftId());
            java.util.Map<Integer, PositionShiftRule> existingMap = existing.stream()
                    .filter(er -> er.getPosition() != null)
                    .collect(java.util.stream.Collectors.toMap(er -> er.getPosition().getPositionId(), er -> er));

            // Upsert: update existing rules or create new ones
            for (var r : dedup.values()) {
                Position pos = positionRepository.findById(r.getPositionId()).orElse(null);
                if (pos == null)
                    continue;
                PositionShiftRule ex = existingMap.remove(pos.getPositionId());
                Integer req = r.getRequiredCount() == null ? 0 : r.getRequiredCount();
                boolean allowed = req >= 1;
                if (ex != null) {
                    ex.setRequiredCount(req);
                    ex.setAllowed(allowed);
                    ruleRepository.save(ex);
                } else {
                    PositionShiftRule rule = PositionShiftRule.builder()
                            .position(pos)
                            .shift(saved)
                            .isAllowed(allowed)
                            .requiredCount(req)
                            .build();
                    ruleRepository.save(rule);
                }
            }

            // Any remaining entries in existingMap are not present in the new
            // payload — delete them.
            if (!existingMap.isEmpty()) {
                ruleRepository.deleteAll(existingMap.values());
            }
        }

        return mapToDto(saved);
    }

    @Override
    public List<ShiftDto> listActiveShifts() {
        List<Shift> all = shiftRepository.findByIsActiveTrue();
        return all.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public ShiftDto getShiftById(Integer id) {
        var opt = shiftRepository.findById(id);
        if (opt.isEmpty())
            return null;
        return mapToDto(opt.get());
    }

    private ShiftDto mapToDto(Shift s) {
        ShiftDto dto = new ShiftDto();
        dto.setShiftId(s.getShiftId());
        dto.setShiftName(s.getShiftName());
        dto.setStartTime(s.getStartTime());
        dto.setEndTime(s.getEndTime());
        dto.setDescription(s.getDescription());
        dto.setActive(s.isActive());
        // do not eagerly load position rules here to keep it simple
        return dto;
    }
}
