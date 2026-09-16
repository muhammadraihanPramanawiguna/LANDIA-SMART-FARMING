package com.landiafarm.irrigationservice.dto;

import jakarta.validation.constraints.NotBlank;

public class IrrigationEventRequest {

    @NotBlank
    private String deviceId;

    @NotBlank
    private String command;

    public IrrigationEventRequest() {
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}