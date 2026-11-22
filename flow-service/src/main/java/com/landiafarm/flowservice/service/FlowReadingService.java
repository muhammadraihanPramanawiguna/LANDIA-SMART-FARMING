package com.landiafarm.flowservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landiafarm.flowservice.dto.FlowReadingRequest;
import com.landiafarm.flowservice.entity.FlowReading;
import com.landiafarm.flowservice.exception.FlowReadingConflictException;
import com.landiafarm.flowservice.repository.FlowReadingRepository;

@Service
public class FlowReadingService {

    private static final String FLOW_UNIT = "L/min";

    private final FlowReadingRepository repository;

    public FlowReadingService(FlowReadingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public FlowReading save(FlowReadingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request tidak boleh null");
        }

        String sensorId = normalizeRequired(request.getSensorId(), "sensorId");
        String unit = normalizeRequired(request.getUnit(), "unit");
        Double value = request.getValue();
        Double totalVolumeLiters = request.getTotalVolumeLiters();

        if (value == null || !Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException("value harus angka finite dan tidak boleh negatif");
        }
        if (!FLOW_UNIT.equals(unit)) {
            throw new IllegalArgumentException("unit flow harus L/min");
        }
        if (totalVolumeLiters != null
                && (!Double.isFinite(totalVolumeLiters) || totalVolumeLiters < 0)) {
            throw new IllegalArgumentException(
                    "totalVolumeLiters harus angka finite dan tidak boleh negatif");
        }

        LocalDateTime now = truncateToMicros(LocalDateTime.now());
        LocalDateTime recordedAt = request.getRecordedAt() == null
                ? now
                : truncateToMicros(request.getRecordedAt());

        String rawRecordId = request.getRecordId();
        if (rawRecordId != null && rawRecordId.isBlank()) {
            throw new IllegalArgumentException("recordId tidak boleh kosong jika dikirim");
        }
        String recordId = rawRecordId == null ? null : rawRecordId.trim();
        String deviceId = normalizeOptional(request.getDeviceId());

        // Kompatibilitas untuk REST/payload lama tanpa recordId.
        if (recordId == null) {
            FlowReading reading = new FlowReading();
            reading.setSensorId(sensorId);
            reading.setValue(value);
            reading.setUnit(unit);
            reading.setTotalVolumeLiters(totalVolumeLiters);
            reading.setRecordedAt(recordedAt);
            reading.setCreatedAt(now);
            return repository.save(reading);
        }

        if (recordId.length() > 64) {
            throw new IllegalArgumentException("recordId maksimal 64 karakter");
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId wajib diisi jika recordId diberikan");
        }
        if (deviceId.length() > 100) {
            throw new IllegalArgumentException("deviceId maksimal 100 karakter");
        }

        repository.insertIfAbsent(
                recordId,
                deviceId,
                sensorId,
                value,
                unit,
                totalVolumeLiters,
                recordedAt,
                now);

        FlowReading stored = repository.findByRecordId(recordId)
                .orElseThrow(() -> new IllegalStateException(
                        "Data Flow tidak ditemukan setelah proses penyimpanan"));

        boolean samePayload =
                Objects.equals(stored.getDeviceId(), deviceId)
                && Objects.equals(stored.getSensorId(), sensorId)
                && Objects.equals(stored.getValue(), value)
                && Objects.equals(stored.getUnit(), unit)
                && Objects.equals(stored.getTotalVolumeLiters(), totalVolumeLiters)
                // Jika timestamp tidak dikirim perangkat, server-generated timestamp
                // boleh berbeda saat pesan QoS 1 dikirim ulang.
                && (request.getRecordedAt() == null
                    || Objects.equals(stored.getRecordedAt(), recordedAt));

        if (!samePayload) {
            throw new FlowReadingConflictException(
                    "recordId sudah digunakan untuk payload Flow yang berbeda");
        }

        return stored;
    }

    @Transactional(readOnly = true)
    public List<FlowReading> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<FlowReading> findBySensorId(String sensorId) {
        return repository.findBySensorIdOrderByRecordedAtDesc(sensorId);
    }

    @Transactional(readOnly = true)
    public FlowReading findLatest(String sensorId) {
        return repository.findFirstBySensorIdOrderByRecordedAtDesc(sensorId).orElse(null);
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " tidak boleh kosong");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LocalDateTime truncateToMicros(LocalDateTime value) {
        return value.withNano((value.getNano() / 1_000) * 1_000);
    }
}
