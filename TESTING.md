# Plan de tests et résultats

## Périmètre

Le projet teste les règles métier de partage de fichiers, l’authentification JWT, la persistance PostgreSQL, le stockage MinIO, l’interface Next.js et les parcours critiques navigateur.

| Niveau | Outil | Cible | Résultat mesuré |
| --- | --- | --- | --- |
| Unitaire backend | JUnit 6 / Spring Boot | services, validation, JWT, stockage | 54 tests, 0 échec |
| Intégration backend | Spring Boot Test | PostgreSQL/MinIO et API | inclus dans les 54 tests Maven, 0 échec |
| Unitaire frontend | Jest + Testing Library | composants, formatage, auth, upload/download | 12 tests, 0 échec |
| End-to-end | Playwright | accueil, inscription, upload, partage, téléchargement, suppression | **5/5 passés** |
| Performance | k6 | health, upload anonyme, metadata, download | **1 740/1 740 checks, 0 % erreur** |

## Exécution complète

Depuis la racine :

```powershell
Set-Location tests
docker compose up --build --abort-on-container-exit --exit-code-from tests
docker compose down -v
```

La stack démarre PostgreSQL et MinIO, attend leurs healthchecks, construit le backend et le frontend, puis lance Maven, Jest, Playwright et k6. Les services communiquent par les noms DNS Docker `postgres-test`, `minio-test`, `backend-test` et `frontend-test`.

Commandes ciblées :

```powershell
Set-Location backend; .\mvnw.cmd verify -B
Set-Location frontend; npm ci; npm test -- --runInBand
Set-Location frontend; npm run test:coverage
Set-Location tests; npx playwright install chromium; npm run test:e2e
Set-Location tests; winget install --id GrafanaLabs.k6 --exact ; k6 version; npm run test:perf
```

## Couverture

Le backend applique un seuil JaCoCo de 70 % sur les instructions pendant `verify`, sur `com.openclassroom.datashare`. Les modèles générés par OpenAPI (`com.datashare`) sont exclus car ils ne représentent pas du code métier directement écrit ou testé. Le rapport filtré affiche **81 % d’instructions** (1 890/2 308) et **72 % de branches**. Une capture est archivée dans `tests/reports/backend/coverage-report.png`. Le frontend a produit :

- statements : **71,91 %** (251/349) ;
- lines : **72,10 %** (243/337) ;
- branches : 58,79 % ;
- functions : 61,90 %.

Le seuil demandé de 70 % est atteint sur le code métier backend et sur les statements frontend. Les rapports détaillés sont générés dans `backend/target/site/jacoco` et `frontend/coverage`, puis copiés dans `tests/reports` lors de l’exécution Docker.

## Résultats et limites

Au 18 septembre 2026, `mvn verify` est vert avec 54 tests et la couverture JaCoCo validée. Jest est vert avec 12 tests. Le build frontend et ESLint passent ; ESLint signale seulement deux avertissements sur les fichiers générés dans `frontend/coverage`.

Le scénario Playwright couvre la création de compte, le téléversement authentifié et anonyme, le téléchargement et la suppression. Les healthchecks backend/frontend garantissent que les tests commencent après readiness. Le scénario k6 mesure le parcours métier upload anonyme, consultation des métadonnées et téléchargement ; ses seuils sont indiqués dans `tests/perf/scenario.js`.

Résultat k6 : 10 VU, 30 secondes, 290 itérations, avec p95 upload **36,74 ms**, metadata **12,85 ms** et download **19,93 ms**. Les seuils sont respectés.

## Critères d’acceptation

- aucun test rouge ;
- seuil JaCoCo backend >= 70 % sur `com.openclassroom.datashare`, hors code OpenAPI généré ;
- couverture frontend statements >= 70 % ;
- taux d’erreur k6 < 1 % ;
- p95 upload et download < 1,5 s, p95 metadata < 500 ms ;
- rapports JUnit, HTML, LCOV et JaCoCo archivés dans `tests/reports`.
