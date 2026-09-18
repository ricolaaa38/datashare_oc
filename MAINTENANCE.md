# Maintenance

## Démarrage et arrêt

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f backend frontend
docker compose down
```

Pour repartir d’un environnement local vierge, utiliser `docker compose down -v`. Cette commande supprime les données PostgreSQL et MinIO locales.

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
