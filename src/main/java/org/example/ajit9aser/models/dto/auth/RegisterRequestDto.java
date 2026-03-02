package org.example.ajit9aser.models.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.ajit9aser.models.enums.Role;

public record RegisterRequest(
        @NotBlank(message = "Le login est obligatoire")
        @Size(min = 3, max = 50, message = "Le login doit contenir entre 3 et 50 caractères")
        String login,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
        String password,

        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email doit être valide")
        String email,

        Role role
) {
}
