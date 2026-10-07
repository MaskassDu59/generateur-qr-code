# generateur-qr-code

## 1. Introduction

Ce projet consiste à développer en Java une application de bureau permettant de **générer un QR code** à partir d'un texte ou d'un lien saisi par l'utilisateur, puis de **l'exporter dans un fichier PDF**.

L'application est construite selon le patron d'architecture **MVC (Modèle – Vue – Contrôleur)**, qui sépare l'interface graphique, la logique métier et la coordination entre les deux. Cette organisation rend le code plus lisible, plus facile à maintenir et à tester.

## 2. Cahier des charges

Les tâches demandées dans le TP sont les suivantes :

| N° | Tâche | Réalisation |
|----|-------|-------------|
| 1 | Interface utilisateur avec Java Swing (saisie d'un texte, d'un lien…) | Classe `InterfaceUtil` |
| 2 | Génération de fichiers PDF avec une bibliothèque Java (iText) | Classe `PdfExporter` |
| 3 | Génération de QR codes à partir des données saisies | Classe `QRCodeGenerator` |
| 4 | Intégration des QR codes dans les PDF | Méthode `PdfExporter.export()` |
| 5 | Gestion des erreurs et des exceptions | Classe `QRCodeException` et blocs `try / catch` |
| 6 | Tests de l'application, dont des tests unitaires | Tests JUnit 5 (section 8) |
| 7 | Documentation du code et rapport | Javadoc, commentaires et ce document |

## 3. Technologies et bibliothèques

| Élément | Version | Rôle |
|---------|---------|------|
| Java (JDK) | 21 | Langage et plateforme d'exécution |
| Java Swing | inclus dans le JDK | Interface graphique |
| Maven | 3.x | Gestion du projet et des dépendances |
| ZXing (`core` et `javase`) | 3.5.3 | Encodage du QR code et conversion en image |
| iText | 5.5.13.3 | Création du document PDF |
| JUnit Jupiter | 5.10.2 | Tests unitaires |

## 4. Architecture MVC

### 4.1 Principe

Chaque couche a une responsabilité unique :

- **Modèle** : contient les données et la logique métier (génération du QR code, export PDF). Il ne connaît ni Swing ni l'interface.
- **Vue** : affiche les informations et transmet les actions de l'utilisateur (clics). Elle ne contient aucune logique métier.
- **Contrôleur** : reçoit les actions de la vue, demande le travail au modèle, puis met la vue à jour.

### 4.2 Schéma

```
                 ┌─────────────────────────┐
                 │   Utilisateur           │
                 └───────────┬─────────────┘
                             │ saisit / clique
                             ▼
              ┌──────────────────────────────┐
              │   VUE : InterfaceUtil        │
              │   (fenêtre Swing)            │
              └───────┬──────────────▲───────┘
         événements   │              │  affichage, messages
                      ▼              │
              ┌──────────────────────┴───────┐
              │ CONTRÔLEUR : QRCodeController│
              └───────┬──────────────▲───────┘
         appels       │              │  résultat / exception
                      ▼              │
              ┌──────────────────────┴───────┐
              │ MODÈLE : QRCodeModel         │
              │   ├─ QRCodeGenerator (ZXing) │
              │   └─ PdfExporter (iText)     │
              └──────────────────────────────┘
```

### 4.3 Organisation des packages

```
src/
├── Main.java                         Point d'entrée
└── main/
    ├── modele/
    │   ├── QRCodeModel.java
    │   ├── QRCodeGenerator.java
    │   └── PdfExporter.java
    ├── vue/
    │   └── InterfaceUtil.java
    ├── controleur/
    │   └── QRCodeController.java
    └── exception/
        └── QRCodeException.java
test/
└── main/modele/                      Tests unitaires
```

## 5. Description des classes

### 5.1 `Main` — point d'entrée

Elle crée les trois éléments du MVC et les assemble :

1. instanciation du modèle (`QRCodeModel`) ;
2. instanciation de la vue (`InterfaceUtil`) ;
3. instanciation du contrôleur (`QRCodeController`) avec le modèle et la vue, puis appel de `start()` pour afficher la fenêtre.

Le tout est exécuté dans `SwingUtilities.invokeLater(...)`. Swing n'est pas *thread-safe* : toute création d'interface doit se faire sur le thread dédié aux événements graphiques (Event Dispatch Thread).

### 5.2 `InterfaceUtil` — la vue

