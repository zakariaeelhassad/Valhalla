package com.example.backend.service;

import com.example.backend.dto.profile.ProfileResponse;
import com.example.backend.dto.profile.ProfileUpdateRequest;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.model.entity.User;
import com.example.backend.model.entity.UserTeam;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.UserTeamRepository;
import com.example.backend.security.JwtUtil;
import com.example.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final int MAX_IMAGE_DATA_LENGTH = 4_500_000;
    private static final String IMAGE_API_PREFIX = "/api/profile/images/";
    private static final Path UPLOAD_ROOT = Paths.get(System.getProperty("user.dir"), "uploads");

    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final UserTeamRepository userTeamRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        User user = securityUtils.getCurrentUser();
        UserTeam team = userTeamRepository.findByUserId(user.getId()).orElse(null);

        return new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                team != null ? team.getTeamName() : null,
                user.getProfileImage(),
            team != null ? team.getTeamImage() : null,
            null);
    }

    @Transactional
    public ProfileResponse updateMyProfile(ProfileUpdateRequest request) {
        User user = securityUtils.getCurrentUser();
        UserTeam team = userTeamRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User team not found"));

        String username = trimToNull(request.getUsername());
        String email = trimToNull(request.getEmail());
        String teamName = trimToNull(request.getTeamName());

        if (username != null && !username.equalsIgnoreCase(user.getUsername())) {
            Optional<User> byUsername = userRepository.findByUsername(username);
            if (byUsername.isPresent() && !byUsername.get().getId().equals(user.getId())) {
                throw new RuntimeException("Username already in use");
            }
            user.setUsername(username);
        }

        if (email != null && !email.equalsIgnoreCase(user.getEmail())) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent() && !byEmail.get().getId().equals(user.getId())) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(email);
        }

        if (teamName != null) {
            team.setTeamName(teamName);
        }

        String newPassword = trimToNull(request.getNewPassword());
        if (newPassword != null) {
            if (newPassword.length() < 6) {
                throw new RuntimeException("New password must be at least 6 characters");
            }
            String currentPassword = trimToNull(request.getCurrentPassword());
            if (currentPassword == null) {
                throw new RuntimeException("Current password is required to change password");
            }
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new RuntimeException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        if (request.getProfileImage() != null) {
            user.setProfileImage(processAndStoreImage(request.getProfileImage(), "profile", "Profile image"));
        }

        if (request.getTeamImage() != null) {
            team.setTeamImage(processAndStoreImage(request.getTeamImage(), "team", "Team image"));
        }

        userRepository.save(user);
        userTeamRepository.save(team);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRole())
                .build();
        String refreshedToken = jwtUtil.generateToken(userDetails);

        return new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                team.getTeamName(),
                user.getProfileImage(),
                team.getTeamImage(),
                refreshedToken);
    }

    @Transactional(readOnly = true)
    public Resource getImageAsResource(String kind, String filename) {
        if (!"profile".equals(kind) && !"team".equals(kind)) {
            throw new ResourceNotFoundException("Image type not found");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new ResourceNotFoundException("Invalid image path");
        }

        try {
            Path directory = UPLOAD_ROOT.resolve(kind).normalize();
            Path file = directory.resolve(filename).normalize();
            if (!file.startsWith(directory) || !Files.exists(file)) {
                throw new ResourceNotFoundException("Image not found");
            }
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Image not found");
        }
    }

    @Transactional(readOnly = true)
    public MediaType getImageMediaType(String kind, String filename) {
        try {
            Path file = UPLOAD_ROOT.resolve(kind).resolve(filename).normalize();
            String probe = Files.probeContentType(file);
            if (probe != null) {
                return MediaType.parseMediaType(probe);
            }
        } catch (Exception ignored) {
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String processAndStoreImage(String imageData, String kind, String label) {
        String normalized = trimToNull(imageData);
        if (normalized == null) {
            return null;
        }

        if (normalized.startsWith(IMAGE_API_PREFIX + kind + "/")) {
            return normalized;
        }

        if (!normalized.startsWith("data:image/")) {
            throw new RuntimeException(label + " must be an image file");
        }

        if (normalized.length() > MAX_IMAGE_DATA_LENGTH) {
            throw new RuntimeException(label + " is too large");
        }

        int commaIndex = normalized.indexOf(',');
        if (commaIndex < 0) {
            throw new RuntimeException(label + " is not a valid image payload");
        }

        String header = normalized.substring(0, commaIndex);
        String base64 = normalized.substring(commaIndex + 1);
        if (!header.contains(";base64")) {
            throw new RuntimeException(label + " must be base64 encoded");
        }

        String extension = imageExtensionFromHeader(header);
        String filename = UUID.randomUUID() + "." + extension;

        try {
            Path directory = UPLOAD_ROOT.resolve(kind);
            Files.createDirectories(directory);
            byte[] bytes = Base64.getDecoder().decode(base64);
            Files.write(directory.resolve(filename), bytes);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(label + " has invalid base64 content");
        } catch (IOException e) {
            throw new RuntimeException("Failed to store " + label.toLowerCase());
        }

        // Keep DB value as URL path to the stored file.
        return IMAGE_API_PREFIX + kind + "/" + filename;
    }

    private String imageExtensionFromHeader(String header) {
        if (header.startsWith("data:image/png")) {
            return "png";
        }
        if (header.startsWith("data:image/jpeg") || header.startsWith("data:image/jpg")) {
            return "jpg";
        }
        if (header.startsWith("data:image/webp")) {
            return "webp";
        }
        if (header.startsWith("data:image/gif")) {
            return "gif";
        }
        return "bin";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
