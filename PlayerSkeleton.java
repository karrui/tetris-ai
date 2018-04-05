import java.io.*;
import java.util.*;

public class PlayerSkeleton {

    static final int NUM_HEURISTICS = 6;

    // config booleans
    private static boolean isTraining = true;
    private static boolean isHeadless = true;

    static String TRAINED_WEIGHTS = "weights.txt";

    
    private ArrayList<Heuristic> heuristics = new ArrayList<>();

    // update these weights, negative for minimize, positive for maximize.
    // Probably doesn't matter since machine will slowly move it to the correct value
    private double[] weights;


    PlayerSkeleton() {
        weights = new double[NUM_HEURISTICS];

        if (!isTraining) {
            // read in the trained weights into our weights array
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(TRAINED_WEIGHTS));
                String line;
                int i = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    weights[i++] = Double.parseDouble(line);
                }
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        heuristics.add(new MaxHeightHeuristic());
        heuristics.add(new RowsClearedHeuristic());
        heuristics.add(new AvgHeightHeuristic());
        heuristics.add(new HolesHeuristic());
        heuristics.add(new ColumnTransitionsHeuristic());
        heuristics.add(new AbsoluteDiffHeuristic());

        // column heuristics
//        for (int i = 0; i < State.COLS; i++) {
//            heuristics.add(new ColumnHeuristic());
//        }
    }


    //implement this function to have a working system
    private int pickMove(State s, int[][] legalMoves) {
        int bestMove = 0;
        double maxUtility = Integer.MIN_VALUE;

        for (int i = 0; i < legalMoves.length; i++) {
            StateCopy sCopy = new StateCopy(s);
            sCopy.makeMove(i);
            // ignore the move if it lost
            if (sCopy.hasLost()) {
                continue;
            }
            double currUtility = valueFunction(sCopy);
            if (maxUtility < currUtility) {
                maxUtility = currUtility;
                bestMove = i;
            }
        }
        return bestMove;
    }


    private double valueFunction(StateCopy s) {
        double value = 0;
        int i = 0;
        for (Heuristic heuristic: heuristics) {
            value += weights[i++] * heuristic.run(s);
        }

        return value;
    }

    // This is the real main(), so you can run non-static;
    private void execute() {
        if (!isTraining) {
            State s = new State();
            if (!isHeadless) {
                new TFrame(s);
            }
            PlayerSkeleton p = new PlayerSkeleton();
            while (!s.hasLost()) {
                s.makeMove(p.pickMove(s, s.legalMoves()));
                if (!isHeadless) {
                    s.draw();
                    s.drawNext(0, 0);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("You have completed " + s.getRowsCleared() + " rows.");
        } else {
            PSO swarm = new PSO();
            swarm.run();
        }
    }

    // training method, feels recursive as hell
    public static int train(State s, PlayerSkeleton p) {
        while(!s.hasLost()) {
            s.makeMove(p.pickMove(s, s.legalMoves()));
        }
        return s.getRowsCleared();
    }


    public static void main(String[] args) {
        PlayerSkeleton ps = new PlayerSkeleton();
        ps.execute();
    }

    public void updateWeights(double[] newWeights) {
        weights = ArrayHelper.deepCopy(newWeights);
    }
}

/**
 * =====================================================================================
 * Deep copy of State class to apply moves without touching the real {@link State} class
 * =====================================================================================
 */
@SuppressWarnings("Duplicates")
class StateCopy {


    private static final int COLS = 10;
    private static final int ROWS = 21;
    private static final int N_PIECES = 7;


    private boolean lost;

    //current turn
    private int turn;
    private int cleared;    // this variable actually keeps track of all the rows cleared so far in game

    // this variable does not exist in State, this is for the maximize rows cleared heuristic
    private int rowsCleared = 0;

    //each square in the grid - int means empty - other values mean the turn it was placed
    private int[][] field;
    //top row+1 of each column
    //0 means empty
    private int[] top;

    // the previous top[] to calculate average height increase
    private int[] previousTop;


    //number of next piece
    private int nextPiece;


    //all legal moves - first index is piece type - then a list of 2-length arrays
    private static int[][][] legalMoves = new int[N_PIECES][][];

    //indices for legalMoves
    private static final int ORIENT = 0;
    private static final int SLOT = 1;

    //possible orientations for a given piece type
    private static int[] pOrients;

    //the next several arrays define the piece vocabulary in detail
    //width of the pieces [piece ID][orientation]
    private static int[][] pWidth;

    //height of the pieces [piece ID][orientation]
    private static int[][] pHeight;

    private static int[][][] pBottom;
    private static int[][][] pTop;


    StateCopy(State toCopy) {
        this.lost = toCopy.hasLost();
        this.turn = toCopy.getTurnNumber();
        this.cleared = toCopy.getRowsCleared();

        this.field = ArrayHelper.deepCopy(toCopy.getField());
        this.top = ArrayHelper.deepCopy(toCopy.getTop());
        this.previousTop = ArrayHelper.deepCopy(toCopy.getTop());   // nothing will change this once its init

        this.nextPiece = toCopy.getNextPiece();

        pOrients = State.getpOrients();
        pWidth = State.getpWidth();
        pHeight = State.getpHeight();
        pBottom = State.getpBottom();
        pTop = State.getpTop();

        initLegalMoves();

    }

    //initialize legalMoves
    private void initLegalMoves() {
        //for each piece type
        for(int i = 0; i < N_PIECES; i++) {
            //figure number of legal moves
            int n = 0;
            for(int j = 0; j < pOrients[i]; j++) {
                //number of locations in this orientation
                n += COLS+1-pWidth[i][j];
            }
            //allocate space
            legalMoves[i] = new int[n][2];
            //for each orientation
            n = 0;
            for(int j = 0; j < pOrients[i]; j++) {
                //for each slot
                for(int k = 0; k < COLS+1-pWidth[i][j];k++) {
                    legalMoves[i][n][ORIENT] = j;
                    legalMoves[i][n][SLOT] = k;
                    n++;
                }
            }
        }

    }

    public int[][] getField() {
        return field;
    }

    public int[] getTop() {
        return top;
    }

    public int[] getPreviousTop() {
        return previousTop;
    }

    public static int[] getpOrients() {
        return pOrients;
    }

    public static int[][] getpWidth() {
        return pWidth;
    }

    public static int[][] getpHeight() {
        return pHeight;
    }

    public static int[][][] getpBottom() {
        return pBottom;
    }

    public static int[][][] getpTop() {
        return pTop;
    }

    public static final int getCols() {
        return COLS;
    }

    public static final int getRows() {
        return ROWS;
    }

    public int getNextPiece() {
        return nextPiece;
    }

    public boolean hasLost() {
        return lost;
    }

    // Renamed to this from State to better explain what this is
    public int getTotalRowsCleared() {
        return cleared;
    }

    public int getRowsCleared() {
        return rowsCleared;
    }

    public int getTurnNumber() {
        return turn;
    }

    //gives legal moves for
    public int[][] legalMoves() {
        return legalMoves[nextPiece];
    }

    //make a move based on the move index - its order in the legalMoves list
    public void makeMove(int move) {
        makeMove(legalMoves[nextPiece][move]);
    }

    //make a move based on an array of orient and slot
    public void makeMove(int[] move) {
        makeMove(move[ORIENT],move[SLOT]);
    }

    //returns false if you lose - true otherwise
    public boolean makeMove(int orient, int slot) {
        turn++;
        //height if the first column makes contact
        int height = top[slot]-pBottom[nextPiece][orient][0];
        //for each column beyond the first in the piece
        for(int c = 1; c < pWidth[nextPiece][orient];c++) {
            height = Math.max(height,top[slot+c]-pBottom[nextPiece][orient][c]);
        }

        //check if game ended
        if(height+pHeight[nextPiece][orient] >= ROWS) {
            lost = true;
            return false;
        }


        //for each column in the piece - fill in the appropriate blocks
        for(int i = 0; i < pWidth[nextPiece][orient]; i++) {

            //from bottom to top of brick
            for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
                field[h][i+slot] = turn;
            }
        }

        //adjust top
        for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
            top[slot+c]=height+pTop[nextPiece][orient][c];
        }

        //check for full rows - starting at the top
        for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
            //check all columns in the row
            boolean full = true;
            for (int c = 0; c < COLS; c++) {
                if (field[r][c] == 0) {
                    full = false;
                    break;
                }
            }
            //if the row was full - remove it and slide above stuff down
            if (full) {
                rowsCleared++;
                cleared++;
                //for each column
                for (int c = 0; c < COLS; c++) {

                    //slide down all bricks
                    for (int i = r; i < top[c]; i++) {
                        field[i][c] = field[i + 1][c];
                    }
                    //lower the top
                    top[c]--;
                    while (top[c] >= 1 && field[top[c] - 1][c] == 0) top[c]--;
                }
            }
        }
        return true;
    }

}


