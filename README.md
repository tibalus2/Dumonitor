# Monitoring Automate Simple

Application Android de monitoring en temps réel pour les systèmes d'automatisation de bâtiments **Distech Controls** (protocole BACnet).

## Présentation

L'application affiche un tableau de bord avec jusqu'à **3 points de données** (température, humidité, pression, etc.) récupérés via l'API REST BACnet d'un contrôleur Distech Controls. Elle est conçue pour être déployée sur un écran dédié en mode kiosque.

## Fonctionnalités

- Affichage en temps réel avec rafraîchissement toutes les **10 secondes**
- **3 métriques configurables** (type BACnet, instance, label, min/max)
- Mode **nuit / jour** persistant
- **Panneau admin secret** (5 taps rapides sur le titre) pour configurer l'IP, les identifiants et les points
- **Démarrage automatique** au boot de l'appareil
- Mode **kiosque** (lock task) pour usage sur tablette dédiée
- Découverte des objets BACnet disponibles depuis le panneau admin

## Stack technique

| Composant | Technologie |
|-----------|------------|
| Plateforme | Android (SDK min 24 / target 36) |
| Langage | Java 11 |
| HTTP | OkHttp 4.12 |
| UI | ConstraintLayout + Material Design |
| Configuration | SharedPreferences |
| Build | Gradle (Kotlin DSL) |

## Architecture

```
MainActivity          → tableau de bord principal
AdminActivity         → configuration (IP, credentials, points)
DistechApiService     → client HTTP BACnet REST (polling async)
ConfigManager         → persistance de la configuration (Singleton)
BootReceiver          → lancement automatique au démarrage
```

## Configuration rapide

1. Lancer l'app et taper **5 fois rapidement** sur le titre pour ouvrir le panneau admin
2. Renseigner l'**URL de base** du contrôleur (ex : `https://192.168.1.12`)
3. Entrer le **login / mot de passe**
4. Configurer les 3 points BACnet (type, instance, label, min, max)
5. Appuyer sur **Tester la connexion** puis **Sauvegarder**

## Mode kiosque (optionnel)

Pour verrouiller l'app sur une tablette dédiée, configurer l'appareil en mode propriétaire via ADB :

```bash
adb shell dpm set-device-owner com.example.monitoring_automate_simple/.MonitoringDeviceAdminReceiver
```

## Notes

- Le certificat SSL auto-signé du contrôleur est accepté sans vérification (réseau local fermé)
- Les identifiants sont stockés en clair dans les SharedPreferences
- Conçu pour un usage sur **réseau interne** uniquement
