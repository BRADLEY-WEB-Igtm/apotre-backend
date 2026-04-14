# ============================================================
# Dockerfile — Backend Doctrine des Apôtres
# Déploiement sur Render.com
# ============================================================

# ÉTAPE 1 : Compilation avec Maven
# Utilise une image Java 21 avec Maven pour compiler le projet
FROM maven:3.9.6-eclipse-temurin-21 AS build
# AS build = nom de cette étape (on la référence plus bas)

# Définit le dossier de travail dans le conteneur
WORKDIR /app

# Copie le fichier pom.xml en premier (optimisation du cache Docker)
COPY pom.xml .

# Télécharge les dépendances Maven sans compiler le code
# Cela permet de mettre en cache les dépendances si pom.xml ne change pas
RUN mvn dependency:go-offline -B

# Copie tout le code source
COPY src ./src

# Compile et crée le JAR (sans lancer les tests)
RUN mvn clean package -DskipTests


# ============================================================
# ÉTAPE 2 : Image finale légère pour exécuter l'application
# ============================================================

# Utilise une image Java 21 légère (sans Maven)
FROM eclipse-temurin:21-jre-alpine
# alpine = version très légère de Linux

# Dossier de travail
WORKDIR /app

# Copie uniquement le JAR compilé depuis l'étape précédente
COPY --from=build /app/target/apotres-backend-1.0.0.jar app.jar
# --from=build = prend le fichier depuis l'étape "build"

# Crée le dossier uploads (pour les fichiers temporaires)
RUN mkdir -p uploads/audios uploads/images uploads/pdfs

# Port exposé par l'application
EXPOSE 8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]
# Lance le JAR avec le profil prod défini via variable SPRING_PROFILES_ACTIVE