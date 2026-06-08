package com.jarus.ai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping({"/", "/resume", "/jobs", "/pipeline", "/company", "/email", "/settings"})
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping("/admin")
    public String admin() {
        return "forward:/index.html";
    }
}
