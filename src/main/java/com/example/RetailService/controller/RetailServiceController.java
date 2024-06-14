package com.example.RetailService.controller;

import com.example.RetailService.entity.User;
import com.example.RetailService.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/retail-service")
public class RetailServiceController {
    @Autowired
    private UserService userService;

    @PostMapping("/user")
    public Mono<User> addUser(@RequestBody Mono<User> user){
        return userService.addUser(user);
    }

    @GetMapping("/user/{userId}")
    public Mono<User> getUser(@PathVariable String userId){
        return userService.getUser(userId);
    }
}
