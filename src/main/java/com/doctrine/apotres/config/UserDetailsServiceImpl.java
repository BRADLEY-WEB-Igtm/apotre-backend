package com.doctrine.apotres.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.doctrine.apotres.entity.Utilisateur;
import com.doctrine.apotres.repository.UtilisateurRepository;

import java.util.Collections;

/**
 * ============================================================
 * IMPLÉMENTATION UserDetailsService
 *
 * Spring Security a besoin de charger les infos d'un utilisateur
 * par son username pour vérifier ses credentials et ses rôles.
 *
 * Cette classe fait le lien entre Spring Security et notre BD :
 * Spring Security demande → on cherche dans la BD → on retourne
 *
 * @Service = composant de service Spring, injectable
 * ============================================================
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;
    // Notre repository pour accéder à la table utilisateurs

    /**
     * Charge un utilisateur par son nom d'utilisateur
     * Appelé automatiquement par Spring Security lors de l'authentification
     *
     * @param username Le nom d'utilisateur à rechercher
     * @return UserDetails — les infos de l'utilisateur pour Spring Security
     * @throws UsernameNotFoundException si l'utilisateur n'existe pas
     */
    @Override
    public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException {

        // Cherche l'utilisateur en base par son username
        Utilisateur utilisateur = utilisateurRepository
            .findByUsername(username)
            // Optional.orElseThrow = lance une exception si pas trouvé
            .orElseThrow(() ->
                new UsernameNotFoundException(
                    "Utilisateur non trouvé : " + username
                )
            );

        // Vérifie que le compte est actif
        if (!utilisateur.getActif()) {
            throw new UsernameNotFoundException(
                "Compte désactivé : " + username
            );
        }

        // Crée l'objet UserDetails attendu par Spring Security
        // SimpleGrantedAuthority = représente un rôle/permission
        // "ROLE_" est le préfixe standard de Spring Security
        return new User(
            utilisateur.getUsername(),
            // Nom d'utilisateur

            utilisateur.getMotDePasse(),
            // Mot de passe hashé BCrypt (Spring Security le compare lui-même)

            Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name())
                // Ex: "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_EDITEUR"
            )
        );
    }
}
