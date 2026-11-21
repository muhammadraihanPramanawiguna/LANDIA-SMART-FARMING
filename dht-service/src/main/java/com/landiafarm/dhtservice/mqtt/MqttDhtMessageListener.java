
package com.landiafarm.dhtservice.mqtt;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            MqttDhtAckPublisher ackPublisher
    ) {
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
                        "DHT MQTT payload tidak boleh kosong"
                );
            }

            log.info(
                    "DHT MQTT message received. Payload length={}",
                    payload.length()
            );

            // 2. Parse JSON.
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

            boolean requiresAck =
                    recordId != null && !recordId.isBlank();

            if (requiresAck &&
                    (deviceId == null || deviceId.isBlank())) {
                throw new IllegalArgumentException(
                        "deviceId wajib diisi untuk record yang membutuhkan ACK"
                );
            }

            // 5. Simpan ke PostgreSQL.
            service.save(request);

            log.info(
                    "DHT record successfully stored. recordId={}",
                    recordId
            );

            // 6. Data legacy tidak membutuhkan ACK.
            if (!requiresAck) {
                log.info(
                        "Legacy DHT message stored without application ACK"
                );
                return;
            }

            // 7. ACK hanya setelah save berhasil.
            ackPublisher.publishStored(recordId, deviceId);

            log.info(
                    "DHT ACK successfully published. recordId={}, deviceId={}",
                    recordId,
                    deviceId
            );

        } catch (Exception e) {
            log.error(
                    "Failed to process DHT MQTT message. "
                            + "recordId={}, deviceId={}, payload={}. "
                            + "ACK not confirmed.",
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

        int maxLength = 300;

        if (payload.length() <= maxLength) {
            return payload;
        }

        return payload.substring(0, maxLength) + "...";
    }
}
