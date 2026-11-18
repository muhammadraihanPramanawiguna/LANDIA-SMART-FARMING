
package com.landiafarm.flowservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landiafarm.flowservice.dto.FlowReadingRequest;
import com.landiafarm.flowservice.entity.FlowReading;
import com.landiafarm.flowservice.repository.FlowReadingRepository;

@Service
public class FlowReadingService {

    private final FlowReadingRepository repository;

    public FlowReadingService(
            FlowReadingRepository repository) {

        this.repository = repository;
    }

    @Transactional
    public FlowReading save(FlowReadingRequest request) {

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

            FlowReading reading = new FlowReading(
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
                    "recordId tidak boleh kosong");
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

        repository.insertIfAbsent(
                recordId,
                deviceId,
                sensorId,
                value,
                unit,
                recordedAt,
                LocalDateTime.now()
        );

        FlowReading stored = repository.findByRecordId(recordId)
                .orElseThrow(() -> new IllegalStateException(
                        "Data Flow tidak ditemukan setelah penyimpanan"));

        boolean samePayload =
                Objects.equals(stored.getDeviceId(), deviceId)
                && Objects.equals(stored.getSensorId(), sensorId)
                && Objects.equals(stored.getValue(), value)
                && Objects.equals(stored.getUnit(), unit);

        if (!samePayload) {
            throw new IllegalArgumentException(
                    "recordId sudah digunakan untuk payload berbeda");
        }

        return stored;
    }

    public List<FlowReading> findAll() {
        return repository.findAll();
    }

    public List<FlowReading> findBySensorId(
            String sensorId) {

        return repository
                .findBySensorIdOrderByRecordedAtDesc(sensorId);
    }

    public FlowReading findLatest(String sensorId) {

        return repository
                .findFirstBySensorIdOrderByRecordedAtDesc(sensorId)
                .orElse(null);
    }
}
