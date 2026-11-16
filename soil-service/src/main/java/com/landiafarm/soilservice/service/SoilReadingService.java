
package com.landiafarm.soilservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landiafarm.soilservice.dto.SoilReadingRequest;
import com.landiafarm.soilservice.entity.SoilReading;
import com.landiafarm.soilservice.repository.SoilReadingRepository;

@Service
public class SoilReadingService {

    private final SoilReadingRepository repository;

    public SoilReadingService(SoilReadingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SoilReading saveReading(SoilReadingRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        if (request.getSensorId() == null
                || request.getSensorId().isBlank()) {
            throw new IllegalArgumentException("sensorId is required");
        }

        if (request.getValue() == null
                || !Double.isFinite(request.getValue())) {
            throw new IllegalArgumentException("Valid soil value is required");
        }

        if (request.getUnit() == null
                || request.getUnit().isBlank()) {
            throw new IllegalArgumentException("unit is required");
        }

        LocalDateTime now = LocalDateTime.now();
        String recordId = request.getRecordId();

        // Kompatibilitas dengan payload lama yang tidak memiliki recordId.
        if (recordId == null) {
            SoilReading reading = new SoilReading();
            reading.setSensorId(request.getSensorId());
            reading.setValue(request.getValue());
            reading.setUnit(request.getUnit());
            reading.setRecordedAt(now);
            reading.setCreatedAt(now);

            return repository.save(reading);
        }

        final String normalizedRecordId = recordId.trim();

        if (normalizedRecordId.isEmpty()
                || normalizedRecordId.length() > 64) {
            throw new IllegalArgumentException(
                    "recordId must contain 1 to 64 characters"
            );
        }

        repository.insertIfAbsent(
                normalizedRecordId,
                request.getSensorId(),
                request.getValue(),
                request.getUnit(),
                now,
                now
        );

        SoilReading stored = repository
                .findByRecordId(normalizedRecordId)
                .orElseThrow(() -> new IllegalStateException(
                        "Record not found after insert: "
                                + normalizedRecordId
                ));

        // ID sama tetapi isi berbeda berarti ada kesalahan protokol.
        boolean samePayload =
                stored.getSensorId().equals(request.getSensorId())
                && Double.compare(
                        stored.getValue(),
                        request.getValue()
                ) == 0
                && stored.getUnit().equals(request.getUnit());

        if (!samePayload) {
            throw new IllegalStateException(
                    "recordId reused with different payload: "
                            + normalizedRecordId
            );
        }

        return stored;
    }

    public List<SoilReading> getAllReadings() {
        return repository.findAll();
    }

    public Optional<SoilReading> getLatestReading(String sensorId) {
        return repository.findFirstBySensorIdOrderByRecordedAtDesc(
                sensorId
        );
    }
}
