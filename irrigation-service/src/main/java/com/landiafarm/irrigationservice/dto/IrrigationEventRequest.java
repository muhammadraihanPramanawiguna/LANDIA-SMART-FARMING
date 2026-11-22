package com.landiafarm.irrigationservice.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IrrigationEventRequest {

    @NotBlank(message = "recordId wajib diisi")
    @Size(max = 40, message = "recordId maksimal 40 karakter")
    private String recordId;

    @NotBlank(message = "deviceId wajib diisi")
    @Size(max = 100, message = "deviceId maksimal 100 karakter")
    private String deviceId;

    @NotBlank(message = "event wajib diisi")
    @Pattern(
        regexp = "IRRIGATION",
        message = "event harus IRRIGATION"
    )
    private String event;

    @NotBlank(message = "state wajib diisi")
    @Pattern(
        regexp = "STARTED|COMPLETED|LOW_WATER|SENSOR_ERROR|FLOW_ERROR|TIMEOUT|SKIPPED|STOPPED",
        message = "state irigasi tidak dikenal"
    )
    private String state;

    private LocalDateTime timestamp;

    public IrrigationEventRequest() {
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

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}