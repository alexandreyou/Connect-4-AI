import java.util.*;
import java.util.Map.Entry;

/**
 * Class used to model the set of belief states already visited and to keep track of their values (in order to avoid visiting multiple times the same states)
 */
class ExploredSet{
	TreeMap<BeliefState, Float> exploredSet;

	/**
	 * construct an empty set
	 */
	public ExploredSet() {
		this.exploredSet = new TreeMap<BeliefState, Float>();
	}

	/**
	 * Search if a given state belongs to the explored set and returns its values if that is the case
	 * @param state the state for which the search takes place
	 * @return the value of the state if it belongs to the set, and null otherwise
	 */
	public Float get(BeliefState state) {
		Entry<BeliefState, Float> entry = this.exploredSet.ceilingEntry(state);
		if(entry == null || state.compareTo(entry.getKey()) != 0) {
			return null;
		}
		return entry.getValue() * state.probaSum() / entry.getKey().probaSum();
	}

	/**
	 * Put a belief state and its corresponding value into the set
	 * @param beliefState the belief state to be added
	 * @param value the
	 */
	public void put(BeliefState beliefState, float value) {
		this.exploredSet.put(beliefState, value);
	}
}

/**
 * Class used to store all possible results of performing an action at a given belief state
 */
class Results implements Iterable<BeliefState>{
	TreeMap<String, BeliefState> results;

	public Results(){
		this.results = new TreeMap<String, BeliefState>();
	}

	/**
	 * Return the belief state of the result that correspond to a given percept
	 * @param percept String that describe what is visible on the board for player 2
	 * @return belief state corresponding percept, or null if such a percept is not possible
	 */
	public BeliefState get(String percept) {
		return this.results.get(percept);
	}

	public void put(String s, BeliefState state) {
		this.results.put(s, state);
	}

	public Iterator<BeliefState> iterator(){
		return this.results.values().iterator();
	}
}

/**
 * Class used to represent a belief state i.e., a set of possible states the agent may be in
 */
class BeliefState implements Comparable<BeliefState>, Iterable<GameState>{
	private byte[] isVisible;

	private TreeSet<GameState> beliefState;

	private int played;

	public BeliefState() {
		this.beliefState = new TreeSet<GameState>();
		this.isVisible = new byte[6];
		for(int i = 0; i < 6; i++) {
			this.isVisible[i] = Byte.MIN_VALUE;
		}
		this.played = 0;
	}

	public BeliefState(byte[] isVisible, int played) {
		this();
		for(int i = 0; i < 6; i++) {
			this.isVisible[i] = isVisible[i];
		}
		this.played = played;
	}

	public void setStates(BeliefState beliefState) {
		this.beliefState = beliefState.beliefState;
		for(int i = 0; i < 6; i++) {
			this.isVisible[i] = beliefState.isVisible[i];
		}
		this.played = beliefState.played;
	}

	public boolean contains(GameState state) {
		return this.beliefState.contains(state);
	}

	/**
	 * returns the number of states in the belief state
	 * @return number of state
	 */
	public int size() {
		return this.beliefState.size();
	}

	public void add(GameState state) {
		if(!this.beliefState.contains(state)) {
			this.beliefState.add(state);
		}
		else {
			GameState copy = this.beliefState.floor(state);
			copy.addProba(state.proba());
		}
	}

