InputManager (classe principale)

- Point d'entrée unique pour tous les inputs
- S'enregistre aux callbacks GLFW
- Gère le cycle de vie (init, update, cleanup)
- Expose des méthodes publiques pour interroger l'état des inputs

KeyboardInput (sous-composant)

- Stocke l'état des touches (pressée/relâchée/maintenue)
- Gère la logique des touches répétées vs single press
- Méthode isKeyPressed(), isKeyDown(), isKeyJustPressed()

MouseInput (sous-composant)

- Position actuelle (x, y)
- Delta de mouvement depuis la dernière frame
- État des boutons
- Scroll wheel
- Possibilité de capturer/libérer le curseur (pour FPS)