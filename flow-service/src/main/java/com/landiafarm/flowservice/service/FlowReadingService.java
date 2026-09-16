package com.landiafarm.flowservice.service;

import com.landiafarm.flowservice.dto.FlowReadingRequest;
import com.landiafarm.flowservice.entity.FlowReading;
import com.landiafarm.flowservice.repository.FlowReadingRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlowReadingService {

    private final FlowReadingRepository repository;

    public FlowReadingService(
            FlowReadingRepository repository) {

        this.repository = repository;
    }

    public FlowReading save(FlowReadingRequest request) {

        LocalDateTime recordedAt =
                request.getRecordedAt();

        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }

        FlowReading reading = new FlowReading(
                request.getSensorId(),
                request.getValue(),
                request.getUnit(),
                recordedAt,
                LocalDateTime.now()
        );

        return repository.save(reading);
    }

    public List<FlowReading> findAll() {
        return repository.findAll();
    }

    public List<FlowReading> findBySensorId(
            String sensorId) {

        return repository
                .findBySensorIdOrderByRecordedAtDesc(sensorId);
    }

    public FlowReading findLatest(String sensorId) {

        return repository
                .findFirstBySensorIdOrderByRecordedAtDesc(sensorId)
                .orElse(null);
    }
}