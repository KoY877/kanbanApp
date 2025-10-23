package com.kanban.kanbanapp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KanbanController {
    
    // Define your endpoints and methods here
    @GetMapping("/hello")
    public String hello() {
        return "Hello, World! hlhglh";
    }
}
