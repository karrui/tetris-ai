import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Random;

public class PlayerSkeleton {

    static final int NUM_FEATURES = 8;
    static final int NUM_ITERATIONS = 10;

    private static int currBestScore;
    private static int globalBestScore;

    // config booleans
    private static boolean isMetaOptimizing = false;
    private static boolean isTraining = false;
    private static boolean isHeadless = false;

//    private static String TRAINED_WEIGHTS = "trained_weights.txt";
    private static String LOG_FILE = "./mo_parameters_log.txt";
    
    private ArrayList<Feature> features = new ArrayList<>();

    // weights for each feature
    private double[] weights = {-5.194814083947793,
                                5.53478180043909,
                                -2.1920993360428604,
                                -5.9838083427614395,
                                -12.880189747449215,
                                -1.3227027198368309,
                                -3.7823922425956837,
                                -2.2176395274310137};

    // beta is represented arbitrarily with 1/3 here. We can change beta to see how it responds
    private static double decreaseFactor = Math.pow(2.0, -1.0/3);

    // parameters in PSO, we initialise here for Meta Optimization
    private static int swarm;
    private static double inertia;
    private static double socialParameter;
    private static double cognitiveParameter;


    // Constructor, add features
    PlayerSkeleton() {
        features.add(new MaxHeightFeature());
        features.add(new RowsClearedFeature());
        features.add(new AvgHeightFeature());
        features.add(new HolesFeature());
        features.add(new ColumnTransitionsFeature());
        features.add(new AbsoluteDiffFeature());
        features.add(new RowTransitionsFeature());
        features.add(new WellSumFeature());
    }


    //implement this function to have a working system
    // The best move is chosen if the state s has the highest utility
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

    // Our main utility function F(s)
    // Returns the utility of the State s
    private double valueFunction(StateCopy s) {
        double value = 0;
        int i = 0;
        for (Feature feature : features) {
            value += weights[i++] * feature.run(s);
        }

        return value;
    }