/**
 * =============================
 * Utility class to clone arrays
 * =============================
 */
class ArrayHelper {
    // Helper method to clone 2d int array instead of reference
    static int[][] deepCopy(int[][] src) {
        int length = src.length;
        int[][] dest = new int[length][src[0].length];
        for (int i = 0; i < length; i++) {
            System.arraycopy(src[i], 0, dest[i], 0, src[i].length);
        }
        return dest;
    }

    // Overloaded helper method to clone 1d int array instead of reference
    static int[] deepCopy(int[] src) {
        return src.clone();
    }

    // Helper method to clone 2d int array instead of reference
    static double[][] deepCopy(double[][] src) {
        int length = src.length;
        double[][] dest = new double[length][src[0].length];
        for (int i = 0; i < length; i++) {
            System.arraycopy(src[i], 0, dest[i], 0, src[i].length);
        }
        return dest;
    }

    // Overloaded helper method to clone 1d int array instead of reference
    static double[] deepCopy(double[] src) {
        return src.clone();
    }
}

/**
 * ==========================================================
 * Interface to encapsulate heuristics used for the Tetris AI
 * ==========================================================
 */
interface Heuristic {
    double run(StateCopy s);
}


/**
 * Returns the number of rows cleared by the piece
 */
class RowsClearedHeuristic implements Heuristic {

