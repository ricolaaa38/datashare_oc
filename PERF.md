# Performance et observabilité

## Scénario

`tests/perf/scenario.js` lance 10 VU pendant 30 secondes. Chaque itération mesure :

1. `GET /actuator/health` ;
2. `POST /anonymous/files` avec un petit fichier multipart ;
3. `GET /downloads/{token}/metadata` ;
4. `GET /downloads/{token}`.

Les métriques métier sont `upload_duration`, `metadata_duration` et `download_duration`. Les seuils sont un taux d’erreur inférieur à 1 %, un p95 inférieur à 1,5 s pour upload/download et inférieur à 500 ms pour metadata.

Exécution :

```powershell
Set-Location tests
docker compose up --build --abort-on-container-exit --exit-code-from tests
```

Le résumé k6 est affiché dans le conteneur `tests`. Les résultats d’une exécution doivent être conservés avec le numéro de build et les variables de charge utilisées ; aucune valeur de latence n’est inventée dans ce document tant que k6 n’a pas été exécuté sur l’environnement cible.

### Résultat du 18 septembre 2026

Avec 10 VU pendant 30 secondes : 290 itérations, 1 160 requêtes HTTP, 1 740 checks réussis sur 1 740 et 0 % d’erreur. Les p95 observés sont :

| Opération | p95 | Seuil | Verdict |
| --- | ---: | ---: | --- |
| Upload | 36,74 ms | < 1 500 ms | OK |
| Metadata | 12,85 ms | < 500 ms | OK |
| Download | 19,93 ms | < 1 500 ms | OK |

Le débit observé est de 9,57 itérations/s, soit environ 38,27 requêtes/s. Cette mesure est un smoke performance local avec de petits fichiers et stockage Docker ; elle ne constitue pas un dimensionnement de production.

Les healthchecks backend et frontend sont requis avant l’exécution. Pour isoler k6 après le démarrage de la stack :

```powershell
Set-Location tests
docker compose run --rm --no-deps tests npm run test:perf
```

## Analyse

Le chemin upload mesure ensemble la réception multipart, la validation, l’écriture PostgreSQL, l’écriture MinIO et la création du token. Le chemin download mesure la résolution du hash, la lecture MinIO et le streaming HTTP. Une hausse de `upload_duration` indique généralement PostgreSQL/MinIO ou la taille du fichier ; une hausse de `download_duration` indique plutôt le stockage ou le réseau.

## Budget de performance frontend

Le budget porte sur la taille du bundle livré au navigateur et sur l’expérience perçue lors du chargement des pages `/`, `/login`, `/register`, `/files` et `/download/[token]`.

| Métrique | Outil | Seuil | Verdict |
| --- | --- | --- | --- |
| Taille JS first load par route | `npm --prefix frontend run build` (colonne « First Load JS » du récapitulatif Next.js) | < 200 Ko par route | à mesurer sur le build de référence |
| Largest Contentful Paint (LCP) | Lighthouse (Chrome DevTools ou `npx lighthouse`) | < 2,5 s | à mesurer |
| Cumulative Layout Shift (CLS) | Lighthouse | < 0,1 | à mesurer |
| Time to Interactive / Total Blocking Time | Lighthouse | TBT < 200 ms | à mesurer |

Procédure de mesure :

```powershell
Set-Location frontend
npm run build
npx lighthouse http://localhost:3000 --output=json --output-path=./lighthouse-report.json --chrome-flags="--headless"
```

Le récapitulatif `npm run build` affiche la taille First Load JS de chaque route ; toute route dépassant le seuil doit être analysée avec `next build --profile` ou un import dynamique (`next/dynamic`) avant merge. Aucune valeur de LCP/CLS/TBT n’est indiquée dans ce document tant que Lighthouse n’a pas été exécuté sur l’environnement cible ; les résultats mesurés doivent être ajoutés ici avec la date et le commit correspondant, à la manière des résultats k6 ci-dessus.

## Suivi des métriques clés

Deux catégories de métriques sont suivies dans la durée :

- **Temps de réponse backend** : p95 upload/metadata/download issus de `tests/perf/scenario.js` (section « Scénario » ci-dessus), à rejouer à chaque changement notable et à consigner avec la date, le build et le nombre de VU.
- **Taille des fichiers échangés** : taille moyenne et p95 des fichiers déposés (`Content-Length` des requêtes `POST /files` et `POST /anonymous/files`), suivie via les logs structurés ECS du backend (champ HTTP request size) ou un tableau de bord agrégeant ces logs. Une hausse significative de la taille moyenne doit être corrélée avec `upload_duration` et la consommation de stockage MinIO.

## Logs structurés

Le backend est configuré avec `logging.structured.format.console=ecs`. Les logs de la console sont donc JSON ECS et peuvent être ingérés par Application Insights, Elastic ou un collecteur Docker. Les champs à suivre sont `@timestamp`, `log.level`, `service.name`, `message`, `trace.id` quand disponible et les codes HTTP.

Métriques opérationnelles à suivre : taux HTTP 4xx/5xx, p50/p95/p99 upload et download, débit, mémoire JVM, connexions PostgreSQL, erreurs S3 et durée des requêtes.

## Réponse en cas de régression

Rejouer le scénario avec le même nombre de VU, comparer p95 et taux d’erreur au build précédent, puis corréler les timestamps avec les logs backend, PostgreSQL et MinIO. Ne pas augmenter les seuils pour masquer une régression sans analyse de cause.
