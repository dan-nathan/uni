import sys
import random as r
import math
import time

from problem_spec import ProblemSpec
from robot_config import write_robot_config_list_to_file, make_robot_config_from_ee1, make_robot_config_from_ee2
from tester import test_config_equality, test_self_collision, test_obstacle_collision, test_environment_bounds, point_is_close
from angle import Angle
#from visualiser import Visualiser

"""
Template file for you to implement your solution to Assignment 2. Contains a class you can use to represent graph nodes,
and a method for finding a path in a graph made up of GraphNode objects.

COMP3702 2020 Assignment 2 Support Code
"""

# max allowable error for floating point comparisons
TOLERANCE = 1e-5
NO_NEW_NODES = 50

class GraphNode:
    """
    Class representing a node in the state graph. You should create an instance of this class each time you generate
    a sample.
    """

    def __init__(self, spec, config):
        """
        Create a new graph node object for the given config.

        Neighbors should be added by appending to self.neighbors after creating each new GraphNode.

        :param spec: ProblemSpec object
        :param config: the RobotConfig object to be stored in this node
        """
        self.spec = spec
        self.config = config
        self.neighbors = []
        self.grapples = []

    def __eq__(self, other):
        return test_config_equality(self.config, other.config, self.spec)

    def __hash__(self):
        return hash(tuple(self.config.points))

    def get_successors(self):
        return self.neighbors

    @staticmethod
    def add_connection(n1, n2):
        """
        Creates a neighbor connection between the 2 given GraphNode objects.

        :param n1: a GraphNode object
        :param n2: a GraphNode object
        """
        n1.neighbors.append(n2)
        n2.neighbors.append(n1)

    @staticmethod
    def add_grapple_connection(n1, n2):
        GraphNode.add_connection(n1, n2)
        n1.grapples.append(n2)
        n2.grapples.append(n1)


def find_graph_path(spec, init_node):
    """
    This method performs a breadth first search of the state graph and return a list of configs which form a path
    through the state graph between the initial and the goal. Note that this path will not satisfy the primitive step
    requirement - you will need to interpolate between the configs in the returned list.

    You may use this method in your solver if you wish, or can implement your own graph search algorithm to improve
    performance.

    :param spec: ProblemSpec object
    :param init_node: GraphNode object for the initial configuration
    :return: List of configs forming a path through the graph from initial to goal
    """
    # search the graph
    init_container = [init_node]

    # here, each key is a graph node, each value is the list of configs visited on the path to the graph node
    init_visited = {init_node: [init_node.config]}

    while len(init_container) > 0:
        current = init_container.pop(0)

        if test_config_equality(current.config, spec.goal, spec):
            # found path to goal
            return init_visited[current]
        successors = current.get_successors()
        for suc in successors:
            if suc not in init_visited:
                init_container.append(suc)
                init_visited[suc] = init_visited[current] + [suc.config]

    return None


def distance_squared(a, b):
    dx = a[0] - b[0]
    dy = a[1] - b[1]
    return dx * dx + dy * dy


def collision_free(config, spec):
    if test_self_collision(config, spec) and test_obstacle_collision(config, spec, spec.obstacles)\
            and test_environment_bounds(config):
        return True
    return False


def uniform_random_sample(spec):
    angles = []
    lengths = []
    for i in range(spec.num_segments):
        angles.append(Angle(degrees = r.uniform(-165, 165)))
        lengths.append(r.uniform(spec.min_lengths[i], spec.max_lengths[i]))
    grapple_point = r.randrange(spec.num_grapple_points)
    if r.random() < 0.5:
        config = make_robot_config_from_ee1(spec.grapple_points[grapple_point][0],
                                            spec.grapple_points[grapple_point][1],
                                            angles, lengths, ee1_grappled=True)
    else:
        config = make_robot_config_from_ee2(spec.grapple_points[grapple_point][0],
                                            spec.grapple_points[grapple_point][1],
                                            angles, lengths, ee2_grappled=True)
    if collision_free(config, spec):
        return config
    return None


