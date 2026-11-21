
package com.landiafarm.waterlevelservice.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class WaterLevelReadingRequest {

    @Size(max = 64, message = "recordId must not exceed 64 characters")
    @Pattern(regexp = ".*\\S.*", message = "recordId must not be blank")
    private String recordId;

    @Size(max = 100, message = "deviceId must not exceed 100 characters")
    private String deviceId;

    @NotBlank(message = "sensorId is required")
    @Size(max = 100, message = "sensorId must not exceed 100 characters")
    private String sensorId;

    @NotNull(message = "value is required")
    @DecimalMin(value = "0.0", message = "value must not be negative")
    private Double value;

    @NotBlank(message = "unit is required")
    @Size(max = 10, message = "unit must not exceed 10 characters")
    private String unit;

    private LocalDateTime recordedAt;

    public WaterLevelReadingRequest() {
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
