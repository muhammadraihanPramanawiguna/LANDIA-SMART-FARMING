package com.landiafarm.waterlevelservice.repository;

import com.landiafarm.waterlevelservice.entity.WaterLevelReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaterLevelReadingRepository
        extends JpaRepository<WaterLevelReading, Long> {

    List<WaterLevelReading> findBySensorIdOrderByRecordedAtDesc(
            String sensorId
    );

    Optional<WaterLevelReading> findFirstBySensorIdOrderByRecordedAtDesc(
            String sensorId
    );
}