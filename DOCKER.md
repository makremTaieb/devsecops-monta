# Démarrage local avec Docker Compose

## Prérequis

- Docker Desktop ou Docker Engine avec Compose v2
- Au moins 12 Go de RAM attribués à Docker Desktop pour lancer toute la démo
- Les ports 3000, 3306, 4200, 5000, 8080 à 8087, 8761, 9001 et 9090 libres

## Démarrage

```bash
docker compose config
docker compose up --build -d
docker compose ps
```

L'interface est disponible sur <http://localhost:4200>. Les outils de la démo sont :

| Composant | URL | Identifiants locaux |
|---|---|---|
| Jenkins | <http://localhost:8080> | `admin` / `admin` |
| SonarQube | <http://localhost:9001> | `admin` / `admin-local` |
| Nexus | <http://localhost:8086> | `admin` / `admin-local` |
| Eureka | <http://localhost:8761> | aucun |
| Prometheus | <http://localhost:9090> | aucun |
| Grafana | <http://localhost:3000> | `admin` / `admin` |

Ces identifiants sont volontairement réservés à la démonstration locale. Modifiez-les
dans `.env` avant toute utilisation partagée ou tout déploiement hors du poste local.

Jenkins crée automatiquement le job `devsecops-pipeline`, basé sur
`Jenkinsfile.local`. Ce pipeline compile et teste les microservices, lance l'analyse
SonarQube, exécute Gitleaks et Trivy, construit quatre images Docker de démonstration,
puis publie les JAR et les rapports dans le dépôt Nexus `devsecops-artifacts`.

Pour changer les mots de passe ou utiliser un autre dépôt Git, copiez `.env.example`
vers `.env` avant le premier démarrage et modifiez les valeurs. L'envoi d'e-mails
reste désactivé tant que les paramètres SMTP ne sont pas renseignés.

## Vérification rapide

```bash
curl http://localhost:8084/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:9001/api/system/status
curl http://localhost:8086/service/rest/v1/status
curl http://localhost:8080/login
curl http://localhost:4200
```

Pour lancer manuellement la preuve de concept : ouvrez Jenkins, sélectionnez
`devsecops-pipeline`, puis **Build with Parameters**. Depuis la plateforme, utilisez
exactement `devsecops-pipeline` comme nom de job lors de la création du pipeline.

## Arrêt

```bash
docker compose down
```

Les données MySQL, Prometheus, Grafana, Jenkins, SonarQube et Nexus sont conservées
dans des volumes nommés.
Pour réinitialiser volontairement toutes les données locales :

```bash
docker compose down -v
```
