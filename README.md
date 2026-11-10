# FilRougeNoahCelian

Application Android de **réveils partagés** : on crée une alarme, on y invite ses contacts, et elle sonne sur le téléphone de chaque membre. Quand une alarme est partagée, toute modification (heure, jours, sonnerie, nom, son, vibrations) passe par un **vote** des membres.

Projet fil rouge du cours de développement mobile — UQAC.

## Fonctionnalités

- **Compte** : inscription, connexion, photo de profil.
- **Contacts** : recherche d'utilisateurs, demandes d'amis, acceptation.
- **Alarmes** : heure, jours de répétition, nom, sonnerie personnalisée, son et vibrations activables. Chaque membre peut activer ou désactiver l'alarme pour lui seul.
- **Alarmes partagées** : ajout de membres, avatars des membres sur chaque carte, mise à jour en temps réel chez tout le monde.
- **Votes** : une modification d'alarme partagée lance un vote à durée limitée ; elle est appliquée quand assez de membres ont voté pour. Un compteur « N votes en cours » sur la carte ouvre la salle de vote.
- **Sonnerie fiable** : l'alarme sonne app fermée, sans réseau (sonneries mises en cache) et après un redémarrage du téléphone.

## Stack

| Domaine | Outils |
|---|---|
| Langage / UI | Kotlin 2.3, Jetpack Compose, Material 3, Navigation Compose |
| Architecture | MVVM : écrans Compose → ViewModels (`StateFlow`) → Repositories |
| Backend | [Supabase](https://supabase.com) via [supabase-kt](https://github.com/supabase-community/supabase-kt) 3.6 : Auth, Postgrest, Realtime, Storage |
| Images | Coil 3 |
| Alarmes | `AlarmManager.setAlarmClock`, `BroadcastReceiver`, service au premier plan |

Android **7.0 (API 24)** minimum.

## Installation

1. Cloner le dépôt et l'ouvrir dans Android Studio.
2. Ajouter les identifiants Supabase dans `local.properties` (à la racine, **jamais commité**) :

   ```properties
   SUPABASE_URL=https://<id-du-projet>.supabase.co
   SUPABASE_PUBLISHABLE_KEY=<clé publishable>
   ```

   Les deux valeurs se trouvent dans le tableau de bord Supabase, **Project Settings → API**. Elles sont injectées dans `BuildConfig` au build. La clé publishable est faite pour être embarquée dans l'app : la sécurité repose sur les règles RLS de la base.

3. Lancer l'app sur un appareil ou un émulateur (`Run` dans Android Studio, ou `./gradlew installDebug`).

### Base de données Supabase

Le projet Supabase doit contenir :

- **Tables** : `accounts`, `friend_requests`, `friendships`, `alarms`, `alarm_polls`, `poll_votes`, avec des règles **RLS** limitant chaque utilisateur à ses propres données.
- **Fonctions SQL (RPC)** : `alarm_set_enabled`, `alarm_add_member`, `alarm_remove_member`, `accept_friend_request`, `poll_create`, `poll_vote`. C'est `poll_vote` qui applique la modification à l'alarme quand le vote est gagné.
- **Realtime** activé sur les tables ci-dessus.
- **Buckets Storage** : `avatars` (photos de profil) et `ringtones` (sonneries).

> Le schéma n'est pas encore versionné dans le dossier `supabase/`. Pour l'exporter :
> `npx supabase db dump --schema public -f supabase/schema.sql` (nécessite Docker),
> ou copier le résultat de `pg_get_functiondef` depuis le SQL Editor.

## Architecture

Code dans `app/src/main/java/ca/uqac/mobile/projetfilrouge/` :

```
data/          Modèles (@Serializable) et Repositories Supabase
  Supabase.kt    client unique, uid courant, observeTable() (temps réel)
viewModel/     Un ViewModel par écran, état exposé en StateFlow
ui/screen/     Écrans Compose (XxxScreen = branchement, XxxContent = affichage + Preview)
ui/components/ Composants partagés (cartes, titres, boutons, switchs…)
ui/theme/      Couleurs, typographie, thème
NavGraph.kt    Routes et barre de navigation
AlarmSync.kt, AlarmScheduler.kt, AlarmSoundService.kt, AppVibrator.kt
receiver/      AlarmReceiver, StopAlarmReceiver, BootReceiver
```

### Temps réel

`observeTable(table) { requête }` s'abonne aux changements Realtime d'une table et relance la requête à chaque changement. Les Repositories exposent ainsi des `Flow` qui se mettent à jour dès qu'un autre membre modifie une alarme ou vote.

### Cycle d'une alarme

1. `AlarmSync` (lancé par `MainActivity`) observe les alarmes actives de l'utilisateur, les programme, annule celles retirées, les sauvegarde localement et met les sonneries en cache.
2. `AlarmScheduler` confie chaque alarme à l'`AlarmManager` (`setAlarmClock`, réveil exact).
3. À l'heure prévue, `AlarmReceiver` reprogramme l'occurrence suivante, lance la vibration et `AlarmSoundService` (notification avec bouton Stop → `StopAlarmReceiver`).
4. Après un redémarrage, `BootReceiver` reprogramme les alarmes depuis la sauvegarde locale.

### Cycle d'un vote

1. Sur une alarme partagée, « Lancer le vote » appelle `poll_create` avec les changements proposés.
2. Le vote apparaît en temps réel chez tous les membres (compteur sur la carte, salle de vote).
3. Chaque membre vote via `poll_vote` ; une fois le seuil atteint, la fonction SQL modifie l'alarme.
4. La modification est propagée par Realtime : chaque téléphone reprogramme l'alarme.

## Textes et traductions

Aucun texte n'est écrit en dur : tout passe par `res/values/strings.xml` (anglais) et `res/values-fr/strings.xml` (français), y compris les pluriels (`plurals`).

## Auteurs

Noah et Célian.
