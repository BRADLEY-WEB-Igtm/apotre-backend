package com.doctrine.apotres.service;

import com.cloudinary.Cloudinary;
import com.doctrine.apotres.dto.PhotoDTO;
import com.doctrine.apotres.entity.Photo;
import com.doctrine.apotres.entity.Photo.StatutPhoto;
import com.doctrine.apotres.repository.PhotoRepository;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PhotoService {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private Cloudinary cloudinary;

    // ── CRÉER ──────────────────────────────────────────────────────
    public PhotoDTO.Response creer(PhotoDTO.Request request) {

        Photo photo = new Photo();
        photo.setTitre(request.getTitre());
        photo.setDescription(request.getDescription());
        photo.setUrlPhoto(request.getUrlPhoto());
        photo.setCategorie(request.getCategorie());
        photo.setStatut(request.getStatut() != null ? request.getStatut() : StatutPhoto.PUBLIE);
        photo.setAuteur(SecurityContextHolder.getContext().getAuthentication().getName());

        return convertir(photoRepository.save(photo));
    }

    // ── MODIFIER ───────────────────────────────────────────────────
    public PhotoDTO.Response modifier(Long id, PhotoDTO.Request request) {

        Photo photo = trouver(id);
        photo.setTitre(request.getTitre());
        photo.setDescription(request.getDescription());
        photo.setCategorie(request.getCategorie());

        if (request.getUrlPhoto() != null && !request.getUrlPhoto().isBlank()) {
            photo.setUrlPhoto(request.getUrlPhoto());
        }
        if (request.getStatut() != null) {
            photo.setStatut(request.getStatut());
        }

        return convertir(photoRepository.save(photo));
    }

    // ── SUPPRIMER ──────────────────────────────────────────────────
    public void supprimer(Long id) {

        Photo photo = trouver(id);

        /* Supprime l'image sur Cloudinary */
        supprimerCloudinary(photo.getUrlPhoto(), "image");

        photoRepository.delete(photo);
    }

    // ── LISTER PUBLIC ──────────────────────────────────────────────
    public Page<PhotoDTO.Response> listerPubliees(String categorie, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        if (categorie != null && !categorie.isBlank()) {
            return photoRepository
                .findByCategorieAndStatutOrderByDateCreationDesc(categorie, StatutPhoto.PUBLIE, pageable)
                .map(this::convertir);
        }

        return photoRepository
            .findByStatutOrderByDateCreationDesc(StatutPhoto.PUBLIE, pageable)
            .map(this::convertir);
    }

    // ── LISTER ADMIN ───────────────────────────────────────────────
    public Page<PhotoDTO.Response> listerToutesPourAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        return photoRepository.findAll(pageable).map(this::convertir);
    }

    // ── TROUVER PAR ID ─────────────────────────────────────────────
    public PhotoDTO.Response trouverParIdDTO(Long id) {
        return convertir(trouver(id));
    }

    // ── SUPPRESSION CLOUDINARY ─────────────────────────────────────
    private void supprimerCloudinary(String url, String resourceType) {
        if (url == null || url.isBlank()) return;
        if (!url.contains("cloudinary.com")) return;
        try {
            String[] parties = url.split("/upload/");
            if (parties.length < 2) return;
            String droite = parties[1].replaceFirst("v\\d+/", "");
            int dernierPoint = droite.lastIndexOf('.');
            String publicId = dernierPoint > 0 ? droite.substring(0, dernierPoint) : droite;
            Map result = cloudinary.uploader().destroy(publicId, Map.of("resource_type", resourceType));
            System.out.println("Cloudinary photo supprimée : " + publicId + " → " + result.get("result"));
        } catch (Exception e) {
            System.err.println("Erreur suppression Cloudinary : " + e.getMessage());
        }
    }

    // ── UTILITAIRE ─────────────────────────────────────────────────
    private Photo trouver(Long id) {
        return photoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Photo introuvable : " + id));
    }

    private PhotoDTO.Response convertir(Photo p) {
        PhotoDTO.Response dto = new PhotoDTO.Response();
        dto.setId(p.getId());
        dto.setTitre(p.getTitre());
        dto.setDescription(p.getDescription());
        dto.setUrlPhoto(p.getUrlPhoto());
        dto.setCategorie(p.getCategorie());
        dto.setStatut(p.getStatut());
        dto.setAuteur(p.getAuteur());
        dto.setDateCreation(p.getDateCreation());
        dto.setDateModification(p.getDateModification());
        return dto;
    }
}
