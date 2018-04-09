import java.util.Random;

class MetaOptimization {

    private static int NUM_ITERATIONS = 3;
    private static int currBestScore;
    private static int globalBestScore;

    // beta is represented arbitrarily with 0.5 here. We can change beta to see how it responds
    private static double decreaseFactor = Math.pow(2.0, -0.5);

    // parameters in PSO
    private static int swarm;
    private static double inertia;
    private static double socialParameter;
    private static double cognitiveParameter;

    // bounds for the hypercube d
    private static int swarmBound;
    private static double inertiaBound;
    private static double socialParameterBound;
    private static double cognitiveParameterBound;

    public static void main(String[] args) {
        MetaOptimization meta = new MetaOptimization();
        meta.run();
    }

    /**
     * We use this method to create run PSO many times such that we try and find the best parameters.
     * The parameters will be updated accordingly.
     */
    private void run() {
        Random r = new Random();

        // initialise x to a random solution in the search space.
        // more specifically, we initialise parameters in PSO, namely swarm size, inertia, social and cognitive parameters
        swarm = r.nextInt(199) + 1;
        // for generating double values within a range: randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble()
        // inertia \in [-2, 2]
        inertia = -2.0 + 4.0 * r.nextDouble();
        // social and cognitive parameters both in [-4, 4]
        socialParameter = -4.0 + 8.0 * r.nextDouble();
        cognitiveParameter = -4.0 + 8.0 * r.nextDouble();

        // set the initial sampling range d to cover the entire search space
        swarmBound = 30;
        inertiaBound = 2.0;
        socialParameterBound = 4.0;
        cognitiveParameterBound = 4.0;

        // we run PSO once to set the globalBest value
        PSO firstPso = new PSO(swarm, inertia, socialParameter, cognitiveParameter);
        globalBestScore = firstPso.run();

        // until a termination criterion is met, repeat the following
        for (int i = 0; i < NUM_ITERATIONS; i ++) {

            // pick a random vector a~U(-d,d)
            int randomSwarm = -1 * swarmBound + (swarmBound * 2) * r.nextInt();
            double randomInertia = -1.0 * inertiaBound + (inertiaBound * 2) * r.nextDouble();
            double randomSocialParameter = -1.0 * socialParameterBound + (socialParameterBound * 2)
                    * r.nextDouble();
            double randomCognitiveParameter = -1.0 * cognitiveParameterBound + (cognitiveParameterBound * 2)
                    * r.nextDouble();

            // add this to the current solution x to create the new potential solution
            int swarmHere;
            if (swarm + randomSwarm > 0) { // if the swarm size is still greater than 0
                swarmHere = swarm + randomSwarm;
            }
            else {
                // swarm will either decrement by 1, stay the same or increment by 1
                // the value 1 has been arbitrarily chosen
                swarmHere = swarm + (-1 + r.nextInt(2));
            }
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
            }
            else { // decrease the search range by multiplication with the factor q (also known as decreaseFactor)
                swarmBound = (int) Math.ceil(swarmBound * decreaseFactor); // we get the ceiling
                inertiaBound = inertiaBound * decreaseFactor;
                socialParameterBound = socialParameterBound * decreaseFactor;
                cognitiveParameterBound = cognitiveParameterBound * decreaseFactor;
            }
        }

        System.out.println("globalBest is now: " + globalBestScore);
    }
}
