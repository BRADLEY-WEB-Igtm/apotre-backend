package com.doctrine.apotres.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * SERVICE FICHIER — gestion des uploads de fichiers
 *
 * Gère l'enregistrement des fichiers uploadés depuis le dashboard :
 * - Fichiers audio MP3 (sessions Zoom, enseignements, radios)
 * - Fichiers PDF (livres téléchargeables)
 *
 * Les fichiers sont stockés dans le dossier "uploads/" sur le serveur
 * et accessibles publiquement via /uploads/audios/... ou /uploads/pdfs/...
 *
 * @Service = composant de service Spring (logique métier)
 * ============================================================
 */
@Service
public class FichierService {

    // Dossier de base pour les uploads (depuis application.properties)
    @Value("${app.upload.dir:uploads}")
    private String dossierUpload;
    // Ex: "uploads" → les fichiers seront dans ./uploads/audios/ et ./uploads/pdfs/

    // Extensions autorisées pour les audios
    private static final List<String> EXTENSIONS_AUDIO = Arrays.asList(
        "mp3", "m4a", "ogg", "wav"
        // mp3 = format principal, m4a/ogg/wav = formats alternatifs acceptés
    );

    // Extensions autorisées pour les PDFs
    private static final List<String> EXTENSIONS_PDF = Arrays.asList(
        "pdf"
    );

    // Taille maximum : 50 MB pour les audios (en bytes)
    private static final long TAILLE_MAX_AUDIO = 50 * 1024 * 1024L;

    // Taille maximum : 20 MB pour les PDFs
    private static final long TAILLE_MAX_PDF = 20 * 1024 * 1024L;

    /**
     * Sauvegarde un fichier audio sur le serveur
     *
     * @param fichier Le fichier audio envoyé depuis le dashboard
     * @return Le chemin relatif du fichier sauvegardé (ex: "uploads/audios/zoom-abc123.mp3")
     * @throws IOException si l'écriture échoue
     * @throws IllegalArgumentException si le fichier est invalide
     */
    public String sauvegarderAudio(MultipartFile fichier) throws IOException {
        // Valide le fichier avant de le sauvegarder
        validerFichier(fichier, EXTENSIONS_AUDIO, TAILLE_MAX_AUDIO, "audio");

        // Crée le dossier de destination s'il n'existe pas
        Path dossierAudios = Paths.get(dossierUpload, "audios");
        // Paths.get() = construit un chemin de fichier : "uploads/audios"
        Files.createDirectories(dossierAudios);
        // createDirectories = crée tous les dossiers manquants

        // Génère un nom de fichier unique pour éviter les collisions
        String nomFichier = genererNomFichier(fichier.getOriginalFilename());
        // Ex: "zoom-2026-03-20.mp3" → "zoom-2026-03-20-a1b2c3d4.mp3"

        // Construit le chemin complet
        Path cheminComplet = dossierAudios.resolve(nomFichier);
        // resolve() = joint deux chemins : "uploads/audios" + "nom.mp3"

        // Copie le fichier uploadé vers le chemin de destination
        Files.copy(fichier.getInputStream(), cheminComplet,
            StandardCopyOption.REPLACE_EXISTING);
        // REPLACE_EXISTING = remplace si un fichier du même nom existe déjà

        // Retourne le chemin relatif (accessible depuis l'URL publique)
        return dossierUpload + "/audios/" + nomFichier;
    }

    /**
     * Sauvegarde un fichier PDF sur le serveur
     *
     * @param fichier Le fichier PDF envoyé depuis le dashboard
     * @return Le chemin relatif du fichier sauvegardé
     */
    public String sauvegarderPdf(MultipartFile fichier) throws IOException {
        validerFichier(fichier, EXTENSIONS_PDF, TAILLE_MAX_PDF, "PDF");

        Path dossierPdfs = Paths.get(dossierUpload, "pdfs");
        Files.createDirectories(dossierPdfs);

        String nomFichier = genererNomFichier(fichier.getOriginalFilename());
        Path cheminComplet = dossierPdfs.resolve(nomFichier);

        Files.copy(fichier.getInputStream(), cheminComplet,
            StandardCopyOption.REPLACE_EXISTING);

        return dossierUpload + "/pdfs/" + nomFichier;
    }