	/**
	 * Compute the possible results from a given believe state, after the opponent perform an action. This function souhd be used only when this is the turn of the opponent.
	 * @return an objet of class result containing all possible result of an action performed by the opponent if this is the turn of the opponent, and null otherwise.
	 */
	public Results predict(){
		if(this.turn()) {
			Results tmstates = new Results();
			for(GameState state: this.beliefState) {
				RandomSelector rs = new RandomSelector();
				ArrayList<Integer> listColumn = new ArrayList<Integer>();
				ArrayList<Integer> listGameOver = new ArrayList<Integer>();
				int minGameOver = Integer.MAX_VALUE;
				for(int column = 0; column < 7; column++) {
					if(!state.isFull(column)) {
						GameState copy = state.copy();
						copy.putPiece(column);
						if(copy.isGameOver()) {
							listColumn.clear();
							listColumn.add(column);
							rs = new RandomSelector();
							rs.add(1);
							break;
						}
						int nbrGameOver = 0;
						for(int i = 0; i < 7; i++) {
							if(!copy.isFull(i)) {
								GameState copycopy = copy.copy();
								copycopy.putPiece(i);
								if(copycopy.isGameOver()) {
									nbrGameOver++;
								}
							}
						}
						if(nbrGameOver == 0) {
							rs.add(ProbabilisticOpponentAI.heuristicValue(state, column));
							listColumn.add(column);
						}
						else {
							if(minGameOver > nbrGameOver) {
								minGameOver = nbrGameOver;
								listGameOver.clear();
								listGameOver.add(column);
							}
							else {
								if(minGameOver == nbrGameOver) {
									listGameOver.add(column);
								}
							}
						}
					}
				}
				int index = 0;
				if(listColumn.isEmpty()) {
					for(int column: listGameOver) {
						listColumn.add(column);
						rs.add(1);
					}
				}
				for(int column: listColumn) {
					GameState copy = state.copy();
					if(!copy.isFull(column)) {
						byte[] tab = new byte[6];
						for(int i = 0; i < 6; i++) {
							tab[i] = this.isVisible[i];
						}
						copy.putPiece(column);
						if(copy.isGameOver()) {
							for(int i = 0; i < 6; i++) {
								for(int j = 0; j < 7; j++) {
									BeliefState.setVisible(i, j, true, tab);
								}
							}
						}
						else {
							boolean isVisible = copy.isGameOver() || copy.isFull(column);
							BeliefState.setVisible(5, column, isVisible, tab);
							for(int row = 4; row > -1; row--) {
								isVisible = isVisible || copy.content(row, column) == 2;
								BeliefState.setVisible(row, column, isVisible, tab);
							}
						}
						String s = "";
						char c = 0;
						for(int i = 0; i < 6; i++) {
							int val = tab[i] + 128;
							s += ((char)(val % 128));
							c += (val / 128) << i;
						}
						s += c;
						copy.multProba(rs.probability(index++));
						BeliefState bs = tmstates.get(s);
						if(bs!= null) {
							bs.add(copy);
						}
						else {
							bs = new BeliefState(tab, this.played + 1);
							bs.add(copy);
							tmstates.put(s, bs);
						}
					}
				}
			}
			return tmstates;
		}
		else {
			return null;
		}
	}

	/**
	 * Perform the action corresponding for the player to play a given column, and return the result of this action for each state of the belief state as a Results
	 * @param column index of the column played
	 * @return object of type Results representing all states resulting from playing the column if this is the turn of the player, and null otherwise
	 */
	public Results putPiecePlayer(int column){
		if(!this.turn()) {
			Results tmstates = new Results();
			for(GameState state: this.beliefState) {
				GameState copy = state.copy();
				byte[] tab = new byte[6];
				for(int i = 0; i < 6; i++) {
					tab[i] = this.isVisible[i];
				}
				copy.putPiece(column);
				if(copy.isGameOver()) {
					for(int i = 0; i < 6; i++) {
						for(int j = 0; j < 7; j++) {
							BeliefState.setVisible(i, j, true, tab);
						}
					}
				}
				else {
					boolean isVisible = copy.isFull(column);
					BeliefState.setVisible(5, column, isVisible, tab);
					for(int row = 4; row > -1; row--) {
						isVisible = isVisible || copy.content(row, column) == 2;
						BeliefState.setVisible(row, column, isVisible, tab);
					}
				}
				String s = "";
				char c = 0;
				for(int i = 0; i < 6; i++) {
					int val = tab[i] + 128;
					s += ((char)(val % 128));
					c += (val / 128) << i;
				}
				s += c;
				BeliefState bs = tmstates.get(s);
				if(bs!= null) {
					bs.add(copy);
				}
				else {
					bs = new BeliefState(tab, this.played + 1);
					bs.add(copy);
					tmstates.put(s, bs);
				}
			}
			return tmstates;
		}
		else {
			return null;
		}

	}

