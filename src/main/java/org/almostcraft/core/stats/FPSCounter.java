package org.almostcraft.core.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compteur de frames par seconde (FPS).
 * <p>
 * Cette classe mesure le nombre de frames rendues par seconde et fournit
 * une moyenne glissante pour des valeurs plus stables. Elle peut également
 * calculer le frame time (temps moyen par frame).
 * </p>
 * <p>
 * Utilisation typique :
 * <pre>{@code
 * FPSCounter fpsCounter = new FPSCounter();
 *
 * // Dans la boucle de jeu
 * while (!window.shouldClose()) {
 *     double currentTime = glfwGetTime();
 *     fpsCounter.update(currentTime);
 *
 *     if (fpsCounter.hasNewSecond()) {
 *         System.out.println("FPS: " + fpsCounter.getFPS());
 *     }
 *
 *     // Rendu...
 * }
 * }</pre>
 * </p>
 *
 * @author Lucas Préaux
 * @version 1.0
 */
public class FPSCounter {

    private static final Logger logger = LoggerFactory.getLogger(FPSCounter.class);

    // ==================== Constantes ====================

    /**
     * Intervalle de mise à jour des FPS en secondes.
     * <p>
     * Les FPS sont recalculés toutes les secondes par défaut.
     * </p>
     */
    private static final double DEFAULT_UPDATE_INTERVAL = 1.0;

    // ==================== Attributs ====================

    /**
     * Nombre de frames rendues depuis la dernière mise à jour.
     */
    private int frameCount;

    /**
     * Dernière valeur de FPS calculée.
     */
    private int currentFPS;

    /**
     * Temps du dernier calcul de FPS.
     */
    private double lastFpsTime;

    /**
     * Temps de la dernière frame.
     */
    private double lastFrameTime;

    /**
     * Temps moyen par frame en millisecondes.
     */
    private double frameTime;

    /**
     * Intervalle de mise à jour des FPS en secondes.
     */
    private final double updateInterval;

    /**
     * Indique si un nouveau calcul de FPS a été effectué cette frame.
     */
    private boolean hasNewSecond;

    // ==================== Constructeur ====================

    /**
     * Crée un nouveau compteur de FPS avec intervalle par défaut (1 seconde).
     */
    public FPSCounter() {
        this(DEFAULT_UPDATE_INTERVAL);
    }

    /**
     * Crée un nouveau compteur de FPS avec intervalle personnalisé.
     *
     * @param updateInterval intervalle de mise à jour en secondes (doit être > 0)
     * @throws IllegalArgumentException si l'intervalle est invalide
     */
    public FPSCounter(double updateInterval) {
        if (updateInterval <= 0) {
            throw new IllegalArgumentException(
                    String.format("Update interval must be positive, got: %.2f", updateInterval)
            );
        }

        this.updateInterval = updateInterval;
        this.frameCount = 0;
        this.currentFPS = 0;
        this.lastFpsTime = 0;
        this.lastFrameTime = 0;
        this.frameTime = 0;
        this.hasNewSecond = false;

        logger.debug("FPSCounter created with update interval: {}s", updateInterval);
    }

    // ==================== Mise à jour ====================

    /**
     * Met à jour le compteur avec le temps actuel.
     * <p>
     * Cette méthode doit être appelée une fois par frame dans la boucle de jeu.
     * Elle incrémente le compteur de frames et recalcule les FPS à intervalles
     * réguliers.
     * </p>
     *
     * @param currentTime le temps actuel en secondes (typiquement depuis glfwGetTime())
     */
    public void update(double currentTime) {
        // Initialiser lastFpsTime si c'est la première frame
        if (lastFpsTime == 0) {
            lastFpsTime = currentTime;
            lastFrameTime = currentTime;
        }

        // Calculer le frame time
        if (lastFrameTime > 0) {
            frameTime = (currentTime - lastFrameTime) * 1000.0; // En millisecondes
        }
        lastFrameTime = currentTime;

        // Incrémenter le compteur de frames
        frameCount++;
        hasNewSecond = false;

        // Vérifier si l'intervalle de mise à jour est écoulé
        double elapsed = currentTime - lastFpsTime;
        if (elapsed >= updateInterval) {
            // Calculer les FPS
            currentFPS = (int) Math.round(frameCount / elapsed);

            // Réinitialiser pour la prochaine période
            frameCount = 0;
            lastFpsTime = currentTime;
            hasNewSecond = true;

            logger.trace("FPS updated: {}", currentFPS);
        }
    }

    /**
     * Réinitialise le compteur de FPS.
     * <p>
     * Utile lors du redémarrage d'une partie ou après une pause prolongée.
     * </p>
     */
    public void reset() {
        frameCount = 0;
        currentFPS = 0;
        lastFpsTime = 0;
        lastFrameTime = 0;
        frameTime = 0;
        hasNewSecond = false;

        logger.debug("FPSCounter reset");
    }

    // ==================== Getters ====================

    /**
     * Retourne le nombre de FPS actuel.
     * <p>
     * Cette valeur est mise à jour à chaque intervalle (par défaut 1 seconde).
     * </p>
     *
     * @return le nombre de frames par seconde
     */
    public int getFPS() {
        return currentFPS;
    }

    /**
     * Retourne le temps moyen par frame en millisecondes.
     * <p>
     * Plus cette valeur est basse, plus le jeu est fluide.
     * À 60 FPS, le frame time est d'environ 16.67ms.
     * </p>
     *
     * @return le temps de rendu d'une frame en millisecondes
     */
    public double getFrameTime() {
        return frameTime;
    }

    /**
     * Retourne le nombre de frames rendues depuis le dernier calcul de FPS.
     * <p>
     * Utile pour le débogage ou les statistiques détaillées.
     * </p>
     *
     * @return le nombre de frames depuis la dernière mise à jour
     */
    public int getFrameCount() {
        return frameCount;
    }

    /**
     * Vérifie si un nouveau calcul de FPS a été effectué cette frame.
     * <p>
     * Permet de détecter quand afficher les nouvelles statistiques
     * sans interroger constamment la valeur des FPS.
     * </p>
     *
     * @return true si les FPS viennent d'être recalculés, false sinon
     */
    public boolean hasNewSecond() {
        return hasNewSecond;
    }

    /**
     * Retourne l'intervalle de mise à jour des FPS.
     *
     * @return l'intervalle en secondes
     */
    public double getUpdateInterval() {
        return updateInterval;
    }

    // ==================== Utilitaires ====================

    /**
     * Retourne une représentation textuelle formatée des statistiques.
     * <p>
     * Format : "FPS: 60 (16.67ms)"
     * </p>
     *
     * @return une chaîne décrivant les FPS et le frame time
     */
    public String getFormattedStats() {
        return String.format("FPS: %d (%.2fms)", currentFPS, frameTime);
    }

    /**
     * Vérifie si les performances sont en dessous d'un seuil critique.
     * <p>
     * Utile pour détecter les problèmes de performance.
     * </p>
     *
     * @param minFPS le seuil minimum de FPS acceptable
     * @return true si les FPS sont en dessous du seuil, false sinon
     */
    public boolean isUnderPerforming(int minFPS) {
        return currentFPS > 0 && currentFPS < minFPS;
    }

    @Override
    public String toString() {
        return String.format("FPSCounter[fps=%d, frameTime=%.2fms, interval=%.1fs]",
                currentFPS, frameTime, updateInterval);
    }
}
