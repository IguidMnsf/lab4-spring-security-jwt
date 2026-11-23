# LAB4 — Sécuriser l'API avec Spring Security et JWT

Dans ce laboratoire, on repart de l'API produits du LAB3 et on la protège. Une personne crée un compte **USER**, se connecte avec son mot de passe, reçoit un jeton JWT temporaire, puis le joint à ses requêtes. Les accès d'administration sont réservés au rôle **ADMIN**.

Le parcours d'une requête protégée est : **Postman → filtre JWT → identité et rôle → règle d'accès → contrôleur → réponse**. L'API est stateless : chaque requête apporte son jeton et le serveur ne stocke pas de session HTTP.

Le rapport est disponible dans [rapport-lab4.md](rapport-lab4.md) et [rapport-lab4.pdf](rapport-lab4.pdf).

## Ce que contient le projet

- Java 21, Spring Boot, Spring Security, Spring Data JPA, MySQL et JJWT.
- Inscription et connexion avec réponses JSON et validation des champs.
- Mots de passe stockés avec BCrypt, jamais en clair.
- JWT signé, expirant après une durée configurable et vérifié par filtre.
- Rôle `USER` attribué automatiquement lors de toute inscription publique.
- Création initiale d'un ADMIN activée uniquement avec des paramètres d'environnement locaux.
- Accès en lecture aux produits et catégories pour USER/ADMIN ; modifications réservées à ADMIN.
- Commandes créées par un USER, visibles uniquement par leur propriétaire ; ADMIN peut consulter et supprimer toutes les commandes.
- Erreurs `401 Unauthorized` (non connecté/jeton invalide) et `403 Forbidden` (rôle insuffisant).
- CORS local restreint, CSRF désactivé pour l'API Bearer stateless.
- Collection Postman, tests H2 et diagrammes Mermaid dans `docs\uml`.

> **Règle essentielle :** le formulaire public d'inscription n'accepte pas le rôle. Même si quelqu'un ajoute `"role":"ADMIN"` à son JSON, le compte créé reste `USER`. Le compte ADMIN est configuré localement par la personne qui démarre le serveur; ses identifiants ne sont pas inclus dans GitHub.

## 1. Prérequis

Installe :

- JDK 21 ;
- Maven 3.6 ou supérieur ;
- Docker Desktop et son moteur démarré, ou MySQL 8 installé ;
- Postman Desktop.

Vérifie Java et Maven dans PowerShell :

```powershell
java -version
mvn -version
```

Java doit indiquer la version 21. Pour installer le JDK 21 avec `winget` :

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e
```

Si `mvn` n'est pas reconnu, installer Apache Maven depuis [la page officielle de téléchargement](https://maven.apache.org/download.cgi), ajouter le dossier `bin` de Maven au `PATH` Windows, puis ouvrir une nouvelle fenêtre PowerShell. Si Chocolatey est déjà installé, utiliser `choco install maven -y`. Vérifier ensuite `java -version` et `mvn -version`. Si plusieurs versions de Java sont installées, configurer le `PATH` pour utiliser le JDK 21.

## 2. Ouvrir le dossier du LAB4 en local

Les commandes supposent un clone sous `$HOME\Documents\Labs\lab4-spring-security-jwt`. Si le dépôt se trouve ailleurs, remplacer ce chemin par son emplacement réel. Pour un clone existant, le mettre à jour puis entrer dans le projet :

```powershell
Set-Location "$HOME\Documents\Labs\lab4-spring-security-jwt"
git pull --ff-only
Get-Location
```

Si le dépôt n'est pas encore cloné :

```powershell
Set-Location $HOME
git clone https://github.com/IguidMnsf/lab4-spring-security-jwt.git
Set-Location .\lab4-spring-security-jwt
```

Les commandes Maven, Docker Compose et les exemples SQL ci-dessous sont à lancer à la racine du dépôt.

## 3. Démarrer MySQL

### Méthode recommandée : Docker Desktop

Ouvre Docker Desktop et attends que son moteur ait démarré. Dans PowerShell, depuis la racine du dépôt, définis un mot de passe local puis lance :

```powershell
$env:MYSQL_ROOT_PASSWORD = Read-Host "Mot de passe MySQL local pour Docker"
docker compose up -d mysql
docker compose ps
docker compose logs mysql
```

Attends que le conteneur `lab4-mysql` soit en état `healthy`. Docker crée la base **`lab4_db`**. Ce LAB4 utilise volontairement sa propre base, distincte de celle du LAB3.

Pour se connecter à la base avec le client MySQL fourni dans Docker :

```powershell
docker compose exec mysql mysql -uroot -p lab4_db
```

Quitter l'invite MySQL avec `exit`. La commande `docker compose down -v` supprime les données locales ; ne pas l'utiliser sauf pour réinitialiser complètement la base.

### MySQL installé localement

1. Démarre MySQL Server.
2. Crée la base `lab4_db` en exécutant `src\main\resources\db\01-create-database.sql` dans MySQL Workbench.
3. Configure les variables dans la même fenêtre PowerShell qui lancera Spring Boot :

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/lab4_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = Read-Host "Mot de passe MySQL (même valeur que MYSQL_ROOT_PASSWORD pour Docker)"
```

