package com.mariafernandes.urlshortener.service;

import com.mariafernandes.urlshortener.domain.User;
import com.mariafernandes.urlshortener.exception.EmailAlreadyRegisteredException;
import com.mariafernandes.urlshortener.repository.UserRepository;
import com.mariafernandes.urlshortener.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public String register(String email, String rawPassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }
        User user = new User(email, passwordEncoder.encode(rawPassword));
        userRepository.save(user);
        return jwtService.generateToken(user);
    }

    public String login(String email, String rawPassword) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, rawPassword)
        );
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        return jwtService.generateToken(user);
    }
}