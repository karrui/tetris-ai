import java.util.ArrayList;

public class TrainPlayerSkeleton {
    private static int HEIGHT_HEURISTIC_INDEX = 0;
    private static int ROWS_CLEARED_HEURISTIC_INDEX = 1;
    private static int AVG_HEIGHT_INCREASE_HEURISTIC_INDEX = 2;

    private ArrayList<Heuristic> heuristics = new ArrayList<>();

    // Machine will learn and update these weights via TD algorithm.
    // Weights are arbitrarily initialised to 0.0, negative for minimize, positive for maximize.
    private double[] weights = {0.0000001, 0.0000001, 0.0000001};

    TrainPlayerSkeleton() {
        heuristics.add(new AvgHeightHeuristic(weights[AVG_HEIGHT_INCREASE_HEURISTIC_INDEX]));
        heuristics.add(new MaxHeightHeuristic(weights[HEIGHT_HEURISTIC_INDEX]));
        heuristics.add(new RowsClearedHeuristic(weights[ROWS_CLEARED_HEURISTIC_INDEX]));
    }


    //implement this function to have a working system
    private int pickMove(State s, int[][] legalMoves) {
        int bestMove = 0;
        double maxUtility = Integer.MIN_VALUE;

        for (int i = 0; i < legalMoves.length; i++) {
            StateCopy sCopy = new StateCopy(s);
            sCopy.makeMove(i);

            double currUtility = sCopy.getRowsCleared() + valueFunction(sCopy);
            if (maxUtility < currUtility) {
                maxUtility = currUtility;
                bestMove = i;
            }
        }
        return bestMove;
    }


    private double valueFunction(StateCopy s) {
        double utility = 0;
        int i = 0; // for weights array
        for (Heuristic heuristic: heuristics) {
            utility += (heuristic.run(s));
        }

        return utility;
    }

    // This is the real main(), so you can run non-static;
    private void execute() {
        TrainPlayerSkeleton p = new TrainPlayerSkeleton();
        for (int i = 0; i < 100; i ++) {
            State s = new State();
            while (!s.hasLost()) {
                StateCopy befMove = new StateCopy(s);
                s.makeMove(p.pickMove(s, s.legalMoves()));
                StateCopy aftMove = new StateCopy(s);

                /**
                 * update weights
                 * Note that alpha, the step cost, is 0.0001. We pick a small value to prevent falling into local min/max.
                 * Discount factor is represented by 1. Since we are only looking ahead 1 move, we leave discount factor as 1.
                 */
                int j = 0;
                for (Heuristic heuristic: heuristics) {
                    weights[j] += (0.0001 * (aftMove.getRowsCleared() + 1.0 * valueFunction(aftMove) - valueFunction(befMove))
                            * heuristic.run(befMove));
                    j++;

                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("You have completed " + s.getRowsCleared() + " rows.");
        }
        System.out.println(weights[0] + " " + weights[1] + " " + weights[2]);
    }


    public static void main(String[] args) {
        TrainPlayerSkeleton ps = new TrainPlayerSkeleton();
        ps.execute();
    }

}