La table `users` est générée par JPA au démarrage. Le script `src\main\resources\db\03-create-users-table.sql` peut également être exécuté après la création de la base.

## 4. Préparer une clé JWT et le compte ADMIN

Le secret JWT doit être privé, aléatoire, base64 et contenir au moins 32 octets. On le crée dans PowerShell sans l'inscrire dans le code ni dans Git :

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Pour Docker, réutiliser comme `DB_PASSWORD` la valeur définie pour `MYSQL_ROOT_PASSWORD`. Pour un MySQL installé localement, saisir son mot de passe. Activer ensuite la création locale du compte ADMIN et choisir un mot de passe de 12 à 72 caractères. Lorsqu'il est demandé par PowerShell, le saisir puis appuyer sur Entrée :

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED = "true"
$env:BOOTSTRAP_ADMIN_USERNAME = "lab-admin"
$env:BOOTSTRAP_ADMIN_EMAIL = "lab-admin@example.test"
$env:BOOTSTRAP_ADMIN_PASSWORD = Read-Host "Choisis le mot de passe du compte ADMIN"
```

Ces variables ne sont disponibles que dans cette fenêtre PowerShell. C'est volontaire : elles ne sont ni enregistrées dans le dépôt, ni partagées avec GitHub. Au premier lancement, l'application crée le compte ADMIN et chiffre son mot de passe avec BCrypt. Si ce nom d'utilisateur existe déjà, elle ne remplace pas son mot de passe.

**Important :** saisis le même identifiant et mot de passe ADMIN dans les variables Postman indiquées à l'étape 7. Ne publie pas une capture montrant la clé JWT ou le mot de passe.

## 5. Compiler et démarrer l'API

Dans la fenêtre qui contient les variables JWT et ADMIN, toujours depuis la racine du dépôt :

```powershell
mvn clean test
mvn spring-boot:run
```

La première commande lance les tests isolés sur H2 et ne requiert pas MySQL. La seconde démarre l'API branchée à MySQL. Laisse ce terminal ouvert : le serveur doit afficher son message de démarrage et écoute sur `http://localhost:8080`.

Dans un second terminal ou dans le navigateur, vérifie que l'API répond :

```powershell
curl.exe -i http://localhost:8080/api/products
```

Sans jeton, cette route doit répondre **`401 Unauthorized`**. C'est un résultat attendu : l'API est bien protégée. Pour arrêter Spring Boot, clique dans le terminal du serveur puis appuie sur `Ctrl+C`.

Après un arrêt puis un redémarrage dans la même fenêtre, les variables JWT et ADMIN restent définies. Dans un nouveau terminal, elles doivent être définies à nouveau. Toute régénération de la clé JWT invalide les jetons existants ; une nouvelle connexion est alors nécessaire.

## 6. Les requêtes disponibles et les rôles

| Méthode | URL | Accès |
|---|---|---|
| POST | `/api/auth/register` | Public : crée toujours un compte USER |
| POST | `/api/auth/login` | Public : vérifie les identifiants et renvoie un JWT |
| GET | `/api/products`, `/api/products/{id}`, `/api/products/search` | USER ou ADMIN |
| POST, PUT, DELETE | `/api/products...` | ADMIN |
| GET | `/api/categories...` | USER ou ADMIN |
| POST, PUT, DELETE | `/api/categories...` | ADMIN ; suppression refusée si la catégorie contient des produits |
| POST | `/api/orders` | USER ; la commande appartient au compte connecté |
| GET | `/api/orders`, `/api/orders/{id}` | USER voit ses commandes ; ADMIN voit toutes les commandes |
| DELETE | `/api/orders/{id}` | ADMIN |

