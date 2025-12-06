# AlmostCraft

Un moteur de jeu voxel 3D développé en Java avec OpenGL, créé dans un objectif d'apprentissage du développement de jeux vidéo.

> *"It's not Minecraft... but it's almost there!"*

## 🎯 Objectif

Projet éducatif pour comprendre les mécanismes d'un moteur de jeu voxel (type Minecraft) : génération procédurale de terrain, rendu 3D optimisé, physique et systèmes de chunks.

## 🚀 Fonctionnalités prévues

- [x] Configuration du projet avec Gradle
- [ ] Rendu OpenGL avec LWJGL
- [ ] Système de caméra FPS
- [ ] Génération procédurale de terrain
- [ ] Gestion des chunks (16×16×256)
- [ ] Greedy meshing pour optimisation
- [ ] Système de collision et physique joueur
- [ ] Placement et destruction de blocs
- [ ] Système d'éclairage (skylight + block light)
- [ ] Textures et shaders

## 🛠️ Technologies

- **Langage** : Java 21
- **Build** : Gradle (Kotlin DSL)
- **Graphique** : LWJGL 3 (OpenGL)
- **Mathématiques** : JOML

## 📦 Installation

```bash
git clone https://github.com/lpreaux/almostcraft.git
cd almostcraft
./gradlew run
```

## 🎮 Contrôles

- **ZQSD** : Déplacement
- **Souris** : Regarder autour
- **Espace** : Sauter
- **Clic gauche** : Détruire un bloc
- **Clic droit** : Placer un bloc
- **Échap** : Menu/Quitter

## 📚 Ressources d'apprentissage

Ce projet suit les concepts de :
- [LWJGL Game Development](http://lwjgl.org/)
- Minecraft Wiki (techniques voxel)
- Articles sur le greedy meshing et l'optimisation

## 🤝 Contribution

Projet personnel d'apprentissage, mais les suggestions et retours sont bienvenus !

## 📝 License

MIT License - Projet éducatif libre d'utilisation

---

*Développé par Lucas Préaux ([@lpreaux](https://github.com/lpreaux)) - Concepteur Développeur Full Stack en reconversion gamedev*