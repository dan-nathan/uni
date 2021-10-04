#!/usr/bin/python
import sys
import time
import queue as queuelib
import copy
from laser_tank import LaserTankMap

"""
Template file for you to implement your solution to Assignment 1.

COMP3702 2020 Assignment 1 Support Code
"""


#
#
# Code for any classes or functions you need can go here.
#
#

def write_output_file(filename, actions):
    """
    Write a list of actions to an output file. You should use this method to write your output file.
    :param filename: name of output file
    :param actions: list of actions where is action is in LaserTankMap.MOVES
    """
    f = open(filename, 'w')
    for i in range(len(actions)):
        f.write(str(actions[i]))
        if i < len(actions) - 1:
            f.write(',')
    f.write('\n')
    f.close()


class State:

    ACTION_LIST = {LaserTankMap.MOVE_FORWARD: 'f', LaserTankMap.TURN_LEFT: 'l', LaserTankMap.TURN_RIGHT: 'r',
                   LaserTankMap.SHOOT_LASER: 's'}
    nodes_generated = 0

    def __init__(self, map, cost, goal):
        self.map = map
        self.cost = cost
        self.goal = goal
        self.value_for_priority = 0

    def __eq__(self, other):
        return tuple(tuple(i) for i in self.map.grid_data) == tuple(tuple(i) for i in other.map.grid_data) \
               and self.map.player_heading == other.map.player_heading and self.map.player_x == other.map.player_x \
               and self.map.player_y == other.map.player_y

    def __hash__(self):
        result = 7
        result = 31 * result + hash(tuple(tuple(i) for i in self.map.grid_data))
        result = 31 * result + self.map.player_heading
        result = 31 * result + self.map.player_x
        result = 31 * result + self.map.player_y
        return result

    def __lt__(self, other):
        return self.value_for_priority < other.value_for_priority

    def get_next_states(self):
        next_states = []
        for action in State.ACTION_LIST:
            next_state = LaserTankMap(self.map.x_size, self.map.y_size, [row[:] for row in self.map.grid_data],
                                      self.map.player_x, self.map.player_y, self.map.player_heading)
            if next_state.apply_move(action) == next_state.SUCCESS:
                State.nodes_generated += 1
                next_states.append((State.ACTION_LIST[action], State(next_state, self.cost + 1, self.goal)))
        return next_states

    def bridges(self):
        result = 0
        for r in range(self.map.y_size):
            for c in range(self.map.x_size):
                if self.map.grid_data[r][c] == self.map.BRIDGE_SYMBOL:
                    result += 1
        return result

    def is_goal(self):
        return self.map.is_finished();

    def manhattan_distance(self, a, b):
        return abs(a[0] - b[0]) + abs(a[1] - b[1])

    def forward(self, action):
        # Returns 1 if the action is not a move forware - heuristic to prioritise moving
        return 0 if action == 'f' else 1

    def ucs_cost(self):
        return self.cost

    def a_star_estimate(self, action):
        return self.cost + self.manhattan_distance((self.map.player_x, self.map.player_y), self.goal) \
               + self.forward(action)

def search_astar(start):
    begin_time = time.time();
    log = dict()
    log['nvextex_explored_(with_duplicates)'] = 0

    fringe = queuelib.PriorityQueue()
    fringe.put(start)
    explored = {start: start.cost} # a dict of `vertex: cost_so_far`
    path = {start: []} # a dict of `vertex: actions`
    log['nvextex_explored_(with_duplicates)'] += 1

    while not fringe.empty():
        current = fringe.get()
        if current.is_goal():
            # Return info about search
            log['nvertex_in_fringe_at_termination'] = fringe.qsize()
            log['nvextex_explored'] = len(explored)
            log['action_path'] = path[current]
            log['solution_path_cost'] = explored[current]
            log['time'] = time.time() - begin_time
            return log

        for action, next in current.get_next_states():
            if (next not in explored) or (next.cost < explored[next]):
                explored[next] = next.cost
                path[next] = path[current] + [action]
                log['nvextex_explored_(with_duplicates)'] += 1
                next.value_for_priority = next.a_star_estimate(action)
                #next.value_for_priority = next.ucs_cost()
                fringe.put(next)

    raise RuntimeError('No solution!')

def find_flag(map):
    for r in range(map.y_size):
        for c in range(map.x_size):
            if map.grid_data[r][c] == map.FLAG_SYMBOL:
                return (c, r)
    return (-1, -1)


def main(arglist):

    input_file = arglist[0]
    output_file = arglist[1]

    # Read the input testcase file
    game_map = LaserTankMap.process_input_file(input_file)

    flag_pos = find_flag(game_map)
    start = State(game_map, 0, flag_pos)

    # Perform the search
    result = search_astar(start)
    actions = result['action_path']
    print("nodes: " + str(State.nodes_generated))
    print("fringe: " + str(result['nvertex_in_fringe_at_termination']))
    print("explored: " + str(result['nvextex_explored']))
    print("time: " + str(result['time']))


    # Write the solution to the output file
    write_output_file(output_file, actions)


if __name__ == '__main__':
    main(sys.argv[1:])

