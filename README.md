# Description du projet

Ce projet implémente une variante du jeu Puissance 4 dans un environnement partiellement observable. La spécificité de cette version du jeu de Puissance 4 est que notre joueur (IA) ne peux pas toujours voir les jetons posés par son adversaire. Plus précisément, il ne peut voir que les jetons qui se trouve en dessous d’un des jetons qu’il a posés. Il peut également voir tous les jetons d’une colonne pleine (afin de voir qu’il ne peut plus jouer dans cette colonne). 

L'objectif principal est d'intégrer une IA capable de jouer au jeu en prenant des décisions stratégiques basées sur des états de croyance. Il s'agit d'un projet académique, le travail que j'ai eu à effecteur est de compléter les méthodes de la classe AI dans le fichier AI.java qui implémente l’intelligence artificielle qui va jouer au Puissance 4

Le projet comprend plusieurs fichiers Java:

* BoardDrawing.java : Gère l'affichage du plateau de jeu.

* Connect4UI.java : Implémente l'interface utilisateur et les interactions.

* GameDisplay.java : Gère l'état visuel et logique du jeu.

* GameState.java : Gère l'état du jeu sous forme interne.

* ProbabilisticOpponentAI.java : Implémente un adversaire probabiliste jouant de manière pseudo-aléatoire.

* RandomSelector.java : Sélectionne des actions en fonction de probabilités.

* AI.java : Implémente l'intelligence artificielle.

# Fonctionnement général

Le jeu suit les règles classiques de Connect 4 : les joueurs placent des pièces chacun leur tour jusqu'à obtenir une séquence gagnante ou atteindre un état d'égalité.

L'IA sélectionne automatiquement le meilleur coup pour nous.

Fin du jeu : Le jeu s'arrête dès qu'un joueur aligne quatre jetons ou que la grille est remplie.

## La classe AI implémente une intelligence artificielle avancée capable d'évaluer les états du jeu et de choisir le coup optimal.

Méthodes principales

### findNextMove(BeliefState beliefState) : 

* Détermine le meilleur coup pour l'IA en utilisant la recherche AND-OR.

* Vérifie s'il existe un coup gagnant immédiat ou une menace immédiate.

* Utilise l'élagage alpha-bêta pour réduire l'espace de recherche.

* Retourne l'indice de la colonne sélectionnée.

### andOrSearch(BeliefState beliefState, int depth, float alpha, float beta, Set<BeliefState> path) : 

* Effectue une recherche AND-OR avec élagage alpha-bêta.

* Explore récursivement les états du jeu jusqu'à atteindre une profondeur limite ou un état terminal.

* Utilise un cache pour éviter les recomptes inutiles.

* Retourne un score estimant la valeur du coup.

### evaluateTerminalState(BeliefState beliefState) :

* Évalue un état terminal :

* Retourne un score élevé si l'IA gagne.

* Retourne un score faible si l'adversaire gagne.

### evaluateNonTerminalState(BeliefState beliefState)

* Utilise une heuristique pour évaluer les positions des jetons.

* Prend en compte le score positionnel et les alignements partiels.

### findImmediateWin(BeliefState beliefState)

* Vérifie s'il existe un coup gagnant immédiat pour l'IA.

### findImmediateThreat(BeliefState beliefState)

* Vérifie si l'adversaire peut gagner immédiatement et tente de le bloquer.

### canonicalizeBeliefState(BeliefState beliefState)

* Normalise et arrondit les probabilités d'un état de croyance pour éviter les erreurs de précision.
