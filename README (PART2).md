## 1. Introduction

La première partie du projet a abouti à une application Java Swing, construite selon le patron **MVC**, qui génère un QR code à partir d'un texte ou d'un lien et l'exporte dans un fichier PDF. Le rendu du PDF était alors figé : titre, QR code et texte en Helvetica noire sur fond blanc.

Cette deuxième partie enrichit l'application avec des fonctionnalités avancées :

- la **personnalisation du PDF** : polices (dont des polices personnalisées), tailles, styles, alignement et couleurs ;
- l'**insertion d'images** dans le PDF, avec choix de l'emplacement, de l'alignement et de la taille.

Ce rapport décrit l'évolution de l'application : les nouvelles fonctionnalités, les choix techniques, les modifications du code existant, les tests réalisés et les difficultés rencontrées.

## 2. Objectifs et état d'avancement

Objectifs pédagogiques de la partie 2 : comprendre la personnalisation avancée des PDF (polices, couleurs, styles), intégrer des images dans les PDF générés, et utiliser des concepts avancés de manipulation de fichiers et de gestion des exceptions.

| Tâche | Intitulé | État |
|-------|----------|------|
| 1 | Personnalisation avancée des PDF (polices, couleurs, styles) | **Réalisée** (section 4) |
| 2 | Intégration d'images (ajout, positionnement, taille) | **Réalisée** (section 5) |
| 3 | Sauvegarde et chargement des projets et des profils | Non réalisée à ce stade (section 12) |
| 4 | Amélioration de l'interface (facultatif) | Partielle : messages d'erreur informatifs, aperçu en direct ; pas de barre de progression |
| 5 | Tests et débogage | Tests automatisés pour la tâche 1 ; vérifications manuelles pour la tâche 2 (section 9) |
| 6 | Documentation et rapport | **Ce document** (sections 10 et suivantes) |

> Ce rapport est à mettre à jour lorsque la tâche 3 sera réalisée.

## 3. Rappel de l'architecture et évolution

### 3.1 Principe MVC conservé

L'architecture de la partie 1 est conservée : le **modèle** contient la logique métier, la **vue** affiche et relaie les actions de l'utilisateur, le **contrôleur** fait le lien. Les nouvelles fonctionnalités se sont ajoutées **sans remettre en cause** cette séparation.

### 3.2 Nouvelles classes

Deux classes de données ont été ajoutées dans le modèle :

| Classe | Rôle |
|--------|------|
| `PdfStyle` | Regroupe tous les réglages de mise en forme d'un PDF (police, tailles, styles, alignement, couleurs) et sait se valider |
| `PdfImage` | Représente une image à insérer : fichier, emplacement, alignement, largeur en %, et sait se valider |

Ces classes ne contiennent que des données et leur validation. Elles ne dépendent pas de Swing (hors `java.awt.Color`) ni d'iText : la vue les remplit, le modèle les mémorise, `PdfExporter` les traduit en éléments iText.

### 3.3 Organisation des packages

```
src/
├── Main.java
└── main/
    ├── modele/
    │   ├── QRCodeModel.java        (modifié)
    │   ├── QRCodeGenerator.java    (modifié)
    │   ├── PdfExporter.java        (modifié)
    │   ├── PdfStyle.java           (nouveau)
    │   └── PdfImage.java           (nouveau)
    ├── vue/
    │   └── InterfaceUtil.java      (modifié)
    ├── controleur/
    │   └── QRCodeController.java   (modifié)
    └── exception/
        └── QRCodeException.java
test/
└── main/modele/                    (tests unitaires)
```

### 3.4 Schéma des échanges

```
              ┌────────────────────────────────────────────┐
              │  VUE : InterfaceUtil                       │
              │  onglet « Style »  → getStyle() / setStyle()│
              │  onglet « Images » → getImages() / setImages()│
              └───────┬─────────────────────────▲──────────┘
        clics         │                         │ aperçu, messages
                      ▼                         │
              ┌───────┴─────────────────────────┴──────────┐
              │  CONTRÔLEUR : QRCodeController             │
              └───────┬─────────────────────────▲──────────┘
   setStyle / setImages│                        │ résultat / exception
   generate / export   ▼                        │
              ┌───────┴─────────────────────────┴──────────┐
              │  MODÈLE : QRCodeModel                      │
              │   ├─ PdfStyle  (réglages de mise en forme) │
              │   ├─ List<PdfImage> (images à insérer)     │
              │   ├─ QRCodeGenerator (ZXing)               │
              │   └─ PdfExporter (iText)                   │
              └────────────────────────────────────────────┘
```

