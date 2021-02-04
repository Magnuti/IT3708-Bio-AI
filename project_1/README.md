# Project 1

## Simple Genetic Algorithm (SGA)

* Mating pool size = population size
* We have two selection functions, one that just replaces the parents with the offsprings, and one that selects the fittest individuals from the parents-offspring-pool. We could also have used a combination of the two (i.e. some form of elitism) to better balance exploration-exploitation.

## Crowding

* Crowding by deterministic crowding.
We can see that some "bad" parents always survive, this is because they rarely get the chance to mate and create offspring, which means that they are rarely compared to another individual (i.e. offfspring) meaning they will survive for a long time. We could have used elitism to solve this issue, say that the worst 5% are always replaced by the best 5%.
