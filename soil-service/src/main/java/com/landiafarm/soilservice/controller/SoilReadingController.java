package com.landiafarm.soilservice.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.landiafarm.soilservice.dto.SoilReadingRequest;
import com.landiafarm.soilservice.entity.SoilReading;
import com.landiafarm.soilservice.service.SoilReadingService;

@RestController
@RequestMapping("/api/soil")
public class SoilReadingController {

    private final SoilReadingService soilReadingService;

    public SoilReadingController(SoilReadingService soilReadingService) {
        this.soilReadingService = soilReadingService;
    }

    @PostMapping("/readings")
    public SoilReading saveReading(@RequestBody SoilReadingRequest request) {
        return soilReadingService.saveReading(request);
    }

    @GetMapping("/readings")
    public List<SoilReading> getAllReadings() {
        return soilReadingService.getAllReadings();
    }

    @GetMapping("/readings/latest")
    public Optional<SoilReading> getLatestReading(
            @RequestParam String sensorId) {

        return soilReadingService.getLatestReading(sensorId);
    }
}