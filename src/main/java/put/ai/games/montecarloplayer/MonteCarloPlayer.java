package put.ai.games.montecarloplayer;

import java.util.ArrayList;
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

    public class Result {
        public int wins = 0;
        public int games = 0;
    }

    @Override
    public Move nextMove(Board board) {
        long timeLimit = getTime() - 10;
        long startTime = System.currentTimeMillis();

        List<Move> moves = board.getMovesFor(getColor());
        List<Result> results = new ArrayList<>();
        for (int i = 0; i < moves.size(); ++i) {
            results.add(new Result());
        }

        while (System.currentTimeMillis() - startTime < timeLimit) {
            int nextMoveIndex = random.nextInt(moves.size());

            board.doMove(moves.get(nextMoveIndex));
            Color result = simulate(board, getOpponent(getColor()));
            results.get(nextMoveIndex).games += 2;
            if (result == getColor()) {
                results.get(nextMoveIndex).wins += 2;
            }
            if (result == Color.EMPTY) {
                results.get(nextMoveIndex).wins += 1;
            }
            board.undoMove(moves.get(nextMoveIndex));
        }

        int bestMoveIndex = 0;
        double max = 0;
        for (int i = 0; i < results.size(); ++i) {
            if (max < (double) results.get(i).wins / results.get(i).games) {
                max = (double) results.get(i).wins / results.get(i).games;
                bestMoveIndex = i;
            }
        }
        return moves.get(bestMoveIndex);
    }

    public Color simulate(Board b, Color player) {
        Color winner = b.getWinner(player);
        if (winner != null) {
            return winner;
        }
        List<Move> moves = b.getMovesFor(player);
        Move nextMove = moves.get(random.nextInt(moves.size()));

        b.doMove(nextMove);
        Color result = simulate(b, getOpponent(player));
        b.undoMove(nextMove);

        return result;
    }
}