def interpolate_configs(config1, config2, spec, strict=True):
    maxdiff = 0
    delta_a = []
    delta_l = []
    for i in range(spec.num_segments):
        if config1.ee1_grappled:
            delta_a.append(config2.ee1_angles[i].in_radians() - config1.ee1_angles[i].in_radians())
        else:
            delta_a.append(config2.ee2_angles[i].in_radians() - config1.ee2_angles[i].in_radians())
        if abs(delta_a[-1]) > maxdiff:
            maxdiff = abs(delta_a[-1])
        delta_l.append(config2.lengths[i] - config1.lengths[i])
        if abs(delta_l[-1]) > maxdiff:
            maxdiff = abs(delta_l[-1])

    # add an extra step to avoid floating point rounding errors
    no_intermediate_states = 1 + math.ceil(maxdiff / ProblemSpec.PRIMITIVE_STEP)

    configs = []
    for i in range(no_intermediate_states + 1):
        if not strict and i % 2 == 1:
            continue
        angles = []
        lengths = []
        if config1.ee1_grappled:
            for j in range(spec.num_segments):
                angles.append(Angle(radians=config1.ee1_angles[j].in_radians()
                                    + delta_a[j] * (i / no_intermediate_states)))
                lengths.append(config1.lengths[j] + i * delta_l[j] / no_intermediate_states)
            config = make_robot_config_from_ee1(config1.points[0][0], config1.points[0][1],
                                                angles, lengths, ee1_grappled=True)
        else:
            for j in range(spec.num_segments):
                angles.append(Angle(radians=config1.ee2_angles[j].in_radians()
                                    + delta_a[j] * (i / no_intermediate_states)))
                lengths.append(config1.lengths[j] + i * delta_l[j] / no_intermediate_states)
            config = make_robot_config_from_ee2(config1.points[-1][0], config1.points[-1][1],
                                                angles, lengths, ee2_grappled=True)
        configs.append(config)

    return configs


def check_path_collision_help(configs, spec):
    if len(configs) == 0:
        return True
    if len(configs) == 1:
        return collision_free(configs[0], spec)
    middle = math.floor(len(configs) / 2)
    if not collision_free(configs[middle], spec):
        return False
    return check_path_collision_help(configs[:middle], spec) and check_path_collision_help(configs[middle:], spec)


def check_path_collision(config1, config2, spec):
    if (config1.ee1_grappled and config2.ee1_grappled and config1.points[0] == config2.points[0]
            or config1.ee2_grappled and config2.ee2_grappled and config1.points[-1] == config2.points[-1]):
        configs = interpolate_configs(config1, config2, spec, strict=False)
        return check_path_collision_help(configs, spec)
    return False


