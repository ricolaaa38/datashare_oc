# Scan de sécurité

## Contrôles réalisés

| Contrôle | Commande | Résultat |
| --- | --- | --- |
| Dépendances frontend | `Set-Location frontend; npm audit --json` | 0 vulnérabilité détectée, 400 dépendances analysées |
| Authentification | tests Spring et Playwright | JWT, accès protégés et mauvais mot de passe couverts |
| Upload | tests métier et intégration | extensions interdites, taille maximale, expiration et mot de passe couverts |

## Mesures observées

- Les mots de passe utilisateur et les mots de passe de fichier sont hachés côté backend.
- Le token de téléchargement brut n’est pas stocké : seul son hash SHA-256 est conservé.
- Les téléchargements publics ne révèlent pas la clé de stockage S3.
- Les endpoints authentifiés exigent un bearer token ; `/downloads/**` et l’upload anonyme sont publics par conception.
- Le compose de test utilise des identifiants éphémères et des volumes `tmpfs` pour PostgreSQL et MinIO.

## Risques à traiter

3. Les identifiants du compose sont de développement uniquement ; ils ne doivent jamais être réutilisés en production.



