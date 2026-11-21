package com.landiafarm.soilservice.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landiafarm.soilservice.dto.SoilReadingRequest;
import com.landiafarm.soilservice.entity.SoilReading;
import com.landiafarm.soilservice.exception.SoilReadingConflictException;
import com.landiafarm.soilservice.repository.SoilReadingRepository;

@Service
public class SoilReadingService {

    private static final ZoneId LANDIA_TIME_ZONE =
            ZoneId.of("Asia/Jakarta");

    private static final String SOIL_UNIT = "%";

    private final SoilReadingRepository repository;

    public SoilReadingService(SoilReadingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SoilReading saveReading(SoilReadingRequest request) {

        validate(request);

        LocalDateTime receivedAt = nowInLandiaTime();

        String recordId = normalize(request.getRecordId());
        String deviceId = normalize(request.getDeviceId());
        String sensorId = request.getSensorId().trim();
        String unit = request.getUnit().trim();

        // Payload legacy tanpa recordId tetap dapat disimpan,
        // tetapi tidak memakai application-level ACK.
        if (recordId == null) {
            SoilReading legacyReading = new SoilReading();

            legacyReading.setDeviceId(deviceId);
            legacyReading.setSensorId(sensorId);
            legacyReading.setValue(request.getValue());
            legacyReading.setUnit(unit);
            legacyReading.setRecordedAt(
                    request.getRecordedAt() != null
                            ? truncateToPostgresPrecision(request.getRecordedAt())
                            : receivedAt
            );
            legacyReading.setCreatedAt(receivedAt);

            return repository.save(legacyReading);
        }

        if (recordId.length() > 64) {
            throw new IllegalArgumentException(
                    "recordId maksimal 64 karakter"
            );
        }

        if (deviceId == null) {
            throw new IllegalArgumentException(
                    "deviceId wajib diisi untuk data dengan recordId"
            );
        }

        if (deviceId.length() > 100) {
            throw new IllegalArgumentException(
                    "deviceId maksimal 100 karakter"
            );
        }

        // Nilai timestamp dari ESP32 diprioritaskan.
        // Fallback waktu server hanya untuk payload firmware lama.
        final boolean timestampProvided =
                request.getRecordedAt() != null;

        final LocalDateTime recordedAt =
                timestampProvided
                        ? truncateToPostgresPrecision(request.getRecordedAt())
                        : receivedAt;

        repository.insertIfAbsent(
                recordId,
                deviceId,
                sensorId,
                request.getValue(),
                unit,
                recordedAt,
                receivedAt
        );

        // Jika recordId sudah ada, ambil data yang tersimpan.
        // Ini membuat retry MQTT aman dan tidak menggandakan histori.
        SoilReading stored = repository.findByRecordId(recordId)
                .orElseThrow(() -> new IllegalStateException(
                        "Soil record tidak ditemukan setelah insert: "
                                + recordId
                ));

        boolean samePayload =
                Objects.equals(stored.getDeviceId(), deviceId)
                && Objects.equals(stored.getSensorId(), sensorId)
                && Double.compare(
                        stored.getValue(),
                        request.getValue()
                ) == 0
                && Objects.equals(stored.getUnit(), unit);

        // Timestamp dibandingkan jika dikirim oleh ESP32.
        // Untuk payload lama tanpa recordedAt, backend tidak
        // membandingkan timestamp fallback yang berubah saat retry.
        if (timestampProvided) {
            samePayload = samePayload
                    && Objects.equals(
                            stored.getRecordedAt(),
                            recordedAt
                    );
        }

        if (!samePayload) {
            throw new SoilReadingConflictException(
                    "recordId sudah digunakan untuk payload soil yang berbeda: "
                            + recordId
            );
        }

        return stored;
    }

    @Transactional(readOnly = true)
    public List<SoilReading> getAllReadings() {
        return repository.findAllByOrderByRecordedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<SoilReading> getLatestReading(String sensorId) {

        if (sensorId == null || sensorId.isBlank()) {
            throw new IllegalArgumentException(
                    "sensorId wajib diisi"
            );
        }

        return repository.findFirstBySensorIdOrderByRecordedAtDesc(
                sensorId.trim()
        );
    }

    private void validate(SoilReadingRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request tidak boleh null"
            );
        }

        if (request.getRecordId() != null
                && request.getRecordId().isBlank()) {
            throw new IllegalArgumentException(
                    "recordId tidak boleh kosong"
            );
        }

        if (request.getSensorId() == null
                || request.getSensorId().isBlank()) {
            throw new IllegalArgumentException(
                    "sensorId wajib diisi"
            );
        }

        if (request.getSensorId().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "sensorId maksimal 100 karakter"
            );
        }

        if (request.getValue() == null
                || !Double.isFinite(request.getValue())) {
            throw new IllegalArgumentException(
                    "value harus berupa angka valid"
            );
        }

        if (request.getValue() < 0.0
                || request.getValue() > 100.0) {
            throw new IllegalArgumentException(
                    "soil moisture harus berada pada rentang 0 sampai 100"
            );
        }

        if (request.getUnit() == null
                || request.getUnit().isBlank()) {
            throw new IllegalArgumentException(
                    "unit wajib diisi"
            );
        }

        if (!SOIL_UNIT.equals(request.getUnit().trim())) {
            throw new IllegalArgumentException(
                    "unit soil moisture harus menggunakan %"
            );
        }

        if (request.getDeviceId() != null
                && request.getDeviceId().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "deviceId maksimal 100 karakter"
            );
        }

        if (request.getRecordId() != null
                && request.getRecordId().trim().length() > 64) {
            throw new IllegalArgumentException(
                    "recordId maksimal 64 karakter"
            );
        }
    }

    private LocalDateTime nowInLandiaTime() {
        return LocalDateTime.now(LANDIA_TIME_ZONE);
    }

    // PostgreSQL timestamp default precision is microseconds.
    // Normalize incoming nanoseconds before insert and duplicate comparison.
    private LocalDateTime truncateToPostgresPrecision(
            LocalDateTime dateTime
    ) {
        int microsecondNanos =
                (dateTime.getNano() / 1_000) * 1_000;

        return dateTime.withNano(microsecondNanos);
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
