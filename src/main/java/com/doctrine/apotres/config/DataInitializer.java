package com.doctrine.apotres.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.doctrine.apotres.repository.UtilisateurRepository;
import com.doctrine.apotres.service.AuthService;

/**
 * ============================================================
 * INITIALISEUR DE DONNÉES
 *
 * Exécuté AUTOMATIQUEMENT au démarrage de l'application.
 * Crée le compte Super Admin par défaut si la base est vide.
 *
 * CommandLineRunner = interface Spring Boot exécutée au démarrage
 * @Component = Spring le détecte et l'exécute automatiquement
 *
 * IMPORTANT : Change le mot de passe après le premier démarrage !
 * ============================================================
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UtilisateurRepository utilisateurRepository;
    // Pour vérifier si des admins existent déjà

    @Autowired
    private AuthService authService;
    // Pour créer le compte admin avec mot de passe hashé

    /**
     * Méthode exécutée au démarrage de l'application
     * args = arguments passés en ligne de commande (non utilisés ici)
     */
    @Override
    public void run(String... args) throws Exception {

        // Vérifie si des utilisateurs existent déjà en base
        if (utilisateurRepository.count() == 0) {
            // count() = SELECT COUNT(*) FROM utilisateurs
            // Si 0 → la table est vide → on crée le premier admin

            System.out.println("🔧 Aucun admin trouvé — création du compte Super Admin par défaut...");

            // Crée le compte Super Admin
            authService.creerAdmin(
                "fridolinbradley",          // username
                "admin@doctrineapotres.com", // email
                "Admin@2026!",              // mot de passe (CHANGE CECI !)
                "Super Admin"               // nom complet
            );

            System.out.println("✅ Compte Super Admin créé !");
            System.out.println("   Username : fridolinbradley");
            System.out.println("   Mot de passe : Admin@2026!");
            System.out.println("   ⚠️  CHANGEZ LE MOT DE PASSE APRÈS LA PREMIÈRE CONNEXION !");

        } else {
            // Des admins existent déjà — pas besoin d'en créer
            System.out.println("✅ " + utilisateurRepository.count() + " admin(s) trouvé(s) en base.");
        }
    }
}
