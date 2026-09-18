# Scan de sécurité

## Contrôles réalisés

| Contrôle | Commande | Résultat |
| --- | --- | --- |
| Dépendances frontend | `Set-Location frontend; npm audit --json` | 0 vulnérabilité détectée, 400 dépendances analysées |
| Images et filesystem | Trivy | non exécuté : image Trivy indisponible localement |
| Authentification | tests Spring et Playwright | JWT, accès protégés et mauvais mot de passe couverts |
| Upload | tests métier et intégration | extensions interdites, taille maximale, expiration et mot de passe couverts |

## Mesures observées

- Les mots de passe utilisateur et les mots de passe de fichier sont hachés côté backend.
- Le token de téléchargement brut n’est pas stocké : seul son hash SHA-256 est conservé.
- Les téléchargements publics ne révèlent pas la clé de stockage S3.
- Les endpoints authentifiés exigent un bearer token ; `/downloads/**` et l’upload anonyme sont publics par conception.
- Le compose de test utilise des identifiants éphémères et des volumes `tmpfs` pour PostgreSQL et MinIO.

## Risques à traiter

1. `backend/src/main/resources/application.properties` contient une valeur JWT par défaut codée en dur. En production, fournir `JWT_SECRET_KEY` par secret manager et refuser le démarrage si la valeur n’est pas remplacée.
2. `minio/minio:latest` n’est pas immuable. Utiliser un tag de version contrôlé et scanner les images avec Trivy dans CI.
3. Les identifiants du compose sont de développement uniquement ; ils ne doivent jamais être réutilisés en production.
4. `npm audit` couvre le frontend, mais le graphe Maven doit aussi être scanné en CI avec OWASP Dependency-Check ou Trivy.

## Procédure CI recommandée

```powershell
Set-Location frontend; npm ci; npm audit --audit-level=high
docker run --rm -v "${PWD}:/repo" aquasec/trivy:latest fs --scanners vuln --severity HIGH,CRITICAL /repo
docker run --rm aquasec/trivy:latest image datashare-backend:ci
```

Une alerte `HIGH` ou `CRITICAL` doit bloquer la livraison jusqu’à mise à jour, justification documentée ou exception approuvée.
