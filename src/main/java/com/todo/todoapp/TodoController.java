package com.todo.todoapp;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TodoController {
    
    // Define your endpoints and methods here
    @RequestMapping("/hello")
    public String hello() {
        return "Hello, World! hlhglh";
    }
}
