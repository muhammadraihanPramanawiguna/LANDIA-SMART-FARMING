
package com.landiafarm.waterlevelservice.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        try {
            String payload =
                    message.getPayload().toString();

            log.info(
                    "Received water level MQTT message: {}",
                    payload
            );

            WaterLevelReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            WaterLevelReadingRequest.class
                    );

            // Simpan data dan tunggu transaksi selesai.
            service.save(request);

            recordId = request.getRecordId();

            // Data historis tanpa recordId tidak memerlukan ACK.
            if (recordId == null || recordId.isBlank()) {
                log.info(
                        "Legacy water level message stored without application ACK"
                );
                return;
            }

            String deviceId = request.getDeviceId();

            if (deviceId == null || deviceId.isBlank()) {
                log.error(
                        "Water level record stored but ACK cannot be published: deviceId is missing, recordId={}",
                        recordId
                );
                return;
            }

            recordId = recordId.trim();
            deviceId = deviceId.trim();

            // ACK dikirim setelah transaksi service selesai.
            ackPublisher.publishStored(recordId, deviceId);

            log.info(
                    "Water level record stored and ACK published: recordId={}",
                    recordId
            );

        } catch (Exception e) {
            log.error(
                    "Failed to process water level MQTT message. recordId={}. ACK not confirmed.",
                    recordId,
                    e
            );
        }
    }
}