    // This is the real main(), so you can run non-static;
    private void execute() {
        if (isMetaOptimizing){
           runMetaOptimzation();
        } else if (!isTraining) {
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
                        Thread.sleep(1);
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

    // training method for our swarm particles
    // Returns rows cleared by using particle's position as weights for utility function
    static int train(State s, PlayerSkeleton p) {
        while(!s.hasLost()) {
            s.makeMove(p.pickMove(s, s.legalMoves()));
        }
        return s.getRowsCleared();
    }

    private void runMetaOptimzation() {
        // bounds for the hypercube d
        int swarmBound;
        double inertiaBound;
        double socialParameterBound;
        double cognitiveParameterBound;
        Random r = new Random();

        // initialise x to a random solution in the search space.
        // more specifically, we initialise parameters in PSO, namely swarm size, inertia, social and cognitive parameters

        // swarm in [0, 200]
        swarm = r.nextInt(199) + 1;
        // for generating double values within a range: randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble()
        // inertia in [-2, 2]
        inertia = -2.0 + 4.0 * r.nextDouble();
        // social and cognitive parameters both in [-4, 4]
        socialParameter = -4.0 + 8.0 * r.nextDouble();
        cognitiveParameter = -4.0 + 8.0 * r.nextDouble();

        // set the initial sampling range d to cover the entire search space
        swarmBound = 200;
        inertiaBound = 2.0;
        socialParameterBound = 4.0;
        cognitiveParameterBound = 4.0;

        // we run PSO once to set the globalBest value
        PSO firstPso = new PSO(swarm, inertia, socialParameter, cognitiveParameter);
        globalBestScore = firstPso.run();

        // until a termination criterion is met, repeat the following
        for (int i = 0; i < NUM_ITERATIONS; i++) {

            // pick a random vector a~U(-d,d)
            int randomSwarm = -1 * swarmBound + r.nextInt(swarmBound * 2 + 1);
            double randomInertia = -1.0 * inertiaBound + (inertiaBound * 2) * r.nextDouble();
            double randomSocialParameter = -1.0 * socialParameterBound + (socialParameterBound * 2)
                    * r.nextDouble();
            double randomCognitiveParameter = -1.0 * cognitiveParameterBound + (cognitiveParameterBound * 2)
                    * r.nextDouble();

            // add this to the current solution x to create the new potential solution
            int swarmHere = Math.abs(swarm + randomSwarm);
            double inertiaHere = inertia + randomInertia;
            double socialParameterHere = socialParameter + randomSocialParameter;
            double cognitiveParameterHere = cognitiveParameter + randomCognitiveParameter;

            PSO pso = new PSO(swarmHere, inertiaHere, socialParameterHere, cognitiveParameterHere);
            currBestScore = pso.run();

            // if we get a more optimal value (i.e., more rows cleared
            if (currBestScore > globalBestScore) {
                globalBestScore = currBestScore;
                swarm = swarmHere;
                inertia = inertiaHere;
                socialParameter = socialParameterHere;
                cognitiveParameter = cognitiveParameterHere;
            } else { // decrease the search range by multiplication with the factor q (also known as decreaseFactor)
                swarmBound = (int) Math.ceil(swarmBound * decreaseFactor); // we get the ceiling
                inertiaBound = inertiaBound * decreaseFactor;
                socialParameterBound = socialParameterBound * decreaseFactor;
                cognitiveParameterBound = cognitiveParameterBound * decreaseFactor;
            }

            System.out.println("globalBest is now: " + globalBestScore);
            writeToLogFile(i);
        }
    }

    void writeToLogFile(int iteration) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(LOG_FILE, true));
            bufferedWriter.append("Iteration ").append(String.valueOf(iteration)).append(", Score: ")
                    .append(String.valueOf(globalBestScore)).append("\n");
            bufferedWriter.append("======== Parameters ========= \n");
            bufferedWriter.append("Swarm size: ").append(String.valueOf(swarm))
                    .append(" inertia: ").append(String.valueOf(inertia))
                    .append(" social: ").append(String.valueOf(socialParameter))
                    .append(" cognitive: ").append(String.valueOf(cognitiveParameter)).append("\n");
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length > 1 && args[0].equals("-t")) {
            isTraining = true;
        }
        PlayerSkeleton ps = new PlayerSkeleton();
        ps.execute();
    }

    // update the current weights of the PlayerSkeleton to the Particle's positions
    void updateWeights(double[] newWeights) {
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


    //all legal moves - first index is piece type - then a futureList of 2-length arrays
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

    int[][] getField() {
        return field;
    }

    int[] getTop() {
        return top;
    }

    int[] getPreviousTop() {
        return previousTop;
    }

    boolean hasLost() {
        return lost;
    }

    // Renamed to this from State to better explain what this is
    public int getTotalRowsCleared() {
        return cleared;
    }

    int getRowsCleared() {
        return rowsCleared;
    }

    public int getTurnNumber() {
        return turn;
    }

    //gives legal moves for
    int[][] legalMoves() {
        return legalMoves[nextPiece];
    }

    //make a move based on the move index - its order in the legalMoves futureList
    void makeMove(int move) {
        makeMove(legalMoves[nextPiece][move]);
    }

    //make a move based on an array of orient and slot
    private void makeMove(int[] move) {
        makeMove(move[ORIENT],move[SLOT]);
    }

    //returns false if you lose - true otherwise
    private boolean makeMove(int orient, int slot) {
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
 * Interface to encapsulate features used for the Tetris AI
 * ==========================================================
 */
interface Feature {
    double run(StateCopy s);
}

/**
 * Returns the number of rows cleared by the piece
 */
class RowsClearedFeature implements Feature {

    public double run(StateCopy s) {
        return s.getRowsCleared();
    }
}

/**
 * Returns the maximum height of the board when the piece is placed
 */
class MaxHeightFeature implements Feature {

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
class AvgHeightFeature implements Feature {

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
class HolesFeature implements Feature {
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
 * Borders count as filled cell
 */
class ColumnTransitionsFeature implements Feature {
    public double run(StateCopy s) {
        int[][] field = s.getField();

        int colTransitions = 0;

        for (int c = 0; c < State.COLS; c++) {
            boolean priorCellFilled = true;
            for (int r = 0; r < State.ROWS - 1; r++) {
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
 * Returns the number of row transitions when the piece is placed
 * A row transition occurs when an empty cell is adjacent to a filled cell on the same row and vice versa.
 * Borders count as a filled cell
 */
class RowTransitionsFeature implements Feature {
    public double run(StateCopy s) {
        int[][] field = s.getField();
        int rowTransitions = 0;

        for (int r = 0; r < State.ROWS; r++) {
            boolean priorCellFilled = true;
            for (int c = 0; c < State.COLS; c++) {
                boolean currCellFilled = false;
                if (field[r][c] != 0) {
                    currCellFilled = true;
                }

                if (priorCellFilled != currCellFilled) {
                    rowTransitions++;
                }

                priorCellFilled = currCellFilled;
            }
            // unfilled Cell next to border
            if (!priorCellFilled) {
                rowTransitions++;
            }
        }

        return rowTransitions;
    }
}

/**
 * Returns the absolute height different amongst all columns
 * This feature aims to reduce the overall "bumpiness" of the top layer
 */
class AbsoluteDiffFeature implements Feature {

    public double run(StateCopy s) {
        //implement features
        int absDiff = 0;
        int[] top = s.getTop();

        for(int i = 0; i < top.length - 1; i++){
            absDiff += Math.abs(top[i] - top[i + 1]);
        }

        return absDiff;
    }
}

/**
 * Returns the sum of the wells when the piece is placed
 * A well is a sequence of empty cells above the top piece in a column where it is surrounded by cells or walls
 */
class WellSumFeature implements Feature {

    public double run(StateCopy s) {
        int[][] field = s.getField();
        int wellSum = 0;


        // check column wells from 2nd column
        for (int c = 1; c < State.COLS - 1; c++) {
            for (int r = 0; r < State.ROWS; r++) {
                // Current cell is empty,  but left and right cells are filled, meaning well
                if (field[r][c] == 0 && field[r][c - 1] != 0 && field[r][c + 1] != 0) {
                    wellSum++;
                    // check depth of well
                    for (int depth = r - 1; depth >= 0; depth--) {
                        if (field[depth][c] != 0) {
                            break;
                        }
                        wellSum++;
                    }
                }
            }
        }

        // check left and right boundary wells
        for (int r = 0; r < State.ROWS; r++) {
            // left boundary: cell at first column is empty, but column 1 is filled
            if (field[r][0] == 0 && field[r][1] != 0) {
                wellSum++;
                // check depth of well
                for (int depth = r - 1; depth >= 0; depth--) {
                    if (field[depth][0] != 0) {
                        break;
                    }
                    wellSum++;
                }
            }

            // right boundary: cell at last column empty, but second last column is filled
            int rightBoundaryIndex = State.COLS - 1;
            if (field[r][rightBoundaryIndex] == 0 && field[r][rightBoundaryIndex - 1] != 0) {
                wellSum++;
                // check depth of well
                for (int depth = r - 1; depth >= 0; depth--) {
                    if (field[depth][rightBoundaryIndex] != 0) {
                        break;
                    }
                    wellSum++;
                }
            }
        }
        return wellSum;
    }
}

/**
 * =============================================================================================================
 * This class contains the particle swarm optimizer algorithm to help us get the best weights for the features
 * =============================================================================================================
 */
class PSO {
    // Bounds the velocity of the Particles in the swarm
    private static int UPPERBOUND_VELOCITY = 5;
    private static int LOWERBOUND_VELOCITY = -5;
    private static int RANGE_VELOCITY = UPPERBOUND_VELOCITY - LOWERBOUND_VELOCITY;

    // Bounds the search space of the swarm
    // Sidenote: Might have been a bad idea to limit it to just +-10, should have made it bigger
    private static int UPPERBOUND_POSITION = 10;
    private static int LOWERBOUND_POSITION = -10;
    private static int RANGE_POSITION = UPPERBOUND_POSITION - LOWERBOUND_POSITION;

    private static int NUM_FEATURES = PlayerSkeleton.NUM_FEATURES;
    private static int NUM_PARTICLES = 161;  // obtained from Meta Optimization

    // Make each particle play NUM_GAMES games to get average score
    static int NUM_GAMES = 3;

    // Number of iterations each particle moves
    private static int NUM_ITERATIONS = 1000;

    // Maximize number of threads to use for parallelization
    private static int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    private boolean hasWeightsFromFile = false;

    private static String LOG_FILE = "./t_weights_log.txt";
    private static String TRAINED_WEIGHTS = "./trained_weights.txt";

    private static Particle[] particles;

    private int globalBest;
    private double[] globalBestPositions = new double[NUM_FEATURES];

    private ExecutorService executor;

    PSO() {

        File file = new File(TRAINED_WEIGHTS);
        if(file.exists() && !file.isDirectory()) {
            readWeightsFromFile(file);
        }

        executor = Executors.newFixedThreadPool(NUM_THREADS);

        globalBest = 0;
        createSwarm();
    }

    // Alternate constructor for meta optimization to set the parameters
    PSO(int numParticles, double inertia, double socialParameter, double cognitiveParameter) {

        NUM_PARTICLES = numParticles;
        Particle.INERTIA = inertia;
        Particle.SOCIAL_PARAMETER = socialParameter;
        Particle.COGNITIVE_PARAMETER = cognitiveParameter;
        System.out.println("NUM_PARTICLES, INERTA, PARAMETERS are: " + NUM_PARTICLES + " " + Particle.INERTIA + " "
            + Particle.COGNITIVE_PARAMETER + " " + Particle.SOCIAL_PARAMETER);

        File file = new File(TRAINED_WEIGHTS);
        if(file.exists() && !file.isDirectory()) {
            readWeightsFromFile(file);
        }

        executor = Executors.newFixedThreadPool(NUM_THREADS);

        globalBest = 0;
        createSwarm();
    }

    // Initiate the swarm by creating random positions and velocities
    private void createSwarm() {
        Random random = new Random();
        particles = new Particle[NUM_PARTICLES];
        for (int i = 0; i < NUM_PARTICLES; i++) {
            double[] fitness = new double[NUM_FEATURES];
            double[] velocity = new double[NUM_FEATURES];
            for (int j = 0; j < NUM_FEATURES; j++) {
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
    public int run() {
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            // Run all Particles and make them play their own game in their own thread
            int[] scoreForAll = playGamesAndReturnScores();

            int k = 0;
            for (Particle particle : particles) {
                // retrieve score from Futures
                int score = scoreForAll[k++];
                particle.updatePersonalBest(score);

                // Update global best score and positions if particle scored higher
                if (score > globalBest) {
                    globalBest = score;
                    globalBestPositions = ArrayHelper.deepCopy(particle.getPosition());
                    writeBestWeightsToFile();
                }

                // Get ready for next iteration
                particle.updateVelocity(globalBestPositions, UPPERBOUND_VELOCITY, LOWERBOUND_VELOCITY);
                particle.updatePosition(UPPERBOUND_POSITION, LOWERBOUND_POSITION);
            }

            // Log details
            System.out.printf("Iteration %d globalBest: %d\n", i, globalBest);
            // Write to log file
            writeToLogFile(i);
        }

        // Best score for this training session here
        System.out.println("Best result: " + globalBest);
        for (double globalBestPosition : globalBestPositions) {
            System.out.print(globalBestPosition + " ");
        }
        System.out.println();

        // Write to trained_weights.txt
        writeBestWeightsToFile();

        // shutdown executor before closing app
        executor.shutdown();

        return globalBest;
    }

    /**
     * Method that creates a thread for each Particle to play game
     * @return average scores of game played
     */
    private int[] playGamesAndReturnScores() {
        List<Future<Integer>> futureList = new ArrayList<>();
        int[] scoreForAll = new int[NUM_PARTICLES];
        for (Particle particle : particles) {
            Future<Integer> future = executor.submit(new CallableTrainer(particle));
            futureList.add(future);
        }

        for (int j = 0; j < futureList.size(); j++) {
            Future<Integer> future = futureList.get(j);
            try {
                scoreForAll[j] += future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return scoreForAll;
    }

    private void readWeightsFromFile(File f) {
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

    private void writeBestWeightsToFile() {
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

    private void writeToLogFile(int iteration) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(LOG_FILE, true));
            bufferedWriter.append("Iteration ").append(String.valueOf(iteration)).append(", Score: ").append(String.valueOf(globalBest)).append("\n");
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
}

/**
 * Trainer class for {@link PSO}.
 * Plays a full game NUM_GAME times for each particle and returns the average rows cleared
 */
class CallableTrainer implements Callable<Integer> {
    private Particle particle;

    CallableTrainer(Particle particle) {
        this.particle = particle;
    }

    public Integer call() {
        // System.out.println("I think multi-threading is happening"); // check

        // Run the simulation NUM_GAME times per Particle and get average to get best positions
        int results = 0;
        for (int gameNum = 0; gameNum < PSO.NUM_GAMES; gameNum++) {
            PlayerSkeleton trainPlayerSkeleton = new PlayerSkeleton();
            State state = new State();

            trainPlayerSkeleton.updateWeights(particle.getPosition());
            results += PlayerSkeleton.train(state, trainPlayerSkeleton);    // this will return rows cleared
        }

        return results / PSO.NUM_GAMES;
    }
}


/**
 * Particle for the {@link PSO} class
 */
class Particle {
    // Using values obtained from Meta Optimization
    public static double INERTIA = 0.6345602753864532;
    public static double COGNITIVE_PARAMETER = 1.925758898746099;
    public static double SOCIAL_PARAMETER = 2.642741397865409;

    private double[] position;
    private double[] velocity;

    private double personalBest;
    private double[] personalBestPositions;

    Particle(double[] position, double[] velocity) {
        personalBest = 0;
        this.position = position;
        this.velocity = velocity;
        personalBestPositions = new double[position.length];
    }

    // updates personalBest and personBestPosition if given score is better than current best
    void updatePersonalBest(double given) {
        if (given > personalBest) {
            personalBest = given;
            personalBestPositions = ArrayHelper.deepCopy(position);
        }
    }

    void updateVelocity(double[] globalBestPositions, int upperBound, int lowerBound) {
        Random random = new Random();
        for (int i = 0; i < velocity.length; i++) {
            velocity[i] = INERTIA * velocity[i]
                        + COGNITIVE_PARAMETER * (personalBestPositions[i] - position[i]) * random.nextDouble()
                        + SOCIAL_PARAMETER * (globalBestPositions[i] - position[i]) * random.nextDouble();
            // check if velocity out of range
            if (velocity[i] > upperBound) {
                velocity[i] = upperBound;
            } else if (velocity[i] < lowerBound) {
                velocity[i] = lowerBound;
            }
        }
    }

    void updatePosition(int upperBound, int lowerBound) {
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

    double[] getPosition() {
        return position;
    }
}