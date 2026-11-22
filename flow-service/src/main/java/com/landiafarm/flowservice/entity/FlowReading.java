package com.landiafarm.flowservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "flow_readings")
public class FlowReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", length = 64, unique = true)
    private String recordId;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "sensor_id", nullable = false, length = 100)
    private String sensorId;

    // Debit air sesaat dalam L/min.
    @Column(nullable = false)
    private Double value;

    @Column(nullable = false, length = 16)
    private String unit;

    // Volume kumulatif dari ESP32; nullable untuk data firmware lama.
    @Column(name = "total_volume_liters")
    private Double totalVolumeLiters;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FlowReading() {
    }

    public Long getId() { return id; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
