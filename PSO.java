import com.sun.org.apache.bcel.internal.generic.POP;

public class PSO {
    /**
     * (Copied from Wikipedia to aid me in understanding PSO. Let's go PJ.)
     * for each particle i = 1, ..., S do
     *    Initialize the particle's position with a uniformly distributed random vector: xi ~ U(blo, bup)
     *    Initialize the particle's best known position to its initial position: pi ← xi
     *    if f(pi) < f(g) then
     *        update the swarm's best known  position: g ← pi
     *    Initialize the particle's velocity: vi ~ U(-|bup-blo|, |bup-blo|)
     * while a termination criterion is not met do:
     *    for each particle i = 1, ..., S do
     *       for each dimension d = 1, ..., n do
     *          Pick random numbers: rp, rg ~ U(0,1)
     *          Update the particle's velocity: vi,d ← ω vi,d + φp rp (pi,d-xi,d) + φg rg (gd-xi,d)
     *       Update the particle's position: xi ← xi + vi
     *       if f(xi) < f(pi) then
     *          Update the particle's best known position: pi ← xi
     *          if f(pi) < f(g) then
     *             Update the swarm's best known position: g ← pi
     */

    // for these values, we start with a small number then play with it to find the idea
    public static final int POPULATION = 16;
    // the naming convention may be a bit confusing but consider that the number of games is our training,
    // whereas the number of iterations is a termination algorithm
    public static final int NUMGAMES = 50;
    public static final int NUMITERATIONS = 100;
    public static final int NUMHEURISTICS = PlayerSkeleton.NUMHEURISTICS;

    // Parameters (to be fine tuned later)
    public static double OMEGA = 0.001;
    public static double PHI_PARTICLE = 0.001;
    public static double PHI_GLOBAL = 0.001;

    // ok we need particle array for the first for loop
    public Particle[] particles;
    public double[] bestGlobalPosition;

    /**
     * Constructor for PSO class. We create the particles in this class and initialise the particles' position with
     * an uniformly distributed random vector.
     */
    public PSO() {
        particles = new Particle[POPULATION];
        bestGlobalPosition = new double[NUMHEURISTICS];
        // initialise the positions of the particles with random values, then set the best known positions
        // of each particle as their beginning position
        for (int i = 0; i < POPULATION; i++) {
            particles[i] = new Particle(NUMITERATIONS, NUMHEURISTICS);
            particles[i].initialisePosition(NUMHEURISTICS);
            particles[i].initialiseBestKnownPosition();
            particles[i].initialiseVelocity();
            // supposed to have if cost function of current position for this particle < cost function of global best
            // position, update global best function to this particle's cost function.
            // Not sure how to write this yet. Should I just write value function?
        }
    }

    // this is a very rudimentary implementation of PSO, I'm following the Wikipedia article here and seeing if
    // it works or not. Right now I am having some issue with array index out of bounds, please feel free to help if possible.
    public void run() {
        PlayerSkeleton p = new PlayerSkeleton();
        for (int i = 0; i < NUMGAMES; i++) {
            for (int j = 0; j < NUMITERATIONS; j++) {
                State s = new State();
                StateCopy copy = new StateCopy(s);
                for (int k = 0; k < POPULATION; k++) {
                    p.updateWeights(particles[k].getBestKnownPosition());
                    while(!s.hasLost()) {
                        s.makeMove(p.pickMove(s,s.legalMoves()));
                        copy.makeMove(p.pickMove(s, s.legalMoves()));
                    }
                }
                System.out.println("Fitness for this game is: " + p.valueFunction(copy));
            }
        }
    }

//    public double calculateFitness
}
