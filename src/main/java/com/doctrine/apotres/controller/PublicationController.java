package com.doctrine.apotres.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.doctrine.apotres.dto.PublicationDTO;
import com.doctrine.apotres.entity.Publication.StatutPublication;
import com.doctrine.apotres.entity.Publication.TypePublication;
import com.doctrine.apotres.service.PublicationService;

import java.util.Map;

/**
 * CONTROLLER PUBLICATION — VERSION CLOUDINARY
 *
 * Les fichiers sont uploadés directement sur Cloudinary par le navigateur.
 * Le backend reçoit seulement des URLs texte dans un JSON simple.
 * Plus de multipart/form-data pour les fichiers — plus de timeout Railway.
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
     * Reçoit un JSON simple avec les URLs Cloudinary déjà prêtes.
     * Exemple de body :
     * {
     *   "type": "ZOOM",
     *   "titre": "Session du 06/04/2026",
     *   "jourZoom": "JEUDI",
     *   "dateSession": "2026-04-06",
     *   "cheminAudio":  "https://res.cloudinary.com/dqmy8sqmg/video/upload/.../p1.mp3",
     *   "cheminAudio2": "https://res.cloudinary.com/dqmy8sqmg/video/upload/.../p2.mp3",
     *   "statut": "PUBLIE"
     * }
     */
    @PostMapping("/api/admin/publications")
    public ResponseEntity<?> creer(
        @RequestBody @Valid PublicationDTO.Request request
        /* @RequestBody = lit le JSON du body directement */
        /* Plus de @RequestPart — plus de multipart */
    ) {
        try {
            PublicationDTO.Response created = publicationService.creer(request);
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Erreur création : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("erreur", e.getMessage()));
        }
    }

    @PutMapping("/api/admin/publications/{id}")
    public ResponseEntity<?> modifier(
        @PathVariable Long id,
        @RequestBody @Valid PublicationDTO.Request request
    ) {
        try {
            return ResponseEntity.ok(publicationService.modifier(id, request));
        } catch (Exception e) {
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