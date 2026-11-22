package com.landiafarm.flowservice.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class FlowReadingRequest {

    @Size(max = 64, message = "recordId maksimal 64 karakter")
    private String recordId;

    @Size(max = 100, message = "deviceId maksimal 100 karakter")
    private String deviceId;

    @NotBlank(message = "sensorId tidak boleh kosong")
    @Size(max = 100, message = "sensorId maksimal 100 karakter")
    private String sensorId;

    @NotNull(message = "value tidak boleh null")
    @DecimalMin(value = "0.0", message = "value tidak boleh negatif")
    private Double value;

    @NotBlank(message = "unit tidak boleh kosong")
    @Pattern(regexp = "L/min", message = "unit harus L/min")
    private String unit;

    // Volume kumulatif yang dihitung ESP32 sejak penghitung terakhir di-reset.
    // Nullable agar payload firmware lama tetap kompatibel selama migrasi.
    @DecimalMin(value = "0.0", message = "totalVolumeLiters tidak boleh negatif")
    private Double totalVolumeLiters;

    private LocalDateTime recordedAt;

    public FlowReadingRequest() {
    }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getTotalVolumeLiters() { return totalVolumeLiters; }
    public void setTotalVolumeLiters(Double totalVolumeLiters) {
        this.totalVolumeLiters = totalVolumeLiters;
    }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
