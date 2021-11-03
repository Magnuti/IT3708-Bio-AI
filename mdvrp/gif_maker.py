import matplotlib.pyplot as plt
from dataclasses import dataclass
import random
from collections import defaultdict
import yaml
import os
import imageio as iio

with iio.get_writer('generational_gif.gif', mode='I', fps=10) as writer:
    for filename in sorted(os.listdir("generations"), key=lambda x: int(x.split(".")[0])):
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
                customers.append(
                    Customer(int(line[0]), int(line[1]), int(line[2])))
            else:
                index = i - (1 + depot_count + customer_count)
                depots[index].x = int(line[1])
                depots[index].y = int(line[2])

        def plot_solution(solution_file, title):
            with open(solution_file, "r", encoding="utf-8") as f:
                lines = f.read().splitlines()

            plt.title("Generation {}".format(
                filename.replace("generation", "").replace(".res", "")))

            for i, line in enumerate(lines[1:]):
                line = line.split()
                depot = next(filter(lambda x: x.id == int(line[0]), depots))

                route = list(map(int, line[5:]))

                route_customers = []
                for customer_id in route:
                    route_customers.append(
                        next(filter(lambda x: x.id == customer_id, customers)))

                from_x = depot.x
                from_y = depot.y
                for customer in route_customers:
                    plt.plot([from_x, customer.x], [
                             from_y, customer.y], c="black")
                    from_x = customer.x
                    from_y = customer.y
                plt.plot([from_x, depot.x], [from_y, depot.y], c="black")

        def plot_depots_and_customer():
            for depot in depots:
                plt.plot(depot.x, depot.y, 'bo')

            for customer in customers:
                plt.plot(customer.x, customer.y, 'r.')

        # Plot only our solution if no test solution exists
        plt.figure(figsize=(8, 8))
        plot_depots_and_customer()
        plot_solution("generations/" + filename, "Solution")

        plt.suptitle("Problem file: " + input_file)

        plt.savefig("temp.png")
        plt.close()

        image = iio.imread("temp.png")
        writer.append_data(image)

os.remove("temp.png")
