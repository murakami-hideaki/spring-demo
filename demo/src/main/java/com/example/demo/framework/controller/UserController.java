package com.example.demo.framework.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.framework.service.UserService;

@RestController
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/logtest")
    public String testLogging() {
        service.registerUser("taro");
        
        return "ログ出力テスト完了";
    }
}