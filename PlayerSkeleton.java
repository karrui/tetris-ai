public class PlayerSkeleton {

    //implement this function to have a working system
    public int pickMove(State s, int[][] legalMoves) {
        // TODO: no weight yet, need to implement weighting
        return evaluateHeightHeuristic(s, legalMoves);
    }

    public int evaluateHeightHeuristic(State s, int[][] legalMoves) {
        int bestMove = 0;

        int minimumHeight = Integer.MAX_VALUE;
        for (int i = 0; i < legalMoves.length; i++) {
            StateCopy sCopy = new StateCopy(s);
            sCopy.makeMove(i);
            int maxHeightOfCopy = getMaxHeight(sCopy);
            if (minimumHeight > maxHeightOfCopy) {
                minimumHeight = maxHeightOfCopy;
                bestMove = i;
            }
        }

        return bestMove;
    }

    public static void main(String[] args) {
        State s = new State();
        new TFrame(s);
        PlayerSkeleton p = new PlayerSkeleton();
        while (!s.hasLost()) {
            s.makeMove(p.pickMove(s, s.legalMoves()));
            s.draw();
            s.drawNext(0, 0);
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("You have completed " + s.getRowsCleared() + " rows.");
    }

    public int getMaxHeight(StateCopy s) {
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
 * Deep copy of State class to apply moves without touching the real {@link State} class
 */
@SuppressWarnings("Duplicates")
class StateCopy {


    public static final int COLS = 10;
    public static final int ROWS = 21;
    public static final int N_PIECES = 7;


    public boolean lost;

    //current turn
    private int turn;
    private int cleared;

    //each square in the grid - int means empty - other values mean the turn it was placed
    private int[][] field;
    //top row+1 of each column
    //0 means empty
    private int[] top;


    //number of next piece
    protected int nextPiece;


    //all legal moves - first index is piece type - then a list of 2-length arrays
    protected static int[][][] legalMoves = new int[N_PIECES][][];

    //indices for legalMoves
    public static final int ORIENT = 0;
    public static final int SLOT = 1;

    //possible orientations for a given piece type
    protected static int[] pOrients;

    //the next several arrays define the piece vocabulary in detail
    //width of the pieces [piece ID][orientation]
    protected static int[][] pWidth;

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

    public int getRowsCleared() {
        return cleared;
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

        int rowsCleared = 0;

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
