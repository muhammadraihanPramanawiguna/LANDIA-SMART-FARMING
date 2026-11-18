
package com.landiafarm.rainservice.mqtt;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landiafarm.rainservice.dto.RainReadingRequest;
import com.landiafarm.rainservice.service.RainReadingService;

@Component
public class MqttRainMessageListener {

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

        try {
            String payload = message.getPayload().toString();

            System.out.println("MQTT Rain Message: " + payload);

            RainReadingRequest request =
                    objectMapper.readValue(
                            payload,
                            RainReadingRequest.class
                    );

            // Simpan dan pastikan transaksi database selesai.
            rainReadingService.save(request);

            String recordId = request.getRecordId();
            String deviceId = request.getDeviceId();

            // Data lama tanpa recordId tidak menggunakan ACK aplikasi.
            if (recordId == null || recordId.isBlank()) {
                System.out.println(
                        "Rain reading tersimpan (legacy, tanpa ACK)."
                );
                return;
            }

            if (deviceId == null || deviceId.isBlank()) {
                throw new IllegalArgumentException(
                        "deviceId tidak tersedia untuk pengiriman ACK"
                );
            }

            // ACK dikirim setelah penyimpanan berhasil.
            ackPublisher.publishStored(
                    recordId.trim(),
                    deviceId.trim()
            );

            System.out.println(
                    "Rain reading tersimpan dan ACK berhasil dikirim: "
                            + recordId
            );

        } catch (Exception e) {
            System.err.println(
                    "Gagal memproses MQTT rain message: "
                            + e.getMessage()
            );
        }
    }
}
