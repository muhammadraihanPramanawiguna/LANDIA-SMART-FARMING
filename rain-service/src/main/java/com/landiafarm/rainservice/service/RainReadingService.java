package com.landiafarm.rainservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.landiafarm.rainservice.dto.RainReadingRequest;
import com.landiafarm.rainservice.entity.RainReading;
import com.landiafarm.rainservice.repository.RainReadingRepository;

@Service
public class RainReadingService {

    private final RainReadingRepository repository;

    public RainReadingService(RainReadingRepository repository) {
        this.repository = repository;
    }

    public RainReading save(RainReadingRequest request) {

        LocalDateTime recordedAt = request.getRecordedAt();

        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }

        RainReading reading = new RainReading(
                request.getSensorId(),
                request.getValue(),
                request.getUnit(),
                recordedAt,
                LocalDateTime.now()
        );

        return repository.save(reading);
    }

    public List<RainReading> findAll() {
        return repository.findAll();
    }

    public List<RainReading> findBySensorId(String sensorId) {
        return repository
                .findBySensorIdOrderByRecordedAtDesc(sensorId);
    }

    public RainReading findLatest(String sensorId) {
        return repository
                .findFirstBySensorIdOrderByRecordedAtDesc(sensorId)
                .orElse(null);
    }
}