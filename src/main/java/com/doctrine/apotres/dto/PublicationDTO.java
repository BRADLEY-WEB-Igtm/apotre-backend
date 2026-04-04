package com.doctrine.apotres.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.doctrine.apotres.entity.Publication.TypePublication;
import com.doctrine.apotres.entity.Publication.StatutPublication;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================
 * DTOs — Data Transfer Objects
 *
 * Un DTO est un objet qui transporte les données entre le
 * frontend (HTML/JS) et le backend (Java).
 *
 * Pourquoi ne pas envoyer directement l'entité ?
 * → L'entité contient parfois des données sensibles (ex: mot de passe)
 * → Le DTO permet de contrôler exactement ce qui est envoyé/reçu
 * → Évite les références circulaires (Publication → Commentaire → Publication...)
 * ============================================================
 */
public class PublicationDTO {

    /**
     * DTO pour RECEVOIR les données de création/modification
     * (depuis le formulaire publication.html du dashboard)
     * "Request" = données envoyées par le client vers le serveur
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        // Type obligatoire — détermine le formulaire affiché
        @NotNull(message = "Le type est obligatoire")
        private TypePublication type;

        // Titre obligatoire
        @NotBlank(message = "Le titre est obligatoire")
        private String titre;

        // Contenu texte (optionnel selon le type)
        private String contenu;

        // Catégorie (ex: "Évangiles", "Enseignements")
        private String categorie;

        // Sous-catégorie
        private String sousCategorie;

        // Statut demandé (PUBLIE ou BROUILLON)
        private StatutPublication statut;

        // Lien vidéo YouTube (pour le type VIDÉO)
        private String lienVideo;

        // Jour du Zoom (LUNDI ou JEUDI)
        private String jourZoom;

        // Date de la session Zoom ou Radio
        private String dateSession;

        // Tags séparés par virgules
        private String tags;
        private String resume;       // Résumé court
        private String predicateur; // Prédicateur/Orateur

        // Autoriser les commentaires ?
        private Boolean commentairesActifs;
    }

    /**
     * DTO pour ENVOYER les données d'une publication
     * (vers le dashboard ou le site client)
     * "Response" = données envoyées par le serveur vers le client
     */
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
        private String cheminAudio;    // URL pour écouter l'audio (partie 1)
        private String cheminAudio2;   // URL pour écouter l'audio (partie 2)
        private String cheminAudio3;   // URL pour écouter l'audio (partie 3)
        private String cheminPdf;      // URL pour télécharger le PDF
        private String imageUne;       // Image à la une
        private String lienVideo;
        private String jourZoom;
        private String dateSession;
        private String tags;
        private String resume;         // Résumé/extrait court
        private String predicateur;    // Prédicateur/Orateur
        private Boolean commentairesActifs;
        private LocalDateTime dateCreation;
        private LocalDateTime dateModification;
        private LocalDateTime datePublication;
        private int nombreCommentaires; // Compte des commentaires approuvés
    }

    /**
     * DTO pour les statistiques du tableau de bord
     * Utilisé pour les 6 cartes en haut du dashboard
     */
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