## 4. Nouvelle fonctionnalité 1 : personnalisation du PDF

### 4.1 Réglages proposés

L'onglet **Style** de la fenêtre permet de régler :

| Réglage | Valeurs possibles | Valeur par défaut |
|---------|-------------------|-------------------|
| Titre | Texte libre (vide = pas de titre) | « QR Code généré » |
| Police | Helvetica, Times, Courier, **police personnalisée** (.ttf / .otf) | Helvetica |
| Taille du titre | 8 à 72 | 20 |
| Taille du texte | 6 à 48 | 12 |
| Style du titre | Gras, italique | Gras |
| Style du texte | Gras, italique | Aucun |
| Alignement | Gauche, centré, droite (titre, QR code et texte) | Centré |
| Couleur du titre | Sélecteur de couleur | Noir |
| Couleur du texte | Sélecteur de couleur | Noir |
| Couleur du fond | Sélecteur de couleur | Blanc |
| Couleur du QR code | Sélecteur de couleur | Noir |

Un bouton **Réinitialiser le style** remet toutes les valeurs par défaut. Les valeurs par défaut reproduisent exactement le rendu de la partie 1 : le comportement historique n'est pas modifié.

> 📷 *Insérer ici une capture de l'onglet « Style ».*

### 4.2 Les polices

Deux familles de polices cohabitent :

- **Polices standard** (Helvetica, Times, Courier) : elles sont connues de tous les lecteurs PDF et n'ajoutent aucun poids au fichier. Le gras et l'italique sont gérés par iText. Le jeu de caractères est Windows-1252, ce qui couvre les accents du français, mais pas les caractères hors de ce jeu (emojis, par exemple).
- **Police personnalisée** : l'utilisateur choisit un fichier `.ttf` ou `.otf`. iText le charge avec l'encodage Unicode `IDENTITY_H` et l'**incorpore** au PDF (`BaseFont.EMBEDDED`). Le document s'affiche donc correctement sur un poste qui n'a pas cette police installée.

La construction de la police est centralisée dans la méthode `PdfExporter.buildFont(...)`, qui renvoie un objet `Font` iText à partir du style (famille, taille, gras/italique, couleur).

### 4.3 Les couleurs

- **Couleur du titre et du texte** : transmises aux polices iText (`BaseColor`).
- **Couleur de fond** : iText n'offre pas de propriété « fond de page ». Un *événement de page* (`PdfPageEventHelper.onStartPage`) peint un rectangle de la taille de la page sur le calque situé **sous** le contenu (`getDirectContentUnder()`). Comme l'événement se déclenche à chaque nouvelle page, le fond est appliqué à toutes les pages.
- **Couleur du QR code** : ZXing produit l'image avec une configuration `MatrixToImageConfig(couleur des modules, couleur du fond)`. Le fond du QR code reste **toujours blanc** pour garantir la lecture ; seuls les modules (petits carrés) sont colorés.

### 4.4 Contrôle de lisibilité

Un PDF « personnalisé » peut devenir illisible (texte blanc sur fond blanc) et un QR code trop clair ne se scanne pas. L'application mesure donc le **contraste** entre deux couleurs avec les formules de la norme d'accessibilité WCAG :

- luminance relative d'une couleur : `L = 0,2126 × R + 0,7152 × V + 0,0722 × B` (composantes d'abord linéarisées) ;
- rapport de contraste : `(L_clair + 0,05) / (L_foncé + 0,05)`, de 1 (couleurs identiques) à 21 (noir sur blanc).

Règles appliquées (contraste minimal de **3** pour le texte large) :

