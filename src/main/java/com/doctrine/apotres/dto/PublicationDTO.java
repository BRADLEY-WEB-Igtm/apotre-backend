package com.doctrine.apotres.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.doctrine.apotres.entity.Publication.TypePublication;
import com.doctrine.apotres.entity.Publication.StatutPublication;

import java.time.LocalDateTime;

/**
 * DTOs PUBLICATION — VERSION CLOUDINARY — 5 PARTIES AUDIO
 */
public class PublicationDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @NotNull(message = "Le type est obligatoire")
        private TypePublication type;

        @NotBlank(message = "Le titre est obligatoire")
        private String titre;

        private String contenu;
        private String categorie;
        private String sousCategorie;
        private StatutPublication statut;
        private String lienVideo;
        private String jourZoom;
        private String dateSession;
        private String tags;
        private String resume;
        private String predicateur;
        private Boolean commentairesActifs;

        /* URLs Cloudinary — 5 parties audio maximum */
        private String cheminAudio;   /* Partie 1 */
        private String cheminAudio2;  /* Partie 2 */
        private String cheminAudio3;  /* Partie 3 */
        private String cheminAudio4;  /* Partie 4 — nouveau */
        private String cheminAudio5;  /* Partie 5 — nouveau */

        private String imageUne;
        private String cheminPdf;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private TypePublication type;
        private String titre;
        private String contenu;
        private String categorie;
        private String sousCategorie;
        private String auteur;
        private StatutPublication statut;

        /* 5 parties audio dans la réponse */
        private String cheminAudio;
        private String cheminAudio2;
        private String cheminAudio3;
        private String cheminAudio4;  /* Partie 4 — nouveau */
        private String cheminAudio5;  /* Partie 5 — nouveau */

        private String cheminPdf;
        private String imageUne;
        private String lienVideo;
        private String jourZoom;
        private String dateSession;
        private String tags;
        private String resume;
        private String predicateur;
        private Boolean commentairesActifs;
        private LocalDateTime dateCreation;
        private LocalDateTime dateModification;
        private LocalDateTime datePublication;
        private int nombreCommentaires;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private long totalEnseignements;
        private long totalAudios;
        private long totalZoom;
        private long totalLivres;
        private long totalVideos;
        private long totalRadios;
        private long commentairesEnAttente;
        private long prieresEnAttente;
    }
}