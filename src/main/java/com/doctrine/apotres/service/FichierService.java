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
 * SERVICE FICHIER — version simple (disque local)
 * Sauvegarde les fichiers dans le dossier "uploads/" sur le serveur
 * Compatible Railway — fonctionne sans Cloudinary
 */
@Service
public class FichierService {

    @Value("${app.upload.dir:uploads}")
    private String dossierUpload;

    private static final List<String> EXTENSIONS_AUDIO = Arrays.asList("mp3", "m4a", "ogg", "wav");
    private static final List<String> EXTENSIONS_IMAGE = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");
    private static final List<String> EXTENSIONS_PDF   = Arrays.asList("pdf");

    private static final long TAILLE_MAX_AUDIO = 200 * 1024 * 1024L; // 200 MB
    private static final long TAILLE_MAX_IMAGE = 5   * 1024 * 1024L; // 5 MB
    private static final long TAILLE_MAX_PDF   = 20  * 1024 * 1024L; // 20 MB

    /** Sauvegarde un fichier audio sur le disque */
    public String sauvegarderAudio(MultipartFile fichier) throws IOException {
        valider(fichier, EXTENSIONS_AUDIO, TAILLE_MAX_AUDIO, "audio");
        return sauvegarder(fichier, "audios");
    }

    /** Sauvegarde une image sur le disque */
    public String sauvegarderImage(MultipartFile fichier) throws IOException {
        valider(fichier, EXTENSIONS_IMAGE, TAILLE_MAX_IMAGE, "image");
        return sauvegarder(fichier, "images");
    }

    /** Sauvegarde un PDF sur le disque */
    public String sauvegarderPdf(MultipartFile fichier) throws IOException {
        valider(fichier, EXTENSIONS_PDF, TAILLE_MAX_PDF, "PDF");
        return sauvegarder(fichier, "pdfs");
    }

    /** Supprime un fichier du disque */
    public void supprimerFichier(String chemin) {
        if (chemin == null || chemin.isBlank()) return;
        try {
            Files.deleteIfExists(Paths.get(chemin));
        } catch (IOException e) {
            System.err.println("Impossible de supprimer : " + chemin);
        }
    }

    // ── Méthodes privées ──────────────────────────────────────────

    private String sauvegarder(MultipartFile fichier, String sousDossier) throws IOException {
        Path dossier = Paths.get(dossierUpload, sousDossier);
        Files.createDirectories(dossier);

        String nomFichier = genererNom(fichier.getOriginalFilename());
        Path chemin = dossier.resolve(nomFichier);
        Files.copy(fichier.getInputStream(), chemin, StandardCopyOption.REPLACE_EXISTING);

        // Retourne le chemin relatif accessible via /uploads/audios/nom.mp3
        return dossierUpload + "/" + sousDossier + "/" + nomFichier;
    }

    private void valider(MultipartFile fichier, List<String> extensions, long tailleMax, String type) {
        if (fichier == null || fichier.isEmpty())
            throw new IllegalArgumentException("Le fichier " + type + " est vide");

        String nom = fichier.getOriginalFilename();
        if (nom == null || !nom.contains("."))
            throw new IllegalArgumentException("Nom de fichier invalide");

        String ext = nom.substring(nom.lastIndexOf('.') + 1).toLowerCase();
        if (!extensions.contains(ext))
            throw new IllegalArgumentException(
                "Extension ." + ext + " non autorisée. Acceptées : " + extensions);

        if (fichier.getSize() > tailleMax)
            throw new IllegalArgumentException(
                "Fichier trop grand : " + (fichier.getSize()/1024/1024) + "MB. Max : " + (tailleMax/1024/1024) + "MB");
    }

    private String genererNom(String nomOriginal) {
        if (nomOriginal == null) nomOriginal = "fichier";
        String ext = "";
        int p = nomOriginal.lastIndexOf('.');
        if (p > 0) {
            ext = nomOriginal.substring(p);
            nomOriginal = nomOriginal.substring(0, p);
        }
        String nom = nomOriginal.toLowerCase()
            .replaceAll("[^a-z0-9]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        return nom + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;
    }
}