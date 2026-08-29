package com.crossfade.crossfade_backend.controller;

import com.crossfade.crossfade_backend.response.CompatibilityResponse;
import com.crossfade.crossfade_backend.service.CompatibilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CompatibilityController {

    @Autowired
    CompatibilityService service;

    @GetMapping("/compatibility")
    public CompatibilityResponse getCompatibility(@RequestParam Long userId, @RequestParam Long withUserId) {
        return service.getCompatibility(userId, withUserId);
    }
}
