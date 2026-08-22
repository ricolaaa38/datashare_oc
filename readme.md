Commande pour executer les tests: 

cd tests
docker compose up --build --abort-on-container-exit --exit-code-from tests
docker compose down -v