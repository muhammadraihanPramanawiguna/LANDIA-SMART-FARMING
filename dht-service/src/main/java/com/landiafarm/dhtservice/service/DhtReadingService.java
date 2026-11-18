
package com.landiafarm.dhtservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public DhtReading save(DhtReadingRequest request) {

        validate(request);

        LocalDateTime now = LocalDateTime.now();

        String recordId = request.getRecordId();

        // Kompatibilitas data lama tanpa recordId.
        if (recordId == null) {

            DhtReading reading = new DhtReading();

            reading.setDeviceId(normalize(request.getDeviceId()));
            reading.setSensorId(request.getSensorId().trim());
            reading.setTemperature(request.getTemperature());
            reading.setHumidity(request.getHumidity());

            reading.setRecordedAt(
                    request.getRecordedAt() != null
                            ? request.getRecordedAt()
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
                        : now;

        // Insert hanya jika recordId belum tersedia.
        repository.insertIfAbsent(
                normalizedRecordId,
                deviceId,
                sensorId,
                request.getTemperature(),
                request.getHumidity(),
                recordedAt,
                now
        );

        // Ambil record baru atau record lama jika duplikat.
        DhtReading stored = repository
                .findByRecordId(normalizedRecordId)
                .orElseThrow(() -> new IllegalStateException(
                        "Record not found after insert: "
                                + normalizedRecordId
                ));

        // Pastikan recordId yang sama memiliki payload sensor yang sama.
        boolean samePayload =
                deviceId.equals(stored.getDeviceId())
                && sensorId.equals(stored.getSensorId())
                && Double.compare(
                        stored.getTemperature(),
                        request.getTemperature()
                ) == 0
                && Double.compare(
                        stored.getHumidity(),
                        request.getHumidity()
                ) == 0;

        if (!samePayload) {
            throw new IllegalStateException(
                    "recordId reused with different payload: "
                            + normalizedRecordId
            );
        }

        return stored;
    }

    @Transactional(readOnly = true)
    public List<DhtReading> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DhtReading> findBySensorId(
            String sensorId) {

        return repository
                .findBySensorIdOrderByRecordedAtDesc(sensorId);
    }

    @Transactional(readOnly = true)
    public DhtReading findLatest(String sensorId) {

        return repository
                .findFirstBySensorIdOrderByRecordedAtDesc(sensorId)
                .orElse(null);
    }

    private void validate(DhtReadingRequest request) {

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

        if (request.getTemperature() == null
                || !Double.isFinite(request.getTemperature())) {
            throw new IllegalArgumentException(
                    "Valid temperature is required"
            );
        }

        if (request.getHumidity() == null
                || !Double.isFinite(request.getHumidity())
                || request.getHumidity() < 0
                || request.getHumidity() > 100) {
            throw new IllegalArgumentException(
                    "Humidity must be between 0 and 100"
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
