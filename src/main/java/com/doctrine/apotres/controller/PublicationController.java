package com.doctrine.apotres.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.doctrine.apotres.dto.PublicationDTO;
import com.doctrine.apotres.entity.Publication.StatutPublication;
import com.doctrine.apotres.entity.Publication.TypePublication;
import com.doctrine.apotres.service.PublicationService;

import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * CONTROLLER PUBLICATION — VERSION CORRIGÉE
 *
 * CORRECTION :
 * - Accepte List<MultipartFile> pour les audios → nombre illimité
 *   Au lieu de 3 paramètres fixes (audio, audio2, audio3),
 *   on reçoit une liste "audios" qui peut contenir 1, 2, 7 fichiers
 * - Accepte un paramètre "image" séparé pour l'image à la une
 * ============================================================
 */
@RestController
@CrossOrigin
public class PublicationController {

    @Autowired
    private PublicationService publicationService;

    // ──────────────────────────────────────────────────────────
    // ENDPOINTS PUBLICS
    // ──────────────────────────────────────────────────────────

    @GetMapping("/api/publications")
    public ResponseEntity<Page<PublicationDTO.Response>> listerPubliees(
        @RequestParam(required = false) TypePublication type,
        @RequestParam(required = false) String categorie,
        @RequestParam(required = false) String jourZoom,
        @RequestParam(required = false) String recherche,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
            publicationService.listerPubliees(type, categorie, jourZoom, recherche, page, size)
        );
    }

    @GetMapping("/api/publications/{id}")
    public ResponseEntity<?> trouverParId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(publicationService.trouverParIdDTO(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ──────────────────────────────────────────────────────────
    // ENDPOINTS ADMIN
    // ──────────────────────────────────────────────────────────

    /**
     * Crée une nouvelle publication
     *
     * URL  : POST /api/admin/publications
     * Body : multipart/form-data
     *
     * Parties multipart :
     *   "publication" → JSON des métadonnées
     *   "audios"      → liste de fichiers audio (1 à N fichiers)
     *                   Le frontend envoie autant de fois "audios"
     *                   qu'il y a de parties
     *   "image"       → image à la une (optionnel)
     *   "pdf"         → fichier PDF (optionnel)
     */
    @PostMapping("/api/admin/publications")
    public ResponseEntity<?> creer(
        @RequestPart("publication") @Valid PublicationDTO.Request request,

        // ✅ CORRECTION : List<MultipartFile> au lieu de 3 fichiers fixes
        // Spring MVC regroupe automatiquement tous les champs "audios"
        // du FormData dans cette liste
        // Ex: formData.append('audios', file1); formData.append('audios', file2);
        // → fichierAudios = [file1, file2]
        @RequestPart(value = "audios", required = false) List<MultipartFile> fichierAudios,

        // Image à la une — champ séparé des audios
        @RequestPart(value = "image", required = false) MultipartFile fichierImage,

        // PDF pour les livres
        @RequestPart(value = "pdf",   required = false) MultipartFile fichierPdf
    ) {
        try {
            PublicationDTO.Response created =
                publicationService.creer(request, fichierAudios, fichierImage, fichierPdf);
            return ResponseEntity.status(201).body(created);

        } catch (IllegalArgumentException e) {
            // Fichier invalide (mauvaise extension, trop grand...)
            return ResponseEntity.badRequest()
                .body(Map.of("erreur", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * Modifie une publication existante
     */
    @PutMapping("/api/admin/publications/{id}")
    public ResponseEntity<?> modifier(
        @PathVariable Long id,
        @RequestPart("publication") @Valid PublicationDTO.Request request,
        @RequestPart(value = "audios", required = false) List<MultipartFile> fichierAudios,
        @RequestPart(value = "image",  required = false) MultipartFile fichierImage,
        @RequestPart(value = "pdf",    required = false) MultipartFile fichierPdf
    ) {
        try {
            PublicationDTO.Response updated =
                publicationService.modifier(id, request, fichierAudios, fichierImage, fichierPdf);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("erreur", e.getMessage()));
        }
    }

    @PutMapping("/api/admin/publications/{id}/suspendre")
    public ResponseEntity<?> suspendre(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(publicationService.suspendre(id));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("erreur", e.getMessage()));
        }
    }

    @PutMapping("/api/admin/publications/{id}/publier")
    public ResponseEntity<?> publier(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(publicationService.publier(id));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("erreur", e.getMessage()));
        }
    }

    @DeleteMapping("/api/admin/publications/{id}")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        try {
            publicationService.supprimer(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("erreur", e.getMessage()));
        }
    }

    @GetMapping("/api/admin/publications")
    public ResponseEntity<Page<PublicationDTO.Response>> listerPourAdmin(
        @RequestParam(required = false) TypePublication type,
        @RequestParam(required = false) StatutPublication statut,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            publicationService.listerToutesPourAdmin(type, statut, page, size)
        );
    }

    @GetMapping("/api/admin/stats")
    public ResponseEntity<PublicationDTO.Stats> getStats() {
        return ResponseEntity.ok(publicationService.getStats());
    }
}