Une requête privée doit porter l'en-tête :

```http
Authorization: Bearer <jeton reçu lors de la connexion>
```

Le jeton expire après une heure par défaut. Le rôle n'est pas accepté depuis le client et l'accès aux commandes est contrôlé aussi par propriétaire, pas seulement par identifiant.

## 7. Postman expliqué pas à pas

Postman sert à envoyer une requête à l'API et à afficher son statut et son JSON. **Postman ne démarre pas MySQL et ne démarre pas l'API** : garder Docker Desktop et le terminal `mvn spring-boot:run` ouverts.

### Importer la collection

1. Ouvrir **Postman Desktop**.
2. Cliquer sur **Import** → **Files**.
3. Dans le clone local, ouvrir `postman` et sélectionner `LAB4-Auth.postman_collection.json`.
4. Dans **Collections**, ouvre **LAB4 - Authentification JWT et rôles**.
5. Ouvrir le menu `...` de la collection puis **Edit** ou **Variables**.
6. Dans **Current value** de `adminUsername`, saisir le nom configuré dans PowerShell (par défaut `lab-admin`).
7. Dans **Current value** de `adminPassword`, saisir le mot de passe ADMIN choisi au démarrage. Conserver cette valeur dans Postman et ne pas la faire apparaître dans les captures.
8. Dans **Current value** de `userPassword`, choisir un mot de passe local pour le compte de démonstration. Laisser `baseUrl` à `http://localhost:8080`. La collection crée un nom USER unique et mémorise les jetons après connexion.

### Exécuter le parcours dans l'ordre

1. Ouvrir **1. Créer un compte USER** et cliquer sur **Send**. Résultat attendu : `201 Created`, avec `"role": "USER"`.
2. Ouvrir **2. Connexion USER et récupération du JWT**, puis cliquer sur **Send**. Résultat attendu : `200 OK`. Le script enregistre le jeton dans `userToken`.
3. Ouvrir **3. Connexion ADMIN et récupération du JWT**, puis cliquer sur **Send**. Le script enregistre le jeton ADMIN dans `adminToken`.
4. Envoyer **4. Refuser les produits sans jeton** : `401 Unauthorized` est attendu.
5. Envoyer **5. Créer une catégorie (ADMIN)**, puis **6. Créer un produit (ADMIN)**. Le statut `201 Created` confirme l'accès ADMIN ; les identifiants sont mémorisés automatiquement.
6. Vérifier les règles des catégories avec **7. ADMIN modifie une catégorie** (`200`), puis **8. USER ne peut pas modifier une catégorie** (`403`).
7. Envoyer **9. USER peut consulter les produits** : la réponse attendue est `200 OK`.
8. Envoyer **10. USER ne peut pas créer un produit** : `403 Forbidden` est attendu. Le compte est connecté, mais ne possède pas le rôle ADMIN.
9. Envoyer **11. USER crée une commande**, puis **12. USER voit ses commandes**. La commande est associée au compte USER.
10. Envoyer **13. ADMIN voit toutes les commandes**.
11. Terminer par **14. USER ne peut pas supprimer une commande** (`403`), **15. ADMIN supprime une commande** (`204`), **16. ADMIN ne supprime pas une catégorie non vide** (`409`) et **17. Refuser un JWT invalide** (`401`).

Dans la réponse en bas de Postman, **Body** montre le JSON, le badge en haut à droite montre le statut (`200`, `401`, etc.) et **Test Results** montre les assertions. Les erreurs `401` et `403` de la collection sont intentionnelles : elles démontrent les protections.

Si la connexion ADMIN échoue, vérifier que l'application a été démarrée avec le bon mot de passe, que `adminUsername`/`adminPassword` sont exacts dans Postman et que le conteneur MySQL est `healthy`. Si Postman ne joint pas `localhost`, ouvrir d'abord `http://localhost:8080/api/products` dans un navigateur, puis utiliser Postman Desktop.

## 8. Tests automatiques

Depuis la racine du dépôt, exécute :

```powershell
mvn test
```

Les tests utilisent une base H2 temporaire. Ils vérifient l'inscription avec rôle USER, le hachage BCrypt, le login, les jetons manquants/invalides, les réponses 401/403, les accès ADMIN et l'isolation des commandes par utilisateur.

