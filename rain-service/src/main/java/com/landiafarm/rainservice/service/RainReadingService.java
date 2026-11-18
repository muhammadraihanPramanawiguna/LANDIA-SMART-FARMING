
package com.landiafarm.rainservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landiafarm.rainservice.dto.RainReadingRequest;
import com.landiafarm.rainservice.entity.RainReading;
import com.landiafarm.rainservice.repository.RainReadingRepository;

@Service
public class RainReadingService {

    private final RainReadingRepository repository;

    public RainReadingService(RainReadingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RainReading save(RainReadingRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request tidak boleh null");
        }

        String sensorId = request.getSensorId();
        Double value = request.getValue();
        String unit = request.getUnit();

        if (sensorId == null || sensorId.isBlank()) {
            throw new IllegalArgumentException(
                    "sensorId tidak boleh kosong");
        }

        if (value == null || !Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "value harus berupa angka valid");
        }

        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException(
                    "unit tidak boleh kosong");
        }

        LocalDateTime recordedAt = request.getRecordedAt();

        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }

        String recordId = request.getRecordId();

        // Kompatibilitas data lama tanpa recordId.
if (recordId == null) {
    // Kompatibilitas data lama tanpa recordId.
    RainReading reading = new RainReading(
            sensorId.trim(),
            value,
            unit.trim(),
            recordedAt,
            LocalDateTime.now()
    );

    return repository.save(reading);
}

if (recordId.isBlank()) {
    throw new IllegalArgumentException(
            "recordId tidak boleh kosong"
    );
}

        recordId = recordId.trim();

        if (recordId.length() > 64) {
            throw new IllegalArgumentException(
                    "recordId maksimal 64 karakter");
        }

        String deviceId = request.getDeviceId();

        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException(
                    "deviceId wajib diisi untuk data dengan recordId");
        }

        deviceId = deviceId.trim();

        if (deviceId.length() > 100) {
            throw new IllegalArgumentException(
                    "deviceId maksimal 100 karakter");
        }

        sensorId = sensorId.trim();
        unit = unit.trim();

        LocalDateTime createdAt = LocalDateTime.now();

        repository.insertIfAbsent(
                recordId,
                deviceId,
                sensorId,
                value,
                unit,
                recordedAt,
                createdAt
        );

        RainReading stored = repository.findByRecordId(recordId)
                .orElseThrow(() -> new IllegalStateException(
                        "Data rain tidak ditemukan setelah proses penyimpanan"));

        boolean samePayload =
                Objects.equals(stored.getDeviceId(), deviceId)
                && Objects.equals(stored.getSensorId(), sensorId)
                && Objects.equals(stored.getValue(), value)
                && Objects.equals(stored.getUnit(), unit);

        if (!samePayload) {
            throw new IllegalArgumentException(
                    "recordId sudah digunakan untuk payload yang berbeda");
        }

        return stored;
    }

    public List<RainReading> findAll() {
        return repository.findAll();
    }

    public List<RainReading> findBySensorId(String sensorId) {
        return repository
                .findBySensorIdOrderByRecordedAtDesc(sensorId);
    }

    public RainReading findLatest(String sensorId) {
        return repository
                .findFirstBySensorIdOrderByRecordedAtDesc(sensorId)
                .orElse(null);
    }
}
