package com.landiafarm.irrigationservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.landiafarm.irrigationservice.entity.IrrigationEvent;

public interface IrrigationEventRepository
        extends JpaRepository<IrrigationEvent, Long> {

    Optional<IrrigationEvent> findByRecordId(String recordId);

    List<IrrigationEvent> findAllByOrderByReceivedAtDesc();

    List<IrrigationEvent> findByDeviceIdOrderByReceivedAtDesc(
            String deviceId
    );

    @Modifying
    @Query(
        value = """
            INSERT INTO irrigation_events (
                record_id,
                device_id,
                command,
                status,
                executed_at,
                created_at
            )
            VALUES (
                :recordId,
                :deviceId,
                :event,
                :state,
                :timestamp,
                :receivedAt
            )
            ON CONFLICT (record_id) DO NOTHING
            """,
        nativeQuery = true
    )
    int insertIfAbsent(
        @Param("recordId") String recordId,
        @Param("deviceId") String deviceId,
        @Param("event") String event,
        @Param("state") String state,
        @Param("timestamp") LocalDateTime timestamp,
        @Param("receivedAt") LocalDateTime receivedAt
    );
}