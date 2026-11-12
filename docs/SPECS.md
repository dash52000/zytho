# Zytho — Spécifications

App Android perso de suivi des dégustations de bière. Dépôt : https://github.com/dash52000/zytho (APK compilé par GitHub Actions, publié dans Releases à chaque push sur `main`).

Version actuelle : **1.4** (novembre 2026).

## Vision

Noter chaque bière goûtée en moins de 30 secondes, gérer sa cave de garde et sa wishlist, et **apprendre à déguster** sans que ce soit une corvée. Hors ligne d'abord, données locales, export CSV pour l'analyse dans R. Thème sombre ambré.

Usage : maison, bar ou brasserie, dégustations entre potes. Univers : bières belges fortes (Trappistes, Dubbel, Tripel, Quad, Strong Golden) et brasseries de Gaume, Lorraine, Luxembourg.

Principes de conception :
- Saisie sans effort : seule la note est obligatoire, aucun champ marqué comme manquant
- Pédagogique : Frère Anselme, moine brasseur bienveillant, guide et récompense chaque fiche par un apprentissage
- Pas de notification, pas de niveaux ni de badges
- Une seule fiche par bière ; chaque dégustation est gardée en historique daté

## Navigation

Barre du bas : **Carnet**, **Cave** (cave + wishlist), **Parcours**, **Palais** (stats + carte du goût). Bouton **+** central : « Encore une ? » (bières habituelles), Scanner, Photo de l'étiquette, Saisie manuelle.

## Fonctions

### Ajout
- **Scan code-barres** (ML Kit, hors ligne) : code connu → bière reconnue ; sinon Open Food Facts (nom, marque, degré, style déduit, format, pays, mention Trappiste, photo) ; sinon saisie manuelle et code mémorisé
- **Photo d'étiquette** : lecture du texte hors ligne (ML Kit) pour pré-remplir nom et degré
- **Écran de choix** : nouvelle dégustation, ajout en cave, wishlist, ou « Déjà goûtée ? » avec ta dernière note
- **Encore une ?** : bières goûtées au moins 2 fois, en un geste, avec la dernière note pré-remplie

### Dégustation
- **Express (défaut)** : note /10 au demi-point, familles d'arômes avec icônes, un mot facultatif, contexte repliable
- **Guidé par Anselme** : 4 cartes Œil (robe), Nez (famille puis arôme, comparaisons du quotidien, ★ = attendu pour le style), Bouche (douceur, amertume, corps, pétillance de 0 à 4), Finale (longueur, note, commentaire, à racheter, contexte)
- **Fiche souvenir** après chaque dégustation : photo, note, arômes, profil, et la réponse d'Anselme (ce que tu as trouvé, ce qu'il faut chercher la prochaine fois et pourquoi, anecdote)
- **Modification** : tout avis est modifiable (bière, brasserie, pays, style, arômes, profil, note, contexte), date d'origine conservée

### Apprendre
- **17 fiches de style** : pourquoi ça goûte ça, bien servir (verre, température), histoire et abbayes, accords mets, une référence classique et une locale vérifiée
- **Parcours d'initiation** : point de départ au choix (Tripel par défaut), puis l'étape suivante dépend des styles aimés (note moyenne ≥ 6) ; étape validée dès qu'on déguste ce style
- **Carte du goût** (Palais) : chaque dégustation est un point sur deux axes, sucré ↔ amer et léger ↔ corsé ; le cercle montre le centre des bières aimées, avec la lecture d'Anselme

