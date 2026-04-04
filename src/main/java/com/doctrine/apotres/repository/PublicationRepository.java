package com.doctrine.apotres.repository;

// JpaRepository = interface Spring Data qui fournit automatiquement
// toutes les opérations CRUD (Create, Read, Update, Delete)
// sans écrire de code SQL manuel
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.doctrine.apotres.entity.Publication;
import com.doctrine.apotres.entity.Publication.TypePublication;
import com.doctrine.apotres.entity.Publication.StatutPublication;

import java.util.List;

/**
 * ============================================================
 * REPOSITORY PUBLICATION
 *
 * @Repository = composant Spring gérant l'accès aux données
 * JpaRepository<Publication, Long> donne automatiquement :
 *   - save(publication)       → INSERT ou UPDATE
 *   - findById(id)            → SELECT WHERE id = ?
 *   - findAll()               → SELECT * FROM publications
 *   - delete(publication)     → DELETE
 *   - count()                 → SELECT COUNT(*)
 *   - existsById(id)          → SELECT EXISTS(...)
 * ============================================================
 */
@Repository
public interface PublicationRepository extends JpaRepository<Publication, Long> {

    /**
     * Trouve toutes les publications d'un certain statut
     * avec pagination (Page = résultats par page)
     * Pageable = contient le numéro de page et la taille
     * Ex: GET /api/publications?page=0&size=10
     */
    Page<Publication> findByStatutOrderByDateCreationDesc(
        StatutPublication statut,
        Pageable pageable
    );

    /**
     * Trouve toutes les publications publiées d'un certain type
     * Ex: toutes les sessions Zoom publiées
     */
    Page<Publication> findByTypeAndStatutOrderByDateCreationDesc(
        TypePublication type,
        StatutPublication statut,
        Pageable pageable
    );

    /**
     * Trouve les publications d'une catégorie donnée
     * Ex: toutes les publications de la catégorie "Évangiles"
     */
    Page<Publication> findByCategorieAndStatutOrderByDateCreationDesc(
        String categorie,
        StatutPublication statut,
        Pageable pageable
    );

    /**
     * Compte les publications par statut
     * Utilisé pour les cartes statistiques du dashboard
     */
    long countByStatut(StatutPublication statut);

    /**
     * Compte les publications par type
     * Ex: combien de sessions Zoom au total
     */
    long countByType(TypePublication type);

    /**
     * Recherche par titre (LIKE = cherche le terme n'importe où dans le titre)
     * %terme% = contient "terme" (peut avoir du texte avant et après)
     * LOWER = insensible à la casse (minuscules/majuscules)
     */
    @Query("SELECT p FROM Publication p WHERE LOWER(p.titre) LIKE LOWER(CONCAT('%', :terme, '%')) AND p.statut = :statut ORDER BY p.dateCreation DESC")
    Page<Publication> rechercherParTitre(
        @Param("terme") String terme,
        @Param("statut") StatutPublication statut,
        Pageable pageable
    );

    /**
     * Trouve les publications Zoom par jour (LUNDI ou JEUDI)
     */
    Page<Publication> findByTypeAndJourZoomAndStatutOrderByDateCreationDesc(
        TypePublication type,
        String jourZoom,
        StatutPublication statut,
        Pageable pageable
    );

    /**
     * Trouve les 6 dernières publications pour le tableau du dashboard
     */
    List<Publication> findTop6ByOrderByDateCreationDesc();
}
