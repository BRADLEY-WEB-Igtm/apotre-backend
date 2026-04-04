package com.doctrine.apotres.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.doctrine.apotres.entity.Commentaire.StatutCommentaire;
import java.time.LocalDateTime;

/**
 * ============================================================
 * DTO COMMENTAIRE
 * Transporte les données des commentaires entre frontend et backend
 * ============================================================
 */
public class CommentaireDTO {

    /**
     * Données reçues quand un visiteur soumet un commentaire
     * Correspond au formulaire de commentaire sur chaque page du site
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        // Nom du visiteur — obligatoire
        @NotBlank(message = "Le nom est obligatoire")
        private String nom;

        // Email du visiteur — optionnel mais doit être valide si fourni
        @Email(message = "Format d'email invalide")
        private String email;

        // Texte du commentaire — obligatoire
        @NotBlank(message = "Le message est obligatoire")
        private String message;

        // ID de la publication commentée (optionnel si source suffit)
        private Long publicationId;

        // Source de la page (zoom, emissions-radios, enseignements-audios...)
        // Correspond aux différents fetch('/api/commentaires/SOURCE') du JS
        private String source;
    }

    /**
     * Données envoyées au dashboard pour la modération
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String nom;
        private String email;
        private String message;
        private String source;
        private StatutCommentaire statut;
        private LocalDateTime dateCreation;
        private String articleTitre; // Titre de la publication associée
        private Long publicationId;
    }
}
