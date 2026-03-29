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
import com.example.backend.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserTeamRepository userTeamRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_shouldCreateUserAndTeam_andReturnToken() {
        RegisterRequest request = new RegisterRequest("manager1", "manager1@mail.com", "secret123");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");

        User savedUser = User.builder()
                .id(10L)
                .username(request.username())
                .email(request.email())
                .password("encoded-password")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");
        when(userMapper.toResponse(savedUser)).thenReturn(
                new UserResponse(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getCreatedAt()));

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.type());
        assertEquals("manager1", response.user().username());

        ArgumentCaptor<UserTeam> teamCaptor = ArgumentCaptor.forClass(UserTeam.class);
        verify(userTeamRepository).save(teamCaptor.capture());

        UserTeam createdTeam = teamCaptor.getValue();
        assertEquals("manager1's Team", createdTeam.getTeamName());
        assertEquals(BigDecimal.valueOf(100.0), createdTeam.getBudget());
        assertEquals(BigDecimal.valueOf(100.0), createdTeam.getRemainingBudget());
        assertEquals(0, createdTeam.getTotalPoints());
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("manager1", "manager1@mail.com", "secret123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(request));

        assertEquals("Email already in use", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(userTeamRepository, never()).save(any(UserTeam.class));
    }

    @Test
    void login_shouldAuthenticateAndReturnToken() {
        LoginRequest request = new LoginRequest("manager1@mail.com", "secret123");

        User user = User.builder()
                .id(7L)
                .username("manager1")
                .email("manager1@mail.com")
                .password("encoded")
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail(request.emailOrUsername())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any())).thenReturn("login-token");
        when(userMapper.toResponse(user)).thenReturn(new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt()));

        AuthResponse response = authService.login(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertEquals("login-token", response.token());
        assertEquals("manager1", response.user().username());
    }

    @Test
    void login_shouldThrow_whenUserNotFoundAfterAuthentication() {
        LoginRequest request = new LoginRequest("unknown", "secret123");
        when(userRepository.findByEmail(request.emailOrUsername())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(request.emailOrUsername())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.login(request));
    }
}
