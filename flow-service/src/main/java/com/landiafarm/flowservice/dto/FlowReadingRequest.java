package com.landiafarm.flowservice.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FlowReadingRequest {

    @NotBlank(message = "sensorId tidak boleh kosong")
    private String sensorId;

    @NotNull(message = "value tidak boleh null")
    private Double value;

    @NotBlank(message = "unit tidak boleh kosong")
    private String unit;

    private LocalDateTime recordedAt;

    public FlowReadingRequest() {
    }

    public FlowReadingRequest(
            String sensorId,
            Double value,
            String unit,
            LocalDateTime recordedAt) {

        this.sensorId = sensorId;
        this.value = value;
        this.unit = unit;
        this.recordedAt = recordedAt;
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