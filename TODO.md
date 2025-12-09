# Phase 3 - Plan détaillé

## 3.0 - Rendu Naïf (Priorité 1)

### 3.0.1 - Système de Shaders
- [ ] Créer classe `Shader` (compile, link, use)
- [ ] Shader vertex basique (MVP matrix)
- [ ] Shader fragment basique (couleur unie)
- [ ] Tester avec un cube centré à l'origine

### 3.0.2 - Classe Mesh basique
- [ ] Créer classe `Mesh` (VAO, VBO, EBO)
- [ ] Méthode `uploadData(float[] vertices, int[] indices)`
- [ ] Méthode `render()`
- [ ] Tester avec un cube

### 3.0.3 - ChunkRenderer naïf
- [ ] Créer classe `ChunkRenderer`
- [ ] Méthode `generateNaiveMesh(Chunk chunk)` : 36 vertices par bloc
- [ ] Ignorer les blocs AIR
- [ ] Render le chunk avec la caméra existante

### 3.0.4 - Rendu de plusieurs chunks
- [ ] Adapter Engine pour render tous les chunks loadés
- [ ] Ajouter matrices Model/View/Projection aux shaders
- [ ] Voir le monde généré en 3D !

## 3.1 - Face Culling (Priorité 2)
- [ ] Vérifier les 6 voisins de chaque bloc
- [ ] Ne générer que les faces exposées
- [ ] Mesurer amélioration FPS

## 3.2 - Greedy Meshing (Priorité 3)
- [ ] Implémenter algorithme greedy meshing 2D (une face à la fois)
- [ ] Fusionner les quads adjacents du même type
- [ ] Mesurer amélioration FPS
- 