package put.ai.games.montecarloplayer;

import put.ai.games.game.Board;
import put.ai.games.game.Move;
import put.ai.games.game.Player;
import put.ai.games.game.moves.RotateMove;
import put.ai.games.pentago.impl.PentagoMove;

import java.util.ArrayList;
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
    public Move nextMove(Board originalBoard) {
        long startTime = System.currentTimeMillis();
        long timeLimit = (long) (getTime() * 0.95);

        FastBoard board = new FastBoard(originalBoard);
        Node root = new Node();
        List<FastMove> moves = board.getMoves();

        int simulationNum = 0;
        while (System.currentTimeMillis() - startTime < timeLimit) {
            select(root, board, getColor());
            ++simulationNum;
        }

        int bestMoveIndex = getBestMove(root);
        FastMove bestMove = moves.get(bestMoveIndex);

        Node bestNode = root.children[bestMoveIndex];
        System.out.println("Rollouts: " + simulationNum);
        System.out.println("Best move index: " + bestMoveIndex);
        System.out.format("W/G/R: %.1f %d %f\n", bestNode.wins / 2.0, bestNode.games,
                (double) bestNode.wins / (2 * bestNode.games));
        System.out.println("Elapsed time: " + (System.currentTimeMillis() - startTime));

        return bestMove.toMove(getColor(), board.getSize());
    }

    public Color select(Node node, FastBoard board, Color player) {
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
        List<FastMove> moves = board.getMoves();
        if (node.children == null) {
            node.children = new Node[moves.size()];
        }

        int nextMoveIndex = getNextMove(node);
        FastMove move = moves.get(nextMoveIndex);

        moves = null;

        board.doMove(move, player);
        if (node.children[nextMoveIndex] == null) {
            node.children[nextMoveIndex] = new Node();
            result = simulate(board, player);
            ++node.children[nextMoveIndex].games;
            if (result == player) {
                node.children[nextMoveIndex].wins += 2;
            }
            else if (result == Color.EMPTY) {
                ++node.children[nextMoveIndex].wins;
            }
        }
        else {
            result = select(node.children[nextMoveIndex], board, getOpponent(player));
        }
        board.undoMove(move, player);

        ++node.games;
        if (result == getOpponent(player)) {
            node.wins += 2;
        }
        else if (result == Color.EMPTY) {
            ++node.wins;
        }

        return result;
    }

    public Color simulate(FastBoard board, Color player) {
        Color winner = board.getWinner(player);
        if (winner != null) {
            return winner;
        }
        List<FastMove> moves = board.getMoves();
        FastMove nextMove = moves.get(random.nextInt(moves.size()));
        moves = null;

        board.doMove(nextMove, player);
        Color result = simulate(board, getOpponent(player));
        board.undoMove(nextMove, player);

        return result;
    }

    public int getNextMove(Node node) {
        int nextMove = 0;
        double maxScore = 0;
        double totalGamesLog = Math.log(node.games);
        for (int i = 0; i < node.children.length; ++i) {
            if (node.children[i] == null) {
                return random.nextInt(node.children.length);
            }
            double score = uctScore(node.children[i].wins, node.children[i].games, totalGamesLog);
            if (maxScore < score) {
                maxScore = score;
                nextMove = i;
            }
        }
        return nextMove;
    }

    public double uctScore(int wins, int games, double totalGamesLog) {
        double C = 1.4;
        return (double) wins / (2 * games) + C * Math.sqrt(totalGamesLog / games);
    }

    public int getBestMove(Node node) {
        int bestMove = 0;
        double maxWinRatio = 0;
        for (int i = 0; i < node.children.length; ++i) {
            if (node.children[i] != null) {
                double winRatio = (double) node.children[i].wins / node.children[i].games;
                if (maxWinRatio < winRatio) {
                    maxWinRatio = winRatio;
                    bestMove = i;
                }
            }
        }
        return bestMove;
    }

    private static class FastBoard {
        final int boardSize = 6;
        final int smallBoardSize = 3;
        long[] player1;
        long[] player2;
        long[] bitmasks;
        int[] rotation;
        FastMove[][][] moves;

        public FastBoard(Board board) {
            if (board.getSize() != 6) {
                throw new IllegalArgumentException("Only 6x6 board supported");
            }

            this.player1 = new long[4];
            this.player2 = new long[4];
            this.rotation = new int[4];

            computeBitmasks();
            computeMoves();
            initializeBoard(board);
        }

        private void initializeBoard(Board board) {
            for (int i = 0; i < boardSize; ++i) {
                for (int j = 0; j < boardSize; ++j) {
                    if (board.getState(j, i) == Color.PLAYER1) {
                        setBoardState(player1, 2 * (i / smallBoardSize) + j / smallBoardSize,
                                smallBoardSize * (i % smallBoardSize) + j % smallBoardSize);
                    }
                    else if (board.getState(j, i) == Color.PLAYER2) {
                        setBoardState(player2, 2 * (i / smallBoardSize) + j / smallBoardSize,
                                smallBoardSize * (i % smallBoardSize) + j % smallBoardSize);
                    }
                }
            }
        }

        private void computeBitmasks() {
            int smallBoardSize = boardSize / 2;
            bitmasks = new long[smallBoardSize * smallBoardSize];

            int[][] board = new int[smallBoardSize][smallBoardSize];
            for (int i = 0; i < smallBoardSize; ++i) {
                for (int j = 0; j < smallBoardSize; ++j) {
                    board[i][j] = i * smallBoardSize + j;
                }
            }
            for (int rotation = 0; rotation < 4; ++rotation) {
                for (int i = 0; i < board.length; ++i) {
                    for (int j = 0; j < board.length; ++j) {
                        int bitIndex = rotation * smallBoardSize * smallBoardSize + i * smallBoardSize + j;
                        bitmasks[board[i][j]] |= 1L << bitIndex;
                    }
                }
                board = rotateArray(board);
            }
        }

        private void computeMoves() {
            moves = new FastMove[4][smallBoardSize * smallBoardSize][8];
            for (int board = 0; board < 4; ++board) {
                for (int field = 0; field < smallBoardSize * smallBoardSize; ++field) {
                    for (int rotation = 0; rotation < 4; ++rotation) {
                        moves[board][field][2 * rotation] = new FastMove(board, field, rotation,
                                RotateMove.Direction.CLOCKWISE);
                        moves[board][field][2 * rotation + 1] = new FastMove(board, field, rotation,
                                RotateMove.Direction.COUNTERCLOCKWISE);
                    }
                }
            }
        }

        private int[][] rotateArray(int[][] array) {
            int[][] rotated = new int[array.length][array.length];
            for (int i = 0; i < array.length; ++i) {
                for (int j = 0; j < array[i].length; ++j) {
                    rotated[j][array.length - i - 1] = array[i][j];
                }
            }
            return rotated;
        }

        public void doMove(FastMove move, Color player) {
            if (player == Color.PLAYER1) {
                setBoardState(player1, move.getBoard(), move.getField());
            }
            else if (player == Color.PLAYER2) {
                setBoardState(player2, move.getBoard(), move.getField());
            }
            if (move.direction == RotateMove.Direction.CLOCKWISE) {
                rotation[move.getRotatedBoard()] = (rotation[move.getRotatedBoard()] + 1) % 4;
            }
            else {
                rotation[move.getRotatedBoard()] = (rotation[move.getRotatedBoard()] + 3) % 4;
            }
        }

        private void setBoardState(long[] state, int board, int field) {
            state[board] |= bitmasks[field];
        }

        public void undoMove(FastMove move, Color player) {
            if (player == Color.PLAYER1) {
                player1[move.getBoard()] &= ~bitmasks[move.getField()];
            }
            else if (player == Color.PLAYER2) {
                player2[move.getBoard()] &= ~bitmasks[move.getField()];
            }
            if (move.getDirection() == RotateMove.Direction.CLOCKWISE) {
                rotation[move.getRotatedBoard()] = (rotation[move.getRotatedBoard()] + 3) % 4;
            }
            else {
                rotation[move.getRotatedBoard()] = (rotation[move.getRotatedBoard()] + 1) % 4;
            }
        }

        public List<FastMove> getMoves() {
            List<FastMove> moves = new ArrayList<>();
            for (int i = 0; i < 4; ++i) {
                for (int j = 0; j < smallBoardSize * smallBoardSize; ++j) {
                    if (((player1[i] | player2[i]) & (1L << j)) == 0) {
                        for (int rotation = 0; rotation < 4; ++rotation) {
                            moves.add(this.moves[i][j][2 * rotation]);
                            moves.add(this.moves[i][j][2 * rotation + 1]);
                        }
                    }
                }
            }
            return moves;
        }

        public int getSize() {
            return boardSize;
        }

        private boolean hasFive(long board1, long board2) {
            long winBitmask = 0xDB;
            long orBitmask = 0x49;
            long result = board1 & (board1 >>> 1) & ((board1 >>> 2) | (orBitmask << 1))
                    & ((board2 << 1) | orBitmask) & board2 & (board2 >>> 1);
            return (result & winBitmask) != 0;
        }

        public boolean isWinner(long[] state) {
            int length = smallBoardSize * smallBoardSize;
            if (hasFive(state[0] >>> (rotation[0] * length), state[1] >>> (rotation[1] * length))) {
                return true;
            }
            if (hasFive(state[2] >>> (rotation[2] * length), state[3] >>> (rotation[3] * length))) {
                return true;
            }
            if (hasFive(state[0] >>> ((rotation[0] + 3) % 4 * length),
                    state[2] >>> ((rotation[2] + 3) % 4 * length))) {
                return true;
            }
            return hasFive(state[1] >>> ((rotation[1] + 3) % 4 * length),
                    state[3] >>> ((rotation[3] + 3) % 4 * length));
        }

        public boolean isDraw() {
            long bitmask = 0x1FF;
            for (int i = 0; i < 4; ++i) {
                if ((~(player1[i] | player2[i]) & bitmask) != 0) {
                    return false;
                }
            }
            return true;
        }

        public Color getWinner(Color nextPlayer) {
            boolean isPlayer1 = isWinner(player1);
            boolean isPlayer2 = isWinner(player2);
            if (isPlayer1 && isPlayer2) {
                return nextPlayer;
            }
            if (isPlayer1) {
                return Color.PLAYER1;
            }
            if (isPlayer2) {
                return Color.PLAYER2;
            }
            if (isDraw()) {
                return Color.EMPTY;
            }
            return null;
        }

        @Override
        public String toString() {
            long[] player1State = new long[4];
            long[] player2State = new long[4];
            for (int i = 0; i < 4; ++i) {
                player1State[i] = player1[i] >> (rotation[i] * smallBoardSize * smallBoardSize);
                player2State[i] = player2[i] >> (rotation[i] * smallBoardSize * smallBoardSize);
            }
            StringBuilder board = new StringBuilder();
            for (int i = 0; i < boardSize; ++i) {
                for (int j = 0; j < boardSize; ++j) {
                    int boardIndex = 2 * (i / smallBoardSize) + j / smallBoardSize;
                    int field = smallBoardSize * (i % smallBoardSize) + j % smallBoardSize;
                    if ((player1State[boardIndex] & (1L << field)) != 0) {
                        board.append('1');
                    }
                    else if ((player2State[boardIndex] & (1L << field)) != 0) {
                        board.append('2');
                    }
                    else {
                        board.append('-');
                    }
                }
                board.append('\n');
            }
            return board.toString();
        }
    }

    private static class FastMove {
        private int board;
        private int field;
        private int rotatedBoard;
        private RotateMove.Direction direction;

        public FastMove(int board, int field, int rotatedBoard, RotateMove.Direction direction) {
            this.board = board;
            this.field = field;
            this.rotatedBoard = rotatedBoard;
            this.direction = direction;
        }

        public RotateMove.Direction getDirection() {
            return direction;
        }

        public int getBoard() {
            return board;
        }

        public int getField() {
            return field;
        }

        public int getRotatedBoard() {
            return rotatedBoard;
        }

        public PentagoMove toMove(Color player, int boardSize) {
            int smallBoardSize = boardSize / 2;
            int upperLeftRow = smallBoardSize * (board / 2);
            int upperLeftColumn = smallBoardSize * (board % 2);

            int rotationRow = smallBoardSize * (rotatedBoard / 2);
            int rotationSourceColumn = smallBoardSize * (rotatedBoard % 2);
            int rotationDestinationColumn = rotationSourceColumn + smallBoardSize - 1;
            if (direction == RotateMove.Direction.COUNTERCLOCKWISE) {
                int tmp = rotationSourceColumn;
                rotationSourceColumn = rotationDestinationColumn;
                rotationDestinationColumn = tmp;
            }

            return new PentagoMove(upperLeftColumn + field % smallBoardSize,upperLeftRow + field / smallBoardSize,
                    rotationSourceColumn, rotationRow, rotationDestinationColumn, rotationRow, player);
        }

        @Override
        public String toString() {
            return "FastMove{" +
                    "board=" + board +
                    ", field=" + field +
                    ", rotatedBoard=" + rotatedBoard +
                    ", direction=" + direction +
                    '}';
        }
    }
}
