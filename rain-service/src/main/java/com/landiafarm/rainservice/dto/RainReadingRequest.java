
package com.landiafarm.rainservice.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RainReadingRequest {

    @Size(max = 64, message = "recordId maksimal 64 karakter")
    private String recordId;

    @Size(max = 100, message = "deviceId maksimal 100 karakter")
    private String deviceId;

    @NotBlank(message = "sensorId tidak boleh kosong")
    @Size(max = 100, message = "sensorId maksimal 100 karakter")
    private String sensorId;

    @NotNull(message = "value tidak boleh null")
    @DecimalMin(value = "0.0", message = "value minimal 0")
    @DecimalMax(value = "100.0", message = "value maksimal 100")
    private Double value;

    @NotBlank(message = "unit tidak boleh kosong")
    @Pattern(regexp = "%", message = "unit harus menggunakan %")
    private String unit;

    private LocalDateTime recordedAt;

    public RainReadingRequest() {
    }

    public RainReadingRequest(
            String sensorId,
            Double value,
            String unit,
            LocalDateTime recordedAt) {

        this.sensorId = sensorId;
        this.value = value;
        this.unit = unit;
        this.recordedAt = recordedAt;
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
}
