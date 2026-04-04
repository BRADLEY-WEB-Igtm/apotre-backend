package com.doctrine.apotres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.doctrine.apotres.entity.Commentaire;
import com.doctrine.apotres.entity.Commentaire.StatutCommentaire;

/**
 * ============================================================
 * REPOSITORY COMMENTAIRE
 * Gère l'accès aux commentaires en base de données
 * ============================================================
 */
@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    /**
     * Trouve les commentaires par statut avec pagination
     * Utilisé par le dashboard pour la modération
     * Ex: tous les commentaires EN_ATTENTE
     */
    Page<Commentaire> findByStatutOrderByDateCreationDesc(
        StatutCommentaire statut,
        Pageable pageable
    );

    /**
     * Trouve les commentaires d'une publication spécifique
     * Seulement les APPROUVES — ceux visibles sur le site
     */
    Page<Commentaire> findByPublicationIdAndStatutOrderByDateCreationDesc(
        Long publicationId,
        StatutCommentaire statut,
        Pageable pageable
    );

    /**
     * Compte les commentaires en attente
     * Utilisé pour le badge rouge "130" dans la sidebar du dashboard
     */
    long countByStatut(StatutCommentaire statut);

    // Compte les commentaires approuvés d'une publication spécifique
    long countByPublicationIdAndStatut(Long publicationId, StatutCommentaire statut);

    /**
     * Trouve les commentaires par source
     * Ex: source = "zoom" → tous les commentaires des pages Zoom
     */
    Page<Commentaire> findBySourceAndStatutOrderByDateCreationDesc(
        String source,
        StatutCommentaire statut,
        Pageable pageable
    );
}