    public double run(StateCopy s) {
        return s.getRowsCleared();
    }
}

/**
 * Returns the maximum height of the board when the piece is placed
 */
class MaxHeightHeuristic implements Heuristic {

    public double run(StateCopy s) {
        return getMaxHeight(s);
    }

    private int getMaxHeight(StateCopy s) {
        int[] top = s.getTop();
        int maxHeight = 0;

        for (int height : top) {
            if (maxHeight < height) {
                maxHeight = height;
            }
        }
        return maxHeight;
    }
}

/**
 * Returns the average height of the board when the piece is placed
 * Average height is calculated by the total sum of the column heights divided by number of columns
 */
class AvgHeightHeuristic implements  Heuristic {

    public double run(StateCopy s) {
        int[] prevTop = s.getPreviousTop();
        int[] top = s.getTop();

        int length = top.length;
        double heightIncrease = 0;

        for (int i = 0; i < length; i++) {
            heightIncrease += top[i] - prevTop[i];
        }
        // System.out.println("weight is: " + weight);
        return heightIncrease / length;
    }
}


/**
 * Returns the number of holes in the board when the piece is placed
 */
class HolesHeuristic implements Heuristic {
    public double run(StateCopy s) {
        int[][] field = s.getField();
        int[] top = s.getTop();
        
        int numOfHoles = 0;
        
        for(int c = 0; c < State.COLS; c++) {
            for(int r = 0; r < top[c] - 2; r++) {
                if(field[r][c] == 0) {
                    numOfHoles++;
                }
            }
        }

        return numOfHoles;
    }
}

