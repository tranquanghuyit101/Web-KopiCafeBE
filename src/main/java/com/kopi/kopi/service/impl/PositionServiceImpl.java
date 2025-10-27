package com.kopi.kopi.service.impl;

import com.kopi.kopi.dto.PositionDto;
import com.kopi.kopi.entity.Position;
import com.kopi.kopi.repository.PositionRepository;
import com.kopi.kopi.service.PositionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PositionServiceImpl implements PositionService {
    private final PositionRepository positionRepository;

    public PositionServiceImpl(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @Override
    public List<PositionDto> listPositions() {
        List<Position> all = positionRepository.findByIsActiveTrue();
        return all.stream().map(p -> new PositionDto(p.getPositionId(), p.getPositionName()))
                .collect(Collectors.toList());
    }
}
