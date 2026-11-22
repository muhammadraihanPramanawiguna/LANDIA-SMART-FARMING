package com.landiafarm.irrigationservice.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "irrigation_events",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_irrigation_record_id",
            columnNames = "record_id"
        )
    }
)
public class IrrigationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

@Column(name = "record_id", length = 40)
private String recordId;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    /*
     * Mempertahankan nama kolom command dari schema lama
     * agar tidak memaksa migrasi penghapusan kolom.
     */
    @Column(name = "command", nullable = false)
    private String event;

    /*
     * Kolom status lama digunakan untuk menyimpan
     * state event dari ESP32.
     */
    @Column(name = "status", nullable = false, length = 30)
    private String state;

    /*
     * Kolom executed_at lama menyimpan timestamp
     * yang dikirim firmware, bukan bukti bahwa backend
     * mengendalikan atau mengeksekusi pompa.
     */
    @Column(name = "executed_at")
    private LocalDateTime timestamp;

    /*
     * Waktu event diterima backend.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime receivedAt;

    public IrrigationEvent() {
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

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}