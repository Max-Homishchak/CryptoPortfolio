package com.khomishchak.cryptoportfolio.controllers;

import com.khomishchak.cryptoportfolio.model.requests.LoginRequest;
import com.khomishchak.cryptoportfolio.model.requests.RegistrationRequest;
import com.khomishchak.cryptoportfolio.model.response.LoginResult;
import com.khomishchak.cryptoportfolio.model.response.RegistrationResult;
import com.khomishchak.cryptoportfolio.services.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResult> register(@RequestBody @Validated RegistrationRequest registrationRequest) {
        return new ResponseEntity<>(userService.registerUser(registrationRequest), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResult> login(@RequestBody LoginRequest loginRequest) {
        return new ResponseEntity<>(userService.authenticateUser(loginRequest), HttpStatus.OK);
    }
}
