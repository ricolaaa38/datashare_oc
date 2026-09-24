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
| Taille JS transférée au premier chargement par route | Edge DevTools, onglet Network filtré sur JS, cache désactivé | < 200 Ko par route | OK — maximum mesuré : 164 Ko |
| Largest Contentful Paint (LCP) | Lighthouse (Chrome DevTools ou `npx lighthouse`) | < 2,5 s | OK — maximum mesuré : 0,6 s |
| Cumulative Layout Shift (CLS) | Lighthouse | < 0,1 | OK — maximum mesuré : 0 |
| Time to Interactive / Total Blocking Time | Lighthouse | TBT < 200 ms | OK — maximum mesuré : 10 ms |

### Tailles JavaScript mesurées le 24 septembre 2026

Les valeurs ont été relevées dans Microsoft Edge DevTools, onglet Network filtré sur JS, avec le cache désactivé et après rechargement de chaque page. Avec un filtre actif, Edge affiche le volume transféré correspondant aux requêtes JS filtrées puis le volume total transféré par la page. Le budget de 200 Ko est évalué uniquement sur le volume JS filtré.

| Route | JS transféré | Total page transféré | Seuil JS | Verdict |
| --- | ---: | ---: | ---: | --- |
| `/` | 156 Ko | 233 Ko | < 200 Ko | OK |
| `/files` | 156 Ko | 234 Ko | < 200 Ko | OK |
| `/login` | 154 Ko | 695 Ko | < 200 Ko | OK |
| `/register` | 154 Ko | 231 Ko | < 200 Ko | OK |
| `/download/{token}` | 164 Ko | 217 Ko | < 200 Ko | OK |

Toutes les routes respectent le budget réseau JavaScript. La route `/download/{token}` est la plus lourde avec 164 Ko transférés, soit 36 Ko de marge sous le seuil.

### Résultats Lighthouse Microsoft edge extension du 24 septembre 2026

| Route | LCP | CLS | TBT | Verdict |
| --- | ---: | ---: | ---: | --- |
| `/` | 0,4 s | 0 | 0 ms | OK |
| `/login` | 0,5 s | 0 | 10 ms | OK |
| `/register` | 0,5 s | 0 | 10 ms | OK |
| `/files` | 0,6 s | 0 | 0 ms | OK |
| `/download/{token}` | 0,5 s | 0 | 0 ms | OK |

Toutes les routes mesurées respectent les budgets LCP (< 2,5 s), CLS (< 0,1) et TBT (< 200 ms).

Procédure de mesure :

```powershell
Set-Location frontend
npm run build
npx lighthouse http://localhost:3000 --output=json --output-path=./lighthouse-report.json --chrome-flags="--headless"
```

Le récapitulatif `npm run build` permet de contrôler la composition du build. La taille transférée doit être mesurée dans Edge DevTools avec le filtre JS et le cache désactivé ; toute route dépassant le seuil doit être analysée avant merge, notamment à l’aide du profileur de build ou d’un import dynamique (`next/dynamic`). Les prochaines mesures doivent être ajoutées ici avec la date et le commit correspondant, à la manière des résultats k6 ci-dessus.

## Suivi des métriques clés

Deux catégories de métriques sont suivies dans la durée :

- **Temps de réponse backend** : p95 upload/metadata/download issus de `tests/perf/scenario.js` (section « Scénario » ci-dessus), à rejouer à chaque changement notable et à consigner avec la date, le build et le nombre de VU.
- **Taille des fichiers échangés** : taille moyenne et p95 des fichiers déposés (`Content-Length` des requêtes `POST /files` et `POST /anonymous/files`), suivie via les logs structurés ECS du backend (champ HTTP request size) ou un tableau de bord agrégeant ces logs. Une hausse significative de la taille moyenne doit être corrélée avec `upload_duration` et la consommation de stockage MinIO.

## Logs structurés

Le backend est configuré avec `logging.structured.format.console=ecs`. Les logs de la console sont donc JSON ECS et peuvent être ingérés par Application Insights, Elastic ou un collecteur Docker. Les champs à suivre sont `@timestamp`, `log.level`, `service.name`, `message`, `trace.id` quand disponible et les codes HTTP.

Métriques opérationnelles à suivre : taux HTTP 4xx/5xx, p50/p95/p99 upload et download, débit, mémoire JVM, connexions PostgreSQL, erreurs S3 et durée des requêtes.

## Réponse en cas de régression

Rejouer le scénario avec le même nombre de VU, comparer p95 et taux d’erreur au build précédent, puis corréler les timestamps avec les logs backend, PostgreSQL et MinIO. Ne pas augmenter les seuils pour masquer une régression sans analyse de cause.