    /**
     * Supprime un fichier du serveur
     * Appelé quand l'admin supprime une publication
     *
     * @param chemin Le chemin relatif du fichier à supprimer
     */
    public void supprimerFichier(String chemin) {
        if (chemin == null || chemin.isBlank()) return;
        // isBlank() = null, vide ou que des espaces

        try {
            Path fichierPath = Paths.get(chemin);
            Files.deleteIfExists(fichierPath);
            // deleteIfExists = ne lance pas d'exception si le fichier n'existe pas
        } catch (IOException e) {
            // Log l'erreur mais ne la propage pas (la publication peut quand même être supprimée)
            System.err.println("Impossible de supprimer le fichier : " + chemin + " — " + e.getMessage());
        }
    }

    /**
     * Valide un fichier avant de le sauvegarder
     * Vérifie l'extension et la taille
     *
     * @throws IllegalArgumentException si le fichier est invalide
     */
    private void validerFichier(
        MultipartFile fichier,
        List<String> extensionsAutorisees,
        long tailleMax,
        String typeFichier
    ) {
        // Vérifie que le fichier n'est pas vide
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Le fichier " + typeFichier + " est vide");
        }

        // Récupère le nom original du fichier
        String nomOriginal = fichier.getOriginalFilename();
        if (nomOriginal == null || !nomOriginal.contains(".")) {
            throw new IllegalArgumentException("Nom de fichier invalide");
        }

        // Vérifie l'extension
        String extension = nomOriginal.substring(nomOriginal.lastIndexOf(".") + 1).toLowerCase();
        // lastIndexOf(".") = position du dernier point dans le nom
        // substring(...+1) = tout ce qui est après le dernier point = l'extension

        if (!extensionsAutorisees.contains(extension)) {
            throw new IllegalArgumentException(
                "Extension ." + extension + " non autorisée pour " + typeFichier +
                ". Extensions acceptées : " + extensionsAutorisees
            );
        }

        // Vérifie la taille
        if (fichier.getSize() > tailleMax) {
            throw new IllegalArgumentException(
                "Fichier trop grand : " + (fichier.getSize() / 1024 / 1024) + "MB. " +
                "Maximum : " + (tailleMax / 1024 / 1024) + "MB"
            );
        }
    }

    /**
     * Génère un nom de fichier unique
     * Combine le nom original (nettoyé) avec un UUID court
     *
     * @param nomOriginal Le nom original du fichier uploadé
     * @return Un nom de fichier unique
     */
    private String genererNomFichier(String nomOriginal) {
        if (nomOriginal == null) nomOriginal = "fichier";

        // Récupère l'extension
        String extension = "";
        int dernierPoint = nomOriginal.lastIndexOf(".");
        if (dernierPoint > 0) {
            extension = nomOriginal.substring(dernierPoint);
            // Ex: ".mp3"
            nomOriginal = nomOriginal.substring(0, dernierPoint);
            // Ex: "zoom-2026-03-20"
        }

        // Nettoie le nom (remplace espaces et caractères spéciaux par "-")
        String nomNettoye = nomOriginal
            .toLowerCase()
            .replaceAll("[^a-z0-9.-]", "-")
            // [^a-z0-9.-] = tout ce qui n'est pas lettres/chiffres/point/tiret
            .replaceAll("-+", "-")
            // Remplace les tirets multiples par un seul
            .replaceAll("^-|-$", "");
            // Retire les tirets en début et fin

        // UUID aléatoire court (8 premiers caractères)
        String idUnique = UUID.randomUUID().toString().substring(0, 8);
        // UUID.randomUUID() = génère un identifiant unique universel
        // Ex: "a1b2c3d4-e5f6-7890-..."  → on garde "a1b2c3d4"

        return nomNettoye + "-" + idUnique + extension;
        // Ex: "zoom-2026-03-20-a1b2c3d4.mp3"
    }
}
