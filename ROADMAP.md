# Feuille de route - Moteur de jeu voxel 3D en Java

Excellente idée de projet ! Voici une roadmap progressive pour construire ton moteur voxel.

## Phase 1 : Fondations (2-3 semaines) - DONE

### 1.1 Setup technique
- **Bibliothèque graphique** : LWJGL (OpenGL) - la référence pour Java
- Structure Maven/Gradle du projet
- Gestion de la boucle de jeu (game loop)
- Système d'input (clavier/souris)

### 1.2 Caméra & Navigation
- Caméra FPS (First Person)
- Matrices de vue et projection
- Contrôles basiques (ZQSD, souris)

## Phase 2 : Système de voxels (3-4 semaines) - DONE

### 2.1 Structure de données
- **Chunk system** : diviser le monde en chunks (16×16×256)
- Représentation des voxels (byte array ou int array)
- Système de coordonnées (world → chunk → local)

### 2.2 Génération de terrain
- Génération procédurale simple (bruit de Perlin/Simplex)
- Différents types de blocs (air, terre, pierre, herbe)
- Chargement/déchargement des chunks autour du joueur

## Phase 3 : Rendu 3D (4-5 semaines)

### 3.1 Meshing
- **Greedy meshing** : optimiser les faces visibles
- Culling des faces cachées
- Construction des vertex buffers (VBO/VAO)

### 3.2 Rendu OpenGL
- Shaders basiques (vertex + fragment)
- Textures (texture atlas pour les blocs)
- Frustum culling (ne rendre que les chunks visibles)

### 3.3 Optimisations
- Occlusion culling
- LOD (Level of Detail) optionnel
- Batching des draw calls

## Phase 4 : Interactions (2-3 semaines)

### 4.1 Physique joueur
- Collision AABB (Axis-Aligned Bounding Box)
- Gravité
- Détection de sol (pour le saut)

### 4.2 Modification du terrain
- Raycasting pour sélectionner un bloc
- Destruction de blocs
- Placement de blocs

## Phase 5 : Éclairage (3-4 semaines)

### 5.1 Lumière du soleil
- Propagation verticale de la lumière
- Système de skylight (0-15)

### 5.2 Lumière des blocs
- Propagation 3D de la lumière (torches, etc.)
- File de propagation (BFS)
- Smooth lighting (AO basique)

### 5.3 Shaders avancés
- Ambient Occlusion
- Fog atmosphérique
- Cycle jour/nuit

## Phase 6 : Fonctionnalités avancées (optionnel)

### 6.1 Eau & Transparence
- Blocs transparents
- Tri des faces transparentes
- Simulation d'eau basique

### 6.2 Multithreading
- Génération de chunks en async
- Meshing parallèle

### 6.3 Sauvegarde
- Sérialisation des chunks (NBT ou format custom)
- Système de région files

### 6.4 IA (basique)
- Pathfinding (A*)
- Entités mobiles simples

## Ressources recommandées

**Bibliothèques Java :**
- LWJGL 3 (OpenGL, GLFW, STB)
- JOML (Java OpenGL Math Library)
- FastNoise (génération procédurale)

**Références :**
- Tutoriels LWJGL sur lwjglgamedev.github.io
- Articles Minecraft Wiki (techniques)
- "Game Engine Architecture" (concepts généraux)

**GitHub pour inspiration :**
- Terasology
- Minetest (C++ mais concepts similaires)

## Conseils pratiques

1. **Commence simple** : un cube qui tourne, puis une grille de cubes
2. **Debug visuel** : affiche les bounding boxes, les normales
3. **Profiling** : mesure les FPS dès le début
4. **Git** : commits réguliers pour chaque feature
5. **Ne pas sur-optimiser** : fais marcher avant d'optimiser

## Ordre d'implémentation suggéré

1. Fenêtre + triangle OpenGL → Cube texturé
2. Caméra FPS fonctionnelle
3. Grid de 10×10×10 cubes
4. Chunk unique avec terrain plat
5. Génération procédurale basique
6. Greedy meshing
7. Plusieurs chunks + chargement dynamique
8. Collisions joueur
9. Destruction/placement blocs
10. Éclairage basique

Bonne chance ! C'est un projet ambitieux mais très formateur. N'hésite pas si tu as des questions sur une phase spécifique.