Fenêtre `JFrame` organisée avec un `BorderLayout` :

| Zone | Contenu |
|------|---------|
| Haut (`NORTH`) | Libellé et zone de saisie `JTextArea` (avec défilement) |
| Centre (`CENTER`) | `JLabel` qui affiche l'image du QR code |
| Bas (`SOUTH`) | Boutons « Generate QR code » et « Export PDF » |

Principales méthodes :

| Méthode | Rôle |
|---------|------|
| `getContent()` | Renvoie le texte saisi |
| `addGenerateListener(...)` / `addExportListener(...)` | Permettent au contrôleur de s'abonner aux clics |
| `displayQRCode(BufferedImage)` | Affiche le QR code |
| `setExportEnabled(boolean)` | Active ou désactive le bouton d'export |
| `showError(...)` / `showInfo(...)` | Boîtes de dialogue d'erreur et d'information |
| `chooseSaveFile()` | Boîte « Enregistrer sous » : filtre `.pdf`, ajout automatique de l'extension, confirmation avant écrasement |

Le bouton d'export est **désactivé au démarrage** : on ne peut exporter qu'après avoir généré un QR code.

### 5.3 `QRCodeController` — le contrôleur

Le constructeur relie les boutons de la vue à deux méthodes :

- `onGenerate()` : récupère le texte, demande la génération au modèle, affiche l'image, active le bouton d'export ;
- `onExport()` : demande un fichier de destination à l'utilisateur, puis demande l'export au modèle.

Dans les deux cas, une `QRCodeException` est interceptée et son message est affiché à l'utilisateur.

### 5.4 `QRCodeModel` — le modèle

Il mémorise l'état de l'application : le **dernier texte encodé** et la **dernière image générée**. Il expose :

- `generate(String)` : génère l'image via `QRCodeGenerator` ; l'état n'est modifié qu'**après** une génération réussie, donc en cas d'échec l'ancien QR code est conservé ;
- `exportToPdf(File)` : exporte l'image via `PdfExporter` (erreur si aucun QR code n'a été généré) ;
- `hasQRCode()`, `getContent()`, `getImage()`.

Un second constructeur permet d'injecter le générateur et l'exporteur (utile pour les tests).

### 5.5 `QRCodeGenerator` — génération du QR code

Utilise la bibliothèque ZXing :

1. **Validation** : contenu non nul et non vide ; taille comprise entre 50 et 2000 pixels ;
2. **Paramètres d'encodage** : jeu de caractères UTF-8 (accents), niveau de correction d'erreur M (environ 15 % du code peut être abîmé sans empêcher la lecture), marge de 1 module ;
3. **Encodage** : `QRCodeWriter.encode(...)` produit une matrice de modules noirs/blancs (`BitMatrix`) ;
4. **Conversion** en `BufferedImage` avec `MatrixToImageWriter`.

### 5.6 `PdfExporter` — export PDF

Utilise la bibliothèque iText pour créer un document **A4** (marges de 50 points) contenant :

1. un titre centré « QR Code généré » ;
2. l'image du QR code, centrée et ajustée à 300 × 300 points au maximum (l'image est d'abord encodée en PNG en mémoire, car iText ne lit pas directement un `BufferedImage`) ;
3. le texte encodé sous l'image.

Le flux de sortie est ouvert avec un `try-with-resources` : il est fermé automatiquement, même en cas d'erreur. L'appel à `document.close()` finalise le PDF.

### 5.7 `QRCodeException` — exception métier

Exception **vérifiée** (hérite de `Exception`) qui regroupe toutes les erreurs prévisibles. Les exceptions techniques des bibliothèques (`WriterException`, `DocumentException`, `IOException`) sont interceptées et converties en `QRCodeException` avec un message lisible. Le contrôleur n'a ainsi qu'un seul type d'exception à gérer.

## 6. Fonctionnement de l'application

### 6.1 Scénario « génération du QR code »

```
Utilisateur      InterfaceUtil     QRCodeController     QRCodeModel     QRCodeGenerator
    │                 │                   │                  │                 │
    │ saisit un texte │                   │                  │                 │
    │ clique Generate │                   │                  │                 │
    ├────────────────►│  événement clic   │                  │                 │
    │                 ├──────────────────►│  onGenerate()    │                 │
    │                 │◄── getContent() ──┤                  │                 │
    │                 │                   ├─ generate(texte)►│  generate(...)  │
    │                 │                   │                  ├────────────────►│
    │                 │                   │                  │◄──── image ─────┤
    │                 │◄─ displayQRCode ──┤◄─── (image) ─────┤                 │
    │                 │◄─ setExportEnabled(true)             │                 │
    │◄── QR affiché ──┤                   │                  │                 │
```

