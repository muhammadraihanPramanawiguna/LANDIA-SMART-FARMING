
package com.landiafarm.waterlevelservice.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landiafarm.waterlevelservice.dto.WaterLevelReadingRequest;
import com.landiafarm.waterlevelservice.entity.WaterLevelReading;
import com.landiafarm.waterlevelservice.repository.WaterLevelReadingRepository;

@Service
public class WaterLevelReadingService {

    private final WaterLevelReadingRepository repository;

    public WaterLevelReadingService(
            WaterLevelReadingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WaterLevelReading save(WaterLevelReadingRequest request) {

        validate(request);

        LocalDateTime now = LocalDateTime.now();

        String recordId = request.getRecordId();

        // Kompatibilitas data lama tanpa recordId.
        if (recordId == null) {

            WaterLevelReading reading = new WaterLevelReading();

            reading.setDeviceId(normalize(request.getDeviceId()));
            reading.setSensorId(request.getSensorId().trim());
            reading.setValue(request.getValue());
            reading.setUnit(request.getUnit().trim());

            reading.setRecordedAt(
                    request.getRecordedAt() != null
                            ? request.getRecordedAt()
                                    .truncatedTo(ChronoUnit.MICROS)
                            : now
            );

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

        LocalDateTime recordedAt =
                request.getRecordedAt() != null
                        ? request.getRecordedAt()
                                .truncatedTo(ChronoUnit.MICROS)
                        : now;

        // Insert hanya jika recordId belum tersedia.
        repository.insertIfAbsent(
                normalizedRecordId,
                deviceId,
                sensorId,
                request.getValue(),
                unit,
                recordedAt,
                now
        );

        // Ambil record baru atau record lama jika duplikat.
        WaterLevelReading stored = repository
                .findByRecordId(normalizedRecordId)
                .orElseThrow(() -> new IllegalStateException(
                        "Record not found after insert: "
                                + normalizedRecordId
                ));

        // Pastikan recordId yang sama memiliki payload yang sama.
        boolean samePayload =
                deviceId.equals(stored.getDeviceId())
                && sensorId.equals(stored.getSensorId())
                && Double.compare(
                        stored.getValue(),
                        request.getValue()
                ) == 0
                && unit.equals(stored.getUnit())
                && recordedAt.equals(stored.getRecordedAt());

        if (!samePayload) {
            throw new IllegalStateException(
                    "recordId reused with different payload: "
                            + normalizedRecordId
            );
        }

        return stored;
    }

    @Transactional(readOnly = true)
    public List<WaterLevelReading> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<WaterLevelReading> findBySensorId(
            String sensorId) {

        return repository
                .findBySensorIdOrderByRecordedAtDesc(sensorId);
    }

    @Transactional(readOnly = true)
    public WaterLevelReading findLatest(String sensorId) {

        return repository
                .findFirstBySensorIdOrderByRecordedAtDesc(sensorId)
                .orElse(null);
    }

    private void validate(WaterLevelReadingRequest request) {

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
                    "Valid water level value is required"
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
