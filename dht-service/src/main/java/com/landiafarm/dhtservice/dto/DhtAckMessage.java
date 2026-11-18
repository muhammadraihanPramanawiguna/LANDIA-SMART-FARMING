
package com.landiafarm.dhtservice.dto;

public record DhtAckMessage(
        String recordId,
        String deviceId,
        String sensorType,
        String status
) {
}
