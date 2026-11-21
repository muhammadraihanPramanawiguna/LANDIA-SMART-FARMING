
package com.landiafarm.soilservice.mqtt;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.soilservice.dto.SoilReadingRequest;
import com.landiafarm.soilservice.service.SoilReadingService;

@Component
public class MqttSoilMessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(MqttSoilMessageListener.class);

    private final SoilReadingService soilReadingService;
    private final MqttSoilAckPublisher ackPublisher;
    private final ObjectMapper objectMapper;

    public MqttSoilMessageListener(
            SoilReadingService soilReadingService,
            MqttSoilAckPublisher ackPublisher,
            ObjectMapper objectMapper) {

        this.soilReadingService = soilReadingService;
        this.ackPublisher = ackPublisher;
        this.objectMapper = objectMapper;
    }

    public void handleMessage(Message<?> message) {

        String recordId = null;
        String deviceId = null;
        String payload = null;

        try {

            // 1. Validasi dan ekstrak payload.
            payload = extractPayload(message);

            if (payload.isBlank()) {
                throw new IllegalArgumentException(
                        "MQTT payload kosong"
                );
            }

            log.info(
                    "MQTT soil message received. Payload length={}",
                    payload.length()
            );

            // 2. Parse JSON menjadi SoilReadingRequest.
            SoilReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            SoilReadingRequest.class
                    );

            if (request == null) {
                throw new IllegalArgumentException(
                        "Soil request tidak boleh null"
                );
            }

            // 3. Normalisasi recordId.
            if (request.getRecordId() != null) {
                recordId = request.getRecordId().trim();
                request.setRecordId(recordId);
            }

            // 4. Normalisasi deviceId.
            if (request.getDeviceId() != null) {
                deviceId = request.getDeviceId().trim();
                request.setDeviceId(deviceId);
            }

            // 5. Tentukan apakah pesan membutuhkan ACK.
            boolean requiresAck =
                    recordId != null && !recordId.isBlank();

            if (requiresAck &&
                    (deviceId == null || deviceId.isBlank())) {

                throw new IllegalArgumentException(
                        "deviceId wajib diisi untuk record yang membutuhkan ACK"
                );
            }

            // 6. Simpan data ke PostgreSQL.
            soilReadingService.saveReading(request);

            log.info(
                    "Soil record successfully stored. recordId={}, deviceId={}",
                    recordId,
                    deviceId
            );

            // 7. Pesan legacy tidak menggunakan application ACK.
            if (!requiresAck) {

                log.info(
                        "Legacy soil message stored without application ACK"
                );

                return;
            }

            // 8. Kirim ACK hanya setelah proses penyimpanan berhasil.
            ackPublisher.publishStored(recordId, deviceId);

            log.info(
                    "Soil ACK successfully published. recordId={}, deviceId={}",
                    recordId,
                    deviceId
            );

        } catch (Exception e) {

            log.error(
                    "Failed to process soil MQTT message. recordId={}, deviceId={}, payload={}",
                    recordId,
                    deviceId,
                    previewPayload(payload),
                    e
            );
        }
    }

    private String extractPayload(Message<?> message) {

        if (message == null || message.getPayload() == null) {
            throw new IllegalArgumentException(
                    "MQTT message atau payload null"
            );
        }

        Object payload = message.getPayload();

        if (payload instanceof byte[]) {

            return new String(
                    (byte[]) payload,
                    StandardCharsets.UTF_8
            );
        }

        if (payload instanceof String) {
            return (String) payload;
        }

        throw new IllegalArgumentException(
                "Unsupported MQTT payload type: "
                        + payload.getClass().getName()
        );
    }

    private String previewPayload(String payload) {

        if (payload == null) {
            return "null";
        }

        // Batasi panjang log agar tidak membanjiri console.
        int maxLength = 300;

        if (payload.length() <= maxLength) {
            return payload;
        }

        return payload.substring(0, maxLength) + "...";
    }
}
