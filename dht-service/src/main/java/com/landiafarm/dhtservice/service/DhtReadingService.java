package com.landiafarm.dhtservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.landiafarm.dhtservice.dto.DhtReadingRequest;
import com.landiafarm.dhtservice.entity.DhtReading;
import com.landiafarm.dhtservice.repository.DhtReadingRepository;

@Service
public class DhtReadingService {

    private final DhtReadingRepository repository;

    public DhtReadingService(
            DhtReadingRepository repository) {
        this.repository = repository;
    }

    public DhtReading save(DhtReadingRequest request) {

        DhtReading reading = new DhtReading();

        reading.setSensorId(request.getSensorId());
        reading.setTemperature(request.getTemperature());
        reading.setHumidity(request.getHumidity());

        reading.setRecordedAt(
                request.getRecordedAt() != null
                        ? request.getRecordedAt()
                        : LocalDateTime.now()
        );

        reading.setCreatedAt(LocalDateTime.now());

        return repository.save(reading);
    }

    public List<DhtReading> findAll() {
        return repository.findAll();
    }

    public List<DhtReading> findBySensorId(
            String sensorId) {

        return repository
                .findBySensorIdOrderByRecordedAtDesc(sensorId);
    }

    public DhtReading findLatest(
            String sensorId) {

        return repository
                .findFirstBySensorIdOrderByRecordedAtDesc(sensorId)
                .orElse(null);
    }
}