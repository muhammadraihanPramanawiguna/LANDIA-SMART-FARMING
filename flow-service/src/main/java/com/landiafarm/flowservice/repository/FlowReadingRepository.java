package com.landiafarm.flowservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.landiafarm.flowservice.entity.FlowReading;

public interface FlowReadingRepository
        extends JpaRepository<FlowReading, Long> {

    List<FlowReading> findBySensorIdOrderByRecordedAtDesc(
            String sensorId);

    Optional<FlowReading> findFirstBySensorIdOrderByRecordedAtDesc(
            String sensorId);
}