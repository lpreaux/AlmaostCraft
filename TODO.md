# 📋 Plan d'implémentation : Résumé des tâches

## 🎯 Objectif global
Séparer **Simulation Distance** (ChunkLoader) et **Render Distance** (CullingManager) avec une architecture propre et extensible.

---

## 📦 Phase 1 : Création des nouvelles classes de base

### **Tâche 1.1 : BoundingBox**
📁 `src/main/java/org/almostcraft/graphics/culling/BoundingBox.java`

**Objectif :** AABB réutilisable pour les chunks
```java
- Constructor(minX, minY, minZ, maxX, maxY, maxZ)
- Factory method: fromChunk(Chunk)
- Getters pour les bounds
```

---

### **Tâche 1.2 : CullingStats**
📁 `src/main/java/org/almostcraft/graphics/culling/CullingStats.java`

**Objectif :** Statistiques de debug détaillées
```java
- Compteurs : totalLoaded, distanceCulled, frustumCulled, occlusionCulled, rendered
- reset()
- getSummary() pour affichage
```

---

### **Tâche 1.3 : CullingManager**
📁 `src/main/java/org/almostcraft/graphics/culling/CullingManager.java`

**Objectif :** Centraliser toute la logique de culling
```java
- Constructor(renderDistance)
- update(Camera, Vector3f playerPos)
- cullChunks(Collection<Chunk>) → List<Chunk>
- setRenderDistance(int)
- getStats() → CullingStats
```

**Logique interne :**
1. Distance culling (render distance)
2. Frustum culling (réutilise ton Frustum existant)
3. Stats tracking

---

## 🔄 Phase 2 : Migration du code existant

### **Tâche 2.1 : Déplacer Frustum**
📁 Déplacer `render/chunk/frustum/` → `graphics/culling/frustum/`

**Objectif :** Frustum n'est plus lié au renderer
- Déplacer `Frustum.java`
- Déplacer `Plane.java`
- Mettre à jour les imports

---

### **Tâche 2.2 : Clarifier ChunkLoader (Simulation)**
📁 `src/main/java/org/almostcraft/world/chunk/ChunkLoader.java`

**Objectif :** Renommer pour clarifier la simulation distance
```java
- renderDistance → simulationDistance
- unloadDistance → simulationUnloadDistance
- Mettre à jour javadoc pour clarifier "simulation vs render"
```

---

### **Tâche 2.3 : Simplifier ChunkRenderer**
📁 `src/main/java/org/almostcraft/render/chunk/ChunkRenderer.java`

**Objectif :** Déléguer le culling au CullingManager
```java
// AVANT (actuel)
for (Chunk chunk : world.getLoadedChunks()) {
    if (!frustum.isChunkVisible(...)) continue;
    // render
}

// APRÈS (simplifié)
List<Chunk> visibleChunks = cullingManager.cullChunks(world.getLoadedChunks());
for (Chunk chunk : visibleChunks) {
    // render
}
```

**Changements :**
- Supprimer `Frustum frustum` (maintenant dans CullingManager)
- Supprimer `Matrix4f viewProjection` (maintenant dans CullingManager)
- Ajouter `CullingManager cullingManager`
- Simplifier `render(Camera)` pour utiliser le manager

---

## 🔌 Phase 3 : Intégration dans le jeu

### **Tâche 3.1 : Créer CullingManager dans Application**
📁 `src/main/java/org/almostcraft/Application.java` (ou équivalent)

**Objectif :** Instancier et injecter le CullingManager
```java
CullingManager cullingManager = new CullingManager(renderDistance);
ChunkRenderer renderer = new ChunkRenderer(world, blockRegistry, shader, textureArray, cullingManager);
```

---

### **Tâche 3.2 : Update dans la game loop**
📁 Dans ta boucle principale

**Objectif :** Mettre à jour le CullingManager chaque frame
```java
// Dans la game loop, AVANT le rendu
cullingManager.update(camera, playerPosition);
chunkRenderer.render(camera);
```

---

### **Tâche 3.3 : Afficher les stats (debug)**
📁 Dans ta boucle ou overlay debug

**Objectif :** Voir l'impact du culling
```java
CullingStats stats = cullingManager.getStats();
System.out.println(stats.getSummary());
// Ou affichage à l'écran avec GUI
```

---

## 🧪 Phase 4 : Tests et validation

### **Tâche 4.1 : Tests unitaires**
📁 `src/test/java/org/almostcraft/graphics/culling/`

- `BoundingBoxTest.java`
- `CullingManagerTest.java`
- `CullingStatsTest.java`

---

### **Tâche 4.2 : Tests en jeu**
- Vérifier que les chunks se chargent/déchargent correctement
- Vérifier que le frustum culling fonctionne (tourner la caméra)
- Comparer les performances avant/après
- Valider que simulationDistance > renderDistance fonctionne

---

## 🚀 Phase 5 : Optimisations avancées (optionnel)

### **Tâche 5.1 : Occlusion Culling simple**
📁 Dans `CullingManager`

**Objectif :** Ne pas rendre les chunks complètement cachés
- Algorithme simple : raycasting ou chunk masking
- Ajout dans `cullChunks()` après frustum

---

### **Tâche 5.2 : LOD System**
📁 Nouvelle classe `LODManager`

**Objectif :** Simplifier les chunks lointains
- Différents niveaux de détail selon distance
- Meshes simplifiés pour chunks lointains

---

### **Tâche 5.3 : Dynamic Render Distance**
📁 Dans `CullingManager`

**Objectif :** Ajuster automatiquement selon FPS
```java
if (fps < 30) {
    renderDistance--;
} else if (fps > 60 && renderDistance < maxRenderDistance) {
    renderDistance++;
}
```

---

## 📊 Ordre d'exécution recommandé

```
1. BoundingBox (15 min)
2. CullingStats (10 min)
3. CullingManager squelette (30 min)
4. Déplacer Frustum (5 min)
5. Clarifier ChunkLoader (20 min)
6. Simplifier ChunkRenderer (30 min)
7. Intégrer dans Application (20 min)
8. Tests et debug (1-2h)
9. [OPTIONNEL] Occlusion culling (2-3h)
10. [OPTIONNEL] LOD (4-6h)
```

**Temps estimé pour Phase 1-4 : ~3-4h**

---

## ✅ Checklist finale

- [x] BoundingBox créé et testé
- [x] CullingStats créé
- [x] CullingManager créé avec distance + frustum culling
- [x] Frustum déplacé dans graphics/culling/
- [x] ChunkLoader clarifié (simulation distance)
- [x] ChunkRenderer simplifié (délègue au manager)
- [ ] Integration dans Application
- [ ] Tests en jeu validés
- [ ] Stats debug affichées
- [ ] Documentation mise à jour

---

## 🎯 Prêt à commencer ?

On commence par la **Tâche 1.1 : BoundingBox** ? C'est la base de tout le système ! 🚀
