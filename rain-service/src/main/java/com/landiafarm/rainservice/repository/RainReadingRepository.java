package com.landiafarm.rainservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.landiafarm.rainservice.entity.RainReading;

public interface RainReadingRepository
        extends JpaRepository<RainReading, Long> {

    List<RainReading> findBySensorIdOrderByRecordedAtDesc(
            String sensorId);

    Optional<RainReading> findFirstBySensorIdOrderByRecordedAtDesc(
            String sensorId);
}