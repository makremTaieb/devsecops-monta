# Démarrage local avec Docker Compose

## Prérequis

- Docker Desktop ou Docker Engine avec Compose v2
- Environ 8 Go de RAM disponibles
- Les ports 4200, 8081, 8082, 8083, 8084, 8085, 8087, 8761, 9090, 3000 et 3306 libres

## Démarrage

```bash
docker compose config
docker compose up --build -d
docker compose ps
```

L'interface est disponible sur <http://localhost:4200>. Eureka est sur
<http://localhost:8761>, Prometheus sur <http://localhost:9090> et Grafana sur
<http://localhost:3000> (`admin` / `admin` par défaut en local).

Les valeurs par défaut permettent de démarrer la plateforme sans Jenkins ni serveur
SMTP. Pour déclencher réellement des pipelines ou envoyer des emails, copier
`.env.example` vers `.env` et renseigner les identifiants correspondants.

## Vérification rapide

```bash
curl http://localhost:8084/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:4200
```

## Arrêt

```bash
docker compose down
```

Les données MySQL, Prometheus et Grafana sont conservées dans des volumes nommés.
Pour réinitialiser volontairement toutes les données locales :

```bash
docker compose down -v
```
