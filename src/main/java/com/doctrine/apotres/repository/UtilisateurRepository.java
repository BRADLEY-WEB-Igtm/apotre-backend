package com.doctrine.apotres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.doctrine.apotres.entity.Utilisateur;
import java.util.Optional;

/**
 * ============================================================
 * REPOSITORY UTILISATEUR
 * Gère les comptes administrateurs
 * ============================================================
 */
@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    /**
     * Trouve un utilisateur par son nom d'utilisateur
     * Optional = peut retourner null si aucun résultat
     * Utilisé lors de la connexion pour vérifier les identifiants
     */
    Optional<Utilisateur> findByUsername(String username);

    /**
     * Trouve un utilisateur par son email
     * Utilisé pour la récupération de mot de passe
     */
    Optional<Utilisateur> findByEmail(String email);

    /**
     * Vérifie si un username est déjà utilisé
     * Utilisé lors de la création d'un nouveau compte admin
     */
    boolean existsByUsername(String username);

    /**
     * Vérifie si un email est déjà utilisé
     */
    boolean existsByEmail(String email);
}
