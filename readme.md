## Tests et couverture

```powershell
cd tests
docker compose up --build --abort-on-container-exit --exit-code-from tests
docker compose down -v
```

La stack démarre PostgreSQL, MinIO, le backend et le frontend, puis exécute les tests Maven, Jest, Playwright et k6.

Les rapports sont exportés dans `tests/reports` :

- `backend/index.html` et `backend/jacoco.xml` : couverture JaCoCo des services backend, avec un seuil Maven de 70 %.
- `frontend/index.html` et `frontend/lcov.info` : couverture Jest du frontend.
- `e2e/index.html` et `e2e/junit.xml` : rapport HTML et JUnit Playwright.

Pour lancer uniquement les tests unitaires du frontend :

```powershell
npm --prefix frontend run test:coverage
```