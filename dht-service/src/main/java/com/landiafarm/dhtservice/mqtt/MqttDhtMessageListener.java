
package com.landiafarm.dhtservice.mqtt;

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
            LoggerFactory.getLogger(
                    MqttDhtMessageListener.class
            );

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

        try {
            String payload =
                    message.getPayload().toString();

            log.info(
                    "Received DHT MQTT message: {}",
                    payload
            );

            DhtReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            DhtReadingRequest.class
                    );

            // Simpan data dan tunggu transaksi selesai.
            service.save(request);

            recordId = request.getRecordId();

            // Data lama tanpa recordId tidak membutuhkan ACK.
            if (recordId == null || recordId.isBlank()) {
                log.info(
                        "Legacy DHT message stored without application ACK"
                );
                return;
            }

            String deviceId = request.getDeviceId();

            if (deviceId == null || deviceId.isBlank()) {
                log.error(
                        "DHT record stored but ACK cannot be published: deviceId is missing, recordId={}",
                        recordId
                );
                return;
            }

            recordId = recordId.trim();
            deviceId = deviceId.trim();

            // ACK hanya dikirim setelah transaksi service selesai.
            ackPublisher.publishStored(recordId, deviceId);

            log.info(
                    "DHT record stored and ACK published: recordId={}",
                    recordId
            );

        } catch (Exception e) {
            log.error(
                    "Failed to process DHT MQTT message. recordId={}. ACK not confirmed.",
                    recordId,
                    e
            );
        }
    }
}
