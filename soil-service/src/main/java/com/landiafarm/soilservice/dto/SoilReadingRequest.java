
package com.landiafarm.soilservice.dto;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SoilReadingRequest {

    @Size(max = 64, message = "recordId maksimal 64 karakter")
    private String recordId;

    @Size(max = 100, message = "deviceId maksimal 100 karakter")
    private String deviceId;

    @NotBlank(message = "sensorId wajib diisi")
    @Size(max = 100, message = "sensorId maksimal 100 karakter")
    private String sensorId;

    @NotNull(message = "value wajib diisi")
    @DecimalMin(value = "0.0", message = "soil moisture minimal 0")
    @DecimalMax(value = "100.0", message = "soil moisture maksimal 100")
    private Double value;

    @NotBlank(message = "unit wajib diisi")
    @Size(max = 16, message = "unit maksimal 16 karakter")
    private String unit;

    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime recordedAt;

    public SoilReadingRequest() {
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public static class FlexibleLocalDateTimeDeserializer
            extends JsonDeserializer<LocalDateTime> {

        private static final DateTimeFormatter LEGACY_FORMAT =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public LocalDateTime deserialize(
                JsonParser parser,
                DeserializationContext context
        ) throws IOException {

            String value = parser.getValueAsString();

            if (value == null || value.isBlank()) {
                return null;
            }

            try {
                // Format ISO: 2026-11-21T09:00:00
                return LocalDateTime.parse(value);

            } catch (DateTimeParseException ignored) {
                // Lanjut mencoba format firmware lama.
            }

            try {
                // Format firmware: 2026-11-21 09:00:00
                return LocalDateTime.parse(value, LEGACY_FORMAT);

            } catch (DateTimeParseException e) {
                throw context.weirdStringException(
                        value,
                        LocalDateTime.class,
                        "Timestamp harus menggunakan format "
                                + "yyyy-MM-dd'T'HH:mm:ss atau "
                                + "yyyy-MM-dd HH:mm:ss"
                );
            }
        }
    }
}
