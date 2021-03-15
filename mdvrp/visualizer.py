import matplotlib.pyplot as plt
from dataclasses import dataclass
import random
from collections import defaultdict
import yaml
from os import path


@dataclass
class Depot:
    id: int
    x: int = None
    y: int = None

    def __str__(self):
        x = "Depot: {\n"
        for key, value in self.__dict__.items():
            x += "\t{}: {}\n".format(key, value)
        x += "}"
        return x


@dataclass
class Customer:
    id: int
    x: int
    y: int

    def __str__(self):
        x = "Customer: {\n"
        for key, value in self.__dict__.items():
            x += "\t{}: {}\n".format(key, value)
        x += "}"
        return x


depots = []
customers = []

with open("config.yaml", "r", encoding="utf-8") as f:
    config_data = yaml.safe_load(f)

input_file = config_data["input_file"]


with open("test_data/" + input_file,  "r", encoding="utf-8") as f:
    lines = f.read().splitlines()

for i, line in enumerate(lines):
    line = line.split()
    if i == 0:
        max_vehicles_per_depot = int(line[0])
        customer_count = int(line[1])
        depot_count = int(line[2])
    elif i < 1 + depot_count:
        depots.append(Depot(int(i)))
    elif i < 1 + depot_count + customer_count:
        customers.append(Customer(int(line[0]), int(line[1]), int(line[2])))
    else:
        index = i - (1 + depot_count + customer_count)
        depots[index].x = int(line[1])
        depots[index].y = int(line[2])


def get_cmap(n, name='hsv'):
    # https://stackoverflow.com/questions/14720331/how-to-generate-random-colors-in-matplotlib
    '''Returns a function that maps each index in 0, 1, ..., n-1 to a distinct 
    RGB color; the keyword argument name must be a standard mpl colormap name.'''
    return plt.cm.get_cmap(name, n + 1)


def plot_solution(solution_file, title):
    with open(solution_file, "r", encoding="utf-8") as f:
        lines = f.read().splitlines()

    plt.title(title + " - total distance: " + lines[0])

    cmap = get_cmap(max_vehicles_per_depot)
    color_indexes = defaultdict(lambda: set(
        [x for x in range(max_vehicles_per_depot)]))

    for i, line in enumerate(lines[1:]):
        line = line.split()
        depot = next(filter(lambda x: x.id == int(line[0]), depots))

        route = list(map(int, line[5:]))

        route_customers = []
        for customer_id in route:
            route_customers.append(
                next(filter(lambda x: x.id == customer_id, customers)))

        color_index = random.choice(tuple(color_indexes[depot.id]))
        color_indexes[depot.id].remove(color_index)

        from_x = depot.x
        from_y = depot.y
        for customer in route_customers:
            plt.plot([from_x, customer.x], [
                from_y, customer.y], c=cmap(color_index))
            from_x = customer.x
            from_y = customer.y
        plt.plot([from_x, depot.x], [from_y, depot.y], c=cmap(color_index))


def plot_depots_and_customer():
    for depot in depots:
        plt.plot(depot.x, depot.y, 'bo')

    for customer in customers:
        plt.plot(customer.x, customer.y, 'r.')


if path.exists("test_solutions/{}.res".format(input_file)):
    plt.figure(figsize=(16, 8))

    plt.subplot(1, 2, 1)
    plot_depots_and_customer()
    plot_solution("solutions/solution.res", "Solution")

    plt.subplot(1, 2, 2)
    plot_depots_and_customer()
    plot_solution("test_solutions/{}.res".format(input_file),
                  "Test solution")
else:
    plt.figure(figsize=(8, 8))
    plot_depots_and_customer()
    plot_solution("solutions/solution.res", "Solution")

plt.suptitle("Problem file: " + input_file)

plt.show()
