package com.openclassroom.datashare.controller;

import com.datashare.api.AuthApi;
import com.datashare.model.LoginRequest;
import com.datashare.model.LoginResponse;
import com.datashare.model.User;
import com.openclassroom.datashare.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final UserService userService;

    @Override
    public ResponseEntity<LoginResponse> authLoginPost(@Valid @RequestBody LoginRequest loginRequest) {
        UserService.LoginResult result = userService.loginUser(loginRequest.getLogin(), loginRequest.getPassword());

        User apiUser = new User()
                .userId(Math.toIntExact(result.user().getId()))
                .login(result.user().getLogin())
                .createdAt(result.user().getCreatedAt())
                .lastLogin(result.user().getLastLogin());

        LoginResponse response = new LoginResponse()
                .accessToken(result.token())
                .user(apiUser);

        return ResponseEntity.ok(response);
    }
}
