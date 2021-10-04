import time
import random
import math

from laser_tank import LaserTankMap

"""
Template file for you to implement your solution to Assignment 4. You should implement your solution by filling in the
following method stubs:
    train_q_learning()
    train_sarsa()
    get_policy()
    
You may add to the __init__ method if required, and can add additional helper methods and classes if you wish.

To ensure your code is handled correctly by the autograder, you should avoid using any try-except blocks in your
implementation of the above methods (as this can interfere with our time-out handling).

COMP3702 2020 Assignment 4 Support Code
"""


class Solver:
    EPSILON = 0.8
    ALPHA = 0.05
    USE_ALPHA = True

    def __init__(self):
        """
        Initialise solver without a Q-value table.
        """

        self.q_values = None

    @staticmethod
    def epsilon_greedy(actions, epsilon):
        best_actions = None
        best_value = float('-inf')
        for action in actions:
            if actions[action][0] == best_value:
                if best_actions is None:
                    best_actions = {action}
                else:
                    best_actions.add(action)
            elif actions[action][0] > best_value:
                best_actions = {action}
                best_value = actions[action][0]
        if random.random() < epsilon or len(set(actions).difference(best_actions)) == 0:
            return random.sample(best_actions, 1)[0]
        else:
            return random.sample(set(actions).difference(best_actions), 1)[0]

    @staticmethod
    def print_moving_average(rewards, q):
        if q:
            filename = "output/Q_alpha=" + str(Solver.ALPHA) + ".txt"
        else:
            filename = "output/sarsa_alpha=" + str(Solver.ALPHA) + ".txt"
        file = open(filename, "w")
        moving_average = 0
        for i in range(len(rewards)):
            if i < 50:
                moving_average = moving_average + (rewards[i] - moving_average) / (i + 1)
            else:
                moving_average = moving_average + (rewards[i] - rewards[i - 50]) / 50
            line = str(moving_average) + ","
            file.write(line)

    def train_q_learning(self, simulator):
        """
        Train the agent using Q-learning, building up a table of Q-values.
        :param simulator: A simulator for collecting episode data (LaserTankMap instance)
        """

        begin_time = time.time()
        # Q(s, a) table
        # Format: key = hash(state), value = (dict(mapping actions to (values, times_visited))
        q_values = dict()
        q_values[hash(simulator)] = {LaserTankMap.MOVE_FORWARD: (0, 0), LaserTankMap.TURN_RIGHT: (0, 0),
                                     LaserTankMap.TURN_LEFT: (0, 0), LaserTankMap.SHOOT_LASER: (0, 0)}

        iterations = 0
        rewards = []

        while time.time() - begin_time < simulator.time_limit * 0.99:# and iterations < 100000:
            episode = simulator.make_clone()
            while True:# and iterations < 100000:
                previous_hash = hash(episode)
                action = Solver.epsilon_greedy(q_values[hash(episode)], Solver.EPSILON)
                reward, done = episode.apply_move(action)

                rewards.append(reward)
                iterations += 1

                new_hash = hash(episode)
                if new_hash not in q_values:
                    q_values[new_hash] = {LaserTankMap.MOVE_FORWARD: (0, 0), LaserTankMap.TURN_RIGHT: (0, 0),
                                          LaserTankMap.TURN_LEFT: (0, 0), LaserTankMap.SHOOT_LASER: (0, 0)}

                best_value = float('-inf')
                for next_action in q_values[new_hash]:
                    if q_values[new_hash][next_action][0] > best_value:
                        best_value = q_values[new_hash][next_action][0]

                value = q_values[previous_hash][action][0]
                k = q_values[previous_hash][action][1] + 1
                if Solver.USE_ALPHA:
                    new_value = value + Solver.ALPHA * (reward + episode.gamma * best_value - value)
                else:
                    new_value = value + (1 / math.sqrt(k)) * (reward + episode.gamma * best_value - value)
                q_values[previous_hash][action] = (new_value, k)
                if done:
                    rewards.append(0)
                    iterations += 1
                    break

        Solver.print_moving_average(rewards, True)
        # store the computed Q-values
        self.q_values = q_values

    def train_sarsa(self, simulator):
        """
        Train the agent using SARSA, building up a table of Q-values.
        :param simulator: A simulator for collecting episode data (LaserTankMap instance)
        """

        begin_time = time.time()
        # Q(s, a) table
        # Format: key = hash(state), value = dict(mapping actions to values)
        q_values = dict()
        q_values[hash(simulator)] = {LaserTankMap.MOVE_FORWARD: (0, 0), LaserTankMap.TURN_RIGHT: (0, 0),
                                     LaserTankMap.TURN_LEFT: (0, 0), LaserTankMap.SHOOT_LASER: (0, 0)}

        # Set initial action
        action = LaserTankMap.MOVE_FORWARD

        iterations = 0
        rewards = []

        while time.time() - begin_time < simulator.time_limit * 0.99:# and iterations < 100000:
            episode = simulator.make_clone()
            while True:# and iterations < 100000:
                previous_hash = hash(episode)
                reward, done = episode.apply_move(action)

                rewards.append(reward)
                iterations += 1

                new_hash = hash(episode)
                if new_hash not in q_values:
                    q_values[new_hash] = {LaserTankMap.MOVE_FORWARD: (0, 0), LaserTankMap.TURN_RIGHT: (0, 0),
                                          LaserTankMap.TURN_LEFT: (0, 0), LaserTankMap.SHOOT_LASER: (0, 0)}

                next_action = Solver.epsilon_greedy(q_values[new_hash], Solver.EPSILON)
                next_value = q_values[new_hash][next_action][0]

                value = q_values[previous_hash][action][0]
                k = q_values[previous_hash][action][1] + 1

                if Solver.USE_ALPHA:
                    new_value = value + Solver.ALPHA * (reward + episode.gamma * next_value - value)
                else:
                    new_value = value + (1 / math.sqrt(k)) * (reward + episode.gamma * next_value - value)

                q_values[previous_hash][action] = (new_value, k)
                action = next_action
                if done:
                    rewards.append(0)
                    iterations += 1
                    break

        Solver.print_moving_average(rewards, False)

        # store the computed Q-values
        self.q_values = q_values

    def get_policy(self, state):
        """
        Get the policy for this state (i.e. the action that should be performed at this state).
        :param state: a LaserTankMap instance
        :return: pi(s) [an element of LaserTankMap.MOVES]
        """

        actions = self.q_values[hash(state)]
        best_action = None
        best_value = float('-inf')
        for action in actions:
            if actions[action][0] > best_value:
                best_action = action
                best_value = actions[action][0]
        return best_action
