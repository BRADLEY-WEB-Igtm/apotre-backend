package com.doctrine.apotres.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.doctrine.apotres.entity.Photo.StatutPhoto;

import java.time.LocalDateTime;

public class PhotoDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @NotBlank(message = "Le titre est obligatoire")
        private String titre;

        private String description;

        @NotBlank(message = "L'URL de la photo est obligatoire")
        private String urlPhoto;
        /* URL Cloudinary envoyée par le frontend après upload */

        private String categorie;
        private StatutPhoto statut;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String titre;
        private String description;
        private String urlPhoto;
        private String categorie;
        private StatutPhoto statut;
        private String auteur;
        private LocalDateTime dateCreation;
        private LocalDateTime dateModification;
    }
}
