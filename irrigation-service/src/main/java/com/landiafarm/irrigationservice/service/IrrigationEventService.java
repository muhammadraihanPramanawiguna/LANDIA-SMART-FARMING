package com.landiafarm.irrigationservice.service;

import com.landiafarm.irrigationservice.dto.IrrigationEventRequest;
import com.landiafarm.irrigationservice.entity.IrrigationEvent;
import com.landiafarm.irrigationservice.mqtt.MqttIrrigationCommandPublisher;
import com.landiafarm.irrigationservice.repository.IrrigationEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class IrrigationEventService {

    private final IrrigationEventRepository repository;
    private final MqttIrrigationCommandPublisher mqttPublisher;

    public IrrigationEventService(
            IrrigationEventRepository repository,
            MqttIrrigationCommandPublisher mqttPublisher) {

        this.repository = repository;
        this.mqttPublisher = mqttPublisher;
    }

    public IrrigationEvent execute(
            IrrigationEventRequest request) {

        LocalDateTime now = LocalDateTime.now();

        IrrigationEvent event = new IrrigationEvent();

        event.setDeviceId(request.getDeviceId());
        event.setCommand(request.getCommand());
        event.setExecutedAt(now);
        event.setCreatedAt(now);

        /*
         * Kirim command ke MQTT.
         */
        mqttPublisher.publish(
                request.getDeviceId(),
                request.getCommand()
        );

        /*
         * Simpan event setelah command dikirim.
         */
        return repository.save(event);
    }

    public List<IrrigationEvent> findAll() {
        return repository.findAll();
    }

    public List<IrrigationEvent> findByDeviceId(
            String deviceId) {

        return repository
                .findByDeviceIdOrderByExecutedAtDesc(deviceId);
    }
}