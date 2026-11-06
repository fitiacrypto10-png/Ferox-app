# ⚡ FEROX

<p align="center">
  <img src="app/src/main/res/drawable/ic_bubble_logo.png" width="130" height="130" alt="Ferox Logo" />
</p>

<p align="center">
  <b>Application mobile officielle Ferox — Salons vocaux et communication en direct.</b><br>
  Créée et développée par <b>delt slay</b>.
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

**Ferox** est l'application mobile officielle conçue pour la communauté. Elle combine un salon vocal haute performance, un maintien audio continu en arrière-plan et une interface fluide et épurée pour communiquer facilement et en direct.

---

## 🚀 Fonctionnalités Clés

### 🎮 Salon Vocal Plein Écran
- **Immersion totale** : Affichage fluide sans barre d'adresse ni encombrement visuel.
- **WebRTC haute fidélité** : Transmission vocale instantanée à faible latence.
- **Adaptation intelligente (Mobile First)** : Verrouillage strict du viewport pour un affichage adapté aux smartphones.
- **Safe Area Insets** : Respect méticuleux de la barre d'état (heure, batterie) et de la barre de navigation du smartphone.

### 🎙️ Contrôle Rapide du Micro & Audio Persistant
- Coupure et activation instantanées du micro avec retour d'état visuel clair.
- Synchronisation bidirectionnelle instantanée avec le flux audio WebRTC.
- Maintien audio continu et stable en arrière-plan.

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
- **Services d'arrière-plan** : Foreground Service (`specialUse`), `PARTIAL_WAKE_LOCK`
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
4. Choisissez le nom du dépôt (ex: `ferox-app`) et validez l'envoi.
5. Votre projet est immédiatement publié avec ce `README.md` officiel, votre logo et l'ensemble du code source !

---

<p align="center">
  <i>Développé avec passion pour toute la communauté Ferox.</i>
</p>
