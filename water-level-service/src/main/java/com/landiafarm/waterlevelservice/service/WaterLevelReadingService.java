package com.landiafarm.waterlevelservice.service;

import com.landiafarm.waterlevelservice.dto.WaterLevelReadingRequest;
import com.landiafarm.waterlevelservice.entity.WaterLevelReading;
import com.landiafarm.waterlevelservice.repository.WaterLevelReadingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WaterLevelReadingService {

    private final WaterLevelReadingRepository repository;

    public WaterLevelReadingService(
            WaterLevelReadingRepository repository) {
        this.repository = repository;
    }

    public WaterLevelReading save(WaterLevelReadingRequest request) {

        WaterLevelReading reading = new WaterLevelReading();

        reading.setSensorId(request.getSensorId());
        reading.setValue(request.getValue());
        reading.setUnit(request.getUnit());

        reading.setRecordedAt(
                request.getRecordedAt() != null
                        ? request.getRecordedAt()
                        : LocalDateTime.now()
        );

        reading.setCreatedAt(LocalDateTime.now());

        return repository.save(reading);
    }

    public List<WaterLevelReading> findAll() {
        return repository.findAll();
    }

    public List<WaterLevelReading> findBySensorId(
            String sensorId) {

        return repository
                .findBySensorIdOrderByRecordedAtDesc(sensorId);
    }

    public WaterLevelReading findLatest(
            String sensorId) {

        return repository
                .findFirstBySensorIdOrderByRecordedAtDesc(sensorId)
                .orElse(null);
    }
}