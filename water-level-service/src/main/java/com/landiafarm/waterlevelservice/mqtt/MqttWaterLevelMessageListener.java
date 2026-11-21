
package com.landiafarm.waterlevelservice.mqtt;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.waterlevelservice.dto.WaterLevelReadingRequest;
import com.landiafarm.waterlevelservice.service.WaterLevelReadingService;

@Component
public class MqttWaterLevelMessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    MqttWaterLevelMessageListener.class
            );

    private final ObjectMapper objectMapper;
    private final WaterLevelReadingService service;
    private final MqttWaterLevelAckPublisher ackPublisher;

    public MqttWaterLevelMessageListener(
            ObjectMapper objectMapper,
            WaterLevelReadingService service,
            MqttWaterLevelAckPublisher ackPublisher) {

        this.objectMapper = objectMapper;
        this.service = service;
        this.ackPublisher = ackPublisher;
    }

    public void handleMessage(Message<?> message) {

        String recordId = null;
        String deviceId = null;
        String payload = null;

        try {
            // 1. Ekstrak payload MQTT.
            payload = extractPayload(message);

            if (payload.isBlank()) {
                throw new IllegalArgumentException(
                        "MQTT payload must not be empty"
                );
            }

            // 2. Parse JSON menjadi DTO.
            WaterLevelReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            WaterLevelReadingRequest.class
                    );

            if (request == null) {
                throw new IllegalArgumentException(
                        "Invalid water level MQTT payload"
                );
            }

            // 3. Normalisasi identitas perangkat dan record.
            recordId = normalize(request.getRecordId());
            deviceId = normalize(request.getDeviceId());

            request.setRecordId(recordId);
            request.setDeviceId(deviceId);

            if (recordId != null && deviceId == null) {
                throw new IllegalArgumentException(
                        "deviceId is required when recordId is provided"
                );
            }

            // 4. Simpan atau verifikasi record secara idempotent.
            service.save(request);

            // 5. Publish Application ACK setelah penyimpanan berhasil.
            if (recordId != null) {

                ackPublisher.publishStored(
                        recordId,
                        deviceId
                );

                log.info(
                        "Application ACK published. recordId={}, deviceId={}",
                        recordId,
                        deviceId
                );

            } else {

                log.info(
                        "Legacy water level record stored without application ACK"
                );
            }

            // 6. Ambil callback MQTT manual acknowledgment.
            SimpleAcknowledgment acknowledgment =
                    StaticMessageHeaderAccessor.getAcknowledgment(
                            message
                    );

            if (acknowledgment == null) {
                throw new IllegalStateException(
                        "MQTT acknowledgment callback is missing"
                );
            }

            // 7. Selesaikan MQTT Protocol ACK.
            acknowledgment.acknowledge();

            log.info(
                    "MQTT message acknowledged successfully. recordId={}, deviceId={}",
                    recordId,
                    deviceId
            );

        } catch (Exception exception) {

            // Jangan menyelesaikan manual ACK jika pemrosesan gagal.
            log.error(
                    "Failed to process water level MQTT message. "
                            + "recordId={}, deviceId={}, payload={}. "
                            + "MQTT ACK not completed.",
                    recordId,
                    deviceId,
                    previewPayload(payload),
                    exception
            );
        }
    }

    private String extractPayload(Message<?> message) {

        if (message == null || message.getPayload() == null) {
            throw new IllegalArgumentException(
                    "MQTT message and payload must not be null"
            );
        }

        Object rawPayload = message.getPayload();

        if (rawPayload instanceof byte[] bytes) {
            return new String(
                    bytes,
                    StandardCharsets.UTF_8
            );
        }

        if (rawPayload instanceof String text) {
            return text;
        }

        throw new IllegalArgumentException(
                "Unsupported MQTT payload type: "
                        + rawPayload.getClass().getName()
        );
    }

    private String normalize(String value) {

        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private String previewPayload(String payload) {

        if (payload == null) {
            return "null";
        }

        int maxLength = 300;

        if (payload.length() <= maxLength) {
            return payload;
        }

        return payload.substring(0, maxLength) + "...";
    }
}
