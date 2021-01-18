from random import randint
from LinReg import *


# TODO test many different sets of variables
FEATURES = 101
POPULATION_SIZE = 100  # aka. generation number ?
SELECTION_SIZE = 30  # How many offsprings that should become parents
CROSSOVER_RATE = 0.7
MUTATION_RATE = 0.1

# TODO implement one SGA and one crowding


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
            bits.append(randint(0, 1))

        bitstring = "".join(map(str, bits))
        population.append(bitstring)

    return population


def parent_selection(rows, selection_size, ml_algorithm):
    population_scores = {}
    for individual in population:
        np_arr = ml_algorithm.get_columns(rows, individual)
        rows_with_removed_column = np_arr[:, :-1]
        values = np_arr[:, -1]
        score = ml_algorithm.get_fitness(rows_with_removed_column, values)
        population_scores[individual] = score

    # Sort in ascending order because we want the ones with lowest error
    keys = list(population_scores.keys())
    keys.sort(key=lambda x: population_scores[x])
    return keys[:selection_size]


def create_offspring(parent_1, parent_2, mutation_chance):
    if(len(parent_1) != len(parent_2)):
        raise ValueError("Parents must be of same length")

    crossover_point = randint(1, len(parent_1) - 1)
    p_1_left = parent_1[crossover_point:]
    p_2_left = parent_2[crossover_point:]
    p_1_right = parent_1[:crossover_point]
    p_2_right = parent_2[:crossover_point]

    child_0 = p_1_left.join(p_2_right)
    child_1 = p_2_left.join(p_1_right)

    # TODO mutatioin

    return child_0, child_1


if __name__ == "__main__":
    rows = read_file("dataset.txt")

    ml_algorithm = LinReg()

    population = generate_init_population(POPULATION_SIZE, FEATURES)

    parents = parent_selection(rows, SELECTION_SIZE, ml_algorithm)
