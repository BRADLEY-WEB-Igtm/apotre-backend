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

import java.util.Map;

/**
 * ============================================================
 * CONTROLLER PUBLICATION
 *
 * Expose tous les endpoints REST pour les publications.
 *
 * Routes PUBLIQUES (site client) :
 *   GET  /api/publications             → liste les publications publiées
 *   GET  /api/publications/{id}        → détail d'une publication
 *
 * Routes PROTÉGÉES (dashboard admin) :
 *   POST   /api/admin/publications          → créer une publication
 *   PUT    /api/admin/publications/{id}     → modifier
 *   PUT    /api/admin/publications/{id}/suspendre
 *   PUT    /api/admin/publications/{id}/publier
 *   DELETE /api/admin/publications/{id}     → supprimer
 *   GET    /api/admin/publications          → lister toutes (avec statuts)
 *   GET    /api/admin/stats                 → statistiques dashboard
 * ============================================================
 */
@RestController
@CrossOrigin
public class PublicationController {

    @Autowired
    private PublicationService publicationService;

    // ============================================================
    // ENDPOINTS PUBLICS — accessibles par le site client
    // ============================================================

    /**
     * Liste les publications publiées avec filtres optionnels
     *
     * URL    : GET /api/publications
     * Accès  : Public
     * Params : type, categorie, jourZoom, recherche, page, size
     *
     * Exemples :
     *   GET /api/publications?type=ZOOM&jourZoom=LUNDI&page=0&size=10
     *   GET /api/publications?type=ENSEIGNEMENT&page=0
     *   GET /api/publications?recherche=dime&page=0
     */
    @GetMapping("/api/publications")
    public ResponseEntity<Page<PublicationDTO.Response>> listerPubliees(
        @RequestParam(required = false) TypePublication type,
        // @RequestParam = lit le paramètre dans l'URL (?type=ZOOM)
        // required = false = paramètre optionnel (peut être absent)

        @RequestParam(required = false) String categorie,
        @RequestParam(required = false) String jourZoom,
        @RequestParam(required = false) String recherche,
        @RequestParam(defaultValue = "0") int page,
        // defaultValue = valeur utilisée si le paramètre est absent
        @RequestParam(defaultValue = "10") int size
    ) {
        Page<PublicationDTO.Response> publications =
            publicationService.listerPubliees(
                type, categorie, jourZoom, recherche, page, size
            );

        return ResponseEntity.ok(publications);
    }

    /**
     * Récupère le détail d'une publication par son ID
     *
     * URL   : GET /api/publications/{id}
     * Accès : Public
     *
     * @PathVariable = lit la valeur depuis l'URL (/publications/42 → id=42)
     */
    @GetMapping("/api/publications/{id}")
    public ResponseEntity<?> trouverParId(@PathVariable Long id) {
        try {
            PublicationDTO.Response pub = publicationService.trouverParIdDTO(id);
            return ResponseEntity.ok(pub);
        } catch (Exception e) {
            // HTTP 404 si la publication n'existe pas
            return ResponseEntity.notFound().build();
        }
    }


    // ============================================================
    // ENDPOINTS ADMIN — nécessitent un token JWT valide
    // ============================================================

    /**
     * Crée une nouvelle publication depuis le dashboard
     *
     * URL    : POST /api/admin/publications
     * Accès  : Admin (token JWT requis)
     * Body   : multipart/form-data (données + fichier audio ou PDF)
     *
     * Correspond au bouton "Publier sur le site" de publication.html
     *
     * @RequestPart = lit une partie d'un formulaire multipart
     * MultipartFile = fichier uploadé
     */
    @PostMapping("/api/admin/publications")
    public ResponseEntity<?> creer(
        @RequestPart("publication") @Valid PublicationDTO.Request request,
        // "publication" = nom de la partie contenant le JSON dans le multipart

        @RequestPart(value = "audio",  required = false) MultipartFile fichierAudio,
        @RequestPart(value = "audio2", required = false) MultipartFile fichierAudio2,
        @RequestPart(value = "audio3", required = false) MultipartFile fichierAudio3,
        @RequestPart(value = "image",  required = false) MultipartFile fichierImage,
        @RequestPart(value = "pdf",    required = false) MultipartFile fichierPdf
        // "pdf" = partie optionnelle contenant le fichier PDF
    ) {
        try {
            PublicationDTO.Response created =
                publicationService.creer(request, fichierAudio, fichierAudio2, fichierAudio3, fichierImage, fichierPdf);

            // HTTP 201 Created (standard pour les créations)
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
     *
     * URL   : PUT /api/admin/publications/{id}
     * Accès : Admin
     */
    @PutMapping("/api/admin/publications/{id}")
    public ResponseEntity<?> modifier(
        @PathVariable Long id,
        @RequestPart("publication") @Valid PublicationDTO.Request request,
        @RequestPart(value = "audio",  required = false) MultipartFile fichierAudio,
        @RequestPart(value = "audio2", required = false) MultipartFile fichierAudio2,
        @RequestPart(value = "audio3", required = false) MultipartFile fichierAudio3,
        @RequestPart(value = "image",  required = false) MultipartFile fichierImage,
        @RequestPart(value = "pdf",    required = false) MultipartFile fichierPdf
    ) {
        try {
            PublicationDTO.Response updated =
                publicationService.modifier(id, request, fichierAudio, fichierPdf);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * Suspend une publication (dépublie sans supprimer)
     * Correspond au bouton "Pause" dans le tableau des publications
     *
     * URL   : PUT /api/admin/publications/{id}/suspendre
     * Accès : Admin
     */
    @PutMapping("/api/admin/publications/{id}/suspendre")
    public ResponseEntity<?> suspendre(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(publicationService.suspendre(id));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * Republie une publication suspendue ou brouillon
     * Correspond au bouton "Play" / "Upload" dans le tableau
     *
     * URL   : PUT /api/admin/publications/{id}/publier
     * Accès : Admin
     */
    @PutMapping("/api/admin/publications/{id}/publier")
    public ResponseEntity<?> publier(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(publicationService.publier(id));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * Supprime définitivement une publication
     * Correspond au bouton "Corbeille" dans le tableau
     *
     * URL   : DELETE /api/admin/publications/{id}
     * Accès : Admin
     */
    @DeleteMapping("/api/admin/publications/{id}")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        try {
            publicationService.supprimer(id);
            // HTTP 204 No Content = succès sans body de réponse
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * Liste toutes les publications pour le dashboard admin
     * (tous statuts : publiées, brouillons, suspendues)
     *
     * URL    : GET /api/admin/publications
     * Accès  : Admin
     * Params : type, statut, page, size
     */
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

    /**
     * Statistiques pour les cartes du tableau de bord
     *
     * URL   : GET /api/admin/stats
     * Accès : Admin
     * Réponse : { "totalEnseignements": 122, "totalAudios": 48, ... }
     */
    @GetMapping("/api/admin/stats")
    public ResponseEntity<PublicationDTO.Stats> getStats() {
        return ResponseEntity.ok(publicationService.getStats());
    }
}