package com.doctrine.apotres;

// SpringApplication = classe qui démarre le serveur Spring Boot
import org.springframework.boot.SpringApplication;

// @SpringBootApplication = annotation principale qui active :
// - La configuration automatique (auto-configuration)
// - Le scan des composants (@Service, @Controller, @Repository...)
// - La configuration Spring
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================
 * CLASSE PRINCIPALE — Point d'entrée du backend
 * C'est ici que démarre toute l'application Spring Boot
 *
 * Pour lancer : mvn spring-boot:run
 * Ou : java -jar apotres-backend-1.0.0.jar
 * ============================================================
 */
@SpringBootApplication
public class ApotresBackendApplication {

    public static void main(String[] args) {
        // SpringApplication.run() démarre le serveur Tomcat intégré
        // et initialise tous les composants de l'application
        SpringApplication.run(ApotresBackendApplication.class, args);

        System.out.println("✅ Backend Doctrine des Apôtres démarré sur http://localhost:8080");
    }
}