| Couples de couleurs comparés | Refus si contraste inférieur à 3 |
|------------------------------|----------------------------------|
| Titre / fond de page (si le titre n'est pas vide) | Oui |
| Texte / fond de page | Oui |
| Modules du QR code / blanc | Oui |

### 4.5 Aperçu cohérent avec le PDF

La couleur du QR code est appliquée dès la génération, et l'aperçu se **met à jour immédiatement** lorsque l'utilisateur change cette couleur après avoir généré un QR code (méthode `QRCodeModel.refreshQRCode()`). À l'export, le QR code est de nouveau régénéré avec la couleur courante : le PDF correspond toujours aux réglages affichés à l'écran.

## 5. Nouvelle fonctionnalité 2 : insertion d'images

### 5.1 Utilisation

L'onglet **Images** affiche la liste des images ajoutées :

1. **Ajouter des images…** ouvre un sélecteur de fichiers (sélection multiple possible) ;
2. chaque image est ajoutée avec des réglages par défaut (en haut du document, centrée, 30 % de la largeur) ;
3. l'utilisateur sélectionne une image dans la liste et modifie ses réglages ; le texte de la liste se met à jour immédiatement (par exemple : *logo.png — Avant le titre, Centré, 30 %*) ;
4. **Supprimer** retire l'image sélectionnée.

> 📷 *Insérer ici une capture de l'onglet « Images » avec quelques images ajoutées.*

### 5.2 Positionnement

Les éléments du PDF se suivent de haut en bas : titre, QR code, texte. L'**emplacement** d'une image indique où l'insérer dans cette suite :

| Emplacement | Position dans le document |
|-------------|---------------------------|
| Avant le titre | Tout en haut |
| Avant le QR code | Entre le titre et le QR code |
| Après le QR code | Entre le QR code et le texte |
| Après le texte | Tout en bas |

Ordre complet : `[images « avant le titre »]` → titre → `[avant le QR code]` → **QR code** → `[après le QR code]` → texte → `[après le texte]`.
Lorsque plusieurs images partagent le même emplacement, elles suivent l'ordre de la liste. L'**alignement** horizontal (gauche, centré, droite) est choisi image par image.

### 5.3 Taille

La taille est exprimée en **pourcentage de la largeur utile de la page** (de 5 % à 100 %). La largeur utile d'une page A4 avec 50 points de marge est d'environ 495 points. Le rapport largeur/hauteur de l'image est toujours conservé (`Image.scaleToFit`), et la hauteur est plafonnée pour qu'une image très haute tienne sur une page.

### 5.4 Limites et formats

| Contrainte | Valeur |
|------------|--------|
| Formats acceptés | PNG, JPG / JPEG, GIF, BMP |
| Nombre maximal d'images | 10 |
| Poids maximal d'un fichier | 10 Mo |
| Largeur | 5 % à 100 % |

Ces limites évitent de charger en mémoire des fichiers démesurés et gardent un PDF raisonnable.

### 5.5 Principe d'implémentation

1. **Validation de chaque image** (`PdfImage.validate()`) : fichier existant, extension acceptée, poids, réglages valides.
2. **Chargement avant l'ouverture du PDF** : toutes les images (comme les polices) sont lues et redimensionnées *avant* de créer le fichier. Si l'une d'elles est invalide, aucune écriture n'a eu lieu : on évite de laisser un PDF vide ou incomplet sur le disque.
3. **Insertion comme une ligne de texte** : chaque image est placée dans un `Paragraph` sous la forme d'un `Chunk` (voir la section 11, défi n° 5), avec 15 points d'espace avant et après.

## 6. Modifications apportées aux classes existantes

| Classe | Modifications |
|--------|---------------|
| `QRCodeGenerator` | Nouvelle méthode `generate(contenu, taille, couleur)` ; contrôle du contraste de la couleur ; l'ancienne méthode à deux paramètres est conservée (noir par défaut) |
| `PdfExporter` | Trois versions de `export` : sans style (3 paramètres), avec style (4), avec style et images (5). Les anciennes signatures sont conservées et délèguent à la plus complète. Construction des polices, fond de page via un événement de page, chargement des images |
| `QRCodeModel` | Attributs `style` et `pdfImages` avec leurs accesseurs ; `generate` utilise la couleur du style ; nouvelle méthode `refreshQRCode()` ; `exportToPdf` régénère le QR code puis passe style et images à l'exporteur |
| `QRCodeController` | Lecture du style et des images dans la vue avant génération et export ; nouvel écouteur `onQrColorChanged()` pour mettre l'aperçu à jour |
| `InterfaceUtil` | Panneau à onglets (*Style* et *Images*) ; composant réutilisable `ColorSelector` (carré de couleur + bouton) ; rendu personnalisé de la liste d'images ; méthodes `getStyle()`, `setStyle()`, `getImages()`, `setImages()` ; taille de fenêtre ajustée automatiquement (`pack()`) |

Les anciennes signatures conservées garantissent la **compatibilité ascendante** : les tests de la partie 1 continuent de fonctionner sans modification.

## 7. Fonctionnement de l'application mise à jour

### 7.1 Génération du QR code

```
Utilisateur     InterfaceUtil     QRCodeController      QRCodeModel        QRCodeGenerator
    │ clique Generate  │                  │                   │                   │
    ├─────────────────►│  événement       │                   │                   │
    │                  ├─────────────────►│  getStyle()       │                   │
    │                  │◄─────────────────┤  (réglages)       │                   │
    │                  │                  ├─ setStyle(style) ►│                   │
    │                  │                  ├─ generate(texte) ►│ generate(texte,   │
    │                  │                  │                   │  taille, couleur) │
    │                  │                  │                   ├──────────────────►│
    │                  │                  │                   │◄───── image ──────┤
    │                  │◄─ displayQRCode ─┤◄──────────────────┤                   │
```

La couleur du QR code vient du style courant. Si elle est trop claire, une `QRCodeException` est levée et affichée.

### 7.2 Export en PDF

1. L'utilisateur clique sur **Export PDF** et choisit un fichier de destination.
2. Le contrôleur lit **le style et la liste d'images** dans la vue, et les transmet au modèle.
3. Le modèle régénère le QR code avec la couleur courante, puis appelle `PdfExporter.export(...)`.
4. `PdfExporter` :
   1. valide le style (`PdfStyle.validate()`) ;
   2. construit les deux polices (titre, texte) ;
   3. valide, charge et redimensionne les images ;
   4. **seulement ensuite**, crée le fichier : fond de page, images « en haut », titre, images « avant le QR code », QR code, images « après le QR code », texte, images « en bas ».
5. Un message confirme l'enregistrement, ou décrit l'erreur.

### 7.3 Changement de couleur du QR code

Quand l'utilisateur modifie la couleur du QR code alors qu'un QR code est déjà affiché, la vue prévient le contrôleur, qui demande au modèle de régénérer l'image puis la réaffiche.

## 8. Gestion des erreurs

Toutes les erreurs prévisibles sont converties en `QRCodeException`, avec un message destiné à l'utilisateur, affiché dans une boîte de dialogue. Nouvelles situations gérées :

| Situation | Détection | Message (résumé) |
|-----------|-----------|------------------|
| Taille du titre ou du texte hors limites | `PdfStyle.validate()` | « La taille du titre doit être comprise entre 8 et 72. » |
| Couleur non définie | `PdfStyle.validate()` | « Toutes les couleurs doivent être définies. » |
| Police personnalisée sans fichier choisi | `PdfStyle.validate()` | « Choisissez un fichier de police (.ttf ou .otf). » |
| Fichier de police introuvable ou d'un autre type | `PdfStyle.validate()` | « Le fichier de police est introuvable… » / « La police doit être un fichier .ttf ou .otf. » |
| Fichier de police corrompu | `BaseFont.createFont`, converti | « Impossible de charger la police … : fichier de police invalide. » |
| Texte ou titre peu lisible sur le fond | Contraste < 3 | « La couleur du texte n'est pas assez contrastée avec le fond. » |
| QR code trop clair | Contraste < 3 | « La couleur du QR code est trop claire : il ne serait pas lisible… » |
| Image introuvable | `PdfImage.validate()` | « Image introuvable : … » |
| Format d'image non pris en charge | `PdfImage.validate()` | « Format d'image non pris en charge… (formats acceptés : PNG, JPG, GIF, BMP) » |
| Image trop volumineuse | `PdfImage.validate()` | « L'image … est trop volumineuse (maximum 10 Mo). » |
| Largeur de l'image hors limites | `PdfImage.validate()` | « La largeur de l'image doit être comprise entre 5 % et 100 %. » |
| Image illisible (corrompue) | `Image.getInstance`, converti | « Impossible de lire l'image … : fichier image invalide ou corrompu. » |
| Plus de 10 images | Vue et `PdfExporter` | « Un PDF ne peut contenir que 10 images au maximum. » |

**Principe commun :** toutes les vérifications et tous les chargements susceptibles d'échouer sont faits **avant** d'ouvrir le fichier PDF. En cas d'erreur, aucun fichier n'est créé.

## 9. Tests

### 9.1 Tests unitaires automatisés (JUnit 5) — tâche 1

Les tests portent sur la couche modèle. À ceux de la partie 1 (15 tests), s'ajoutent **35 nouveaux tests**, soit **50 tests** au total.

**`PdfStyleTest` (12 tests)** — validation du style et calcul du contraste

| Test | Vérifie que… |
|------|--------------|
| `defaultStyleIsValid` | le style par défaut est valide |
| `contrastOfBlackOnWhiteIsMaximal` / `contrastOfIdenticalColorsIsMinimal` | le contraste vaut 21 pour noir sur blanc et 1 pour deux couleurs identiques |
| `titleSizeOutOfRangeThrows` / `textSizeOutOfRangeThrows` | les tailles hors limites sont refusées |
| `whiteTextOnWhiteBackgroundThrows` / `whiteTitleOnWhiteBackgroundThrows` | un texte ou un titre blanc sur fond blanc est refusé |
| `lowContrastTitleIsIgnoredWhenTitleIsEmpty` | un titre vide n'est pas soumis au contrôle de contraste |
| `lightTextOnDarkBackgroundIsValid` | texte clair sur fond sombre accepté |
| `customFontWithoutFileThrows` / `customFontWithMissingFileThrows` | police personnalisée sans fichier ou avec fichier absent refusée |
| `nullColorThrows` | une couleur nulle est refusée |

**`QRCodeGeneratorColorTest` (5 tests)** — QR codes colorés

| Test | Vérifie que… |
|------|--------------|
| `coloredQRCodeUsesRequestedColor` | l'image contient bien la couleur demandée sur fond blanc |
| `coloredQRCodeCanStillBeDecoded` | un QR code coloré se relit et redonne le texte d'origine |
| `tooLightColorThrows` | jaune et blanc sont refusés |
| `nullColorThrows` | une couleur nulle est refusée |
| `twoArgumentVersionStillWorks` | l'ancienne méthode fonctionne toujours |

**`PdfExporterStyleTest` (10 tests)** — export avec style

| Test | Vérifie que… |
|------|--------------|
| `everyStandardFontFamilyProducesAPdf` | Helvetica, Times et Courier produisent un PDF dont le texte est relisible |
| `titleAndAccentedTextAreWrittenInThePdf` | titre et texte accentué sont bien présents dans le PDF (extraction du texte) |
| `emptyTitleIsNotWritten` | un titre vide n'est pas écrit |
| `allStyleOptionsCombinedProduceAValidPdf` | toutes les options combinées donnent un PDF valide |
| `leftAndCenterAlignmentsBothWork` | les trois alignements fonctionnent |
| `backgroundColorIsPaintedOnThePage` | l'instruction de couleur de fond est présente dans le contenu de la page |
| `invalidStyleThrowsAndCreatesNoFile` / `nullStyleThrows` | un style invalide ou absent est refusé, sans créer de fichier |
| `customFontWithInvalidFileThrowsAndCreatesNoFile` | une fausse police est refusée, sans créer de fichier |
| `customFontIsEmbeddedInThePdf` | une vraie police `.ttf` est incorporée (le test est ignoré si aucune police système n'est trouvée) |

**`QRCodeModelStyleTest` (8 tests)** — modèle et style

| Test | Vérifie que… |
|------|--------------|
| `newModelHasDefaultStyle` / `nullStyleIsRejected` | style par défaut présent, style nul refusé |
| `generateUsesQrColorOfStyle` | la génération utilise la couleur du style |
| `generateWithTooLightColorThrowsAndKeepsPreviousQRCode` | une couleur trop claire est refusée sans perdre l'ancien QR code |
| `refreshWithoutGenerationThrows` / `refreshAppliesNewColorToExistingContent` | la régénération exige un QR code existant et applique la nouvelle couleur |
| `exportAppliesStyle` / `exportWithInvalidStyleThrows` | l'export applique le style ; un style invalide est refusé sans créer de fichier |

Exécution : `mvn test`

> 📷 *Insérer ici une capture du résultat de `mvn test` (50 tests, 0 échec).*

### 9.2 Vérifications de la tâche 2 (images)

Aucun test unitaire automatisé n'a été écrit pour l'insertion d'images. Cette fonctionnalité est vérifiée **manuellement** avec les scénarios ci-dessous (à exécuter dans l'application, puis à ouvrir dans un lecteur PDF).

| N° | Scénario | Résultat attendu |
|----|----------|------------------|
| 1 | Onglet Images au démarrage | Liste vide, réglages et bouton « Supprimer » désactivés |
| 2 | Ajouter un PNG | L'image apparaît dans la liste (« Avant le titre, Centré, 30 % ») et est sélectionnée |
| 3 | Ajouter plusieurs images d'un coup | Toutes sont ajoutées à la liste |
| 4 | Changer emplacement, alignement et largeur | Le texte de la liste se met à jour immédiatement |
| 5 | Sélectionner une autre image puis revenir | Les réglages de chaque image sont conservés |
| 6 | Exporter avec une image « Avant le titre » | Image tout en haut, puis titre, QR code et texte |
| 7 | Une image à chacun des quatre emplacements | Ordre : image, titre, image, QR code, image, texte, image |
| 8 | Alignements gauche, centré, droite | L'image est positionnée en conséquence |
| 9 | Largeur 10 % puis 100 % | L'image est petite puis occupe toute la largeur utile |
| 10 | Document trop long pour une page | Les éléments passent à la page suivante **dans le bon ordre**, avec le fond de page sur chaque page |
| 11 | Ajouter un fichier `.txt` renommé en `.png` | Message « fichier image invalide ou corrompu », aucun PDF créé |
| 12 | Image supprimée du disque après ajout, puis export | Message « Image introuvable », aucun PDF créé |
| 13 | Ajouter plus de 10 images | Message d'erreur, la 11ᵉ image n'est pas ajoutée |
| 14 | Supprimer une image de la liste | L'image disparaît, une voisine est sélectionnée |
| 15 | Exporter sans aucune image | Rendu identique à la partie 1 |

> 📷 *Insérer ici une capture d'un PDF obtenu avec images aux quatre emplacements.*

### 9.3 Vérifications manuelles de l'interface (tâche 1)

| N° | Scénario | Résultat attendu |
|----|----------|------------------|
| 1 | Démarrage | Onglet Style avec les valeurs par défaut ; export désactivé |
| 2 | Changer la police et les tailles puis exporter | PDF avec la police et les tailles choisies |
| 3 | Choisir « Police personnalisée » sans fichier, puis exporter | Message « Choisissez un fichier de police » |
| 4 | Choisir un `.ttf` valide et exporter | PDF dans cette police (visible dans les propriétés du document : police incorporée) |
| 5 | Changer la couleur du QR code après génération | L'aperçu se met à jour ; le QR code reste lisible avec un téléphone |
| 6 | Couleur du QR code jaune clair | Message « trop claire », QR code précédent conservé |
| 7 | Fond bleu foncé, titre doré, texte blanc | PDF lisible, fond sur toute la page |
| 8 | Texte blanc sur fond blanc | Message « pas assez contrastée », aucun PDF créé |
| 9 | Titre vide | Aucun titre dans le PDF |
| 10 | Bouton « Réinitialiser le style » | Retour aux valeurs par défaut, aperçu du QR code remis en noir |

> 📷 *Insérer ici une capture d'un PDF avec un style personnalisé.*

## 10. Documentation du code

### 10.1 Conventions

- Chaque classe possède un commentaire **Javadoc** qui indique son rôle dans le MVC ;
- les méthodes publiques sont documentées avec `@param`, `@return` et `@throws` ;
- à l'intérieur des méthodes, des commentaires expliquent les **choix** (pourquoi) plus que les instructions (quoi) ;
- les noms sont en anglais pour les identifiants et les commentaires sont en français.

### 10.2 Points documentés pour les nouvelles fonctionnalités

| Classe | Éléments documentés |
|--------|---------------------|
| `PdfStyle` | Rôle de la classe, énumérations `FontFamily` et `Alignment`, valeurs par défaut, règles de validation, formule du contraste |
| `PdfImage` | Constantes (limites), énumération `Position`, règles de validation, méthode `copy()` |
| `PdfExporter` | Les trois versions de `export`, l'ordre des éléments, le chargement préalable des polices et des images, le rôle de l'événement de page, le choix du `Chunk` pour les images |
| `QRCodeGenerator` | Les deux versions de `generate`, le contrôle du contraste, le fond blanc |
| `QRCodeModel` | Rôle des attributs `style` et `pdfImages`, différence entre `generate` et `refreshQRCode` |
| `InterfaceUtil` | Disposition en onglets, composant `ColorSelector`, indicateur `updatingImageControls` (évite les boucles d'événements) |

### 10.3 Génération de la documentation

La documentation HTML se génère avec l'outil Javadoc du JDK :

- dans Eclipse : *Project → Generate Javadoc…* ;
- en ligne de commande : `mvn javadoc:javadoc`.

## 11. Défis rencontrés et solutions

### Défi 1 — Appliquer une couleur de fond à toute la page
iText ne propose pas de propriété « couleur de fond de page ». **Solution :** un événement de page (`onStartPage`) dessine un rectangle plein sur le calque situé sous le contenu. Il se déclenche pour chaque page, donc le fond couvre aussi les pages suivantes. L'événement doit être enregistré **avant** l'ouverture du document.

### Défi 2 — Utiliser des polices personnalisées sans perdre les accents
Les polices standard n'incluent pas tous les caractères Unicode, et une police absente du poste du lecteur serait remplacée. **Solution :** pour les polices personnalisées, chargement avec l'encodage `IDENTITY_H` et incorporation du fichier dans le PDF. Les polices standard restent proposées pour des PDF légers.

### Défi 3 — Éviter les PDF illisibles ou incomplets
Deux risques : des combinaisons de couleurs illisibles, et un fichier PDF vide laissé sur le disque quand une police ou une image est invalide. **Solution :** un contrôle de contraste fondé sur la norme WCAG, et une organisation du code où **tout ce qui peut échouer est préparé avant** la création du fichier.

### Défi 4 — Garder l'aperçu cohérent avec le PDF
La couleur du QR code influe sur l'image affichée *et* sur celle du PDF. **Solution :** régénération de l'image lors d'un changement de couleur (aperçu), et régénération à l'export (le PDF correspond toujours aux réglages visibles).

### Défi 5 — L'ordre des éléments cassé par les images
Lors des premiers essais avec quatre images, le contenu dépassait une page. iText a alors repoussé **seule** l'image « après le QR code » sur la page suivante, alors que le texte, ajouté après elle, restait sur la première page : l'ordre n'était plus respecté. **Solution :** insérer chaque image dans un `Paragraph` sous la forme d'un `Chunk`, c'est-à-dire comme une ligne de texte. L'image suit alors le même flux que le reste, et passe à la page suivante avec tout ce qui la suit. Ce problème a été trouvé en **relisant visuellement** le PDF d'essai, ce qui montre l'intérêt de ne pas se limiter à vérifier que le fichier existe.

### Défi 6 — Une interface qui se charge
L'ajout de nombreux réglages risquait de rendre la fenêtre confuse. **Solution :** deux onglets (*Style* et *Images*), un composant réutilisable `ColorSelector` pour les quatre couleurs, et des libellés explicites.

### Défi 7 — Synchroniser la liste d'images et les réglages
Quand on change d'image sélectionnée, les contrôles sont remplis avec ses valeurs ; ce remplissage ne doit pas être pris pour une modification de l'utilisateur et réécrit dans l'image. **Solution :** un indicateur booléen (`updatingImageControls`) qui désactive temporairement la prise en compte des événements pendant le remplissage.

### Défi 8 — Ne pas casser l'existant
Les nouveaux paramètres de `export` et de `generate` auraient pu rendre les tests de la partie 1 inutilisables. **Solution :** conserver les anciennes signatures sous forme de surcharges qui délèguent à la version complète.

## 12. Travail restant et perspectives

### Tâche 3 — Sauvegarde et chargement des projets et des profils (non réalisée)

La préparation est faite :

- `PdfStyle` regroupe **tous** les réglages d'un profil (police, couleurs…) dans un seul objet ;
- `PdfImage` et la liste d'images décrivent le contenu d'un projet ;
- la vue sait déjà **afficher** un style et une liste d'images (`setStyle`, `setImages`) et les **relire** (`getStyle`, `getImages`).

Il restera à choisir un format de fichier (par exemple JSON ou `Properties`), à écrire la lecture/écriture dans le modèle, à ajouter les boutons ou menus *Enregistrer / Ouvrir* et à traiter les cas d'erreur (fichier corrompu, police ou image disparue).

### Tâche 4 — Améliorations facultatives
- barre de progression et génération dans un thread séparé (`SwingWorker`) pour les gros contenus ;
- aperçu du PDF dans l'application.

### Autres idées
- changer l'ordre des images dans la liste (boutons haut / bas) ;
- positionnement libre d'une image (coordonnées x / y) ;
- tests unitaires pour `PdfImage` et l'insertion d'images ;
- export du QR code seul au format PNG.

## 13. Conclusion

La partie 2 a fait évoluer l'application d'un générateur au rendu fixe vers un outil de mise en page de PDF : polices, couleurs, alignement, images. Grâce à l'architecture MVC de la partie 1, ces fonctionnalités se sont ajoutées par l'**extension** du modèle (deux classes de données, un exporteur plus riche) et de la vue (deux onglets), sans réécrire le contrôleur ni casser les tests existants.

Le projet a permis de travailler la personnalisation avancée avec iText (polices incorporées, événements de page, flux de contenu), la manipulation de fichiers et la gestion d'exceptions (tout valider avant d'écrire), ainsi que la qualité du résultat : contrôle du contraste, cohérence entre l'aperçu et le PDF, et vérification visuelle des PDF produits. Il reste à réaliser la sauvegarde et le chargement des projets et des profils, pour lesquels les classes `PdfStyle` et `PdfImage` constituent une base directement exploitable.

## 14. Annexes

### 14.1 Bibliothèques utilisées

| Bibliothèque | Version | Rôle |
|--------------|---------|------|
| Java (JDK) | 21 | Langage, Swing |
| iText | 5.5.13.3 | Création du PDF |
| ZXing (`core`, `javase`) | 3.5.3 | Génération du QR code |
| JUnit Jupiter | 5.10.2 | Tests unitaires |
| Maven | 3.x | Gestion du projet |

### 14.2 Lancement

```bash
mvn test                # lancer les 50 tests unitaires
mvn compile exec:java   # lancer l'application
```

Dans Eclipse : clic droit sur `Main.java` → *Run As* → *Java Application*.

### 14.3 Classes de données — récapitulatif

**`PdfStyle`** : `titleText`, `fontFamily` (HELVETICA, TIMES, COURIER, CUSTOM), `customFontFile`, `titleSize`, `textSize`, `titleBold`, `titleItalic`, `textBold`, `textItalic`, `alignment` (LEFT, CENTER, RIGHT), `titleColor`, `textColor`, `backgroundColor`, `qrColor`.

**`PdfImage`** : `file`, `position` (TOP, BEFORE_QR, AFTER_QR, BOTTOM), `alignment`, `widthPercent`.

### 14.4 Fichiers d'exemple

- `exemple_police_perso_fond_bleu.pdf` : police personnalisée incorporée, fond bleu, titre doré, QR code bleu, alignement à gauche ;
- `exemple_times_fond_jaune.pdf` : police Times, titre en italique, fond jaune clair, QR code bordeaux, alignement à droite ;
- `exemple_pdf_avec_images.pdf` : images aux quatre emplacements, document sur deux pages.