1. L'utilisateur saisit un texte ou un lien et clique sur **Generate QR code**.
2. Le contrôleur récupère le texte et appelle `QRCodeModel.generate(...)`.
3. Le modèle délègue à `QRCodeGenerator`, qui valide les données et produit l'image.
4. Le contrôleur demande à la vue d'afficher l'image et d'activer le bouton d'export.
5. Si une erreur survient (texte vide, trop long…), un message d'erreur s'affiche et l'ancien QR code reste en place.

### 6.2 Scénario « export en PDF »

1. L'utilisateur clique sur **Export PDF**.
2. La vue ouvre la boîte « Enregistrer sous » (nom proposé : `qrcode.pdf`). Si l'utilisateur annule, rien ne se passe.
3. Le contrôleur appelle `QRCodeModel.exportToPdf(fichier)`.
4. `PdfExporter` crée le PDF (titre, QR code, texte).
5. Un message confirme l'enregistrement avec le chemin complet du fichier, ou un message d'erreur s'affiche en cas d'échec.

## 7. Gestion des erreurs

| Situation | Détection | Message affiché |
|-----------|-----------|-----------------|
| Texte vide, ou ne contenant que des espaces | `QRCodeGenerator` | « Le contenu du QR code ne peut pas être vide. » |
| Taille hors des bornes (50–2000 px) | `QRCodeGenerator` | « La taille doit être comprise entre 50 et 2000 pixels. » |
| Texte trop long pour un QR code | `WriterException` de ZXing, convertie | « Impossible de générer le QR code : le contenu est probablement trop long. » |
| Export demandé sans QR code généré | `QRCodeModel` | « Générez d'abord un QR code avant de l'exporter. » |
| Image ou fichier de destination absent | `PdfExporter` | « Aucun QR code à exporter. » / « Aucun fichier de destination choisi. » |
| Dossier inexistant, fichier verrouillé, disque plein | `IOException`, convertie | « Impossible d'écrire le fichier PDF : … » |
| Erreur interne iText | `DocumentException`, convertie | « Erreur lors de la création du document PDF. » |

Autres protections :

- le bouton d'export est désactivé tant qu'aucun QR code n'existe ;
- une confirmation est demandée avant d'écraser un fichier existant ;
- l'extension `.pdf` est ajoutée automatiquement si elle est absente ;
- le flux de sortie est fermé automatiquement grâce au `try-with-resources`.

## 8. Tests

### 8.1 Tests unitaires (JUnit 5)

Les tests portent sur la couche **modèle**, qui contient la logique métier et ne dépend pas de l'interface graphique.

**`QRCodeGeneratorTest`**

| Test | Vérifie que… |
|------|--------------|
| `generateReturnsImageOfRequestedSize` | l'image a les dimensions demandées |
| `generatedQRCodeCanBeDecodedBack` | en relisant le QR code généré, on retrouve exactement le texte d'origine (test « aller-retour ») |
| `emptyContentThrows` | un texte vide ou fait d'espaces est refusé |
| `nullContentThrows` | un contenu `null` est refusé |
| `invalidSizeThrows` | les tailles trop petites ou trop grandes sont refusées |
| `tooLongContentThrows` | un texte de 10 000 caractères est refusé |

**`PdfExporterTest`**

| Test | Vérifie que… |
|------|--------------|
| `exportCreatesValidPdfFile` | le fichier est créé, non vide, et commence par la signature `%PDF-` |
| `exportWithoutImageThrows` | l'export sans image est refusé |
| `exportWithoutFileThrows` | l'export sans fichier de destination est refusé |
| `exportToInvalidPathThrows` | un chemin invalide produit une `QRCodeException` |

**`QRCodeModelTest`**

| Test | Vérifie que… |
|------|--------------|
| `newModelHasNoQRCode` | l'état initial est vide |
| `generateStoresContentAndImage` | le contenu et l'image sont mémorisés après génération |
| `failedGenerationKeepsPreviousQRCode` | un échec ne remplace pas le QR code précédent |
| `exportWithoutGenerationThrows` | l'export avant génération est refusé |
| `exportAfterGenerationWorks` | le scénario complet générer puis exporter fonctionne |

