
package com.landiafarm.dhtservice.mqtt;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.SimpleAcknowledgment;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.dhtservice.dto.DhtReadingRequest;
import com.landiafarm.dhtservice.service.DhtReadingService;

@Component
public class MqttDhtMessageListener {

    private static final Logger log =
            LoggerFactory.getLogger(MqttDhtMessageListener.class);

    private final ObjectMapper objectMapper;
    private final DhtReadingService service;
    private final MqttDhtAckPublisher ackPublisher;

    public MqttDhtMessageListener(
            ObjectMapper objectMapper,
            DhtReadingService service,
            MqttDhtAckPublisher ackPublisher) {

        this.objectMapper = objectMapper;
        this.service = service;
        this.ackPublisher = ackPublisher;
    }

    public void handleMessage(Message<?> message) {

        String recordId = null;
        String deviceId = null;
        String payload = null;

        try {

            // 1. Ambil manual acknowledgment.
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
                        "DHT MQTT payload tidak boleh kosong"
                );
            }

            // 3. Parse JSON.
            DhtReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            DhtReadingRequest.class
                    );

            if (request == null) {
                throw new IllegalArgumentException(
                        "DHT request tidak boleh null"
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

            // 5. Simpan data.
            service.save(request);

            log.info(
                    "DHT record stored. recordId={}, deviceId={}",
                    recordId,
                    deviceId
            );

            // 6. Kirim Application ACK.
            if (requiresAck) {

                ackPublisher.publishStored(
                        recordId,
                        deviceId
                );

                log.info(
                        "DHT Application ACK published. recordId={}",
                        recordId
                );
            }

            // 7. Selesaikan MQTT Protocol ACK.
            acknowledgment.acknowledge();

            log.info(
                    "DHT MQTT message acknowledged. recordId={}",
                    recordId
            );

        } catch (Exception exception) {

            // Jangan acknowledge jika proses gagal.
            log.error(
                    "Failed to process DHT MQTT. "
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
