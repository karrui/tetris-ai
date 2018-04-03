import java.util.ArrayList;

public class Particle {

    // |h| dimension matrix for the position, best known position and velocity.
    // We can also understand the bestKnownSolution as ideally the "solution" to the problem.
    private double[] position;
    private double[] bestKnownPosition;
    private double[] velocity;

    private double fitnessHere; // fitness for this iteration
    private double fitnessOverall; // best fitness among all iterations so far
    private double[] fitnessHistory;

    public Particle(int numIterations, int numHeuristics) {
        position = new double[numHeuristics];
        bestKnownPosition = new double[numHeuristics];
        velocity = new double[numHeuristics];

        fitnessHistory = new double[numIterations];
        fitnessOverall = Double.NEGATIVE_INFINITY;
    }

    // accessors
    public double[] getPosition() {
        return position;
    }

    public double[] getBestKnownPosition() {
        return bestKnownPosition;
    }
}
