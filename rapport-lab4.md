# Rapport de réalisation — LAB4

## Authentification JWT et contrôle des rôles

**Étudiant(e) :** ____________________

**Date de remise :** ____________________
**Projet :** [lab4-spring-security-jwt](https://github.com/IguidMnsf/lab4-spring-security-jwt)

## 1. Objectif

Ce laboratoire ajoute l'authentification et l'autorisation à l'API du LAB3. Il met en pratique Spring Security, les jetons JWT, le hachage des mots de passe et les rôles d'accès.

## 2. Réalisation

L'inscription publique crée toujours un compte `USER`. Le mot de passe est encodé avec BCrypt. Après authentification, l'API remet un JWT signé et limité dans le temps ; un filtre vérifie ce jeton pour les requêtes protégées.

Les rôles déterminent les opérations autorisées : les utilisateurs authentifiés peuvent consulter les produits et les catégories, tandis que leurs modifications sont réservées à `ADMIN`. Les commandes appartiennent au compte qui les a créées ; un utilisateur ne voit que ses commandes, alors que l'administrateur peut les consulter et les supprimer.

La clé JWT et les paramètres du compte administrateur initial sont fournis par des variables d'environnement locales. Ils ne sont pas inclus dans le code livré. Docker Compose configure une base `lab4_db` séparée de celle du LAB3.

## 3. Vérifications

La commande `mvn clean verify` a été exécutée avec Java 21. Les **5 tests d'intégration H2 ont réussi**, sans échec ni erreur. Les tests couvrent notamment l'inscription, le hachage BCrypt, la connexion, les jetons invalides, les réponses `401` et `403`, les rôles et la confidentialité des commandes.

La configuration Docker Compose a été analysée avec `docker compose config`. Le moteur Docker n'était pas démarré lors de la révision ; par conséquent, le fonctionnement de l'application contre un serveur MySQL local n'a pas été vérifié durant cette session. Les secrets doivent rester masqués dans les captures et ne jamais être publiés.

## 4. Conclusion

Le projet met en œuvre une API stateless protégée par JWT et des autorisations fondées sur les rôles. Le contrôle des commandes tient compte du propriétaire de la ressource et ne repose pas uniquement sur la connaissance de son identifiant.

## 5. Captures à joindre

Les captures doivent être prises après exécution réelle sur l'ordinateur utilisé pour la démonstration. Masquer les mots de passe, les jetons et la clé JWT :

1. MySQL Docker ou local démarré.
2. Terminal PowerShell montrant l'application Spring Boot démarrée, sans secrets.
3. Postman affichant une inscription `USER` et une connexion réussie.
4. Postman démontrant un accès refusé (`403`) pour une opération réservée à `ADMIN`.
5. Postman affichant la création ou la consultation d'une commande par son propriétaire.
6. Page GitHub du projet ou résultat du workflow GitHub Actions.

**Aucune capture d'écran n'est intégrée à ce rapport.**
