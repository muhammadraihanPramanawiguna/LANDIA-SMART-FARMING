package com.landiafarm.irrigationservice.exception;

public class IrrigationEventConflictException
        extends RuntimeException {

    public IrrigationEventConflictException(String message) {
        super(message);
    }
}