def grapple_to(node, grapple_point, dist, spec):
    base_config = node.config
    second_last = -2 if base_config.ee1_grappled else 1
    if base_config.ee1_grappled:
        angles = base_config.ee1_angles[:-1]
        lengths = base_config.lengths[:-1]
    else:
        angles = base_config.ee2_angles[:-1]
        lengths = base_config.lengths[1:]
    angle_to_grapple = math.atan2(grapple_point[1] - base_config.points[second_last][1],
                                  grapple_point[0] - base_config.points[second_last][0])

    cum_angle = 0
    for angle in angles:
        cum_angle += angle.in_radians()

    new_angle = Angle(radians=angle_to_grapple - cum_angle)
    if new_angle.in_degrees() >= 165 or new_angle.in_degrees() <= -165:
        return None

    angles.append(new_angle)
    if base_config.ee1_grappled:
        lengths.append(math.sqrt(dist))
    else:
        lengths.insert(0, math.sqrt(dist))
    if base_config.ee1_grappled:
        config1 = make_robot_config_from_ee1(base_config.points[0][0], base_config.points[0][1],
                                             angles, lengths, ee1_grappled=True)
    else:
        config1 = make_robot_config_from_ee2(base_config.points[-1][0], base_config.points[-1][1],
                                             angles, lengths, ee2_grappled=True)

    if not check_path_collision(base_config, config1, spec):
        return None

    if angle_to_grapple >= 0:
        new_angle = -math.pi + angle_to_grapple
    else:
        new_angle = math.pi + angle_to_grapple

    new_angles = [new_angle]
    for angle in reversed(angles[:-1]):
        if angle.in_radians() >= 0:
            new_angles.append(Angle(radians=math.pi - angle.in_radians()))
        else:
            new_angles.append(Angle(radians=-math.pi - angle.in_radians()))

    if base_config.ee1_grappled:
        config2 = make_robot_config_from_ee2(grapple_point[0], grapple_point[1], config1.ee2_angles,
                                             config1.lengths, ee2_grappled=True)
    else:
        config2 = make_robot_config_from_ee1(config1.points[0][0], config1.points[0][1], config1.ee1_angles,
                                             config1.lengths, ee1_grappled=True)
    grapple_node1 = GraphNode(spec, config1)
    GraphNode.add_connection(node, grapple_node1)
    grapple_node2 = GraphNode(spec, config2)
    GraphNode.add_grapple_connection(grapple_node1, grapple_node2)
    return grapple_node2


def add_to_graph(nodes, new_node, spec):
    end = new_node.config.ee1_grappled
    end_point = 0 if end else -1
    for node in nodes:
        if (node.config.ee1_grappled != end or
                not point_is_close(new_node.config.points[end_point][0], new_node.config.points[end_point][1],
                                   node.config.points[end_point][0], node.config.points[end_point][1], TOLERANCE)):
            continue
        if abs(node.config.ee1_angles[end_point].in_radians() - new_node.config.ee1_angles[end_point].in_radians()) \
                > math.pi:
            continue
        if check_path_collision(node.config, new_node.config, spec):
            GraphNode.add_connection(node, new_node)
    nodes.append(new_node)


def main(arglist):
    begin_time = time.time()
    input_file = arglist[0]
    output_file = arglist[1]

    spec = ProblemSpec(input_file)
    init_node = GraphNode(spec, spec.initial)
    goal_node = GraphNode(spec, spec.goal)

    steps = [init_node.config]

    nodes = [init_node, goal_node]

    while True:
        for i in range(NO_NEW_NODES):
            new_node = GraphNode(spec, uniform_random_sample(spec))
            if new_node.config is None:
                continue
            start_point = 0 if new_node.config.ee1_grappled else -1
            second_end_point = -2 if new_node.config.ee1_grappled else 1
            end_point = -1 if new_node.config.ee1_grappled else 0
            for gp in spec.grapple_points:
                if gp != new_node.config.points[start_point]:
                    dist = distance_squared(new_node.config.points[second_end_point], gp)
                    if dist < spec.max_lengths[end_point] * spec.max_lengths[end_point]:
                        grapple_node = grapple_to(new_node, gp, dist, spec)
                        if grapple_node is not None:
                            add_to_graph(nodes, grapple_node, spec)
            add_to_graph(nodes, new_node, spec)

        temp_steps = find_graph_path(spec, init_node)
        if temp_steps is not None:
            break

    for i in range(len(temp_steps) - 1):
        if temp_steps[i].ee1_grappled == temp_steps[i + 1].ee1_grappled:
            steps.extend(interpolate_configs(temp_steps[i], temp_steps[i + 1], spec)[1:])
        else:
            steps.append(temp_steps[i + 1])
    steps.append(goal_node.config)

    print("Solution found in: " + str(time.time() - begin_time))
    if len(arglist) > 1:
        write_robot_config_list_to_file(output_file, steps)

    #
    # You may uncomment this line to launch visualiser once a solution has been found. This may be useful for debugging.
    # *** Make sure this line is commented out when you submit to Gradescope ***
    #
    # v = Visualiser(spec, steps)


if __name__ == '__main__':
    main(sys.argv[1:])
