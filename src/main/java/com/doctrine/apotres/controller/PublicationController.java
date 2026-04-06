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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CONTROLLER PUBLICATION
 * Reçoit : audio, audio2, audio3 (noms exacts que le frontend envoie)
 */
@RestController
@CrossOrigin
public class PublicationController {

    @Autowired
    private PublicationService publicationService;

    // ── ENDPOINTS PUBLICS ──────────────────────────────────────────

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

    // ── ENDPOINTS ADMIN ────────────────────────────────────────────

    /**
     * POST /api/admin/publications
     *
     * Le frontend envoie :
     *   "publication" → JSON des métadonnées
     *   "audio"       → fichier audio partie 1
     *   "audio2"      → fichier audio partie 2 (optionnel)
     *   "audio3"      → fichier audio partie 3 (optionnel)
     *   "image"       → image à la une (optionnel)
     *   "pdf"         → PDF (optionnel)
     */
    @PostMapping("/api/admin/publications")
    public ResponseEntity<?> creer(
        @RequestPart("publication") @Valid PublicationDTO.Request request,
        @RequestPart(value = "audio",  required = false) MultipartFile audio,
        @RequestPart(value = "audio2", required = false) MultipartFile audio2,
        @RequestPart(value = "audio3", required = false) MultipartFile audio3,
        @RequestPart(value = "image",  required = false) MultipartFile image,
        @RequestPart(value = "pdf",    required = false) MultipartFile pdf
    ) {
        try {
            // Construit la liste des audios dans l'ordre
            List<MultipartFile> audios = Arrays.asList(audio, audio2, audio3)
                .stream()
                .filter(f -> f != null && !f.isEmpty())
                .collect(Collectors.toList());

            PublicationDTO.Response created =
                publicationService.creer(request, audios, image, pdf);
            return ResponseEntity.status(201).body(created);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Erreur création publication : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("erreur", e.getMessage()));
        }
    }

    @PutMapping("/api/admin/publications/{id}")
    public ResponseEntity<?> modifier(
        @PathVariable Long id,
        @RequestPart("publication") @Valid PublicationDTO.Request request,
        @RequestPart(value = "audio",  required = false) MultipartFile audio,
        @RequestPart(value = "audio2", required = false) MultipartFile audio2,
        @RequestPart(value = "audio3", required = false) MultipartFile audio3,
        @RequestPart(value = "image",  required = false) MultipartFile image,
        @RequestPart(value = "pdf",    required = false) MultipartFile pdf
    ) {
        try {
            List<MultipartFile> audios = Arrays.asList(audio, audio2, audio3)
                .stream()
                .filter(f -> f != null && !f.isEmpty())
                .collect(Collectors.toList());

            return ResponseEntity.ok(
                publicationService.modifier(id, request, audios, image, pdf)
            );
        } catch (Exception e) {
            System.err.println("Erreur modification : " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("erreur", e.getMessage()));
        }
    }

    @PutMapping("/api/admin/publications/{id}/suspendre")
    public ResponseEntity<?> suspendre(@PathVariable Long id) {
        try { return ResponseEntity.ok(publicationService.suspendre(id)); }
        catch (Exception e) { return ResponseEntity.status(500).body(Map.of("erreur", e.getMessage())); }
    }

    @PutMapping("/api/admin/publications/{id}/publier")
    public ResponseEntity<?> publier(@PathVariable Long id) {
        try { return ResponseEntity.ok(publicationService.publier(id)); }
        catch (Exception e) { return ResponseEntity.status(500).body(Map.of("erreur", e.getMessage())); }
    }

    @DeleteMapping("/api/admin/publications/{id}")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        try { publicationService.supprimer(id); return ResponseEntity.noContent().build(); }
        catch (Exception e) { return ResponseEntity.status(500).body(Map.of("erreur", e.getMessage())); }
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