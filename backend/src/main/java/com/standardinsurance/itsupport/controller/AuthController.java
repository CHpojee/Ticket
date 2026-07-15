package com.standardinsurance.itsupport.controller;

import com.standardinsurance.itsupport.dto.AuthDtos.LoginRequest;
import com.standardinsurance.itsupport.dto.AuthDtos.LoginResponse;
import com.standardinsurance.itsupport.dto.AuthDtos.UserDto;
import com.standardinsurance.itsupport.security.AuthenticatedUser;
import com.standardinsurance.itsupport.security.CurrentUser;
import com.standardinsurance.itsupport.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserDto me() {
        AuthenticatedUser user = currentUser.get();
        return new UserDto(user.userId(), user.name(), user.role());
    }
}
