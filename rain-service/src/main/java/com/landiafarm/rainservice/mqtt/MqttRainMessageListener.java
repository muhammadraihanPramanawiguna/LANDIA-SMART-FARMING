
package com.landiafarm.rainservice.mqtt;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.rainservice.dto.RainReadingRequest;
import com.landiafarm.rainservice.service.RainReadingService;

@Component
public class MqttRainMessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(MqttRainMessageListener.class);

    private final ObjectMapper objectMapper;
    private final RainReadingService rainReadingService;
    private final MqttRainAckPublisher ackPublisher;

    public MqttRainMessageListener(
            ObjectMapper objectMapper,
            RainReadingService rainReadingService,
            MqttRainAckPublisher ackPublisher) {

        this.objectMapper = objectMapper;
        this.rainReadingService = rainReadingService;
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

            // 2. Parse JSON.
            RainReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            RainReadingRequest.class
                    );

            if (request == null) {
                throw new IllegalArgumentException(
                        "Invalid rain MQTT payload"
                );
            }

            // 3. Normalisasi identitas.
            recordId = normalize(request.getRecordId());
            deviceId = normalize(request.getDeviceId());

            request.setRecordId(recordId);
            request.setDeviceId(deviceId);

            if (recordId != null && deviceId == null) {
                throw new IllegalArgumentException(
                        "deviceId is required when recordId is provided"
                );
            }

            // 4. Simpan atau verifikasi data secara idempotent.
            rainReadingService.save(request);

            // 5. Kirim Application ACK setelah penyimpanan berhasil.
            if (recordId != null) {

                ackPublisher.publishStored(
                        recordId,
                        deviceId
                );

                log.info(
                        "Rain Application ACK published. recordId={}, deviceId={}",
                        recordId,
                        deviceId
                );

            } else {

                log.info(
                        "Legacy rain record stored without Application ACK"
                );
            }

            // 6. Ambil callback MQTT Manual ACK.
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
                    "Rain MQTT message acknowledged. recordId={}, deviceId={}",
                    recordId,
                    deviceId
            );

        } catch (Exception exception) {

            // Jangan menyelesaikan MQTT ACK ketika proses gagal.
            log.error(
                    "Failed to process rain MQTT message. "
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
            return new String(bytes, StandardCharsets.UTF_8);
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
