import java.util.ArrayList;
import java.util.Arrays;

public class PlayerSkeleton {


    private ArrayList<Heuristic> heuristics = new ArrayList<>();

    // update these weights, negative for minimize, positive for maximize.
    // Probably doesn't matter since machine will slowly move it to the correct value
    private double[] weights = {0.5, 1, 0.5, 3};


    PlayerSkeleton() {
        // update these weights, negative for minimize, positive for maximize.
        // Probably doesn't matter since machine will slowly move it to the correct value
        heuristics.add(new MaxHeightHeuristic());
        heuristics.add(new RowsClearedHeuristic());
        heuristics.add(new AvgHeightHeuristic());
        heuristics.add(new HolesHeuristic());

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
        State s = new State();
        new TFrame(s);
        PlayerSkeleton p = new PlayerSkeleton();
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
        PlayerSkeleton ps = new PlayerSkeleton();
        ps.execute();
    }

}


/**
 * Deep copy of State class to apply moves without touching the real {@link State} class
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
}


interface Heuristic {
    double run(StateCopy s);

    double getDerivative(StateCopy bef, StateCopy aft);
}


// MAXIMIZE - RETURN POSITIVE
class RowsClearedHeuristic implements Heuristic {

    public double run(StateCopy s) {
        return s.getRowsCleared();
    }

    public double getDerivative(StateCopy bef, StateCopy aft) {
        return aft.getTotalRowsCleared() - bef.getTotalRowsCleared();
    }
}

// MINIMIZE - RETURN NEGATIVE
class MaxHeightHeuristic implements Heuristic {

    public double run(StateCopy s) {
        return getMaxHeight(s);
    }

    public double getDerivative(StateCopy bef, StateCopy aft) {
        return getMaxHeight(aft);
    }

    private int getMaxHeight(StateCopy s) {
        int[] top = s.getTop();
        int maxHeight = 0;

        for (int height : top) {
            if (maxHeight < height) {
                maxHeight = height;
            }
        }
        return -(maxHeight);
    }
}

// MINIMIZE - RETURN NEGATIVE
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
        return -(heightIncrease / length);
    }

    public double getDerivative(StateCopy bef, StateCopy aft) {
        int[] prevTop = bef.getTop();
        int[] top = aft.getTop();

        int length = top.length;
        double heightIncrease = 0;

        for (int i = 0; i < length; i++) {
            heightIncrease += top[i] - prevTop[i];
        }

        return (heightIncrease / length);
    }
}


// MINIMIZE - RETURN NEGATIVE
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

        return -(numOfHoles);
    }

    public double getDerivative(StateCopy bef, StateCopy aft) {
        return run(aft);
    }
}

class ColumnHeuristic implements Heuristic {
    private int index;

    ColumnHeuristic(int index) {
        this.index = index;
    }

    public double run(StateCopy s) {
        int[] tops = s.getTop();
        return tops[index];
    }

    public double getDerivative(StateCopy bef, StateCopy aft) {
        return run(aft);
    }
}

class RowTransitionsHeuristic implements Heuristic {
    public double run(StateCopy s) {
        int[][] field = s.getField;
        int maxRow = getMaxHeight(s);
        int rowTransitions = 0;
        
        for(int r = 0; r < maxRow; r++) {
            int tmp1 = field[r][0];
            int tmp2 = field[r][1];
            
            for(int c = 1; c < State.COLS; c++) {
                if(((tmp1 || tmp2) == 0) && ((tmp1 || tmp2) != 0)) {
                    rowTransitions++;
                }
                tmp1 = tmp2;
                tmp2 = field[r][c+1];
            }
        }
            
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
