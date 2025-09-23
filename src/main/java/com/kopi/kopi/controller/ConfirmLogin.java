/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kopi.kopi.controller;

import com.kopi.kopi.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author ADMIN
 */
@RestController
@RequestMapping("/v1")
public class ConfirmLogin {
    
    private static final String confirm = "Hello, %s";
    
    @GetMapping("/ConfirmLogin")
    public User confirm(@RequestParam(value = "userName", defaultValue = "Default") String userName){
        return User.builder()
                .userId(Integer.SIZE)
                .username(String.format(confirm, userName))
                .build();
    }
}
