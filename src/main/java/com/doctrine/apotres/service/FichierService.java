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
 * SERVICE FICHIER — VERSION CORRIGÉE
 *
 * CORRECTIONS :
 * 1. Ajout de sauvegarderImage() pour les images .jpg/.png/.webp
 *    Avant : sauvegarderAudio() était utilisé → plantait sur les images
 *    Maintenant : méthode dédiée avec extensions images autorisées
 * 2. Taille max audio augmentée à 200MB (sessions Zoom peuvent être longues)
 * ============================================================
 */
@Service
public class FichierService {

    @Value("${app.upload.dir}")
    private String dossierUpload;

    // Extensions audio autorisées
    private static final List<String> EXTENSIONS_AUDIO = Arrays.asList(
        "mp3", "m4a", "ogg", "wav"
    );

    // Extensions image autorisées
    private static final List<String> EXTENSIONS_IMAGE = Arrays.asList(
        "jpg", "jpeg", "png", "webp", "gif"
    );

    // Extensions PDF
    private static final List<String> EXTENSIONS_PDF = Arrays.asList("pdf");

    // Taille max audio : 200 MB (sessions Zoom longues)
    private static final long TAILLE_MAX_AUDIO = 200 * 1024 * 1024L;

    // Taille max image : 5 MB
    private static final long TAILLE_MAX_IMAGE = 5 * 1024 * 1024L;

    // Taille max PDF : 20 MB
    private static final long TAILLE_MAX_PDF = 20 * 1024 * 1024L;


    /**
     * Sauvegarde un fichier audio
     */
    public String sauvegarderAudio(MultipartFile fichier) throws IOException {
        validerFichier(fichier, EXTENSIONS_AUDIO, TAILLE_MAX_AUDIO, "audio");

        Path dossierAudios = Paths.get(dossierUpload, "audios");
        Files.createDirectories(dossierAudios);

        String nomFichier    = genererNomFichier(fichier.getOriginalFilename());
        Path   cheminComplet = dossierAudios.resolve(nomFichier);
        Files.copy(fichier.getInputStream(), cheminComplet, StandardCopyOption.REPLACE_EXISTING);

        return dossierUpload + "/audios/" + nomFichier;
    }


    /**
     * ✅ NOUVELLE MÉTHODE : Sauvegarde une image
     *
     * Avant, sauvegarderAudio() était appelé pour les images.
     * Cela causait une IllegalArgumentException :
     * "Extension .jpg non autorisée pour audio"
     * et faisait planter toute la requête → audio2 jamais sauvegardé.
     */
    public String sauvegarderImage(MultipartFile fichier) throws IOException {
        validerFichier(fichier, EXTENSIONS_IMAGE, TAILLE_MAX_IMAGE, "image");

        Path dossierImages = Paths.get(dossierUpload, "images");
        Files.createDirectories(dossierImages);

        String nomFichier    = genererNomFichier(fichier.getOriginalFilename());
        Path   cheminComplet = dossierImages.resolve(nomFichier);
        Files.copy(fichier.getInputStream(), cheminComplet, StandardCopyOption.REPLACE_EXISTING);

        return dossierUpload + "/images/" + nomFichier;
    }


    /**
     * Sauvegarde un fichier PDF
     */
    public String sauvegarderPdf(MultipartFile fichier) throws IOException {
        validerFichier(fichier, EXTENSIONS_PDF, TAILLE_MAX_PDF, "PDF");

        Path dossierPdfs = Paths.get(dossierUpload, "pdfs");
        Files.createDirectories(dossierPdfs);

        String nomFichier    = genererNomFichier(fichier.getOriginalFilename());
        Path   cheminComplet = dossierPdfs.resolve(nomFichier);
        Files.copy(fichier.getInputStream(), cheminComplet, StandardCopyOption.REPLACE_EXISTING);

        return dossierUpload + "/pdfs/" + nomFichier;
    }


    /**
     * Supprime un fichier du serveur
     */
    public void supprimerFichier(String chemin) {
        if (chemin == null || chemin.isBlank()) return;
        try {
            Files.deleteIfExists(Paths.get(chemin));
        } catch (IOException e) {
            System.err.println("Impossible de supprimer : " + chemin + " — " + e.getMessage());
        }
    }


    /**
     * Valide un fichier (extension + taille)
     */
    private void validerFichier(
        MultipartFile fichier,
        List<String> extensionsAutorisees,
        long tailleMax,
        String typeFichier
    ) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Le fichier " + typeFichier + " est vide");
        }

        String nomOriginal = fichier.getOriginalFilename();
        if (nomOriginal == null || !nomOriginal.contains(".")) {
            throw new IllegalArgumentException("Nom de fichier invalide");
        }

        String extension = nomOriginal
            .substring(nomOriginal.lastIndexOf(".") + 1)
            .toLowerCase();

        if (!extensionsAutorisees.contains(extension)) {
            throw new IllegalArgumentException(
                "Extension ." + extension + " non autorisée pour " + typeFichier +
                ". Extensions acceptées : " + extensionsAutorisees
            );
        }

        if (fichier.getSize() > tailleMax) {
            throw new IllegalArgumentException(
                "Fichier trop grand : " + (fichier.getSize() / 1024 / 1024) + "MB. " +
                "Maximum : " + (tailleMax / 1024 / 1024) + "MB"
            );
        }
    }


    /**
     * Génère un nom de fichier unique
     */
    private String genererNomFichier(String nomOriginal) {
        if (nomOriginal == null) nomOriginal = "fichier";

        String extension = "";
        int dernierPoint = nomOriginal.lastIndexOf(".");
        if (dernierPoint > 0) {
            extension  = nomOriginal.substring(dernierPoint);
            nomOriginal= nomOriginal.substring(0, dernierPoint);
        }

        String nomNettoye = nomOriginal
            .toLowerCase()
            .replaceAll("[^a-z0-9.-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");

        String idUnique = UUID.randomUUID().toString().substring(0, 8);
        return nomNettoye + "-" + idUnique + extension;
    }
}