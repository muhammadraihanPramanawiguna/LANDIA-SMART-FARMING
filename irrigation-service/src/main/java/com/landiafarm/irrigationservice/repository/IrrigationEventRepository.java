package com.landiafarm.irrigationservice.repository;

import com.landiafarm.irrigationservice.entity.IrrigationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IrrigationEventRepository
        extends JpaRepository<IrrigationEvent, Long> {

    List<IrrigationEvent> findByDeviceIdOrderByExecutedAtDesc(
            String deviceId
    );
}