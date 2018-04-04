import java.util.Arrays;

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

    // initialise
    public void initialisePosition(int numHeuristics) {
        Arrays.fill(position, Math.random());
    }

    // accessors
    public double[] getPosition() {
        return position;
    }

    public double[] getBestKnownPosition() {
        return bestKnownPosition;
    }

    public double getFitnessHere() {
        return fitnessHere;
    }

    public double getFitnessOverall() {
        return fitnessOverall;
    }

    // mutators
    public void initialiseVelocity() {
        Arrays.fill(velocity, 0.0); // initialised to 0 according to ResearchGate
    }
    public void initialiseBestKnownPosition() {
        bestKnownPosition = position.clone();
    }
    public void setBestKnownPosition(double[] best) {
        bestKnownPosition = best.clone();
    }

    public void updateFitnessHistory(int idx, double fitness) {
        fitnessHistory[idx] = fitness;
    }

    public double computeAvgFitness() {
        return 0.0; // not sure how to implement this method yet because I don't know what's the point of it

    }
}
