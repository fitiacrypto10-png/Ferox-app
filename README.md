# ⚡ FEROX — Chat Vocal & Bulle Flottante (Truckers of Europe)

<p align="center">
  <img src="app/src/main/res/drawable/ic_bubble_logo.png" width="130" height="130" alt="Ferox Logo" />
</p>

<p align="center">
  <b>Application mobile Android de communication vocale en direct pour la communauté <i>Truckers of Europe</i>.</b><br>
  Bulle ronde style Messenger en jeu, maintien audio continu en arrière-plan et design épuré inspiré d'iOS.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-Bêta%201.0-0A84FF?style=for-the-badge" alt="Version Bêta 1.0" />
  <img src="https://img.shields.io/badge/Statut-100%25%20Gratuit-10B981?style=for-the-badge" alt="100% Gratuit" />
  <img src="https://img.shields.io/badge/Android-7.0%2B%20(API%2024%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Télécharger-APK%20(Direct)-FF7043?style=for-the-badge&logo=android&logoColor=white" alt="Télécharger APK" />
  <img src="https://img.shields.io/badge/Créateur-delt%20slay-blue?style=for-the-badge&logo=facebook&logoColor=white" alt="delt slay" />
</p>

---

## 📥 Télécharger l'APK

Vous pouvez installer directement l'application **Ferox** sur votre smartphone Android :

- **Option 1 (Recommandée - GitHub Releases)** : Rendez-vous dans la section [Releases](../../releases) de ce dépôt GitHub pour télécharger la dernière version de `Ferox-v1.0-Beta.apk`.
- **Option 2 (Fichier inclus dans le dépôt)** : Téléchargez directement le fichier [`release/Ferox-v1.0-Beta.apk`](release/Ferox-v1.0-Beta.apk) présent dans ce dépôt.

> 💡 **Installation sur Android** : Une fois le fichier `.apk` téléchargé, appuyez dessus pour lancer l'installation. Si Android affiche un message de sécurité, autorisez l'installation depuis cette source (*Paramètres > Sécurité > Installer des applications inconnues*).

---

## 🌟 Présentation

**Ferox** est l'application compagnon ultime conçue sur mesure pour les chauffeurs et joueurs de **Truckers of Europe**. Elle combine un salon vocal haute performance avec une **bulle flottante ronde extérieure** (style Messenger) permettant de communiquer et de couper son micro en pleine partie sans jamais devoir quitter le jeu.

---

## 🚀 Fonctionnalités Clés

### 🎮 Salon Vocal Communautaire Plein Écran
- **Immersion totale** : Affichage fluide sans barre d'adresse ni encombrement visuel.
- **WebRTC haute fidélité** : Transmission vocale instantanée à faible latence.
- **Adaptation intelligente (Mobile First)** : Verrouillage strict du viewport pour éviter tout débordement horizontal ou effet de zoom arrière.
- **Safe Area Insets** : Respect méticuleux de la barre d'état (heure, batterie) et de la barre de navigation gesture du smartphone pour des boutons 100% accessibles.

### 💬 Bulle Ronde Flottante Extérieure (Style Messenger)
- **Affichage exclusif en jeu** : La bulle apparaît automatiquement lorsque vous quittez l'app ou lancez *Truckers of Europe*, et s'efface proprement quand vous revenez dans l'appli.
- **Auto-Snap Magnétique (« Pile Poil »)** : Déplacement fluide au doigt avec retour automatique et fixation douce contre le bord gauche ou droit le plus proche.
- **Audio persistant en arrière-plan** : Service d'avant-plan avec `WAKE_LOCK` évitant toute coupure de voix en pleine partie.

### 🎙️ Mini-Fenêtre de Contrôle Ultra-Compacte (125 dp)
- Un simple tap sur la bulle ouvre un menu rapide compact avec l'action directe :
  - 🔴 **« Couper micro »**
  - 🟢 **« Activer micro »**
- Synchronisation bidirectionnelle instantanée avec le flux audio WebRTC et le microphone Android.

### 🛡️ Fiche Créateur & Avertissement Anti-Scam
- **Créateur officiel** : Conçu et développé exclusivement par **[delt slay](https://www.facebook.com/profile.php?id=61594951162159)**.
- **Photo de profil Facebook certifiée** : Intégrée avec médaillon néon bleu et badge vérifié.
- **100% Gratuit & Anti-Fraude** : Mise en garde explicite contre les arnaques, faux vendeurs et versions payantes frauduleuses.

### 🍎 Design & Thème Inspiré d'iOS Dark Mode
- Arrière-plan noir absolu OLED (`#000000`).
- Cartes en verre dépoli translucide avec micro-bordures élégantes.
- Bouton d'accès direct fixé en bas d'écran (*Sticky Footer*), immédiatement visible sans aucun défilement.
- Écran de démarrage avec animation soignée et indicateur rotatif fluide.

---

## 👤 Propriété & Licence Officielle

| Propriété | Détails |
| :--- | :--- |
| **Auteur & Développeur** | **delt slay** |
| **Profil Facebook officiel** | [facebook.com/profile.php?id=61594951162159](https://www.facebook.com/profile.php?id=61594951162159) |
| **Licence d'utilisation** | Propriété originale exclusive de delt slay. Strictement gratuit. |
| **Version actuelle** | **Bêta 1.0** |

---

## 🛠️ Stack Technique

- **Langage** : 100% Kotlin
- **Interface UI** : Jetpack Compose (Material Design 3)
- **Architecture** : MVVM (Model-View-ViewModel), StateFlow, Coroutines
- **Réseau & WebRTC** : Android WebView avec accélération matérielle, cookies tiers et hooks JavaScript de capture audio
- **Services d'arrière-plan** : Foreground Service (`specialUse`), `SYSTEM_ALERT_WINDOW`, `PARTIAL_WAKE_LOCK`
- **Détection réseau** : `ConnectivityManager` avec debounce antibruit de 1,8 s (évite les micro-coupures en Wi-Fi)
- **Chargement d'images** : Coil Compose

---

## 📦 Compilation & Déploiement

### Prérequis
- Android SDK 24+ (Android 7.0 Nougat ou supérieur)
- JDK 11 ou 17
- Gradle 8.x

### Commandes utiles

```bash
# Lancer les tests unitaires et Robolectric
gradle :app:testDebugUnitTest

# Compiler l'APK de débogage
gradle :app:assembleDebug

# Compiler le bundle de release
gradle :app:bundleRelease
```

---

## 📤 Comment publier sur votre compte GitHub depuis AI Studio

1. Dans **Google AI Studio Build**, ouvrez le menu **Settings** (icône d'engrenage en haut à droite).
2. Sélectionnez **« Push to GitHub »** (ou **« Connect GitHub »**).
3. Autorisez l'accès à votre compte GitHub personnel.
4. Choisissez le nom du dépôt (ex: `ferox-voice-truckers`) et validez l'envoi.
5. Votre projet est immédiatement publié avec ce `README.md` officiel, votre logo et l'ensemble du code source !

---

<p align="center">
  <i>Développé avec passion pour tous les chauffeurs de la communauté Truckers of Europe. 🚚💨</i>
</p>
