package com.doctrine.apotres.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.doctrine.apotres.entity.Publication.TypePublication;
import com.doctrine.apotres.entity.Publication.StatutPublication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * ============================================================
 * DTOs PUBLICATION — VERSION CORRIGÉE
 *
 * CORRECTION dans Response :
 * - Ajout de List<String> cheminsAudio pour les audios illimités
 * - Les anciens champs cheminAudio/2/3 conservés (backward compat)
 * ============================================================
 */
public class PublicationDTO {

    /**
     * DTO pour RECEVOIR les données (depuis le formulaire dashboard)
     */
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
    }

    /**
     * DTO pour ENVOYER les données (vers dashboard ou site client)
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

        // ── Anciens champs audio (backward compat pour publications existantes) ──
        private String cheminAudio;
        private String cheminAudio2;
        private String cheminAudio3;

        // ── NOUVEAU : liste complète de tous les audios ──
        // Contient TOUS les chemins dans l'ordre (Partie 1, 2, 3... illimité)
        // Le frontend utilise cette liste en priorité
        private List<String> cheminsAudio = new ArrayList<>();

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

    /**
     * DTO pour les statistiques du tableau de bord
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