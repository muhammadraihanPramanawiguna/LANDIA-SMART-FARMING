
package com.landiafarm.rainservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.landiafarm.rainservice.dto.RainReadingRequest;
import com.landiafarm.rainservice.entity.RainReading;
import com.landiafarm.rainservice.service.RainReadingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rain")
public class RainReadingController {

    private final RainReadingService rainReadingService;

    public RainReadingController(
            RainReadingService rainReadingService) {

        this.rainReadingService = rainReadingService;
    }

    // CREATE RAIN READING

    @PostMapping("/readings")
    public ResponseEntity<RainReading> createReading(
            @Valid @RequestBody RainReadingRequest request) {

        RainReading saved = rainReadingService.save(request);

        return ResponseEntity.ok(saved);
    }

    // GET ALL RAIN READINGS

    @GetMapping("/readings")
    public ResponseEntity<List<RainReading>> getAllReadings() {

        return ResponseEntity.ok(
                rainReadingService.findAll()
        );
    }

    // GET READINGS BY SENSOR ID

    @GetMapping("/readings/{sensorId}")
    public ResponseEntity<List<RainReading>> getBySensorId(
            @PathVariable String sensorId) {

        return ResponseEntity.ok(
                rainReadingService.findBySensorId(sensorId)
        );
    }

    // GET LATEST READING

    @GetMapping("/readings/{sensorId}/latest")
    public ResponseEntity<RainReading> getLatest(
            @PathVariable String sensorId) {

        RainReading latest =
                rainReadingService.findLatest(sensorId);

        if (latest == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(latest);
    }
}
