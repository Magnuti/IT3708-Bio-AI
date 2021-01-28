import random
from LinReg import *
import matplotlib.pyplot as plt
import numpy as np
import time

# TODO test many different sets of variables
POPULATION_SIZE = 100
GENERATION_NUMBER = 20  # ? restrict after generation number ?
CROSSOVER_RATE = 0.7  # The chance that a crossover happens
MUTATION_RATE = 0.001


def read_file(filename):
    with open(filename, "r", encoding="utf-8") as f:
        lines = f.read().splitlines()
        rows = []
        for line in lines:
            numbers = line.split(",")
            numbers = list(map(float, numbers))
            rows.append(numbers)

    return rows


def bitstring_to_int(bitstring, scaling_factor=1.0):
    return int(bitstring, 2) * scaling_factor


def generate_init_population(population_size, features):
    population = []
    for i in range(population_size):
        bits = []
        for i in range(features):
            bits.append(random.randint(0, 1))

        bitstring = "".join(map(str, bits))
        population.append(bitstring)

    return population


def get_population_scores(population, scaling_factor=1.0):
    population_scores = {}
    for individual in population:
        population_scores[individual] = np.sin(
            bitstring_to_int(individual, scaling_factor))

    return population_scores


def parent_selection_roulette_wheel(population, number_of_parents, scaling_factor=1.0):
    '''
    Returns the chosen parents of the population in a stochastic manner because we use Roulette wheel selection.
    The returned mating pool is of the same size as the input population.
    '''
    population_scores = get_population_scores(population, scaling_factor)

    mating_pool = []
    for i in range(number_of_parents):
        parent = random.choices(
            list(population_scores.keys()), weights=list(population_scores.values()))
        mating_pool.append(parent[0])
        # TODO maybe calculate cumulative beforehand outside the loop to save time

    random.shuffle(mating_pool)

    return mating_pool


def parent_selection_tournament():
    # Implement if you have time, not necessary unless you are a group of two.
    raise NotImplementedError()

# def parent_selection_dataset(rows, selection_size, ml_algorithm):
#     population_scores = {}
#     for individual in population:
#         np_arr = ml_algorithm.get_columns(rows, individual)
#         rows_with_removed_column = np_arr[:, :-1]
#         values = np_arr[:, -1]
#         score = ml_algorithm.get_fitness(rows_with_removed_column, values)
#         population_scores[individual] = score

#     # Sort in ascending order because we want the ones with lowest error
#     keys = list(population_scores.keys())
#     keys.sort(key=lambda x: population_scores[x])
#     return keys[:selection_size]


def create_offsprings(parent_0, parent_1, crossover_rate, mutation_chance):
    '''
    Creates two offspring from two parents through crossover given by a crossover rate. Each offspring has a mutation chance.

    Returns a tuple with the offspring.
    '''
    if(len(parent_0) != len(parent_1)):
        raise ValueError("Parents must be of same length")

    crossover_roll = random.random()
    if(crossover_roll < crossover_rate):
        crossover_point = random.randint(1, len(parent_0) - 1)
        p_0_left = parent_0[crossover_point:]
        p_1_left = parent_1[crossover_point:]
        p_0_right = parent_0[:crossover_point]
        p_1_right = parent_1[:crossover_point]

        child_0 = p_0_left + p_1_right
        child_1 = p_1_left + p_0_right
    else:
        child_0 = parent_0
        child_1 = parent_1

    child_0 = add_mutation(child_0, mutation_chance)
    child_1 = add_mutation(child_1, mutation_chance)

    return (child_0, child_1)


def add_mutation(individual, mutation_chance):
    '''
    Adds a 1-bit mutation at a random index in the bitstring
    '''
    modified_individual = list(individual)
    for i, bit in enumerate(individual):
        mutation_roll = random.random()
        if(mutation_roll < mutation_chance):
            if(int(bit)):
                modified_individual[i] = "0"
            else:
                modified_individual[i] = "1"

    return "".join(modified_individual)


def survivor_selection(population, number_of_survivors, scaling_factor=1.0):
    '''
    Returns the best individuals of the population (i.e. parents + offspring).
    The returned population is of the same size as the original population.
    This function is deterministic.
    '''
    population_scores = get_population_scores(population, scaling_factor)

    # Sort in ascending order because we want the ones with lowest error
    keys = list(population_scores.keys())
    # Here we want the ones with the highest value, so we reverse the sort
    keys.sort(key=lambda x: population_scores[x], reverse=True)
    return keys[:number_of_survivors]


def plot_sin_population(population, max_range, scaling_factor, generation):
    sin_x = np.linspace(0, max_range, max_range * 10)
    sin_y = np.sin(sin_x)

    population_x = list(
        map(lambda x: bitstring_to_int(x, scaling_factor), population))
    population_y = np.sin(population_x)

    plt.clf()
    plt.plot(sin_x, sin_y)
    plt.scatter(population_x, population_y)
    plt.suptitle("Generation {}".format(generation))
    plt.show(block=False)
    plt.pause(0.001)
    time.sleep(0.2)


if __name__ == "__main__":

    # Sin function
    BITSTRING_LENGTH = 15
    MAX_SIN_RANGE = 128
    SCALING_FACTOR = MAX_SIN_RANGE / (2**BITSTRING_LENGTH)

    population = generate_init_population(POPULATION_SIZE, BITSTRING_LENGTH)
    plot_sin_population(population, MAX_SIN_RANGE, SCALING_FACTOR, 0)

    # TODO stop at some stop criteria (gen number, fitnes score etc.)
    for i in range(50):
        parents = parent_selection_roulette_wheel(
            population, POPULATION_SIZE, SCALING_FACTOR)

        # plot_sin_population(parents, MAX_RANGE, SCALING_FACTOR)

        for k in range(0, len(parents) // 2, 2):
            offsprings = create_offsprings(
                parents[k], parents[k + 1], CROSSOVER_RATE, MUTATION_RATE)
            population.extend(offsprings)

        population = survivor_selection(
            population, POPULATION_SIZE, SCALING_FACTOR)

        plot_sin_population(population, MAX_SIN_RANGE, SCALING_FACTOR, i + 1)

    plt.show()

    FEATURES = 101

    # Dataset
    # rows = read_file("dataset.txt")

    # ml_algorithm = LinReg()
