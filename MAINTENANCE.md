# Maintenance

## Démarrage et arrêt

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f backend frontend
docker compose down
```

Pour repartir d’un environnement local vierge, utiliser `docker compose down -v`. Cette commande supprime les données PostgreSQL et MinIO locales.

## Documentation technique

### Architecture

DataShare est une application web composée des services suivants :

| Composant | Technologie | Responsabilité |
| --- | --- | --- |
| `frontend` | Next.js 16, React 19, Tailwind CSS 4 | Interface web, session utilisateur et appels REST |
| `backend` | Java 21, Spring Boot 4.0.8 | API REST, règles métier, authentification et orchestration du stockage |
| `postgres` | PostgreSQL 16 | Utilisateurs, fichiers, jetons de téléchargement et tags |
| `minio` | MinIO, compatible S3 | Contenu binaire des fichiers |

Le frontend écoute sur le port `3000`, le backend sur `8080`, PostgreSQL sur `5432` et MinIO sur `9000` (API) / `9001` (console). Les services backend et frontend sont construits à partir de `backend/dockerfile` et `frontend/dockerfile`.

### Flux principaux

- **Téléversement** : le frontend envoie le fichier au backend ; le backend valide la taille et l’extension, enregistre les métadonnées dans PostgreSQL et le contenu dans le bucket S3/MinIO.
- **Partage** : le backend génère un jeton aléatoire, ne conserve que son hash SHA-256, puis renvoie une URL publique avec expiration et mot de passe optionnel.
- **Téléchargement** : le frontend appelle `/downloads/{token}` ; le backend vérifie le jeton, l’expiration et le mot de passe avant de streamer l’objet depuis S3/MinIO.
- **Authentification** : les comptes utilisent un JWT. Les endpoints `/users`, `/auth/login`, `/downloads/**` et le téléversement anonyme sont publics ; les autres opérations nécessitent un bearer token.

### Organisation du code

- `backend/src/main/java` contient les contrôleurs, services, entités JPA, configuration, sécurité JWT et gestionnaire d’exceptions.
- `backend/src/main/resources/static/openapi.yaml` est le contrat d’API. Les interfaces et modèles générés dans `backend/target/generated-sources/openapi` ne doivent pas être modifiés manuellement.
- `frontend/app` contient les routes App Router : `/`, `/login`, `/register`, `/files` et `/download/[token]`.
- `frontend/components` regroupe les composants d’interface et les composants métier ; `frontend/lib/api` contient les clients REST par tag OpenAPI.

### Persistance et stockage

Hibernate utilise `spring.jpa.hibernate.ddl-auto=update` pour créer ou mettre à jour le schéma PostgreSQL au démarrage. Il n’existe pas encore de migrations versionnées Flyway/Liquibase : sauvegarder la base avant toute évolution de modèle en environnement partagé.

Les métadonnées et le contenu sont séparés : une suppression doit retirer l’enregistrement PostgreSQL et l’objet S3. Le bucket configuré par `S3_BUCKET` est créé automatiquement par le backend s’il n’existe pas.

### Paramètres techniques importants

| Paramètre | Usage | Valeur ou contrainte |
| --- | --- | --- |
| `NEXT_PUBLIC_API_URL` | URL de l’API injectée dans le bundle frontend | définie au build, pas à l’exécution |
| `APP_PUBLIC_URL` | Base des liens de téléchargement générés | doit être accessible par les utilisateurs |
| `CORS_ALLOWED_ORIGINS` | Origines autorisées par le backend | doit inclure l’URL frontend |
| `UPLOAD_FORBIDDEN_EXTENSIONS` | Extensions refusées | contrôle complémentaire côté backend |
| `spring.servlet.multipart.max-file-size` | Taille maximale d’un fichier | `1GB` par défaut |
| `CLEANUP_CRON` / `CLEANUP_BATCH_SIZE` | Nettoyage des fichiers expirés | toutes les 15 minutes / 200 par défaut |

Les paramètres complets sont définis dans `backend/src/main/resources/application.properties` et surchargés par les variables d’environnement dans Docker Compose.

## Cycle de validation

Avant toute livraison :

1. lancer `Set-Location backend; .\mvnw.cmd verify -B` ;
2. lancer `Set-Location frontend; npm ci; npm run lint; npm run test:coverage; npm run build` ;
3. lancer la stack de tests Docker complète ;
4. archiver `tests/reports` et le résumé k6 ;
5. lancer les scans de `SECURITY.md`.

## Dépannage rapide

| Symptôme | Vérification |
| --- | --- |
| backend ne démarre pas | `docker compose logs backend`, puis healthcheck PostgreSQL/MinIO |
| upload échoue | vérifier `S3_ENDPOINT`, le bucket `S3_BUCKET`, la taille et l’extension |
| frontend ne joint pas l’API | vérifier `NEXT_PUBLIC_API_URL` au build et `CORS_ALLOWED_ORIGINS` |
| tests E2E instables | attendre les healthchecks, supprimer les volumes de test et rejouer |
| couverture sous le seuil | consulter `backend/target/site/jacoco/index.html` et `frontend/coverage/lcov-report` |

## Données et secrets

Ne jamais committer de secret. En production, injecter les paramètres DB, S3 et JWT via l’environnement ou un secret manager. Remplacer la clé JWT codée en dur de la configuration locale avant déploiement.

## Nettoyage des fichiers expirés

Le job backend exécute le nettoyage selon `CLEANUP_CRON` et traite au plus `CLEANUP_BATCH_SIZE` fichiers par lot. Surveiller ses logs et vérifier que les objets MinIO et les métadonnées PostgreSQL diminuent après expiration.

## Mise à jour des dépendances

### Fréquence

| Type | Fréquence | Déclencheur |
| --- | --- | --- |
| Vulnérabilité de sécurité (CVE haute/critique) | Immédiate | alerte `npm audit` / Dependabot / scan `SECURITY.md` |
| Correctifs (patch) | Mensuelle | revue planifiée |
| Versions mineures | Mensuelle à trimestrielle | revue planifiée, groupées par lot |
| Versions majeures (Spring Boot, Next.js, React) | Ponctuelle, planifiée à l’avance | changelog amont, fin de support d’une version |

### Procédure

1. Identifier les mises à jour disponibles :
   - Frontend : `Set-Location frontend; npm outdated` et `npm audit`.
   - Backend : `Set-Location backend; .\mvnw.cmd versions:display-dependency-updates`.
2. Mettre à jour par petits lots (une dépendance majeure à la fois, les patches/mineures peuvent être groupés).
3. Rejouer le cycle de validation complet (voir « Cycle de validation » ci-dessus) : `mvnw verify`, lint/tests/build frontend, stack Docker de tests.
4. Mettre à jour les lockfiles (`package-lock.json` via `npm install`, puis `npm ci` en CI) et le `pom.xml`.
5. Documenter le changement (version avant/après, raison) dans la pull request.

### Risques par type de dépendance

- **Patch/correctif de sécurité** : risque faible, à appliquer rapidement ; une CI verte (tests + build) suffit à valider.
- **Mineure** : risque faible à modéré (nouvelles API, dépréciations) ; revue du changelog et CI verte requises.
- **Majeure côté backend** (Spring Boot, Java) : risque élevé de changements cassants (auto-configuration, sécurité, JPA) ; exécuter `mvnw verify` complet et rejouer les tests d’intégration avant merge.
- **Majeure côté frontend** (Next.js, React) : risque élevé sur le routage, les Server Components et le build ; exécuter `npm run build`, les tests Jest et impérativement la suite Playwright avant merge.
- **Images Docker de base** (`postgres`, `minio/minio`, images `node`/`eclipse-temurin` du Dockerfile) : figer des tags de version explicites plutôt que `latest`, et scanner l’image avec Trivy avant mise en production (voir [SECURITY.md](SECURITY.md)).
