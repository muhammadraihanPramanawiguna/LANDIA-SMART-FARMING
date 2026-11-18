
package com.landiafarm.irrigationservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.landiafarm.irrigationservice.entity.IrrigationEvent;

public interface IrrigationEventRepository
        extends JpaRepository<IrrigationEvent, Long> {

    List<IrrigationEvent> findByDeviceIdOrderByExecutedAtDesc(
            String deviceId
    );

    List<IrrigationEvent> findByDeviceIdAndStatusOrderByCreatedAtDesc(
            String deviceId,
            String status
    );
}
