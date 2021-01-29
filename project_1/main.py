import random
from LinReg import *
import matplotlib.pyplot as plt
import numpy as np
import time


def read_file(filename):
    with open(filename, "r", encoding="utf-8") as f:
        lines = f.read().splitlines()
        rows = []
        for line in lines:
            numbers = line.split(",")
            numbers = list(map(float, numbers))
            rows.append(numbers)

    return rows


def generate_init_population(population_size, features):
    population = []
    for i in range(population_size):
        bits = []
        for i in range(features):
            bits.append(random.randint(0, 1))

        population.append(tuple(bits))

    return population


def get_population_scores(population, fitness_function):
    population_scores = {}
    for individual in population:
        population_scores[individual] = fitness_function(individual)

    return population_scores


def parent_selection_roulette_wheel(population, number_of_parents, fitness_function, cumulative_weight_scaler=lambda x: x):
    '''
    Returns the chosen parents of the population in a stochastic manner because we use Roulette wheel selection.
    The returned mating pool is of the same size as the input population.
    '''
    population_scores = get_population_scores(population, fitness_function)

    # Set the population_scores as the cumulative sums for performance gain in random.choices()
    cum_sum = 0
    for key, value in population_scores.items():
        if(value < 0):
            raise ValueError(
                "Value cannot be negative for cumulative weights to work")

        population_scores[key] = cumulative_weight_scaler(value) + cum_sum
        cum_sum += value

    mating_pool = []
    keys = list(population_scores.keys())
    cum_weights = list(population_scores.values())
    for i in range(number_of_parents):
        parent = random.choices(keys, cum_weights=cum_weights)
        mating_pool.append(parent[0])

    random.shuffle(mating_pool)

    return mating_pool


def parent_selection_tournament():
    # Implement if you have time, not necessary unless you are a group of two.
    raise NotImplementedError()


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
            if(bit):
                modified_individual[i] = 0
            else:
                modified_individual[i] = 1

    return tuple(modified_individual)


def get_offsprings(parents, crossover_rate, mutation_rate):
    offsprings = []
    for k in range(0, len(parents) // 2, 2):
        offsprings.extend(create_offsprings(
            parents[k], parents[k + 1], crossover_rate, mutation_rate))

    return offsprings


def survivor_selection_generational(population, offsprings):
    return offsprings


def survivor_selection_fitness(population, offsprings, number_of_survivors, fitness_function, want_highest_scores):
    '''
    Returns the best individuals of the population (i.e. parents + offspring).
    The returned population is of the same size as the original population.
    This function is deterministic.
    '''
    population.extend(offsprings)

    population_scores = get_population_scores(population, fitness_function)

    # Sort in ascending order because we want the ones with lowest error
    keys = list(population_scores.keys())
    # Here we want the ones with the highest value, so we reverse the sort
    keys.sort(key=lambda x: population_scores[x], reverse=want_highest_scores)
    return keys[:number_of_survivors]


def run_sin():
    BITSTRING_LENGTH = 15
    MAX_SIN_RANGE = 128
    SCALING_FACTOR = MAX_SIN_RANGE / (2**BITSTRING_LENGTH)

    POPULATION_SIZE = 100  # Fifites - low hundreds (page 100 Eiben/Smith)
    CROSSOVER_RATE = 0.7  # Between 0.6-0.8 (page 100 Eiben/Smith)
    # MUTATION_RATE should be between 1/BITSTRING_LENGTH and 1/POPULATION_SIZE (page 100 Eiben/Smith)
    MUTATION_RATE = 0.02

    def bitstring_to_int(bitstring, scaling_factor=1.0):
        bitstring = "".join(map(str, bitstring))
        return int(bitstring, 2) * scaling_factor

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

    # Could have used a fitness_cache here as well
    def fitness_function(individual):
        # + 1 because we don't want negative weights in our cumulative weights choice
        return np.sin(bitstring_to_int(individual, SCALING_FACTOR)) + 1

    population = generate_init_population(POPULATION_SIZE, BITSTRING_LENGTH)
    plot_sin_population(population, MAX_SIN_RANGE, SCALING_FACTOR, 0)

    # TODO stop at some stop criteria (gen number, fitnes score etc.)
    for i in range(50):
        parents = parent_selection_roulette_wheel(
            population, POPULATION_SIZE, fitness_function, lambda x: 1/x)

        # plot_sin_population(parents, MAX_SIN_RANGE, SCALING_FACTOR, i + 1)

        offsprings = get_offsprings(parents, CROSSOVER_RATE, MUTATION_RATE)

        # population = survivor_selection_generational(population, offsprings)
        population = survivor_selection_fitness(
            population, offsprings, POPULATION_SIZE, fitness_function, True)

        plot_sin_population(population, MAX_SIN_RANGE, SCALING_FACTOR, i + 1)

    plt.show()


def run_lin_reg():
    FEATURES = 101
    POPULATION_SIZE = 100  # Fifites - low hundreds (page 100 Eiben/Smith)
    CROSSOVER_RATE = 0.7  # Between 0.6-0.8 (page 100 Eiben/Smith)
    # MUTATION_RATE should be between 1/FEATURES and 1/POPULATION_SIZE (page 100 Eiben/Smith)
    MUTATION_RATE = 0.01

    ROWS = read_file("dataset.txt")
    ML_ALGORITHM = LinReg()

    VALUES = np.asarray(ROWS)[:, -1]

    rmse_original = ML_ALGORITHM.get_fitness(
        np.asarray(ROWS)[:, :-1], VALUES)
    print("Original root mean squared error: {:.5f}".format(rmse_original))

    fitness_cache = dict()

    def fitness_function(individual):
        if(individual not in fitness_cache):
            np_arr = ML_ALGORITHM.get_columns(ROWS, individual)
            fitness_cache[individual] = ML_ALGORITHM.get_fitness(
                np_arr[:, :-1], VALUES)

        return fitness_cache[individual]

    population = generate_init_population(POPULATION_SIZE, FEATURES)

    for i in range(100):
        # We use 1/(x**2) because we want to use lower scores as the best scores,
        # which makes it easier to use them as cumulative weights in roulette wheel selection.
        parents = parent_selection_roulette_wheel(
            population, POPULATION_SIZE, fitness_function, lambda x: 1/(x**2))

        offsprings = get_offsprings(parents, CROSSOVER_RATE, MUTATION_RATE)

        # population = survivor_selection_generational(population, offsprings)

        population = survivor_selection_fitness(
            population, offsprings, POPULATION_SIZE, fitness_function, False)

        scores = list(map(lambda x: fitness_function(x), population))
        print("Generation {}:\n\tBest: {:5f}, max: {:.5f}, average: {:.5f}".format(
            i, min(scores), max(scores), np.mean(scores)))


if __name__ == "__main__":
    # run_sin()
    run_lin_reg()
