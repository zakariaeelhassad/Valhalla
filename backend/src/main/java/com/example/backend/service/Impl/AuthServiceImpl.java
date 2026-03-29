package com.example.backend.service.Impl;

import com.example.backend.dto.auth.AuthResponse;
import com.example.backend.dto.auth.LoginRequest;
import com.example.backend.dto.auth.RegisterRequest;
import com.example.backend.dto.profile.UserResponse;
import com.example.backend.mapper.UserMapper;
import com.example.backend.model.entity.User;
import com.example.backend.model.entity.UserTeam;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.UserTeamRepository;
import com.example.backend.service.AuthService;
import com.example.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

        private final UserRepository userRepository;
        private final UserTeamRepository userTeamRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final AuthenticationManager authenticationManager;
        private final UserMapper userMapper;

        @Transactional
        public AuthResponse register(RegisterRequest request) {
                // Check if user already exists
                if (userRepository.existsByEmail(request.email())) {
                        throw new RuntimeException("Email already in use");
                }
                if (userRepository.existsByUsername(request.username())) {
                        throw new RuntimeException("Username already in use");
                }

                // Create new user
                User user = User.builder()
                                .username(request.username())
                                .email(request.email())
                                .password(passwordEncoder.encode(request.password()))
                                .role("USER")
                                .build();

                user = userRepository.save(user);

                // Auto-create a blank fantasy team for the new user
                UserTeam userTeam = UserTeam.builder()
                                .user(user)
                                .teamName(user.getUsername() + "'s Team")
                                .budget(BigDecimal.valueOf(100.0))
                                .remainingBudget(BigDecimal.valueOf(100.0))
                                .totalPoints(0)
                                .build();
                userTeamRepository.save(userTeam);

                // Generate JWT token
                UserDetails userDetails = buildUserDetails(user);
                String token = jwtUtil.generateToken(userDetails);

                UserResponse userResponse = userMapper.toResponse(user);

                return new AuthResponse(token, userResponse);
        }

        public AuthResponse login(LoginRequest request) {
                // Authenticate user
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.emailOrUsername(),
                                                request.password()));

                // Find user by email or username
                User user = userRepository.findByEmail(request.emailOrUsername())
                                .or(() -> userRepository.findByUsername(request.emailOrUsername()))
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                // Generate JWT token
                UserDetails userDetails = buildUserDetails(user);
                String token = jwtUtil.generateToken(userDetails);

                UserResponse userResponse = userMapper.toResponse(user);

                return new AuthResponse(token, userResponse);
        }

        private UserDetails buildUserDetails(User user) {
                return org.springframework.security.core.userdetails.User
                                .withUsername(user.getEmail())
                                .password(user.getPassword())
                                .authorities(user.getRole())
                                .build();
        }
}


