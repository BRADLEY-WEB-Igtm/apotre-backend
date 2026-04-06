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
 * DTOs PUBLICATION — VERSION CLOUDINARY
 *
 * Request inclut maintenant les URLs Cloudinary directement.
 * Le frontend uploade sur Cloudinary puis envoie les URLs ici.
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

        /* URLs Cloudinary des fichiers — envoyées par le frontend */
        private String cheminAudio;
        /* URL complète Cloudinary de l'audio partie 1 */
        /* Ex: "https://res.cloudinary.com/dqmy8sqmg/video/upload/doctrine-apotres/p1.mp3" */

        private String cheminAudio2;
        /* URL de l'audio partie 2 (null si pas de partie 2) */

        private String cheminAudio3;
        /* URL de l'audio partie 3 (null si pas de partie 3) */

        private String imageUne;
        /* URL de l'image à la une sur Cloudinary */

        private String cheminPdf;
        /* URL du PDF sur Cloudinary */
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
        private String cheminAudio;
        private String cheminAudio2;
        private String cheminAudio3;
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