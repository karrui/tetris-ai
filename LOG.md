# Log

## 040418

So I've attempted to implement the PSO algorithm as much as I can, 
while trying my best to link the general algorithm listed on Wikipedia site to our Tetris AI setting.

### Accomplishments
1. Wrote the Particle class, it should be ok now. I believe that it has all the necessary methods.
2. Write a section of the full PSO code, a section because I cannot understand some stuff. This PSO code will create and initialise the 
problem correctly.
3. I've updated PlayerSkeleton to call PSO instead of running the training inside TrainPlayerSkeleton.

### Issues faced
1. I am facing Arrays out of bounds index problem when I run the PSO algorithm. Specifically, StateCopy will have this issue
in the while (!s.hasLost()) loop. I'm unsure why this is so, I'm mirroring State in every step, so 
StateCopy shouldn't go out of bounds if State doesn't right??
2. I'm still unsure about the learning part, specifically updating of the weights. 
3. I'm unsure if we are calculating our fitness
correctly. We implemented fitness as value function for TD, I'm not sure if we did that correctly.

### Goals
By end of Week 11, we can at least implement the PSO algorithm to learn and update the weights correctly. Multi-threading will hopefully be done in week 12.