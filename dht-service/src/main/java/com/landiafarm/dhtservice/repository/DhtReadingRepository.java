
package com.landiafarm.dhtservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.landiafarm.dhtservice.entity.DhtReading;

public interface DhtReadingRepository
        extends JpaRepository<DhtReading, Long> {

    List<DhtReading> findBySensorIdOrderByRecordedAtDesc(
            String sensorId
    );

    Optional<DhtReading> findFirstBySensorIdOrderByRecordedAtDesc(
            String sensorId
    );

    Optional<DhtReading> findByRecordId(String recordId);

    @Modifying
    @Query(value = """
            INSERT INTO dht_readings
                (
                    record_id,
                    device_id,
                    sensor_id,
                    temperature,
                    humidity,
                    recorded_at,
                    created_at
                )
            VALUES
                (
                    :recordId,
                    :deviceId,
                    :sensorId,
                    :temperature,
                    :humidity,
                    :recordedAt,
                    :createdAt
                )
            ON CONFLICT (record_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("recordId") String recordId,
            @Param("deviceId") String deviceId,
            @Param("sensorId") String sensorId,
            @Param("temperature") Double temperature,
            @Param("humidity") Double humidity,
            @Param("recordedAt") LocalDateTime recordedAt,
            @Param("createdAt") LocalDateTime createdAt
    );
}
