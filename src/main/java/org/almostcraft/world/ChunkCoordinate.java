package org.almostcraft.world;

/**
 * Représente les coordonnées d'un chunk dans le monde.
 * <p>
 * Utilisé comme clé dans les HashMap pour identifier de manière unique
 * un chunk par ses coordonnées X et Z.
 * </p>
 *
 * @param x la coordonnée X du chunk
 * @param z la coordonnée Z du chunk
 * @author Lucas Préaux
 * @version 1.0
 */
public record ChunkCoordinate(int x, int z) {
}