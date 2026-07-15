package com.standardinsurance.itsupport.service;

import com.standardinsurance.itsupport.dto.AuthDtos.LoginRequest;
import com.standardinsurance.itsupport.dto.AuthDtos.LoginResponse;
import com.standardinsurance.itsupport.dto.AuthDtos.UserDto;
import com.standardinsurance.itsupport.entity.User;
import com.standardinsurance.itsupport.exception.InvalidCredentialsException;
import com.standardinsurance.itsupport.repository.UserRepository;
import com.standardinsurance.itsupport.security.JwtService;
import com.standardinsurance.itsupport.security.Roles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Authenticates seeded users and issues JWTs. See docs/specs/01-user-login.md. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        String role = Roles.forUserId(user.getUserId());
        String token = jwtService.generate(user.getUserId(), user.getName(), role);
        return new LoginResponse(token, new UserDto(user.getUserId(), user.getName(), role));
    }
}