/**
 * Returns the number of column transitions when the piece is placed
 * A column transition occurs when an empty cell is adjacent to a filled cell on the same column and vice versa.
 */
class ColumnTransitionsHeuristic implements  Heuristic {
    public double run(StateCopy s) {
        int[][] field = s.getField();
        int[] top = s.getTop();

        int colTransitions = 0;

        for (int c = 0; c < State.COLS; c++) {
            boolean priorCellFilled = false;
            if (field[0][c] != 0) {
                priorCellFilled = true;
            }
            for (int r = 1; r < top[c]; r++) {
                boolean currCellFilled = false;
                if (field[r][c] != 0) {
                    currCellFilled = true;
                }

                if (priorCellFilled != currCellFilled) {
                    colTransitions++;
                }

                priorCellFilled = currCellFilled;
            }
        }

        return colTransitions;
    }
}

/**
 * Returns the absolute height different amongst all columns
 * This heuristic aims to reduce the overall "bumpiness" of the top layer
 */
class AbsoluteDiffHeuristic implements Heuristic {

    public double run(StateCopy s) {
        //implement heuristics
        int absDiff = 0;
        int[] top = s.getTop();

        for(int i = 0; i < top.length - 1; i++){
            absDiff += Math.abs(top[i] - top[i + 1]);
        }

        return absDiff;
    }
}

/**
 * =============================================================================================================
 * This class contains the particle swarm optimizer algorithm to help us get the best weights for the heuristics
 * Not multithreaded yet
 * =============================================================================================================
 */
class PSO {
    static int UPPERBOUND_VELOCITY = 5;
    static int LOWERBOUND_VELOCITY = -5;
    static int RANGE_VELOCITY = UPPERBOUND_VELOCITY - LOWERBOUND_VELOCITY;

    static int UPPERBOUND_POSITION = 10;
    static int LOWERBOUND_POSITION = -10;
    static int RANGE_POSITION = UPPERBOUND_POSITION - LOWERBOUND_POSITION;

    static int NUM_HEURISTICS = PlayerSkeleton.NUM_HEURISTICS;
    static int NUM_PARTICLES = 10;  // general rule of thumb seems to be n < N < 2n, where n = numHeuristics
    static int NUM_ITERATIONS = 100;

    boolean hasWeightsFromFile = false;

    private static String LOG_FILE = "./t_weights_log.txt";
    private static String TRAINED_WEIGHTS = "./trained_weights.txt";

    private static Particle[] particles;

    private int globalBest;
    private double[] globalBestPositions = new double[NUM_HEURISTICS];

