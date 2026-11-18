
package com.landiafarm.flowservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.landiafarm.flowservice.entity.FlowReading;

public interface FlowReadingRepository
        extends JpaRepository<FlowReading, Long> {

    List<FlowReading> findBySensorIdOrderByRecordedAtDesc(
            String sensorId);

    Optional<FlowReading> findFirstBySensorIdOrderByRecordedAtDesc(
            String sensorId);

    Optional<FlowReading> findByRecordId(String recordId);

    @Modifying
    @Query(value = """
            INSERT INTO flow_readings (
                record_id,
                device_id,
                sensor_id,
                value,
                unit,
                recorded_at,
                created_at
            )
            VALUES (
                :recordId,
                :deviceId,
                :sensorId,
                :value,
                :unit,
                :recordedAt,
                :createdAt
            )
            ON CONFLICT (record_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("recordId") String recordId,
            @Param("deviceId") String deviceId,
            @Param("sensorId") String sensorId,
            @Param("value") Double value,
            @Param("unit") String unit,
            @Param("recordedAt") LocalDateTime recordedAt,
            @Param("createdAt") LocalDateTime createdAt
    );
}
