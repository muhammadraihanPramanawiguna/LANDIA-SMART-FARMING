package com.landiafarm.dhtservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.landiafarm.dhtservice.entity.DhtReading;

public interface DhtReadingRepository
        extends JpaRepository<DhtReading, Long> {

    List<DhtReading> findBySensorIdOrderByRecordedAtDesc(
            String sensorId
    );

    Optional<DhtReading> findFirstBySensorIdOrderByRecordedAtDesc(
            String sensorId
    );
}