### Anselme prescripteur
- **Catalogue** embarqué (`app/src/main/assets/candidates.csv`, 93 bières : trappistes, abbayes, classiques belges, locales Gaume / Lorraine / Luxembourg, rares). Chaque bière est placée sur les curseurs douceur, amertume, corps (0–4) ; colonnes `disponibilite` (courante / locale / rare), `verif` (web / connu / a_confirmer / damien) et `source`. Modifiable dans R, intégré au build suivant
- **Dans Palais, sous la carte du goût** : 🎯 Dans tes cordes (la plus proche du centre de tes bières aimées, avantage aux styles que tu aimes), 🧭 Un pas de côté (au bord de ta zone, vers ce que tu as le moins exploré), 💎 Chasse au trésor (une rare dans ta zone, si elle existe). 🎯 et 🧭 ne proposent que des bières courantes ou locales
- Chaque conseil : le pourquoi dans la voix d'Anselme, boutons **→ Wishlist**, **Autre idée**, **Pas pour moi** (exclusion définitive)
- Conseils figés : ils ne changent que sur « Autre idée » (ou s'ils sont goûtés, écartés, mis en wishlist)
- Démarrage après 3 dégustations notées et au moins une ≥ 7 ; avant, renvoi vers le classique de l'étape du parcours
- Points pâles sur la carte : les bières du catalogue pas encore goûtées
- **Recalage** : quand une bière du catalogue est goûtée en mode guidé, ton profil remplace l'estimation ; après 2 écarts dans le même sens sur un style, tout le style est décalé
- **Fiche souvenir** : Anselme compare ta note et ton profil à sa prédiction quand la bière vient de son catalogue
- **Autocomplétion** de la saisie depuis le catalogue (nom, brasserie, pays, style, degré) ; après un scan Open Food Facts ou une photo d'étiquette, le style du catalogue l'emporte

### Profils
- Plusieurs dégustateurs sur le même téléphone ; profil « Moi » par défaut (renommable)
- Pastille du profil actif en haut du Carnet et de Palais : changer, créer, renommer
- **Propre à chaque profil** : dégustations (carnet, carte du goût, stats), wishlist, parcours, conseils d'Anselme et « Pas pour moi »
- **Commun** : fiches bière et brasseries, cave, codes-barres, carte des brasseries (comptes du profil actif)
- Une bière goûtée à deux = une fiche par profil (on change de profil entre les deux)

### Cave et wishlist
- Cave : stock par bière, millésime et format ; « à boire avant » conseillé selon le style ; bouton « Ouvrir » (stock −1 et fiche de dégustation pré-remplie)
- Wishlist : bouton « Goûtée ! »

### Carte des brasseries
- Carte OpenStreetMap (osmdroid), accessible depuis Carnet et Palais
- Placement d'une brasserie par recherche Nominatim ou au centre de la carte
- Marqueur : nom (✝ si Trappiste), nombre de bières, de dégustations, note moyenne

### Export
- CSV des dégustations de tous les profils (une ligne par dégustation, colonne `profil` en tête, virgule, UTF-8, lisible par `readr::read_csv()`), avec les colonnes du mode guidé
- Sauvegarde et restauration JSON complètes (sans les photos)

## Modèle de données (Room, version 4)

| Table | Champs clés |
| --- | --- |
| brewery | nom, pays, région, trappiste, lat/lon |
| beer | nom, brasserie, style, degré, photo, à racheter |
| barcode | code EAN, bière, format |
| profile | nom, date de création |
| tasting | profil, bière, date, note, arômes, lieu, service, millésime, format, commentaire, robe, douceur, amertume, corps, pétillance, finale |
| cellar | bière, quantité, millésime, format, à boire avant |
| wish | profil, bière, note, date d'ajout |
| aroma | libellé, famille, perso |
| catalog_exclusion | profil, nom normalisé (« Pas pour moi »), date |

Migration v1 → v2 : colonnes du mode guidé ajoutées à `tasting`. Migration v2 → v3 : table `catalog_exclusion`. Migration v3 → v4 : table `profile` (profil 1 « Moi »), colonne `profileId` (défaut 1) dans `tasting`, `wish`, `catalog_exclusion`. Conseils figés, historique des conseils et départ du parcours dans les préférences, par profil.

## Références locales vérifiées

La Rulles (Blonde, Brune, Triple), Orval, Brasseurs de Lorraine (Loroyse, Abbaye des Prémontrés), La Châouette (Ambrée, Blanche), Brasserie des Trévires (La Recevresse Imperial Stout). Pas de référence locale fiable pour Quad, Saison, Gueuze, Lambic, Kriek, Scotch, Barley wine, IPA, Strong Golden.

## Technique

Kotlin 2.0, Jetpack Compose, Material 3, Room, CameraX, ML Kit (code-barres et texte, embarqués), Coil, osmdroid, kotlinx.serialization. Android 8.0 minimum. Signature fixe (keystore dans le dépôt) pour que les mises à jour s'installent par-dessus.

## Historique

| Version | Contenu |
| --- | --- |
| 1.4 | Profils (dégustations, wishlist, parcours et conseils par profil ; cave commune) |
| 1.3 | Anselme prescripteur (catalogue de 93 bières, 🎯 🧭 💎, recalage), autocomplétion de la saisie |
| 1.2 | Carte des brasseries, départ du parcours au choix, avis modifiables |
| 1.1 | Frère Anselme, dégustation guidée, fiches de style, parcours, carte du goût, scan enrichi, « Encore une ? » |
| 1.0 | Carnet, scan, photo d'étiquette, cave, wishlist, stats, export |

## Idées pour la suite

- Fiche souvenir partageable en image
- Stats : timeline mensuelle, top/flop
- Brasseries préchargées (Trappistes, Gaume) avec position
