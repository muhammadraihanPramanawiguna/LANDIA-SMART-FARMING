
package com.landiafarm.irrigationservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.landiafarm.irrigationservice.dto.IrrigationEventRequest;
import com.landiafarm.irrigationservice.entity.IrrigationEvent;
import com.landiafarm.irrigationservice.mqtt.MqttIrrigationCommandPublisher;
import com.landiafarm.irrigationservice.repository.IrrigationEventRepository;

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

        if (request == null) {
            throw new IllegalArgumentException(
                    "Request tidak boleh null");
        }

        String deviceId = request.getDeviceId();
        String command = request.getCommand();

        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException(
                    "deviceId tidak boleh kosong");
        }

        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException(
                    "command tidak boleh kosong");
        }

        deviceId = deviceId.trim();
        command = command.trim();

        if (deviceId.length() > 100) {
            throw new IllegalArgumentException(
                    "deviceId maksimal 100 karakter");
        }

        LocalDateTime now = LocalDateTime.now();

        IrrigationEvent event = new IrrigationEvent();

        event.setDeviceId(deviceId);
        event.setCommand(command);
        event.setStatus("PENDING");
        event.setCreatedAt(now);

        // Belum ada konfirmasi eksekusi dari ESP32.
        event.setExecutedAt(null);

        /*
         * Simpan event terlebih dahulu.
         * Repository.save() menyelesaikan transaksi repository
         * sebelum pemanggilan publish berikutnya.
         */
        IrrigationEvent savedEvent = repository.save(event);

        /*
         * Kirim command setelah event tersimpan.
         * Jika publish gagal, status tetap PENDING.
         */
        mqttPublisher.publish(
                deviceId,
                command
        );

        return savedEvent;
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
