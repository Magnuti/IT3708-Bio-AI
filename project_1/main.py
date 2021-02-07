import random
from LinReg import LinReg
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
        for j in range(features):
            bits.append(random.randint(0, 1))

        population.append(tuple(bits))

    return population


def get_population_scores(population, fitness_function):
    population_scores = {}
    for individual in population:
        population_scores[individual] = fitness_function(individual)

    return population_scores


def parent_selection_roulette_wheel(population, number_of_parents, fitness_function, cumulative_weight_scaler=lambda x: x, only_unique=False):
    '''
    Returns the chosen parents of the population in a stochastic manner because we use Roulette wheel selection.
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
        parent = random.choices(keys, cum_weights=cum_weights)[0]
        if only_unique:
            # Redraw if we get an already picked parent
            while parent in mating_pool:
                parent = random.choices(keys, cum_weights=cum_weights)[0]
        mating_pool.append(parent)
    random.shuffle(mating_pool)

    return mating_pool


def parent_selection_tournament(population, number_of_parents, fitness_function, k, want_highest_scores):
    mating_pool = []
    for _ in range(number_of_parents):
        # competitors = random.choices(population, k=k)
        competitors = random.sample(population, k)
        population_scores = get_population_scores(
            competitors, fitness_function)
        if(want_highest_scores):
            top_score = max(population_scores.values())
        else:
            top_score = min(population_scores.values())

        for key, value in population_scores.items():
            if value == top_score:
                mating_pool.append(key)
                break

    return mating_pool


def create_offsprings(parent_0, parent_1, crossover_rate, mutation_chance):
    '''
    Creates two offspring from two parents through crossover given by a crossover rate.
    Each offspring has a mutation chance.

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
    for k in range(0, len(parents), 2):
        x = create_offsprings(
            parents[k], parents[k + 1], crossover_rate, mutation_rate)
        offsprings.extend(x)

    return offsprings


def survivor_selection_generational(population, offsprings):
    return offsprings

# TODO can implement elitism


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


def survivor_selection_deterministic_crowding(parents, offsprings, fitness_function, want_highest_scores):
    assert len(parents) == 2
    assert len(offsprings) == 2

    if(want_highest_scores):
        def is_fitter_than(a, b):
            return a > b
    else:
        def is_fitter_than(a, b):
            return a < b

    def d(a, b):
        '''
        Returns the Hamming distance between two bitstrings.
        '''
        distance = 0
        for i, _ in enumerate(a):
            if(a[i] != b[i]):
                distance += 1
        return distance

    scores = get_population_scores(
        parents, fitness_function) | get_population_scores(offsprings, fitness_function)

    # This algorithm is inspired from figure 8.14 in Evolutionary Optimization Algorithms, Simon, Wiley
    p1, p2 = parents
    for i in range(2):
        # For each child
        c = offsprings[i]
        if(d(p1, c) < d(p2, c) and is_fitter_than(scores[c], scores[p1])):
            p1 = c
        elif(d(p2, c) < d(p1, c) and is_fitter_than(scores[c], scores[p2])):
            p2 = c

    return [p1, p2]


def entropy(population):
    # Assuming all individuals are of the same length
    bistring_length = len(population[0])
    counters = np.zeros(bistring_length)
    for individual in population:
        for i, bit in enumerate(individual):
            if(bit):
                counters[i] += 1

    probabilities = np.empty(bistring_length)
    for i, value in enumerate(counters):
        probabilities[i] = value / len(population)

    x = 0
    for p in probabilities:
        if(p == 0):
            continue
        x -= (p * np.log2(p))

    return x


def plot_entropy(sga_entropy, crowding_entropy):
    plt.plot([i for i in range(len(crowding_entropy))],
             crowding_entropy, label="Crowding")
    plt.plot([i for i in range(len(sga_entropy))], sga_entropy, label="SGA")
    plt.legend()
    plt.ylabel("Entropy")
    plt.xlabel("Generation")
    plt.show()


def score_print(generation, fitness_function, population):
    scores = list(map(lambda x: fitness_function(x), population))
    print("Generation {}:\n\tmin: {:5f}, max: {:.5f}, average: {:.5f}".format(
        generation, min(scores), max(scores), np.mean(scores)))


