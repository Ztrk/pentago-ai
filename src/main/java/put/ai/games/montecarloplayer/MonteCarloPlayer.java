package put.ai.games.montecarloplayer;

import java.util.List;
import java.util.Random;
import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

public class MonteCarloPlayer extends Player {

    private Random random = new Random(0xdeadbeef);

    @Override
    public String getName() {
        return "Marcin Zatorski 136834 Sebastian Michoń 136770";
    }

    @Override
    public Move nextMove(Board b) {
        List<Move> moves = b.getMovesFor(getColor());
        return moves.get(random.nextInt(moves.size()));
    }
}
