package put.ai.games.montecarloplayer;

import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;

import java.util.List;
import java.util.Random;

public class MonteCarloPlayer extends Player {

    private Random random = new Random(0xdeadbeef);

    @Override
    public String getName() {
        return "Marcin Zatorski 136834 Sebastian Michoń 136770";
    }

    public static class Node {
        public int wins = 0;
        public int games = 0;
        public Node[] children;
    }

    @Override
    public Move nextMove(Board board) {
        long timeLimit = getTime() - 500;
        long startTime = System.currentTimeMillis();

        Node root = new Node();
        List<Move> moves = board.getMovesFor(getColor());

        int simulationNum = 0;
        while (System.currentTimeMillis() - startTime < timeLimit) {
            select(root, board, getColor());
            ++simulationNum;
        }
        System.out.println(simulationNum);

        int bestMoveIndex = getBestMove(root);
        Node bestNode = root.children[bestMoveIndex];
        System.out.println("Best move index: " + bestMoveIndex);
        System.out.format("W/G/R: %.1f %d %f\n", bestNode.wins / 2.0, bestNode.games,
                (double) bestNode.wins / (2 * bestNode.games));
        return moves.get(bestMoveIndex);
    }

    public Color select(Node node, Board board, Color player) {
        Color winner = board.getWinner(player);
        if (winner != null) {
            ++node.games;
            if (winner == getOpponent(player)) {
                node.wins += 2;
            }
            else if (winner == Color.EMPTY) {
                ++node.wins;
            }
            return winner;
        }

        Color result;
        if (node.children != null) {
            int nextMoveIndex = getNextMove(node);
            Move move = board.getMovesFor(player).get(nextMoveIndex);

            board.doMove(move);
            result = select(node.children[nextMoveIndex], board, getOpponent(player));
            board.undoMove(move);
        }
        else {
            node.children = new Node[board.getMovesFor(player).size()];
            for (int i = 0; i < node.children.length; ++i) {
                node.children[i] = new Node();
            }
            result = simulate(board, player);
        }

        ++node.games;
        if (result == getOpponent(player)) {
            node.wins += 2;
        }
        else if (result == Color.EMPTY) {
            ++node.wins;
        }

        return result;
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

    public int getNextMove(Node node) {
        int nextMove = 0;
        double maxScore = 0;
        for (int i = 0; i < node.children.length; ++i) {
            if (node.children[i].games == 0) {
                return random.nextInt(node.children.length);
            }
            double score = uctScore(node.children[i].wins, node.children[i].games, node.games);
            if (maxScore < score) {
                maxScore = score;
                nextMove = i;
            }
        }
        return nextMove;
    }

    public double uctScore(int wins, int games, int totalGames) {
        double C = 1.4;
        return (double) wins / (2 * games) + C * Math.sqrt(Math.log(totalGames) / games);
    }

    public int getBestMove(Node node) {
        int bestMove = 0;
        double maxWinRatio = 0;
        for (int i = 0; i < node.children.length; ++i) {
            if (node.children[i].games != 0) {
                double winRatio = (double) node.children[i].wins / node.children[i].games;
                if (maxWinRatio < winRatio) {
                    maxWinRatio = winRatio;
                    bestMove = i;
                }
            }
        }
        return bestMove;
    }
}
