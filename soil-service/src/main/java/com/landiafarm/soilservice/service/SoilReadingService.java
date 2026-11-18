
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

        validate(request);

        LocalDateTime now = LocalDateTime.now();

        String recordId = request.getRecordId();

        // Kompatibilitas data lama tanpa recordId.
        if (recordId == null) {

            SoilReading reading = new SoilReading();

            reading.setDeviceId(normalize(request.getDeviceId()));
            reading.setSensorId(request.getSensorId().trim());
            reading.setValue(request.getValue());
            reading.setUnit(request.getUnit().trim());
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

        final String deviceId = normalize(request.getDeviceId());
        final String sensorId = request.getSensorId().trim();
        final String unit = request.getUnit().trim();

        if (deviceId == null) {
            throw new IllegalArgumentException(
                    "deviceId is required"
            );
        }

        if (deviceId.length() > 100) {
            throw new IllegalArgumentException(
                    "deviceId must not exceed 100 characters"
            );
        }

        // Insert hanya jika recordId belum ada.
        repository.insertIfAbsent(
                normalizedRecordId,
                deviceId,
                sensorId,
                request.getValue(),
                unit,
                now,
                now
        );

        // Ambil record baru atau record lama jika duplikat.
        SoilReading stored = repository
                .findByRecordId(normalizedRecordId)
                .orElseThrow(() -> new IllegalStateException(
                        "Record not found after insert: "
                                + normalizedRecordId
                ));

        // Pastikan ID yang sama memiliki payload yang sama.
        boolean samePayload =
                deviceId.equals(stored.getDeviceId())
                && sensorId.equals(stored.getSensorId())
                && Double.compare(
                        stored.getValue(),
                        request.getValue()
                ) == 0
                && unit.equals(stored.getUnit());

        if (!samePayload) {
            throw new IllegalStateException(
                    "recordId reused with different payload: "
                            + normalizedRecordId
            );
        }

        return stored;
    }

    @Transactional(readOnly = true)
    public List<SoilReading> getAllReadings() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<SoilReading> getLatestReading(String sensorId) {
        return repository.findFirstBySensorIdOrderByRecordedAtDesc(
                sensorId
        );
    }

    private void validate(SoilReadingRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request must not be null"
            );
        }

        if (request.getSensorId() == null
                || request.getSensorId().isBlank()) {
            throw new IllegalArgumentException(
                    "sensorId is required"
            );
        }

        if (request.getValue() == null
                || !Double.isFinite(request.getValue())) {
            throw new IllegalArgumentException(
                    "Valid soil value is required"
            );
        }

        if (request.getUnit() == null
                || request.getUnit().isBlank()) {
            throw new IllegalArgumentException(
                    "unit is required"
            );
        }

        if (request.getRecordId() != null
                && request.getRecordId().isBlank()) {
            throw new IllegalArgumentException(
                    "recordId must not be blank"
            );
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
