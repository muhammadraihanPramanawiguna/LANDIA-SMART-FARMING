
package com.landiafarm.waterlevelservice.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landiafarm.waterlevelservice.dto.WaterLevelReadingRequest;
import com.landiafarm.waterlevelservice.entity.WaterLevelReading;
import com.landiafarm.waterlevelservice.exception.WaterLevelReadingConflictException;
import com.landiafarm.waterlevelservice.repository.WaterLevelReadingRepository;

@Service
public class WaterLevelReadingService {

    private static final String EXPECTED_UNIT = "cm";

    private final WaterLevelReadingRepository repository;

    public WaterLevelReadingService(
            WaterLevelReadingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WaterLevelReading save(WaterLevelReadingRequest request) {

        validate(request);

        // Samakan presisi waktu dengan PostgreSQL (mikrodetik).
        LocalDateTime now = LocalDateTime.now()
                .truncatedTo(ChronoUnit.MICROS);

        String recordId = normalize(request.getRecordId());
        String deviceId = normalize(request.getDeviceId());
        String sensorId = request.getSensorId().trim();
        String unit = normalizeUnit(request.getUnit());
        Double value = request.getValue();

        // Timestamp dari request juga dinormalisasi ke mikrodetik.
        LocalDateTime recordedAt =
                request.getRecordedAt() != null
                        ? request.getRecordedAt()
                                .truncatedTo(ChronoUnit.MICROS)
                        : now;

        // Kompatibilitas data lama tanpa recordId.
        if (recordId == null) {

            WaterLevelReading reading = new WaterLevelReading();

            reading.setDeviceId(deviceId);
            reading.setSensorId(sensorId);
            reading.setValue(value);
            reading.setUnit(unit);
            reading.setRecordedAt(recordedAt);
            reading.setCreatedAt(now);

            return repository.save(reading);
        }

        if (recordId.length() > 64) {
            throw new IllegalArgumentException(
                    "recordId must not exceed 64 characters"
            );
        }

        if (deviceId == null) {
            throw new IllegalArgumentException(
                    "deviceId is required when recordId is provided"
            );
        }

        if (deviceId.length() > 100) {
            throw new IllegalArgumentException(
                    "deviceId must not exceed 100 characters"
            );
        }

        // Insert hanya jika recordId belum tersedia.
        repository.insertIfAbsent(
                recordId,
                deviceId,
                sensorId,
                value,
                unit,
                recordedAt,
                now
        );

        // Ambil record baru atau record lama jika duplikat.
        WaterLevelReading stored = repository
                .findByRecordId(recordId)
                .orElseThrow(() -> new IllegalStateException(
                        "Record not found after insert: " + recordId
                ));

        // Pastikan recordId yang sama memiliki payload identik.
       boolean samePayload =
        deviceId.equals(stored.getDeviceId())
        && sensorId.equals(stored.getSensorId())
        && Double.compare(
                value,
                stored.getValue()
        ) == 0
        && unit.equals(stored.getUnit())
        && (
                request.getRecordedAt() == null
                || recordedAt.equals(stored.getRecordedAt())
        );
        if (!samePayload) {
            throw new WaterLevelReadingConflictException(
                    "recordId already exists with different payload: "
                            + recordId
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

        if (sensorId == null || sensorId.isBlank()) {
            throw new IllegalArgumentException(
                    "sensorId is required"
            );
        }

        return repository.findBySensorIdOrderByRecordedAtDesc(
                sensorId.trim()
        );
    }

    @Transactional(readOnly = true)
    public WaterLevelReading findLatest(String sensorId) {

        if (sensorId == null || sensorId.isBlank()) {
            throw new IllegalArgumentException(
                    "sensorId is required"
            );
        }

        return repository
                .findFirstBySensorIdOrderByRecordedAtDesc(
                        sensorId.trim()
                )
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

        if (request.getSensorId().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "sensorId must not exceed 100 characters"
            );
        }

        Double value = request.getValue();

        if (value == null || !Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Valid water level value is required"
            );
        }

        if (value < 0) {
            throw new IllegalArgumentException(
                    "Water level value must not be negative"
            );
        }

        normalizeUnit(request.getUnit());

        if (request.getRecordId() != null
                && request.getRecordId().isBlank()) {
            throw new IllegalArgumentException(
                    "recordId must not be blank"
            );
        }
    }

    private String normalizeUnit(String unit) {

        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException(
                    "unit is required"
            );
        }

        String normalized = unit.trim().toLowerCase(Locale.ROOT);

        if (!EXPECTED_UNIT.equals(normalized)) {
            throw new IllegalArgumentException(
                    "Water level unit must be cm"
            );
        }

        return EXPECTED_UNIT;
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
