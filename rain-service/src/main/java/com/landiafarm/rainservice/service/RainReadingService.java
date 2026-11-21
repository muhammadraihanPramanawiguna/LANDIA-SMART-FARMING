
package com.landiafarm.rainservice.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landiafarm.rainservice.dto.RainReadingRequest;
import com.landiafarm.rainservice.entity.RainReading;
import com.landiafarm.rainservice.exception.RainReadingConflictException;
import com.landiafarm.rainservice.repository.RainReadingRepository;

@Service
public class RainReadingService {

    private static final String EXPECTED_UNIT = "%";

    private static final double MIN_VALUE = 0.0;
    private static final double MAX_VALUE = 100.0;

    private static final int MAX_RECORD_ID_LENGTH = 64;
    private static final int MAX_DEVICE_ID_LENGTH = 100;
    private static final int MAX_SENSOR_ID_LENGTH = 100;

    private final RainReadingRepository repository;

    public RainReadingService(RainReadingRepository repository) {
        this.repository = repository;
    }

    // ==========================================
    // SAVE RAIN READING
    // ==========================================

    @Transactional
    public RainReading save(RainReadingRequest request) {

        validate(request);

        LocalDateTime now = LocalDateTime.now()
                .truncatedTo(ChronoUnit.MICROS);

        String recordId = normalize(request.getRecordId());
        String deviceId = normalize(request.getDeviceId());
        String sensorId = request.getSensorId().trim();
        String unit = normalizeUnit(request.getUnit());

        Double value = request.getValue();

        LocalDateTime recordedAt =
                request.getRecordedAt() != null
                        ? request.getRecordedAt()
                                .truncatedTo(ChronoUnit.MICROS)
                        : now;

        // ==========================================
        // LEGACY DATA WITHOUT RECORD ID
        // ==========================================

        if (recordId == null) {

            RainReading reading = new RainReading(
                    sensorId,
                    value,
                    unit,
                    recordedAt,
                    now
            );

            reading.setDeviceId(deviceId);

            return repository.save(reading);
        }

        // ==========================================
        // IDEMPOTENT INSERT
        // ==========================================

        validateRecordIdentity(recordId, deviceId);

        repository.insertIfAbsent(
                recordId,
                deviceId,
                sensorId,
                value,
                unit,
                recordedAt,
                now
        );

        // ==========================================
        // FIND STORED RECORD
        // ==========================================

        RainReading stored = repository
                .findByRecordId(recordId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Record not found after insert: " + recordId
                        )
                );

        // ==========================================
        // VERIFY PAYLOAD
        // ==========================================

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

            throw new RainReadingConflictException(
                    "recordId already exists with different payload: "
                            + recordId
            );
        }

        return stored;
    }

    // ==========================================
    // FIND ALL RAIN READINGS
    // ==========================================

    @Transactional(readOnly = true)
    public List<RainReading> findAll() {

        return repository.findAll();
    }

    // ==========================================
    // FIND BY SENSOR ID
    // ==========================================

    @Transactional(readOnly = true)
    public List<RainReading> findBySensorId(String sensorId) {

        if (sensorId == null || sensorId.isBlank()) {

            throw new IllegalArgumentException(
                    "sensorId is required"
            );
        }

        String normalizedSensorId = sensorId.trim();

        if (normalizedSensorId.length() > MAX_SENSOR_ID_LENGTH) {

            throw new IllegalArgumentException(
                    "sensorId must not exceed "
                            + MAX_SENSOR_ID_LENGTH + " characters"
            );
        }

        return repository.findBySensorIdOrderByRecordedAtDesc(
                normalizedSensorId
        );
    }

    // ==========================================
    // FIND LATEST READING
    // ==========================================

    @Transactional(readOnly = true)
    public RainReading findLatest(String sensorId) {

        if (sensorId == null || sensorId.isBlank()) {

            throw new IllegalArgumentException(
                    "sensorId is required"
            );
        }

        String normalizedSensorId = sensorId.trim();

        if (normalizedSensorId.length() > MAX_SENSOR_ID_LENGTH) {

            throw new IllegalArgumentException(
                    "sensorId must not exceed "
                            + MAX_SENSOR_ID_LENGTH + " characters"
            );
        }

        return repository
                .findFirstBySensorIdOrderByRecordedAtDesc(
                        normalizedSensorId
                )
                .orElse(null);
    }

    // ==========================================
    // VALIDATION
    // ==========================================

    private void validate(RainReadingRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Request must not be null"
            );
        }

        // SENSOR ID

        if (request.getSensorId() == null
                || request.getSensorId().isBlank()) {

            throw new IllegalArgumentException(
                    "sensorId is required"
            );
        }

        if (request.getSensorId().trim().length()
                > MAX_SENSOR_ID_LENGTH) {

            throw new IllegalArgumentException(
                    "sensorId must not exceed "
                            + MAX_SENSOR_ID_LENGTH + " characters"
            );
        }

        // RAIN VALUE

        Double value = request.getValue();

        if (value == null || !Double.isFinite(value)) {

            throw new IllegalArgumentException(
                    "Valid rain value is required"
            );
        }

        if (value < MIN_VALUE || value > MAX_VALUE) {

            throw new IllegalArgumentException(
                    "Rain value must be between 0 and 100"
            );
        }

        // UNIT

        normalizeUnit(request.getUnit());

        // RECORD ID

        String recordId = normalize(request.getRecordId());

        if (request.getRecordId() != null
                && recordId == null) {

            throw new IllegalArgumentException(
                    "recordId must not be blank"
            );
        }

        if (recordId != null
                && recordId.length() > MAX_RECORD_ID_LENGTH) {

            throw new IllegalArgumentException(
                    "recordId must not exceed "
                            + MAX_RECORD_ID_LENGTH + " characters"
            );
        }

        // DEVICE ID
        // Required when recordId is provided.

        String deviceId = normalize(request.getDeviceId());

        if (recordId != null && deviceId == null) {

            throw new IllegalArgumentException(
                    "deviceId is required when recordId is provided"
            );
        }

        if (deviceId != null
                && deviceId.length() > MAX_DEVICE_ID_LENGTH) {

            throw new IllegalArgumentException(
                    "deviceId must not exceed "
                            + MAX_DEVICE_ID_LENGTH + " characters"
            );
        }
    }

    // ==========================================
    // RECORD IDENTITY VALIDATION
    // ==========================================

    private void validateRecordIdentity(
            String recordId,
            String deviceId
    ) {

        if (recordId == null || recordId.isBlank()) {

            throw new IllegalArgumentException(
                    "recordId is required"
            );
        }

        if (deviceId == null || deviceId.isBlank()) {

            throw new IllegalArgumentException(
                    "deviceId is required"
            );
        }
    }

    // ==========================================
    // UNIT NORMALIZATION
    // ==========================================

    private String normalizeUnit(String unit) {

        if (unit == null || unit.isBlank()) {

            throw new IllegalArgumentException(
                    "unit is required"
            );
        }

        String normalized = unit.trim();

        if (!EXPECTED_UNIT.equals(normalized)) {

            throw new IllegalArgumentException(
                    "Rain sensor unit must be %"
            );
        }

        return EXPECTED_UNIT;
    }

    // ==========================================
    // STRING NORMALIZATION
    // ==========================================

    private String normalize(String value) {

        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
