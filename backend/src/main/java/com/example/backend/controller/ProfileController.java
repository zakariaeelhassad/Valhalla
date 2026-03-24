package com.example.backend.controller;

import com.example.backend.dto.ProfileResponse;
import com.example.backend.dto.ProfileUpdateRequest;
import com.example.backend.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Manage user profile and team identity settings")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "Get my profile settings")
    public ResponseEntity<ProfileResponse> getMyProfile() {
        return ResponseEntity.ok(profileService.getMyProfile());
    }

    @PutMapping
    @Operation(summary = "Update my profile settings")
    public ResponseEntity<ProfileResponse> updateMyProfile(@RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.updateMyProfile(request));
    }

    @GetMapping("/images/{kind}/{filename:.+}")
    @Operation(summary = "Get stored profile/team image")
    public ResponseEntity<Resource> getImage(
            @PathVariable String kind,
            @PathVariable String filename) {
        Resource image = profileService.getImageAsResource(kind, filename);
        MediaType mediaType = profileService.getImageMediaType(kind, filename);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(image);
    }
}
