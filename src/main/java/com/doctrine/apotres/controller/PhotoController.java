package com.doctrine.apotres.controller;

import com.doctrine.apotres.dto.PhotoDTO;
import com.doctrine.apotres.service.PhotoService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * CONTROLLER PHOTO
 *
 * Routes publiques  : GET /api/photos
 * Routes admin      : POST/PUT/DELETE /api/admin/photos
 */
@RestController
@CrossOrigin
public class PhotoController {

    @Autowired
    private PhotoService photoService;


    /* ── ROUTES PUBLIQUES ── */

    @GetMapping("/api/photos")
    public ResponseEntity<Page<PhotoDTO.Response>> listerPubliees(
        @RequestParam(required = false) String categorie,
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(photoService.listerPubliees(categorie, page, size));
    }

    @GetMapping("/api/photos/{id}")
    public ResponseEntity<PhotoDTO.Response> trouverParId(@PathVariable Long id) {
        return ResponseEntity.ok(photoService.trouverParIdDTO(id));
    }


    /* ── ROUTES ADMIN ── */

    @GetMapping("/api/admin/photos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PhotoDTO.Response>> listerAdmin(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(photoService.listerToutesPourAdmin(page, size));
    }

    @PostMapping("/api/admin/photos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PhotoDTO.Response> creer(@Valid @RequestBody PhotoDTO.Request request) {
        return ResponseEntity.ok(photoService.creer(request));
    }

    @PutMapping("/api/admin/photos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PhotoDTO.Response> modifier(
        @PathVariable Long id,
        @Valid @RequestBody PhotoDTO.Request request
    ) {
        return ResponseEntity.ok(photoService.modifier(id, request));
    }

    @DeleteMapping("/api/admin/photos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        photoService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
