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

## Mise à jour

Mettre à jour les lockfiles avec `npm ci` en CI, vérifier les changements par `npm audit`, puis reconstruire les images. Pour Maven, conserver le wrapper `mvnw` et valider chaque mise à jour par `verify` et le compose de tests.