    public PSO() {

        File f = new File(TRAINED_WEIGHTS);
        if(f.exists() && !f.isDirectory()) {
            try {
                hasWeightsFromFile = true;
                BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
                String line;
                int i = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    globalBestPositions[i++] = Double.parseDouble(line);
                }
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        globalBest = 0;
        createSwarm();
    }

    private void createSwarm() {
        Random random = new Random();
        particles = new Particle[NUM_PARTICLES];
        for (int i = 0; i < NUM_PARTICLES; i++) {
            double[] fitness = new double[NUM_HEURISTICS];
            double[] velocity = new double[NUM_HEURISTICS];
            for (int j = 0; j < NUM_HEURISTICS; j++) {
                // generate random fitness if no current weights
                if (hasWeightsFromFile) {
                    fitness[j] = globalBestPositions[j];
                } else {
                    fitness[j] = random.nextDouble() * RANGE_POSITION;
                }
                // generate random velocity
                velocity[j] = random.nextDouble() * RANGE_VELOCITY;
                if (random.nextDouble() > 0.5) {    // velocity has 50/50 of being positive or negative
                    velocity[j] *= -1;
                }
            }
            particles[i] = new Particle(fitness, velocity);
        }
    }

    // main method
    public void run() {
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            for (Particle particle : particles) {
                int score = evaluate(particle);
                particle.updatePersonalBest(score);
                if (score > globalBest) {
                    globalBest = score;
                    globalBestPositions = ArrayHelper.deepCopy(particle.getPosition());
                }

                particle.updateVelocity(globalBestPositions, RANGE_VELOCITY);
                particle.updatePosition(UPPERBOUND_POSITION, LOWERBOUND_POSITION);
            }

            // Log details
            System.out.printf("Iteration %d globalBest: %d\n", i, globalBest);
            // Write to log file
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(LOG_FILE, true));
                bufferedWriter.append("Iteration ").append(String.valueOf(i)).append(", Score: ").append(String.valueOf(globalBest)).append("\n");
                bufferedWriter.append("======== Scores ========= \n");
                for (double globalBestPosition : globalBestPositions) {
                    bufferedWriter.append(String.valueOf(globalBestPosition)).append(" ");
                }
                bufferedWriter.append("\n");
                bufferedWriter.flush();
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Best score for this training session here
        System.out.println("Best result: " + globalBest);
        for (double globalBestPosition : globalBestPositions) {
            System.out.print(globalBestPosition + " ");
        }
        System.out.println();

        // Write to trained_weights.txt
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(TRAINED_WEIGHTS));
            for (double globalBestPosition : globalBestPositions) {
                bufferedWriter.append(String.valueOf(globalBestPosition)).append("\n");
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private int evaluate(Particle particle) {
        PlayerSkeleton trainPlayerSkeleton = new PlayerSkeleton();
        State state = new State();

        trainPlayerSkeleton.updateWeights(particle.getPosition());

        return PlayerSkeleton.train(state, trainPlayerSkeleton);    // this will return rows cleared
    }

}


/**
 * Particle for the {@link PSO} class
 */
class Particle {

    // Using values stated to be decent in
    // https://pdfs.semanticscholar.org/94b5/2262c526dbe38919d53b4c15c81130a12c3e.pdf
    //
    static double INERTIA = 0.6571; //0.7298;
    static double COGNITIVE_PARAMETER = 1.6319; // 1.49618;
    static double SOCIAL_PARAMETER = 0.6239; //1.49618;

    double[] position;
    double[] velocity;

    double personalBest;
    double[] personalBestPositions;

    public Particle(double[] position, double[] velocity) {
        personalBest = 0;
        this.position = position;
        this.velocity = velocity;
        personalBestPositions = new double[position.length];
    }

    // updates personalBest and personBestPosition if given score is better than current best
    public void updatePersonalBest(double given) {
        if (given > personalBest) {
            personalBest = given;
            personalBestPositions = ArrayHelper.deepCopy(position);
        }
    }

    public void updateVelocity(double[] globalBestPositions, int range) {
        Random random = new Random();
        for (int i = 0; i < velocity.length; i++) {
            velocity[i] = INERTIA * velocity[i]
                        + COGNITIVE_PARAMETER * (personalBestPositions[i] - position[i]) * random.nextDouble()
                        + SOCIAL_PARAMETER * (globalBestPositions[i] - position[i]) * random.nextDouble();
            // check if velocity out of range
            if (velocity[i] > range) {
                velocity[i] = range;
            } else if (velocity[i] < -range) {
                velocity[i] = -range;
            }
        }
    }

    public void updatePosition(int upperBound, int lowerBound) {
        for (int i = 0; i < position.length; i++) {
            position[i] += velocity[i];
            // check if position out of range
            if (position[i] > upperBound) {
                position[i] = upperBound;
            } else if (position[i] < lowerBound) {
                position[i] = lowerBound;
            }
        }
    }

    public double[] getPosition() {
        return position;
    }
}