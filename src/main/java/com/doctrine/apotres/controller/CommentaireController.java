package com.doctrine.apotres.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.doctrine.apotres.dto.CommentaireDTO;
import com.doctrine.apotres.entity.Commentaire.StatutCommentaire;
import com.doctrine.apotres.service.CommentaireService;

import java.util.Map;

/**
 * ============================================================
 * CONTROLLER COMMENTAIRE
 *
 * Routes PUBLIQUES (site client) :
 *   POST /api/commentaires              → soumettre un commentaire
 *   POST /api/commentaires/{source}     → soumettre avec source (zoom, radio...)
 *   GET  /api/commentaires/approuves/{publicationId}
 *
 * Routes ADMIN (dashboard) :
 *   GET    /api/admin/commentaires               → lister pour modération
 *   PUT    /api/admin/commentaires/{id}/approuver
 *   PUT    /api/admin/commentaires/{id}/rejeter
 *   DELETE /api/admin/commentaires/{id}
 * ============================================================
 */
@RestController
@CrossOrigin
public class CommentaireController {

    @Autowired
    private CommentaireService commentaireService;

    // ============================================================
    // ENDPOINTS PUBLICS — utilisés par le site client
    // ============================================================

    /**
     * Soumet un commentaire (route générique)
     *
     * URL   : POST /api/commentaires
     * Accès : Public
     * Body  : { "nom": "Hannah", "email": "...", "message": "Amen", "publicationId": 5 }
     *
     * Correspond aux fetch('/api/commentaires', ...) dans dimes.js,
     * marque de bete.js, les ministeres.js, renverse.js
     */
    @PostMapping("/api/commentaires")
    public ResponseEntity<?> soumettre(
        @Valid @RequestBody CommentaireDTO.Request request,
        HttpServletRequest httpRequest
        // HttpServletRequest = pour récupérer l'adresse IP du visiteur
    ) {
        try {
            CommentaireDTO.Response response =
                commentaireService.soumettre(request, "general", httpRequest);

            // HTTP 201 Created
            return ResponseEntity.status(201).body(Map.of(
                "message", "Commentaire soumis avec succès. Il sera visible après modération.",
                "id", response.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * Soumet un commentaire avec une source spécifique
     *
     * URL   : POST /api/commentaires/{source}
     * Accès : Public
     *
     * Correspond aux fetch() avec source dans le JS :
     *   fetch('/api/commentaires/zoom', ...)              → zoom.js
     *   fetch('/api/commentaires/emissions-radios', ...)  → radio.js
     *   fetch('/api/commentaires/enseignements-audios', ...)→ enseign-audio.js
     *
     * @PathVariable source = "zoom", "emissions-radios", "enseignements-audios"...
     */
    @PostMapping("/api/commentaires/{source}")
    public ResponseEntity<?> soumettreAvecSource(
        @PathVariable String source,
        @Valid @RequestBody CommentaireDTO.Request request,
        HttpServletRequest httpRequest
    ) {
        try {
            CommentaireDTO.Response response =
                commentaireService.soumettre(request, source, httpRequest);

            return ResponseEntity.status(201).body(Map.of(
                "message", "Commentaire soumis avec succès. Il sera visible après modération.",
                "id", response.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * Récupère les commentaires approuvés d'une publication
     * Utilisé par le site client pour afficher les commentaires sous un article
     *
     * URL   : GET /api/commentaires/approuves/{publicationId}?page=0&size=10
     * Accès : Public
     */
    @GetMapping("/api/commentaires/approuves/{publicationId}")
    public ResponseEntity<Page<CommentaireDTO.Response>> listerApprouves(
        @PathVariable Long publicationId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
            commentaireService.listerApprouvesPourPublication(
                publicationId, page, size
            )
        );
    }

    // ============================================================
    // ENDPOINTS ADMIN — protégés par JWT
    // ============================================================

    /**
     * Liste les commentaires pour la modération (dashboard)
     *
     * URL    : GET /api/admin/commentaires?statut=EN_ATTENTE&page=0&size=20
     * Accès  : Admin
     *
     * Correspond au tableau "Commentaires en attente" de admin.html
     */
    @GetMapping("/api/admin/commentaires")
    public ResponseEntity<Page<CommentaireDTO.Response>> listerPourAdmin(
        @RequestParam(required = false) StatutCommentaire statut,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
            commentaireService.listerPourAdmin(statut, page, size)
        );
    }

    /**
     * Approuve un commentaire — le rend visible sur le site
     *
     * URL   : PUT /api/admin/commentaires/{id}/approuver
     * Accès : Admin
     *
     * Correspond au bouton ✓ dans le tableau des commentaires de admin.html
     */
    @PutMapping("/api/admin/commentaires/{id}/approuver")
    public ResponseEntity<?> approuver(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(commentaireService.approuver(id));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * Rejette un commentaire
     *
     * URL   : PUT /api/admin/commentaires/{id}/rejeter
     * Accès : Admin
     *
     * Correspond au bouton ✗ dans le tableau des commentaires
     */
    @PutMapping("/api/admin/commentaires/{id}/rejeter")
    public ResponseEntity<?> rejeter(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(commentaireService.rejeter(id));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * Supprime définitivement un commentaire
     *
     * URL   : DELETE /api/admin/commentaires/{id}
     * Accès : Admin
     */
    @DeleteMapping("/api/admin/commentaires/{id}")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        try {
            commentaireService.supprimer(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("erreur", e.getMessage()));
        }
    }
}
