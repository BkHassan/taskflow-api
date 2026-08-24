# TaskFlow API

API REST de gestion de tâches, niveau **Junior Java Backend**.

Authentification JWT, rôles USER / ADMIN, CRUD des tâches, PostgreSQL, Docker Compose, CI GitHub Actions. Le déploiement AWS EC2 n'est **pas** terminé tant que tu n'as pas suivi [MANUAL_TASKS.md](MANUAL_TASKS.md).

## Stack

| Technologie | Choix réel dans ce repo |
| --- | --- |
| Java | 17 |
| Spring Boot | 4.0.8 (patch 4.0, pas 4.1) |
| Security | Spring Security + JWT (jjwt) |
| Persistence | Spring Data JPA / Hibernate |
| Base locale / tests | H2 (profils `dev` et `test`) |
| Base Docker / cloud | PostgreSQL 16 |
| Docs | springdoc-openapi 3.1 (Swagger UI) |
| Conteneurs | Docker + Docker Compose |
| CI | GitHub Actions (`./mvnw verify`) |
| CD | workflow **manuel** vers EC2 — à activer après AWS |

## Lancer en local

Prérequis : Docker Desktop. JDK 17 est nécessaire pour `mvnw` hors Docker.

```powershell
cd D:\taskflow-api
copy .env.example .env
docker compose up --build
```

- API : http://localhost:8080
- Santé : http://localhost:8080/actuator/health
- Swagger : http://localhost:8080/swagger-ui.html

Comptes de démo (uniquement si `SEED_DEMO_USERS=true`) :

| username | password | rôle |
| --- | --- | --- |
| `demo` | `Demo123!` | USER |
| `admin` | `Admin123!` | ADMIN |

Sans Docker, après installation du JDK 17 :

```powershell
.\mvnw.cmd spring-boot:run
```

Le profil par défaut est `dev` (H2 en mémoire, console H2 sur http://localhost:8080/h2-console).

## API

| Méthode | URL | Accès |
| --- | --- | --- |
| POST | `/api/auth/register` | public |
| POST | `/api/auth/login` | public |
| GET | `/api/auth/me` | JWT |
| GET | `/api/tasks` | JWT — USER : ses tâches ; ADMIN : toutes |
| POST | `/api/tasks` | JWT |
| GET | `/api/tasks/{id}` | JWT + propriétaire ou ADMIN |
| PUT | `/api/tasks/{id}` | JWT + propriétaire ou ADMIN |
| DELETE | `/api/tasks/{id}` | JWT + propriétaire ou ADMIN |
| GET | `/api/admin/users` | ADMIN |

Exemple register / login :

```json
POST /api/auth/register
{
  "username": "hassan",
  "email": "hassan@example.com",
  "password": "Password1"
}
```

Ensuite, header `Authorization: Bearer <token>`.

## Architecture

```
controller/   HTTP, validation @Valid, pas de règle métier
service/      transactions, ownership, mapping DTO
repository/   Spring Data JPA
entity/       tables users + tasks
dto/          request / response (jamais le mot de passe)
security/     JWT create / parse / filter
exception/    erreurs JSON homogènes
config/       SecurityFilterChain, OpenAPI, seeder
```

Flux d'une requête protégée :

1. `JwtAuthenticationFilter` lit `Authorization: Bearer ...`
2. Le token est vérifié (signature HS256 + expiration)
3. L'utilisateur est chargé depuis PostgreSQL / H2
4. `SecurityContext` reçoit `ROLE_USER` ou `ROLE_ADMIN`
5. Le controller appelle le service ; le service vérifie le propriétaire de la tâche

## Décisions d'architecture (à retenir pour les entretiens)

**1. JWT, pas de session serveur.**  
Une API REST est appelée par un front, Postman ou mobile. Le serveur ne garde pas de session HTTP. Le client renvoie le token à chaque requête. C'est pour ça que `SessionCreationPolicy.STATELESS` est configuré.

**2. CSRF désactivé.**  
La protection CSRF sert surtout aux cookies de session navigateur. Ici l'auth est un header `Authorization`. Désactiver CSRF est le choix standard pour une API JWT. Si un jour tu passes en cookie `HttpOnly`, il faudra réactiver CSRF.

**3. BCrypt, jamais le mot de passe en clair.**  
`PasswordEncoder` hashe à l'inscription. Au login, Spring Security compare le hash. Même avec un dump SQL, le mot de passe d'origine n'est pas lisible.

**4. On ne s'inscrit pas ADMIN.**  
`/register` force `Role.USER`. Un ADMIN se crée en base (seeder de démo, ou un script). Sinon n'importe qui pourrait s'auto-promouvoir.

**5. Permissions sur les tâches.**  
USER : CRUD uniquement sur **ses** tâches. ADMIN : lecture / modification de toutes les tâches, plus `GET /api/admin/users`. La règle est dans `TaskService`, pas seulement dans l'URL, pour ne pas oublier un endpoint.

**6. DTO, pas d'entités dans le JSON.**  
L'entité `User` contient le hash du mot de passe. Les records `UserResponse` / `TaskResponse` exposent uniquement ce que le client a le droit de voir.

**7. `open-in-view: false`.**  
Sinon Hibernate peut lancer des requêtes SQL pendant la sérialisation JSON, hors du service. Ici le mapping DTO se fait dans une méthode `@Transactional`, donc la relation `Task.owner` (LAZY) est lisible sans `LazyInitializationException`.

**8. H2 en test / dev, PostgreSQL en Docker.**  
Les tests ne dépendent pas d'une base externe. Docker Compose lance Postgres comme en "prod de junior". `ddl-auto: update` en Docker est acceptable pour un projet junior. En entreprise tu passerais à Flyway.

**9. Spring Boot 4.0.8, pas 4.1.**  
Le repo Initializr était déjà en 4.0. On reste sur la ligne 4.0 (dernier patch) pour Java 17 et moins de casses. springdoc 3.x cible Boot 4 mais s'appuie encore sur Jackson 2 : `jackson-databind` 2.19 est ajouté **uniquement** pour Swagger.

**10. CI ≠ CD.**  
`ci.yml` compile et teste à chaque push : c'est fait. `deploy.yml` est en `workflow_dispatch` : il restera rouge tant que AWS et les secrets GitHub n'existent pas. Ce n'est pas "déployé sur AWS" tant que [MANUAL_TASKS.md](MANUAL_TASKS.md) n'est pas fini.

## Tests

```powershell
.\mvnw.cmd -B test
```

Couverture volontairement junior et utile :

- register / login / validation / 401 sans token
- CRUD d'une tâche par son propriétaire
- un USER ne lit pas la tâche d'un autre (403)
- un ADMIN liste toutes les tâches et les utilisateurs
- 404 si la tâche n'existe pas

## Déploiement

| État | Réalité |
| --- | --- |
| Dockerfile + Compose | Fait |
| CI GitHub Actions | Fait (après `git push`) |
| Instance EC2 + secrets | **À faire à la main** |

Suivre [MANUAL_TASKS.md](MANUAL_TASKS.md) étape par étape.

## Auteur

Hassan Boukatena — Java / Backend Developer