## 9. Captures d'écran à préparer

Utiliser `Win + Shift + S`, sélectionner la zone, puis choisir **Enregistrer sous** dans l'outil Capture d'écran. Enregistrer les images dans un dossier dédié, par exemple `Documents\Captures-Labs\LAB4`. Masquer les mots de passe, clés et jetons.

**Captures de démonstration locale :**

1. PowerShell avec `docker compose ps` montrant `lab4-mysql` `healthy`.
2. Terminal serveur après le démarrage réussi de Spring Boot (sans afficher les variables d'environnement).
3. Postman sur **1. Créer un compte USER**, avec le statut `201` et le rôle USER visible.
4. Postman sur **2. Connexion USER**, avec `200` et `tokenType: Bearer`; cache la valeur du jeton avant la capture.
5. Postman sur **8. USER ne peut pas créer un produit**, avec le statut `403`.
6. Postman sur **9. USER crée une commande** ou **10. USER voit ses commandes**, avec `201`/`200`.
7. Facultatif : PowerShell après `mvn test` avec `BUILD SUCCESS`.

**Captures du projet GitHub :**

1. Ouvre [le dépôt du LAB4 sur GitHub](https://github.com/IguidMnsf/lab4-spring-security-jwt) et capture les fichiers.
2. Capture le README affiché par GitHub.
3. Si un workflow de tests est configuré dans **Actions**, ouvre un run récent et capture le job terminé avec succès.
4. Facultatif : affiche la collection Postman ou un diagramme dans `docs/uml`.

GitHub héberge le code ; les tests se lancent localement avec Maven. L'API n'est pas hébergée sur Internet par ce dépôt : les requêtes Postman `localhost:8080` sont servies par l'ordinateur local.

## 10. CORS, CSRF et secrets

- La politique CORS de démonstration n'autorise que `http://localhost:3000` et `http://127.0.0.1:5500`, avec les méthodes API utiles. Adapter cette liste si le frontend local utilise une autre adresse.
- CSRF est désactivé parce que l'API reçoit un jeton Bearer dans l'en-tête et n'utilise pas d'authentification par cookie de navigateur. Si l'authentification est un jour déplacée vers des cookies, cette décision devra être revue.
- La clé JWT et les identifiants ADMIN viennent des variables d'environnement locales. Ne les écris pas dans le code, dans Git, dans Postman partagé ou dans une capture.
- Les fichiers `docs/uml/auth-sequence.md` et `docs/uml/protected-request.md` contiennent les diagrammes de séquence.

## Dépannage pas à pas

**`docker` n'est pas reconnu ou Docker Desktop ne répond pas**

1. Installer et ouvrir Docker Desktop, puis attendre que le moteur soit démarré.
2. Depuis la racine du dépôt, vérifier `docker version` puis `docker compose ps`.
3. En cas d'échec, redémarrer Docker Desktop et consulter **Troubleshoot** dans l'application avant de relancer `docker compose up -d mysql`.

**Le port `3306` est déjà utilisé ou `lab4-mysql` ne démarre pas**

Les LAB3 et LAB4 publient tous deux MySQL sur le port local `3306`; ils ne peuvent pas l'utiliser simultanément avec le même port. Arrêter l'autre base depuis son dossier avec `docker compose down`, sans ajouter `-v`. Vérifier ensuite :

```powershell
docker compose ps
docker compose logs --tail 50 mysql
```

Attendre l'état `healthy`. Si un MySQL Windows occupe toujours le port, l'arrêter depuis **Services** ou modifier temporairement le port hôte dans `docker-compose.yml` en `3307:3306`, puis définir dans la même fenêtre que Spring Boot :

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3307/lab4_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

**`Communications link failure`, `Access denied` ou `Unknown database`**

1. Vérifier que `lab4-mysql` est `healthy` et que la base utilisée est `lab4_db`, distincte de `lab3_db`.
2. Avec Docker, vérifier le nom d'utilisateur `root`, le mot de passe local défini dans `MYSQL_ROOT_PASSWORD` et la base `lab4_db`. Pour MySQL installé localement, exécuter `src\main\resources\db\01-create-database.sql` puis vérifier les variables `$env:DB_URL`, `$env:DB_USERNAME` et `$env:DB_PASSWORD`.
3. Si le mot de passe du conteneur a été changé après la création initiale du volume, le conteneur conserve le mot de passe précédent. Réutiliser les valeurs de ce volume ou le réinitialiser seulement si ses données peuvent être perdues.
4. Consulter `docker compose logs --tail 50 mysql`, puis redémarrer Spring Boot après correction.

**L'application refuse de démarrer à cause de `JWT_SECRET_BASE64`**

La clé doit être base64 et représenter au moins 32 octets aléatoires. Refaire la génération indiquée à l'étape 4 dans la même fenêtre PowerShell que `mvn spring-boot:run`. Les variables d'environnement d'une fenêtre ne sont pas automatiquement disponibles dans une autre. Ne pas copier la clé dans le dépôt, un rapport ou une capture.

**Le démarrage échoue car aucun compte ADMIN n'est configuré**

Vérifier que `BOOTSTRAP_ADMIN_ENABLED`, `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_EMAIL` et `BOOTSTRAP_ADMIN_PASSWORD` sont définies avant le démarrage. Respecter les règles de longueur du mot de passe décrites à l'étape 4. Toute variable nécessaire doit être définie à nouveau dans un nouveau terminal.

**La connexion ADMIN échoue alors que les variables semblent correctes**

Le compte ADMIN n'est créé que s'il n'existe pas déjà. Changer la variable de mot de passe ne modifie pas un compte existant. Vérifier le nom et le mot de passe déjà enregistrés, ou définir un nouveau nom d'utilisateur ADMIN et redémarrer l'application. Mettre ensuite à jour `adminUsername` et `adminPassword` dans les variables Postman locales.

**Postman reçoit `401 Unauthorized`**

1. Envoyer d'abord la requête de connexion du compte concerné et vérifier `200 OK`.
2. Vérifier que le script a enregistré le jeton dans `userToken` ou `adminToken` et que l'en-tête `Authorization` est activé pour la requête.
3. Se reconnecter si le jeton a expiré ou si la clé JWT a été régénérée.
4. `401` sur une requête sans jeton ou avec un jeton invalide est attendu pour les scénarios de démonstration.

**Postman reçoit `403 Forbidden`**

Le jeton est valide, mais le rôle n'autorise pas l'opération. Les modifications de produits et catégories exigent ADMIN ; un USER ne peut voir ou supprimer que ses propres commandes. Utiliser le compte et le jeton correspondant au scénario. Dans cette collection, plusieurs `403` sont des résultats attendus.

**Postman reçoit `Could not get any response` ou une erreur de connexion**

Vérifier d'abord `curl.exe -i http://localhost:8080/api/products` dans une seconde fenêtre. Une réponse `401` confirme que l'API est joignable et protégée. Si la connexion échoue, vérifier le terminal Spring Boot, MySQL et le port `8080`. Avec Postman Web, utiliser Postman Desktop ou l'agent Desktop/local.

**La collection échoue sur l'inscription ou les identifiants**

Exécuter les requêtes dans l'ordre et relancer les deux connexions après l'inscription USER. Si le compte ADMIN existe déjà, utiliser les identifiants de ce compte ; modifier les variables de démarrage ne remplace pas son mot de passe. Ne pas partager les valeurs de la clé JWT ou des mots de passe dans des captures.

**Les tests échouent ou l'API semble difficile à démarrer**

Depuis la racine du dépôt, vérifier `Get-Location`, `Test-Path .\pom.xml`, `java -version` (21) et `mvn -version`. `mvn test` utilise H2 temporaire : MySQL n'a pas besoin d'être démarré pour ces tests. Lire la première erreur Maven avant de corriger les erreurs secondaires.

**Réinitialiser la base Docker**

La commande `docker compose down -v` supprime définitivement le volume du LAB4, les comptes et les données de démonstration. Ne l'utiliser que si ces données peuvent être perdues :

```powershell
docker compose down -v
docker compose up -d mysql
docker compose ps
```

Attendre `healthy`, puis redémarrer Spring Boot et recréer le compte ADMIN avec les variables locales. La commande simple `docker compose down`, sans `-v`, arrête le conteneur mais conserve la base.

## Résultat attendu

**Résultat attendu :** un compte USER peut être créé, recevoir un JWT après authentification, consulter l'API selon son rôle et créer uniquement ses propres commandes. Les opérations d'administration nécessitent un compte ADMIN créé localement de manière contrôlée.