Les tests d'export utilisent `@TempDir` : JUnit fournit un dossier temporaire supprimé automatiquement, donc aucun fichier parasite n'est laissé.

Exécution : `mvn test`

> 📷 *Insérer ici une capture d'écran du résultat de `mvn test` (ou de la vue JUnit d'Eclipse).*

### 8.2 Tests manuels de l'interface

| Test | Action | Résultat attendu |
|------|--------|------------------|
| 1 | Lancer l'application | La fenêtre s'ouvre, le bouton d'export est grisé |
| 2 | Cliquer sur Generate avec un champ vide | Message d'erreur, aucun QR code |
| 3 | Saisir `https://www.example.com` puis Generate | Le QR code s'affiche, l'export devient actif |
| 4 | Scanner le QR code avec un téléphone | Le lien saisi est reconnu |
| 5 | Saisir un texte avec accents (« Café à Cambrai ») | Le QR code est lisible et le texte est correct |
| 6 | Cliquer sur Export PDF puis annuler | Aucun fichier créé, aucun message |
| 7 | Export PDF vers un dossier valide | Message de confirmation, PDF créé |
| 8 | Ouvrir le PDF | Titre, QR code et texte sont présents |
| 9 | Exporter vers un fichier existant | Confirmation demandée avant écrasement |
| 10 | Saisir un très long texte (plusieurs milliers de caractères) | Message d'erreur « contenu trop long » |

> 📷 *Insérer ici des captures d'écran : fenêtre au démarrage, QR code généré, message d'erreur, PDF obtenu.*

## 9. Installation et lancement

### Prérequis

- JDK 21
- Maven 3.x (ou Eclipse avec m2e)

### Structure du projet

Le `pom.xml` déclare `src` comme dossier des sources et `test` comme dossier des tests (`<testSourceDirectory>test</testSourceDirectory>`).

### Commandes

```bash
mvn test                # lancer les tests unitaires
mvn compile exec:java   # lancer l'application
```

Dans Eclipse : clic droit sur `Main.java` → *Run As* → *Java Application*.

## 10. Difficultés rencontrées

| Problème | Cause | Solution |
|----------|-------|----------|
| « refers to the missing type MainView » | Noms de classes différents entre l'import et l'utilisation, et différences de casse (`QRCodemodel` / `QRCodeModel`) | Harmoniser les noms et les imports ; utiliser *Refactor → Rename* d'Eclipse |
| Interface mal disposée | `BorderLayout.NORTH/CENTER/SOUTH` ignorés : le panneau utilisait un `FlowLayout` par défaut | Créer le panneau avec `new JPanel(new BorderLayout())` |
| Erreur « JavaSE-17 / JRE 21 » | Version Java du projet différente du JDK installé | Aligner la version dans Eclipse et `<release>21</release>` dans le `pom.xml` |
| Tests JUnit 5 non exécutés | Version de Surefire trop ancienne | Déclarer `maven-surefire-plugin` en version 3.x |
| Accents potentiellement déformés | Encodage non précisé dans Maven | Ajouter `project.build.sourceEncoding` à `UTF-8` |

## 11. Améliorations possibles

- Choix de la **taille** et du **niveau de correction d'erreur** depuis l'interface ;
- Choix de la **couleur** du QR code ;
- Export en **image PNG** en plus du PDF ;
- Ajout d'un **logo** au centre du QR code ;
- Génération de **plusieurs QR codes** dans un même PDF ;
- **Tests de l'interface** graphique (par exemple avec AssertJ Swing) ;
- Génération du QR code dans un **thread séparé** (`SwingWorker`) pour ne pas figer la fenêtre avec de gros contenus.

## 12. Conclusion

L'application répond aux exigences du TP : elle permet de saisir un texte ou un lien, de générer un QR code, de l'afficher et de l'exporter dans un PDF, avec une gestion des erreurs qui évite les plantages.
L'architecture MVC apporte une séparation claire des responsabilités : le modèle est entièrement testable sans interface graphique, la vue peut évoluer sans toucher à la logique métier, et le contrôleur reste très court. Le projet m'a permis de mettre en pratique le patron MVC, la gestion d'exceptions personnalisées, l'utilisation de bibliothèques externes avec Maven et l'écriture de tests unitaires avec JUnit 5.# generateur-qr-code