	public static BeliefState filter(Results beliefStates, GameState state) {
		byte tab[] = new byte[6];
		for(int i = 0; i < 6; i++) {
			tab[i] = Byte.MIN_VALUE;
		}
		for(int column = 0; column < 7; column++) {
			boolean isVisible = state.isGameOver() || state.isFull(column);
			BeliefState.setVisible(5, column, isVisible, tab);
			for(int row = 4; row > -1; row--) {
				isVisible = isVisible || (state.content(row, column) == 2);
				BeliefState.setVisible(row, column, isVisible, tab);
			}
		}
		String s = "";
		char c = 0;
		for(int i = 0; i < 6; i++) {
			int val = tab[i] + 128;
			s += ((char)(val % 128));
			c += (val / 128) << i;
		}
		s += c;
		BeliefState beliefState = beliefStates.get(s);
		RandomSelector rs = new RandomSelector();
		for(GameState st: beliefState.beliefState) {
			rs.add(st.proba());
		}
		int i = 0;
		for(GameState st: beliefState.beliefState) {
			st.setProba(rs.probability(i++));
		}
		return beliefState;
	}

	/**
	 * Make a copy of the belief state containing the same states
	 * @return copy of the belief state
	 */
	public BeliefState copy() {
		BeliefState bs = new BeliefState();
		for(GameState state: this.beliefState) {
			bs.add(state.copy());
		}
		for(int i = 0; i < 6; i++) {
			bs.isVisible[i] = this.isVisible[i];
		}
		bs.played = this.played;
		return bs;
	}

	public Iterator<GameState> iterator(){
		return this.beliefState.iterator();
	}

	/**
	 * Return the list of the column where a piece can be played (columns which are not full)
	 * @return
	 */
	public ArrayList<Integer> getMoves(){
		if(!this.isGameOver()) {
			ArrayList<Integer> moves = new ArrayList<Integer>();
			GameState state = this.beliefState.first();
			for(int i = 0; i < 7; i++) {
				if(!state.isFull(i))
					moves.add(i);
			}
			return moves;
		}
		else {
			return new ArrayList<Integer>();
		}
	}

	/**
	 * Provide information about the next player to play
	 * @return true if the next to play is the opponent, and false otherwise
	 */
	public boolean turn() {
		return this.beliefState.first().turn();
	}

	public boolean isVisible(int row, int column) {
		int pos = row * 7 + column;
		int index = pos / 8;
		pos = pos % 8;
		return ((this.isVisible[index] + 128) >> pos) % 2 == 1;
	}

	public void setVisible(int row, int column, boolean val) {
		int pos = row * 7 + column;
		int index = pos / 8;
		pos = pos % 8;
		int delta = ((val? 1: 0) - (this.isVisible(row, column)? 1: 0)) << pos;
		this.isVisible[index] = (byte) (this.isVisible[index] + delta);
	}

	public static void setVisible(int row, int column, boolean val, byte[] tab) {
		int pos = row * 7 + column;
		int index = pos / 8;
		pos = pos % 8;
		int posValue = tab[index] + 128;
		int delta = ((val? 1: 0) - ((posValue >> pos) % 2)) << pos;
		tab[index] = (byte) (posValue + delta - 128);
	}

