# Zytho

Carnet de dégustation de bières pour Android : carnet noté sur 10 avec arômes, cave de garde, wishlist, scan de code-barres (Open Food Facts) et lecture d'étiquette hors ligne.

## Installer

1. Ouvre la page **Releases** du dépôt et télécharge `zytho.apk` de la dernière version.
2. Sur le téléphone, ouvre le fichier et autorise l'installation depuis cette source.
3. Les versions suivantes s'installent par-dessus et gardent les données.

## Fonctions V1

- **Bouton +** : scanner le code-barres, photographier l'étiquette ou saisir à la main
- **Scan** : code connu → bière reconnue ; sinon recherche Open Food Facts ; sinon saisie manuelle et code mémorisé
- **Actions** : nouvelle dégustation, ajout en cave (+1), wishlist, et affichage de ta note si déjà goûtée
- **Carnet** : recherche « Déjà goûtée ? », filtres style / pays / note / arôme, historique par bière
- **Cave** : millésime, format, « à boire avant » conseillé selon le style, bouton « Ouvrir »
- **Wishlist** : bouton « Goûtée ! »
- **Stats** : top 5, par style, arômes fréquents
- **Export** : CSV des dégustations (pour R), sauvegarde et restauration JSON

## Technique

Kotlin, Jetpack Compose, Room, CameraX, ML Kit (code-barres et texte, embarqués). L'APK est compilé par GitHub Actions à chaque push sur `main`.
