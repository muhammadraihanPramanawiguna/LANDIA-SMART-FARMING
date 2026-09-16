package com.landiafarm.soilservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.landiafarm.soilservice.entity.SoilReading;

public interface SoilReadingRepository extends JpaRepository<SoilReading, Long> {

    Optional<SoilReading> findFirstBySensorIdOrderByRecordedAtDesc(String sensorId);
}