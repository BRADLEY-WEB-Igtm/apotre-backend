package com.doctrine.apotres.config;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * GESTIONNAIRE GLOBAL DES EXCEPTIONS
 *
 * Intercepte toutes les exceptions non gérées dans les
 * controllers et retourne des réponses JSON propres.
 *
 * Sans ce handler, Spring retournerait des pages HTML d\'erreur
 * incompréhensibles côté frontend.
 *
 * @RestControllerAdvice = intercepte les exceptions de tous les controllers
 * ============================================================
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Entité non trouvée — HTTP 404
     * Ex: publication.id=999 n\'existe pas en BD
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(404).body(
            Map.of("erreur", "Ressource introuvable", "message", e.getMessage())
        );
    }

    /**
     * Identifiants incorrects — HTTP 401
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(401).body(
            Map.of("erreur", "Identifiants incorrects",
                   "message", "Nom d\'utilisateur ou mot de passe invalide")
        );
    }

    /**
     * Accès refusé — HTTP 403
     * Ex: un EDITEUR essaie de supprimer un utilisateur
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(403).body(
            Map.of("erreur", "Accès refusé",
                   "message", "Vous n\'avez pas les permissions pour cette action")
        );
    }

    /**
     * Validation échouée — HTTP 400
     * Ex: titre manquant, email invalide
     * Retourne le détail de chaque champ invalide
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> erreurs = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(err ->
            erreurs.put(err.getField(), err.getDefaultMessage())
        );
        return ResponseEntity.status(400).body(
            Map.of("erreur", "Données invalides", "champs", erreurs)
        );
    }

    /**
     * Fichier trop grand — HTTP 413
     * Ex: audio MP3 > 50MB
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleFileTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(413).body(
            Map.of("erreur", "Fichier trop grand",
                   "message", "La taille maximum autorisée est 50MB")
        );
    }

    /**
     * Arguments invalides — HTTP 400
     * Ex: mauvaise extension de fichier audio
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(400).body(
            Map.of("erreur", "Requête invalide", "message", e.getMessage())
        );
    }

    /**
     * Erreur serveur générique — HTTP 500
     * Captures toutes les autres exceptions non prévues
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception e) {
        /* Ne pas exposer les détails internes en production */
        System.err.println("Erreur non gérée : " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(500).body(
            Map.of("erreur", "Erreur interne du serveur",
                   "message", "Une erreur inattendue s\'est produite. Réessayez plus tard.")
        );
    }
}
