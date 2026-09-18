# DataShare

Application de partage de fichiers sécurisé : dépôt d'un fichier, génération d'un lien de téléchargement avec expiration et mot de passe optionnel, gestion d'un espace personnel pour les utilisateurs inscrits.

- **Backend** : Spring Boot (Java), API REST générée depuis un contrat OpenAPI, authentification JWT, persistance PostgreSQL, stockage des fichiers S3 (MinIO en local).
- **Frontend** : Next.js (App Router) / React / Tailwind CSS.
- **Tests** : JUnit + JaCoCo, Jest, Playwright, k6 — détails dans [TESTING.md](TESTING.md).

## Prérequis

- Docker et Docker Compose (mode d'installation recommandé, seul mode documenté ici).
- Optionnel pour développer sans conteneur : JDK compatible avec `backend/pom.xml`, Node.js 20+ et npm.

## Installation et démarrage

```powershell
docker compose up --build -d
docker compose ps
```

Cette commande construit et démarre l'ensemble de la stack :

| Service | Port | Rôle |
| --- | --- | --- |
| postgres | 5432 | Base de données applicative |
| minio | 9000 (API) / 9001 (console) | Stockage des fichiers (compatible S3) |
| backend | 8080 | API Spring Boot |
| frontend | 3000 | Interface Next.js |

**Il n'existe pas de script d'installation ou de configuration de base de données séparé, et ce par conception :**

- Le schéma PostgreSQL est créé et mis à jour automatiquement par Hibernate au démarrage du backend (`spring.jpa.hibernate.ddl-auto=update`, voir `backend/src/main/resources/application.properties`). Il n'y a pas de migration versionnée type Flyway/Liquibase à ce jour.
- La base, l'utilisateur et le mot de passe PostgreSQL sont créés par le service `postgres` de `docker-compose.yml` à partir des variables `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD`.
- Le bucket S3/MinIO est créé automatiquement par le backend au démarrage s'il n'existe pas (`FileStorageService`).

`docker compose up --build` constitue donc à lui seul la procédure d'installation et de configuration complète.

Arrêt :

```powershell
docker compose down       # conserve les données PostgreSQL et MinIO
docker compose down -v    # supprime aussi les volumes (retour à un environnement vierge)
```

## Utilisation

1. Ouvrir http://localhost:3000.
2. Déposer un fichier en anonyme depuis la page d'accueil, ou créer un compte / se connecter pour accéder à l'espace personnel (`/files`).
3. Récupérer le lien de partage généré (`/download/{token}`), configurable avec une expiration (1 à 7 jours) et un mot de passe optionnel.
4. Depuis l'espace personnel, gérer (lister, taguer, supprimer) ses fichiers déposés.

La documentation interactive de l'API est disponible sur http://localhost:8080/swagger-ui/index.html une fois le backend démarré.

## Configuration (variables d'environnement)

| Variable | Défaut (docker-compose) | Rôle |
| --- | --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | `postgres` / `5432` / `datashare` / `datashare` / `datashare` | Connexion PostgreSQL |
| `S3_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_BUCKET` | `http://minio:9000` / `minioadmin` / `minioadmin` / `datashare-files` | Stockage MinIO/S3 |
| `APP_PUBLIC_URL` | `http://localhost:8080` | URL publique utilisée dans les liens de téléchargement générés |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000`, `3001` | Origines autorisées par le backend |
| `UPLOAD_FORBIDDEN_EXTENSIONS` | `.exe,.bat,.cmd,.sh,.msi,.dll,.com,.scr,.jar,.vbs,.ps1` | Extensions refusées à l'upload |
| `CLEANUP_CRON`, `CLEANUP_BATCH_SIZE` | toutes les 15 min / 200 | Nettoyage des fichiers expirés |
| `JWT_SECRET_KEY`, `JWT_EXPIRATION_MS` | valeur de développement codée en dur | **À remplacer obligatoirement en production**, voir [SECURITY.md](SECURITY.md) |
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080` | URL de l'API injectée au build du frontend (build arg, pas une variable runtime) |

## Développement local sans la stack Docker complète

```powershell
# Démarrer uniquement les dépendances
docker compose up -d postgres minio

# Backend
Set-Location backend
.\mvnw.cmd spring-boot:run

# Frontend
Set-Location frontend
npm ci
npm run dev
```

## Documentation qualité et suivi

- [TESTING.md](TESTING.md) — plan de tests, niveaux couverts et couverture mesurée.
- [SECURITY.md](SECURITY.md) — contrôles de sécurité réalisés et risques ouverts.
- [PERF.md](PERF.md) — performance backend, budget frontend et suivi des métriques clés.
- [MAINTENANCE.md](MAINTENANCE.md) — exploitation, dépannage et mise à jour des dépendances.