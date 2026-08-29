package com.crossfade.crossfade_backend.controller;

import com.crossfade.crossfade_backend.response.FeedItemResponse;
import com.crossfade.crossfade_backend.service.FeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class FeedController {

    @Autowired
    FeedService feedService;

    @GetMapping("/{id}/feed")
    public List<FeedItemResponse> getFeed(@PathVariable Long id) {
        return feedService.getFeed(id);
    }
}
