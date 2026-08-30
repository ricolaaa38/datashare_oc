package com.openclassroom.datashare.controller;

import com.datashare.api.UsersApi;
import com.datashare.model.User;
import com.datashare.model.UserCreateRequest;
import com.openclassroom.datashare.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RegisterController implements UsersApi {

    private final UserService userService;

    @Override
    public ResponseEntity<User> usersPost(@Valid @RequestBody UserCreateRequest request) {
        com.openclassroom.datashare.entity.User entity = new com.openclassroom.datashare.entity.User();
        entity.setLogin(request.getLogin());
        entity.setPassword(request.getPassword());

        com.openclassroom.datashare.entity.User saved = userService.registerUser(entity);

        User apiUser = new User()
                .userId(Math.toIntExact(saved.getId()))
                .login(saved.getLogin())
                .createdAt(saved.getCreatedAt())
                .lastLogin(saved.getLastLogin());

        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(apiUser);
    }
}
