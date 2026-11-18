
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

    @Column(nullable = false)
    private String sensorId;

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public FlowReading() {
    }

    public FlowReading(
            String sensorId,
            Double value,
            String unit,
            LocalDateTime recordedAt,
            LocalDateTime createdAt) {

        this.sensorId = sensorId;
        this.value = value;
        this.unit = unit;
        this.recordedAt = recordedAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
