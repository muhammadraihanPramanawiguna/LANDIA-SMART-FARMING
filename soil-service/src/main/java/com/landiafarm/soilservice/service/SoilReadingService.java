package com.landiafarm.soilservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.landiafarm.soilservice.dto.SoilReadingRequest;
import com.landiafarm.soilservice.entity.SoilReading;
import com.landiafarm.soilservice.repository.SoilReadingRepository;

@Service
public class SoilReadingService {

    private final SoilReadingRepository repository;

    public SoilReadingService(SoilReadingRepository repository) {
        this.repository = repository;
    }

    public SoilReading saveReading(SoilReadingRequest request) {

        SoilReading reading = new SoilReading();

        reading.setSensorId(request.getSensorId());
        reading.setValue(request.getValue());
        reading.setUnit(request.getUnit());

        LocalDateTime now = LocalDateTime.now();

        reading.setRecordedAt(now);
        reading.setCreatedAt(now);

        return repository.save(reading);
    }

    public List<SoilReading> getAllReadings() {
        return repository.findAll();
    }

    public Optional<SoilReading> getLatestReading(String sensorId) {
        return repository.findFirstBySensorIdOrderByRecordedAtDesc(sensorId);
    }
}