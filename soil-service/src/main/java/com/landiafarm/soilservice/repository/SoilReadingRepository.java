
package com.landiafarm.soilservice.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.landiafarm.soilservice.entity.SoilReading;

public interface SoilReadingRepository
        extends JpaRepository<SoilReading, Long> {

    Optional<SoilReading> findFirstBySensorIdOrderByRecordedAtDesc(
            String sensorId
    );

    Optional<SoilReading> findByRecordId(String recordId);

   
@Modifying
@Query(value = """
        INSERT INTO soil_readings
            (
                record_id,
                device_id,
                sensor_id,
                value,
                unit,
                recorded_at,
                created_at
            )
        VALUES
            (
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
