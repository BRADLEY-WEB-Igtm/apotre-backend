package com.doctrine.apotres.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.doctrine.apotres.entity.Utilisateur.RoleUtilisateur;

/**
 * ============================================================
 * DTO AUTHENTIFICATION
 * Gère la connexion et la réponse avec le token JWT
 * ============================================================
 */
public class AuthDTO {

    /**
     * Données de connexion envoyées par l'admin
     * Correspond au formulaire login.html du dashboard
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {

        // Nom d'utilisateur (ex: "fridolinbradley")
        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        private String username;

        // Mot de passe en clair (sera comparé au hash BCrypt)
        @NotBlank(message = "Le mot de passe est obligatoire")
        private String motDePasse;
    }

    /**
     * Réponse envoyée après une connexion réussie
     * Contient le token JWT que le frontend stocke
     * et envoie dans chaque requête suivante
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {

        // Le token JWT — le frontend le stocke dans localStorage
        private String token;

        // Type du token — toujours "Bearer" (standard HTTP)
        private String type = "Bearer";

        // Infos de l'admin connecté (pour afficher dans le dashboard)
        private Long id;
        private String username;
        private String nomComplet;
        private RoleUtilisateur role;

        // Durée de validité en ms (86400000 = 24h)
        private long expiresIn;
    }
}
