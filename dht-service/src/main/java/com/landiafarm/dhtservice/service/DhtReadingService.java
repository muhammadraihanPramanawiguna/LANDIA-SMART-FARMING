
package com.landiafarm.dhtservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landiafarm.dhtservice.dto.DhtReadingRequest;
import com.landiafarm.dhtservice.entity.DhtReading;
import com.landiafarm.dhtservice.exception.DhtReadingConflictException;
import com.landiafarm.dhtservice.repository.DhtReadingRepository;

@Service
public class DhtReadingService {

    private final DhtReadingRepository repository;

    public DhtReadingService(DhtReadingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DhtReading save(DhtReadingRequest request) {

        validate(request);

        LocalDateTime now = LocalDateTime.now();

        String recordId = normalize(request.getRecordId());
        String deviceId = normalize(request.getDeviceId());
        String sensorId = request.getSensorId().trim();

        LocalDateTime recordedAt =
                request.getRecordedAt() != null
                        ? request.getRecordedAt()
                        : now;

        // Kompatibilitas dengan data lama tanpa recordId.
        if (recordId == null) {

            DhtReading reading = new DhtReading();

            reading.setDeviceId(deviceId);
            reading.setSensorId(sensorId);
            reading.setTemperature(request.getTemperature());
            reading.setHumidity(request.getHumidity());
            reading.setRecordedAt(recordedAt);
            reading.setCreatedAt(now);

            return repository.save(reading);
        }

        // Validasi identitas perangkat.
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

        // Validasi panjang recordId.
        if (recordId.length() > 64) {
            throw new IllegalArgumentException(
                    "recordId must not exceed 64 characters"
            );
        }

        // Insert hanya jika recordId belum tersedia.
        repository.insertIfAbsent(
                recordId,
                deviceId,
                sensorId,
                request.getTemperature(),
                request.getHumidity(),
                recordedAt,
                now
        );

        // Ambil record baru atau record yang sudah tersimpan.
        DhtReading stored = repository.findByRecordId(recordId)
                .orElseThrow(() -> new IllegalStateException(
                        "Record not found after insert: " + recordId
                ));

        // Periksa apakah recordId digunakan dengan payload yang sama.
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
                ) == 0
                && (
                        request.getRecordedAt() == null
                        || request.getRecordedAt().equals(
                                stored.getRecordedAt()
                        )
                );

        if (!samePayload) {
            throw new DhtReadingConflictException(
                    "recordId already exists with different payload: "
                            + recordId
            );
        }

        // Payload identik: kembalikan record lama tanpa duplikasi.
        return stored;
    }

    @Transactional(readOnly = true)
    public List<DhtReading> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DhtReading> findBySensorId(String sensorId) {

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
    public DhtReading findLatest(String sensorId) {

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

        String recordId = request.getRecordId();

        if (recordId != null && recordId.isBlank()) {
            throw new IllegalArgumentException(
                    "recordId must not be blank"
            );
        }

        if (recordId != null && recordId.trim().length() > 64) {
            throw new IllegalArgumentException(
                    "recordId must not exceed 64 characters"
            );
        }

        String deviceId = request.getDeviceId();

        if (deviceId != null && deviceId.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "deviceId must not exceed 100 characters"
            );
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