	/**
	 * Check if the game is over in all state of the belief state. Note that when the game is over, the board is revealed and the environment becomes observable.
	 * @return true if the game is over, and false otherwise
	 */
	public boolean isGameOver() {
		for(GameState state: this.beliefState) {
			if(!state.isGameOver()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if all the games in the belief state are full
	 * @return
	 */
	public boolean isFull() {
		return this.beliefState.first().isFull();
	}


	public void restart() {
		this.beliefState = new TreeSet<GameState>();
		this.isVisible = new byte[6];
		for(int i = 0; i < 6; i++) {
			this.isVisible[i] = Byte.MIN_VALUE;
		}
		this.played = 0;
	}

	public String toString() {
		String s = "BeliefState: size = " + this.beliefState.size() + " played = " + this.played + "\n";
		for(int row = 5; row > -1; row--) {
			for(int column = 0; column < 7; column++) {
				s += this.isVisible(row, column)? "1": "0";
			}
			s += "\n";
		}
		for(GameState state:this.beliefState) {
			s += state.toString() + "\n";
		}
		return s;
	}

	public int compareTo(BeliefState bs) {
		if(this.played != bs.played)
			return this.played > bs.played? 1: -1;
		for(int i = 0; i < 6; i++) {
			if(this.isVisible[i] != bs.isVisible[i])
				return this.isVisible[i] > bs.isVisible[i]? 1: -1;
		}
		if(this.beliefState.size() != bs.beliefState.size()) {
			return this.beliefState.size() > bs.beliefState.size()? 1: -1;
		}
		Iterator<GameState> iter = bs.beliefState.iterator();
		for(GameState next: this.beliefState) {
			GameState otherNext = iter.next();
			int comp = next.compareTo(otherNext);
			if(comp != 0)
				return comp;
		}
		iter = bs.beliefState.iterator();
		float sum1 = this.probaSum(), sum2 = bs.probaSum();
		for(GameState next: this.beliefState) {
			GameState otherNext = iter.next();
			if(Math.abs(next.proba() * sum1 - otherNext.proba() * sum2) > 0.001) {
				return next.proba() > otherNext.proba()? 1: -1;
			}
		}
		return 0;
	}

	public float probaSum() {
		float sum = 0;
		for(GameState state: this.beliefState) {
			sum += state.proba();
		}
		return sum;
	}
}

public class AI {


	private static final ExploredSet cache = new ExploredSet(); // shared cache for multiple games
	private static final int[][] POSITIONAL_SCORE = {
			{1, 2, 3, 5, 3, 2, 1},
			{2, 4, 6, 8, 6, 4, 2},
			{5, 8, 11, 13, 11, 8, 5},
			{5, 8, 11, 13, 11, 8, 5},
			{4, 6, 8, 10, 8, 6, 4},
			{3, 4, 5, 7, 5, 4, 3}
	};
	private static final float PROBA_THRESHOLD = 0.00001f;
	private static final int DEPTH = 6;

	public AI() {
	}

		/**
		 * determines the best next move for the AI using the AND-OR search alg
		 *
		 * @param beliefState The current belief state of the game.
		 * @return The index of the best column to play.
		 */
	public static int findNextMove(BeliefState beliefState) {
		int[] preferredOrder = {3, 2, 4, 1, 5, 0, 6};
		ArrayList<Integer> availableMoves = beliefState.getMoves();

		// check for an immediate winning move
		int winMove = findImmediateWin(beliefState);
		if (winMove != -1) {
			System.out.println("winning move at column " + winMove);
			return winMove;
		}

		// check for immediate threats to block
		int immediateThreat = findImmediateThreat(beliefState);
		if (immediateThreat != -1) {
			System.out.println("blocking immediate threat at column " + immediateThreat);
			return immediateThreat;
		}

		// prioritize moves based on preferred order
		ArrayList<Integer> prioritizedMoves = new ArrayList<>();
		for (int column : preferredOrder) {
			if (availableMoves.contains(column)) {
				prioritizedMoves.add(column);
			}
		}



		if (availableMoves.isEmpty()) {
			System.out.println("no available moves. returning -1.");
			return -1; // no moves possible
		}

		// evaluate moves using AND-OR search
		int bestMove = -1;
		float bestScore = Float.NEGATIVE_INFINITY;

		for (int move : prioritizedMoves) {
			Results results = beliefState.putPiecePlayer(move);
			if (results == null || results.results.isEmpty()) continue;

			float moveScore = Float.NEGATIVE_INFINITY;
			for (BeliefState nextState : results) {
				float score = andOrSearch(nextState, DEPTH, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, new HashSet<>());
				moveScore = Math.max(moveScore, score);
			}

			if (moveScore > bestScore) {
				bestScore = moveScore;
				bestMove = move;
			}
		}

		if (bestScore == Float.NEGATIVE_INFINITY) {
			System.out.println("Debug: No beneficial move found, choosing a random column.");
			if (!availableMoves.isEmpty()) {
				return availableMoves.get(0); // Fallback to the first available move
			}
			return -1; // No moves possible
		}

		System.out.println("selected move: " + bestMove + " with score: " + bestScore);
		return bestMove;
	}

		/**
		 * Performs an AND-OR search on the belief states
		 *
		 * @param beliefState Current belief state.
		 * @param depth       Remaining search depth.
		 * @param alpha       Alpha value for pruning.
		 * @param beta        Beta value for pruning.
		 * @param path        Set of visited states to avoid cycles.
		 * @return A score representing the quality of the belief state.
		 */
	private static float andOrSearch(BeliefState beliefState, int depth, float alpha, float beta, Set<BeliefState> path) {
		BeliefState canonicalState = canonicalizeBeliefState(beliefState);
		if (path.contains(canonicalState)) {
			return Float.NEGATIVE_INFINITY; // prevent cycles
		}
		path.add(canonicalState);

		Float cachedScore = cache.get(canonicalState);
		if (cachedScore != null) {
			return cachedScore;
		}

		// base cases
		if (beliefState.isGameOver()) {
			return evaluateTerminalState(beliefState);
		}
		if (depth == 0) {
			return evaluateNonTerminalState(beliefState);
		}

		float bestScore;
		if (beliefState.turn()) { // opponent's turn (AND node)
			bestScore = Float.POSITIVE_INFINITY;
			for (BeliefState nextState : beliefState.predict()) {
				float score = andOrSearch(nextState, depth - 1, alpha, beta, path);
				bestScore = Math.min(bestScore, score);
				beta = Math.min(beta, score);
				if (beta <= alpha) break; // prune
			}
		} else { // AI's turn (OR node)
			bestScore = Float.NEGATIVE_INFINITY;
			for (int move : beliefState.getMoves()) {
				Results results = beliefState.putPiecePlayer(move);
				if (results == null) continue;

				for (BeliefState nextState : results) {
					float score = andOrSearch(nextState, depth - 1, alpha, beta, path);
					bestScore = Math.max(bestScore, score);
					alpha = Math.max(alpha, score);
					if (alpha >= beta) break; // prune
				}
			}
		}

		path.remove(canonicalState);

		// cache the result and return the score
		bestScore = Math.round(bestScore * 1e4) / 1e4f; // round for precision
		cache.put(canonicalState, bestScore);
		return bestScore;
	}

	/**
		 * evaluates terminal states
		 *
		 * @param beliefState The terminal belief state.
		 * @return A score representing the outcome.
		 */
		private static float evaluateTerminalState(BeliefState beliefState) {
			float totalScore = 0;

			for (GameState gameState : beliefState) {
				if (gameState.isGameOver()) {
					boolean aiWins = !gameState.turn();
					totalScore += aiWins ? 10000 : -10000;
				}
			}
			return totalScore;
		}

		/**
		 * evaluates non-terminal states using a heuristic function
		 *
		 * @param beliefState The belief state to evaluate.
		 * @return A heuristic score.
		 */
		private static float evaluateNonTerminalState(BeliefState beliefState) {
			float totalScore = 0;

			for (GameState gameState : beliefState) {
				for (int row = 0; row < 6; row++) {
					for (int col = 0; col < 7; col++) {
						int content = gameState.content(row, col);
						if (content == 1) { // AI's pieces
							totalScore += evaluateLine(gameState, row, col, 0, 1, 1);
							totalScore += evaluateLine(gameState, row, col, 1, 0, 1);
							totalScore += POSITIONAL_SCORE[row][col];
						} else if (content == 2) { // Opponent's pieces
							totalScore -= evaluateLine(gameState, row, col, 0, 1, 2);
							totalScore -= POSITIONAL_SCORE[row][col];
						}
					}
				}
			}

			return totalScore;
		}

		/**
		 * Evaluates a line of tokens in a specific direction.
		 */
		private static float evaluateLine(GameState gameState, int row, int col, int deltaRow, int deltaCol, int player) {
			int count = 0;
			boolean openStart = false, openEnd = false;

			// check line ends for openness
			if (isWithinBounds(row - deltaRow, col - deltaCol) && gameState.content(row - deltaRow, col - deltaCol) == 0) {
				openStart = true;
			}
			for (int i = 0; i < 4; i++) {
				int newRow = row + i * deltaRow;
				int newCol = col + i * deltaCol;

				if (isWithinBounds(newRow, newCol)) {
					int content = gameState.content(newRow, newCol);
					if (content == player) {
						count++;
					} else if (content != 0) {
						return 0; // blocked line
					}
				} else {
					return 0; // out of bounds
				}
			}
			if (isWithinBounds(row + 4 * deltaRow, col + 4 * deltaCol) && gameState.content(row + 4 * deltaRow, col + 4 * deltaCol) == 0) {
				openEnd = true;
			}

			int openEnds = (openStart ? 1 : 0) + (openEnd ? 1 : 0);
			if (count == 3 && openEnds > 0) return player == 1 ? 1000 : -1000; // three-in-a-row
			if (count == 2 && openEnds > 0) return player == 1 ? 300 : -300;   // two-in-a-row

			return count * openEnds; // general scoring for partial lines
		}

	private static boolean isWithinBounds(int row, int col) {
		return row >= 0 && row < 6 && col >= 0 && col < 7;
	}
	// finds immediate threats
	private static int findImmediateThreat(BeliefState beliefState) {

		Map<Integer, Double> threatProbabilities = new HashMap<>();
		int totalBeliefStates = 0;

		// 遍历所有 BeliefState
		for (GameState gameState : beliefState) {
			totalBeliefStates++;
			for (int move : beliefState.getMoves()) {
				if (!gameState.isFull(move)) {
					GameState simulatedState = gameState.copy();

					// simulate opponent's moves
					simulatedState.changeTurn();
					simulatedState.putPiece(move);
					simulatedState.changeTurn();

					// is it a direct threat?
					if (simulatedState.isGameOver()) {
						threatProbabilities.put(move, threatProbabilities.getOrDefault(move, 0.0) + 1.0);
					}
				}
			}
		}

		// calculate threat prob. for each col
		for (Map.Entry<Integer, Double> entry : threatProbabilities.entrySet()) {
			threatProbabilities.put(entry.getKey(), entry.getValue() / totalBeliefStates);
		}

		// find col with highest threat prob
		double threshold = 0.3; //
		int mostProbableThreat = -1;
		double highestProbability = 0.0;

		for (Map.Entry<Integer, Double> entry : threatProbabilities.entrySet()) {
			double probability = entry.getValue();
			System.out.println("Move: " + entry.getKey() + ", Threat probability: " + probability);

			if (probability > threshold && probability > highestProbability) {
				mostProbableThreat = entry.getKey();
				highestProbability = probability;
			}
		}

		// return col with highest threat prob
		if (mostProbableThreat != -1) {
			System.out.println("Most probable threat detected at column: " + mostProbableThreat);
			return mostProbableThreat;
		}

		return -1; // no threat found
	}


	private static int findImmediateWin(BeliefState beliefState) {
		for (int move : beliefState.getMoves()) {
			// Make a copy of the current belief state
			BeliefState beliefStateCopy = beliefState.copy();

			Results results = beliefStateCopy.putPiecePlayer(move);
			if (results == null) continue;

			for (BeliefState nextState : results) {
				//System.out.println("next states considered by the findimmediatewin");
				//System.out.println(nextState.toString());
				if (nextState.isGameOver() && !nextState.turn()) {
					// AI wins if this move leads to a game over
					return move;
				}
			}
		}
		return -1; // No immediate winning move found
	}

	/**
	 * utility method to count the total number of pieces played on the board
	 * @param gameState The GameState instance to evaluate.
	 * @return The total number of pieces played on the board.
	 */
	private static int countPiecesPlayed(GameState gameState) {
		int count = 0;
		for (int row = 0; row < 6; row++) {
			for (int col = 0; col < 7; col++) {
				if (gameState.content(row, col) != 0) {
					count++;
				}
			}
		}
		return count;
	}

	private static BeliefState canonicalizeBeliefState(BeliefState beliefState) {
		BeliefState copy = beliefState.copy(); // Avoid modifying the original
		float sum = copy.probaSum();

		// Normalize and round probabilities
		for (GameState state : copy) {
			float normalizedProba = (state.proba() / sum);
			float roundedProba = Math.round(normalizedProba * 1e6) / 1e6f; // Round to 6 decimals
			state.setProba(roundedProba);
		}

		return copy; // Return the canonicalized state
	}


}
