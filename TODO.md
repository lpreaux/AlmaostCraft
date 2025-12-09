# Phase 3 - Plan détaillé

## 3.0 - Rendu Naïf (Priorité 1)

### 3.0.1 - Système de Shaders
- [x] Créer classe `Shader` (compile, link, use)
- [x] Shader vertex basique (MVP matrix)
- [x] Shader fragment basique (couleur unie)
- [x] Tester avec un cube centré à l'origine

### 3.0.2 - Classe Mesh basique
- [x] Créer classe `Mesh` (VAO, VBO, EBO)
- [x] Méthode `uploadData(float[] vertices, int[] indices)`
- [x] Méthode `render()`
- [x] Tester avec un cube

### 3.0.3 - ChunkRenderer naïf
- [x] Créer classe `ChunkRenderer`
- [x] Méthode `generateNaiveMesh(Chunk chunk)` : 36 vertices par bloc
- [x] Ignorer les blocs AIR
- [x] Render le chunk avec la caméra existante

### 3.0.4 - Rendu de plusieurs chunks
- [x] Adapter Engine pour render tous les chunks loadés
- [x] Ajouter matrices Model/View/Projection aux shaders
- [x] Voir le monde généré en 3D !

## 3.1 - Face Culling (Priorité 2)
- [x] Vérifier les 6 voisins de chaque bloc
- [x] Ne générer que les faces exposées
- [x] Mesurer amélioration FPS

## 3.2 - Greedy Meshing (Priorité 3)
- [x] Implémenter algorithme greedy meshing 2D (une face à la fois)
- [x] Fusionner les quads adjacents du même type
- [x] Mesurer amélioration FPS