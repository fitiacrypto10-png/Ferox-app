# Licence d'Appartenance, Conditions d'Utilisation & Profil Créateur Officiel (Ferox v1.0 Bêta)

Implémentation d'un écran d'accueil avec transition après le logo de l'application, présentant les Conditions Générales d'Utilisation (CGU), la licence d'appartenance officielle créée par le développeur Facebook, l'avertissement anti-scam/fraude, la mention de version Bêta 1.0 et la gratuité intégrale.

## User Review & Critical Decisions

> [!IMPORTANT]
> Décisions validées lors de la phase de clarification :
> - **Fréquence d'affichage** : Affiché au premier lancement après l'animation/splash logo, avec sauvegarde de l'acceptation (l'utilisateur n'est plus bloqué aux démarrages suivants). Un bouton d'accès rapide aux conditions reste accessible depuis l'application en cas de besoin.
> - **Profil Facebook en temps réel** : Chargement direct de la photo de profil via le point d'accès Facebook (`https://graph.facebook.com/61594951162159/picture?type=large`) avec fallback local sécurisé, et ouverture du profil au clic sur l'avatar ou le badge créateur.
> - **Contenu textuel & Orthographe** : Correction rigoureuse et formulation juridique / préventive soignée en français (gratuité totale, mise en garde explicite contre le scam/fraude, licence d'appartenance et règles de bonne conduite en vocal pour la communauté *Truckers of Europe*).

- **Choix confirmé 1** : Affichage au premier lancement avec bouton « J'accepte et je continue ».
- **Choix confirmé 2** : Profil Facebook officiel lié dynamiquement (`id=61594951162159`).
- **Choix confirmé 3** : Avertissement anti-scam proéminent avec badge de vérification créateur.

---

## 1. Overview & Core Concept

- **Rôle principal** : Établir la légitimité, la propriété intellectuelle et la sécurité des utilisateurs en affichant un écran de licence et de conditions d'utilisation directement après le splash du logo de Ferox.
- **Transparence & Sécurité** : Identifier sans ambiguïté l'auteur officiel via son profil Facebook direct, certifier la gratuité totale de l'application, et protéger les joueurs de la communauté contre les arnaques et reventes illégales de l'application.
- **Expérience utilisateur** : Fluide, moderne, inspirée des interfaces gaming sombres (Dark Gaming M3) avec les teintes bleu électrique et noir de Ferox.

---

## 2. User Experience & Visual Design

### Parcours utilisateur
1. **Lancement de l'application** : Le logo électrique Ferox s'affiche de manière nette et centrée.
2. **Premier lancement (Conditions d'utilisation)** :
   - Affichage d'une carte plein écran élégante sur fond sombre bleuté (`#0B0E17`).
   - **En-tête de version** : Badge « Version Bêta 1.0 » avec statut « 100% Gratuit ».
   - **Fiche Créateur & Licence d'appartenance** :
     - Avatar rond avec bordure électrique bleue chargeant la photo de profil Facebook en temps réel (`https://graph.facebook.com/61594951162159/picture?type=large`).
     - Nom officiel du créateur avec badge vérifié et lien direct vers `https://www.facebook.com/profile.php?id=61594951162159`.
     - Mention légale de développement et de propriété intellectuelle.
   - **Bloc Avertissement Anti-Fraude & Sécurité (Anti-Scam)** :
     - Boîte d'alerte stylisée (ambre/rouge braise) : « Attention aux arnaques : Ferox est strictement gratuite. N'achetez jamais cette application et ne communiquez aucune coordonnée bancaire. »
   - **Conditions d'utilisation du chat vocal** :
     - Respect mutuel entre chauffeurs / joueurs *Truckers of Europe*.
     - Utilisation responsable du micro et de la bulle en arrière-plan.
   - **Action d'engagement** : Bouton d'action large et interactif « Accepter et continuer » qui enregistre la préférence et ouvre la communauté vocale.
3. **Lancements ultérieurs** : L'accès au web en plein écran se fait directement. Une discrète icône d'information permet de reconsulter la licence et le profil créateur à tout moment.

### Palette de couleurs et style graphique
- **Arrière-plan** : Dégradé profond `#0B0E17` vers `#121726`.
- **Accents principaux** : Bleu électrique néon (`#38BDF8` / `#0284C7`) assorti au logo.
- **Statut Bêta & Alerte Scam** : Ambre lumineux (`#F59E0B`) et Rouge alerte (`#EF4444`).
- **Typographie** : Hiérarchie nette M3 avec textes soignés et orthographe corrigée sans la moindre faute.

---

## 3. Key Product Decisions & Trade-Offs

### Décision 1 : Stockage de l'acceptation
- *Approche choisie* : Persistance locale via `SharedPreferences` (`is_terms_accepted`).
- *Pourquoi* : Léger, instantané, sans latence au démarrage, ne nécessite aucun compte ni serveur distant.

### Décision 2 : Intégration du profil Facebook
- *Approche choisie* : Utilisation de l'URL Graph de Facebook avec Coil pour l'avatar en direct, avec repli automatique sur le logo Ferox en cas d'absence temporaire de connexion internet, et ouverture via `Intent(Intent.ACTION_VIEW)` vers l'application Facebook ou le navigateur.
- *Pourquoi* : Respecte la consigne de temps réel sans dépendre d'un SDK lourd ou d'un token développeur Facebook complexe susceptible d'expirer.

### Décision 3 : Rédaction et correction orthographique
- *Approche choisie* : Relecture complète et formulation juridique professionnelle :
  - *« Licence d'utilisation & Propriété intellectuelle »*
  - *« Application créée par [Nom / Profil Facebook] »*
  - *« Version Bêta 1.0 - Application 100% Gratuite »*
  - *« Avertissement officiel contre la fraude (Anti-Scam) »*
- *Pourquoi* : Corrige toutes les fautes du texte original tout en renforçant la crédibilité de l'application auprès des joueurs.

---

## 4. Technical Architecture & Data Strategy

### Architecture visuelle et composants

```
┌────────────────────────────────────────────────────────┐
│                      MainActivity                      │
│                           │                            │
│           Vérification isTermsAccepted()               │
│             /                          \               │
│     [Non accepté]                  [Déjà accepté]      │
│           │                               │            │
│   ┌───────▼─────────────┐       ┌─────────▼────────┐   │
│   │   TermsAndLicense   │       │   VoiceNookApp   │   │
│   │       Screen        │       │ (Fullscreen Web  │   │
│   │ ─────────────────── │       │   + Bubble FGS)  │   │
│   │ • Avatar Facebook   │       └──────────────────┘   │
│   │ • Profil & Lien     │                              │
│   │ • Version Bêta 1.0  │                              │
│   │ • Alerte Anti-Scam  │                              │
│   │ • CGU Chat Vocal    │                              │
│   │ • Bouton Accepter   │                              │
│   └──────────┬──────────┘                              │
│              │ (Clic Accepter)                         │
│              ▼                                         │
│   setTermsAccepted(true) ──► Transition vers VoiceApp  │
└────────────────────────────────────────────────────────┘
```

### Plan de test et de validation
1. **Compilation** : Vérification du build avec `compile_applet`.
2. **Tests JVM / Robolectric** : Test unitaire vérifiant l'état d'acceptation des conditions d'utilisation et la transition.
3. **Validation visuelle** : Conformité de l'avatar Facebook en temps réel, lien cliquable, alerte anti-scam et design plein écran M3.
