import java.util.ArrayList;

public class TrainPlayerSkeleton {
    private static int HEIGHT_HEURISTIC_INDEX = 0;
    private static int ROWS_CLEARED_HEURISTIC_INDEX = 1;
    private static int AVG_HEIGHT_INCREASE_HEURISTIC_INDEX = 2;

    private ArrayList<Heuristic> heuristics = new ArrayList<>();

    TrainPlayerSkeleton() {
        // Machine will learn and update these weights via TD algorithm, negative for minimize, positive for maximize.
        // Weights are arbitrarily initialised to 0.0.
        double[] weights = {0.0, 0.0, 0.0};
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
        for (Heuristic heuristic: heuristics) {
            utility += (heuristic.run(s));
        }

        return utility;
    }

    // This is the real main(), so you can run non-static;
    private void execute() {
        State s = new State();
        new TFrame(s);
        TrainPlayerSkeleton p = new TrainPlayerSkeleton();
        while (!s.hasLost()) {
            s.makeMove(p.pickMove(s, s.legalMoves()));
            s.draw();
            s.drawNext(0, 0);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("You have completed " + s.getRowsCleared() + " rows.");
    }


    public static void main(String[] args) {
        TrainPlayerSkeleton ps = new TrainPlayerSkeleton();
        ps.execute();
    }

}