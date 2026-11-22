
package com.landiafarm.soilservice.mqtt;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
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

            // 1. Ambil MQTT manual acknowledgment.
            SimpleAcknowledgment acknowledgment =
                    StaticMessageHeaderAccessor.getAcknowledgment(message);

            if (acknowledgment == null) {
                throw new IllegalStateException(
                        "MQTT manual acknowledgment tidak tersedia"
                );
            }

            // 2. Ekstrak payload.
            payload = extractPayload(message);

            if (payload.isBlank()) {
                throw new IllegalArgumentException(
                        "MQTT soil payload tidak boleh kosong"
                );
            }

            // 3. Parse JSON.
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

            // 4. Normalisasi identitas.
            recordId = normalize(request.getRecordId());
            deviceId = normalize(request.getDeviceId());

            request.setRecordId(recordId);
            request.setDeviceId(deviceId);

            boolean requiresAck =
                    recordId != null;

            if (requiresAck && deviceId == null) {
                throw new IllegalArgumentException(
                        "deviceId wajib diisi jika recordId tersedia"
                );
            }

            // 5. Simpan data secara idempotent.
            soilReadingService.saveReading(request);

            log.info(
                    "Soil record stored. recordId={}, deviceId={}",
                    recordId,
                    deviceId
            );

            // 6. Application ACK setelah penyimpanan berhasil.
            if (requiresAck) {

                ackPublisher.publishStored(
                        recordId,
                        deviceId
                );

                log.info(
                        "Soil Application ACK published. recordId={}",
                        recordId
                );
            }

            // 7. MQTT Protocol ACK setelah seluruh proses berhasil.
            acknowledgment.acknowledge();

            log.info(
                    "Soil MQTT message acknowledged. recordId={}",
                    recordId
            );

        } catch (Exception exception) {

            // Jangan acknowledge jika penyimpanan atau
            // Application ACK gagal.
            log.error(
                    "Failed to process Soil MQTT. "
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

        return payload.length() <= maxLength
                ? payload
                : payload.substring(0, maxLength) + "...";
    }
}