def run_genetic_algorithm(population_size, features, crossover_rate, mutation_rate, fitness_function,
                          max_generation_number, parent_selection_function, k,
                          survivor_selection_function, want_highest_scores=True,
                          cumulative_weight_scaler=lambda x: x, plot_function=None, verbose=False, stop_at_score=None):
    population = generate_init_population(population_size, features)
    entropy_history = np.empty(max_generation_number)

    if plot_function is not None:
        plot_function(population, 0)

    # TODO stop at some stop criteria (gen number, fitnes score etc.)
    for i in range(max_generation_number):
        if survivor_selection_function == survivor_selection_fitness:
            if(parent_selection_function == parent_selection_roulette_wheel):
                parents = parent_selection_function(
                    population, population_size, fitness_function, cumulative_weight_scaler)
            elif(parent_selection_function == parent_selection_tournament):
                parents = parent_selection_tournament(
                    population, population_size, fitness_function, k, want_highest_scores)

            offsprings = get_offsprings(parents, crossover_rate, mutation_rate)

            population = survivor_selection_function(
                population, offsprings, population_size, fitness_function, want_highest_scores)

        elif survivor_selection_function == survivor_selection_deterministic_crowding:
            new_population = []
            for _ in range(population_size // 2):
                if(parent_selection_function == parent_selection_roulette_wheel):
                    parents = parent_selection_function(
                        population, 2, fitness_function, cumulative_weight_scaler, only_unique=True)
                elif(parent_selection_function == parent_selection_tournament):
                    parents = parent_selection_tournament(
                        population, 2, fitness_function, k, want_highest_scores)

                offsprings = get_offsprings(
                    parents, crossover_rate, mutation_rate)

                survivors = survivor_selection_deterministic_crowding(
                    parents, offsprings, fitness_function, want_highest_scores)

                new_population.extend(survivors)

            population = new_population

        elif survivor_selection_function == survivor_selection_generational:
            if(parent_selection_function == parent_selection_roulette_wheel):
                parents = parent_selection_function(
                    population, population_size, fitness_function, cumulative_weight_scaler)
            elif(parent_selection_function == parent_selection_tournament):
                parents = parent_selection_tournament(
                    population, population_size, fitness_function, k, want_highest_scores)

            offsprings = get_offsprings(parents, crossover_rate, mutation_rate)

            population = survivor_selection_generational(
                population, offsprings)

        if plot_function is not None:
            plot_function(population, i + 1)

        if verbose:
            score_print(i, fitness_function, population)

        entropy_history[i] = entropy(population)

        # TODO stop at some score

    if plot_function is not None:
        plt.show()

    population_scores = get_population_scores(population, fitness_function)

    keys = list(population_scores.keys())
    keys.sort(key=lambda x: population_scores[x], reverse=want_highest_scores)
    best_individual = keys[0]

    return entropy_history, best_individual


if __name__ == "__main__":
    def run_sin():
        features = 15
        population_size = 100  # Fifites - low hundreds (page 100 Eiben/Smith)
        crossover_rate = 0.7  # Between 0.6-0.8 (page 100 Eiben/Smith)
        # mutation_rate should be between 1/features and 1/population_size (page 100 Eiben/Smith)
        mutation_rate = 0.02
        max_generation_number = 50

        max_sin_range = 128
        # Scales integers down from [0, 2**features] to [0, max_sin_range]
        SCALING_FACTOR = max_sin_range / (2**features)

        def bitstring_to_int(bitstring):
            bitstring = "".join(map(str, bitstring))
            return int(bitstring, 2) * SCALING_FACTOR

        def plot_sin_population(population, generation):
            sin_x = np.linspace(0, max_sin_range, max_sin_range * 10)
            sin_y = np.sin(sin_x)

            population_x = list(
                map(lambda x: bitstring_to_int(x), population))
            population_y = np.sin(population_x)

            plt.clf()
            plt.plot(sin_x, sin_y)
            plt.scatter(population_x, population_y)
            plt.suptitle("Generation {}".format(generation))
            plt.show(block=False)
            plt.pause(0.001)
            time.sleep(0.01)

        fitness_cache = dict()

        def fitness_function_sin(individual):
            if individual not in fitness_cache:
                # + 1 because we don't want negative weights in our cumulative weights choice
                fitness_cache[individual] = np.sin(
                    bitstring_to_int(individual)) + 1

            return fitness_cache[individual]

        sga_entropy, best_individual_sga = run_genetic_algorithm(population_size, features, crossover_rate, mutation_rate, fitness_function_sin,
                                                                 max_generation_number, parent_selection_roulette_wheel, 10, survivor_selection_fitness,
                                                                 plot_function=plot_sin_population, verbose=False)

        crossover_rate = 0.7  # Between 0.6-0.8 (page 100 Eiben/Smith)
        # mutation_rate should be between 1/features and 1/population_size (page 100 Eiben/Smith)
        mutation_rate = 0.02
        max_generation_number = 50
        crowding_entropy, best_individual_crowding = run_genetic_algorithm(population_size, features, crossover_rate, mutation_rate, fitness_function_sin,
                                                                           max_generation_number, parent_selection_roulette_wheel, 10,
                                                                           survivor_selection_deterministic_crowding, plot_function=plot_sin_population, verbose=False)

        plot_entropy(sga_entropy, crowding_entropy)
        print("Best individual SGA: {} with a score of {}.\nBest individual crowding: {} with a score of {}".format(best_individual_sga,
                                                                                                                    fitness_function_sin(
                                                                                                                        best_individual_sga), best_individual_crowding,
                                                                                                                    fitness_function_sin(best_individual_crowding)))

    def run_lin_reg():
        features = 101
        population_size = 100  # Fifites - low hundreds (page 100 Eiben/Smith)
        crossover_rate = 0.7  # Between 0.6-0.8 (page 100 Eiben/Smith)
        # mutation_rate should be between 1/FEATURES and 1/population_size (page 100 Eiben/Smith)
        mutation_rate = 0.005
        max_generation_number = 100

        ROWS = read_file("dataset.txt")
        ML_ALGORITHM = LinReg()

        rmse_original = ML_ALGORITHM.get_fitness(
            np.asarray(ROWS)[:, :-1], np.asarray(ROWS)[:, -1])
        print("Original root mean squared error: {:.5f}".format(rmse_original))

        fitness_cache = dict()

        def fitness_function_lin_reg(individual):
            if(individual not in fitness_cache):
                np_arr = ML_ALGORITHM.get_columns(ROWS, individual)
                fitness_cache[individual] = ML_ALGORITHM.get_fitness(
                    np_arr[:, :-1], np_arr[:, -1])

            return fitness_cache[individual]

        # We use 1/(x**2) because we want to use lower scores as the best scores,
        # # which makes it easier to use them as cumulative weights in roulette wheel selection.
        sga_entropy, best_individual_sga = run_genetic_algorithm(population_size, features, crossover_rate, mutation_rate, fitness_function_lin_reg,
                                                                 max_generation_number, parent_selection_roulette_wheel, 5, survivor_selection_fitness,
                                                                 want_highest_scores=False, cumulative_weight_scaler=lambda x: 1/(x**2), verbose=True)

        # TODO maybe break when score is < 0.124
        crossover_rate = 0.7  # Between 0.6-0.8 (page 100 Eiben/Smith)
        # mutation_rate should be between 1/features and 1/population_size (page 100 Eiben/Smith)
        mutation_rate = 0.01
        crowding_entropy, best_individual_crowding = run_genetic_algorithm(population_size, features, crossover_rate, mutation_rate, fitness_function_lin_reg,
                                                                           max_generation_number, parent_selection_roulette_wheel, 5, survivor_selection_deterministic_crowding,
                                                                           want_highest_scores=False, cumulative_weight_scaler=lambda x: 1/x, verbose=True)

        plot_entropy(sga_entropy, crowding_entropy)
        print("Best individual SGA: {} with a score of {}.\nBest individual crowding: {} with a score of {}".format(best_individual_sga,
                                                                                                                    fitness_function_lin_reg(
                                                                                                                        best_individual_sga), best_individual_crowding,
                                                                                                                    fitness_function_lin_reg(best_individual_crowding)))

    # run_sin()
    run_lin_reg()
