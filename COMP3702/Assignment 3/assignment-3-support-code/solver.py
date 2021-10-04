import time
import math
import random
from laser_tank import LaserTankMap, DotDict

"""
Template file for you to implement your solution to Assignment 3. You should implement your solution by filling in the
following method stubs:
    run_value_iteration()
    run_policy_iteration()
    get_offline_value()
    get_offline_policy()
    get_mcts_policy()
    
You may add to the __init__ method if required, and can add additional helper methods and classes if you wish.

To ensure your code is handled correctly by the autograder, you should avoid using any try-except blocks in your
implementation of the above methods (as this can interfere with our time-out handling).

COMP3702 2020 Assignment 3 Support Code
"""


class MCTSTree:
    ACTION_SET = {LaserTankMap.MOVE_FORWARD,
                  LaserTankMap.TURN_LEFT,
                  LaserTankMap.TURN_RIGHT,
                  LaserTankMap.SHOOT_LASER}

    def __init__(self, state, cost, parent=None, action=-1):
        self.plays = 0
        self.total_reward = 0
        self.preceding_action = action
        # cost of the action to reach this state
        self.cost = cost
        self.state = state
        # dict(action -> (no_plays, dict(probability -> outcome)))
        self.actions = dict()
        # set of actions that have been explored
        self.explored = set()
        self.parent = parent
        if cost == state.game_over_cost:
            self.terminal = True
        else:
            self.terminal = False

    def get_expected_value(self):
        return self.total_reward / self.plays

    def calc_expected_value(self, lottery):
        ev = 0
        total_prob = 0
        for outcome in lottery:
            if outcome.plays != 0:
                total_prob += lottery[outcome]
                ev += lottery[outcome] * (self.state.gamma * outcome.get_expected_value() + outcome.cost)
        return ev / total_prob

    def confidence_interval_max(self, total_plays, plays, expected_reward):
        return expected_reward + math.sqrt((2 * math.log(total_plays)) / plays)

    def UCB1_score(self, action):
        if action not in self.actions:
            return float('-inf')
        plays, lottery = self.actions[action]
        return self.confidence_interval_max(self.plays, plays, self.calc_expected_value(lottery))

    def get_lottery_result(self, lottery):
        running_total = 0
        result = random.random()
        for state in lottery:
            running_total += lottery[state]
            if result < running_total:
                return state
        # failsafe
        return None

    def get_new_child(self):
        options = self.ACTION_SET.difference(self.explored)
        result = random.sample(options, 1)[0]
        self.explored.add(result)
        return result

    def get_forward_next_states(self, row, col, direction):
        if direction == LaserTankMap.UP:
            forward = (LaserTankMap.UP, row - 1, col)
            forward_fails = ((LaserTankMap.UP, row, col), (LaserTankMap.UP, row, col + 1),
                             (LaserTankMap.UP, row, col - 1), (LaserTankMap.UP, row - 1, col - 1),
                             (LaserTankMap.UP, row - 1, col + 1))
        elif direction == LaserTankMap.LEFT:
            forward = (LaserTankMap.LEFT, row, col - 1)
            forward_fails = ((LaserTankMap.LEFT, row, col), (LaserTankMap.LEFT, row - 1, col),
                             (LaserTankMap.LEFT, row + 1, col), (LaserTankMap.LEFT, row - 1, col - 1),
                             (LaserTankMap.LEFT, row + 1, col - 1))
        elif direction == LaserTankMap.RIGHT:
            forward = (LaserTankMap.RIGHT, row, col + 1)
            forward_fails = ((LaserTankMap.RIGHT, row, col), (LaserTankMap.RIGHT, row - 1, col),
                             (LaserTankMap.RIGHT, row + 1, col), (LaserTankMap.RIGHT, row - 1, col + 1),
                             (LaserTankMap.RIGHT, row + 1, col + 1))
        else:
            forward = (LaserTankMap.DOWN, row + 1, col)
            forward_fails = ((LaserTankMap.DOWN, row, col), (LaserTankMap.DOWN, row, col + 1),
                             (LaserTankMap.DOWN, row, col - 1), (LaserTankMap.DOWN, row + 1, col - 1),
                             (LaserTankMap.DOWN, row + 1, col + 1))

        forward = self.get_forward_end_position(forward[0], forward[1], forward[2], row, col)
        forward_fails_2 = []
        for i in range(len(forward_fails)):
            forward_fails_2.append(self.get_forward_end_position(forward_fails[i][0], forward_fails[i][1], forward_fails[i][2], row, col))
        forward_fails = tuple(forward_fails_2)
        return forward, forward_fails

    def get_forward_end_position(self, direction, row, col, init_row, init_col):
        if self.state.cell_is_blocked(row, col):
            cost = self.state.collision_cost
            return direction, init_row, init_col, cost
        if self.state.cell_is_game_over(row, col):
            cost = self.state.game_over_cost
            return direction, init_row, init_col, cost

        cost = self.state.move_cost
        return direction, row, col, cost


    def select(self):
        # TODO update the number of plays
        if self.terminal:
            return self

        if len(self.explored) == len(self.ACTION_SET):
            selected_action = None
            best_value = float('-inf')
            for action in self.explored:
                value = self.UCB1_score(action)
                if value > best_value:
                    best_value = value
                    selected_action = action
            return self.get_lottery_result(self.actions[selected_action][1]).select()
        else:
            return self

    def expand(self):
        action = self.get_new_child()
        if action == LaserTankMap.MOVE_FORWARD:
            lottery = dict()
            forward, forward_fails = self.get_forward_next_states(self.state.player_y, self.state.player_x,
                                                                  self.state.player_heading)
            direction, row, col, cost = forward
            state = self.state.make_clone()
            state.player_y = row
            state.player_x = col
            new_node = MCTSTree(state, cost, parent=self, action=action)
            lottery.setdefault(new_node, self.state.t_success_prob)
            for fail in forward_fails:
                direction, row, col, cost = fail
                state = self.state.make_clone()
                state.player_y = row
                state.player_x = col
                new_node = MCTSTree(state, cost, parent=self, action=action)
                lottery.setdefault(new_node, (1 - self.state.t_success_prob) / 5)
            self.actions[action] = (1, lottery)
            return self.get_lottery_result(self.actions[action][1])
        else:
            state = self.state.make_clone()
            state.apply_move(action)
            new_node = MCTSTree(state, self.state.move_cost, parent=self, action=action)
            self.actions[action] = (1, {new_node: 1})
            return new_node

    def simulate(self):
        if self.state.grid_data[1][4] == LaserTankMap.FLAG_SYMBOL:
            iterations = 1
        else:
            iterations = 50
        rewards = []
        for i in range(iterations):
            state = self.state.make_clone()
            total_reward = 0
            action = random.sample(self.ACTION_SET, 1)[0]
            reward = state.apply_move(action)
            iterations = 1
            while iterations < 30 and reward != self.state.game_over_cost and not state.is_finished():
                total_reward += reward
                iterations += 1
                action = random.sample(self.ACTION_SET, 1)[0]
                reward = state.apply_move(action)
            total_reward += reward
            rewards.append(total_reward)
        # print("Iterations - " + str(iterations) + " Game over - " + str(reward == self.state.game_over_cost) + " Won - " + str(state.is_finished()) + " Reward - " + str(total_reward) + " Action - " + str(self.preceding_action))
        return rewards

    def backpropogate(self, rewards):
        for i in range(len(rewards)):
            self.plays += 1
            rewards[i] += self.cost
            self.total_reward += rewards[i]
        if self.parent is not None:
            self.parent.actions[self.preceding_action] = (self.parent.actions[self.preceding_action][0] + 1,
                                                          self.parent.actions[self.preceding_action][1])
            self.parent.backpropogate(rewards)



class Solver:

    def __init__(self, game_map):
        self.game_map = game_map

        self.teleporters = []
        for row in range(game_map.y_size):
            for col in range(game_map.x_size):
                if game_map.grid_data[row][col] == LaserTankMap.TELEPORT_SYMBOL:
                    self.teleporters.append((row, col))
        #
        # TODO
        # Write any environment preprocessing code you need here (e.g. storing teleport locations).
        #
        # You may also add any instance variables (e.g. root node of MCTS tree) here.
        #
        # The allowed time for this method is 1 second, so your Value Iteration or Policy Iteration implementation
        # should be in the methods below, not here.
        #

        self.values = None
        self.policy = None

    # for debugging purposes
    def print_policy(self, policy):
        print("UP")
        for row in range(1, self.game_map.y_size - 1):
            line = ""
            for col in range(1, self.game_map.x_size - 1):
                if policy[col - 1][row - 1][LaserTankMap.UP] == -1:
                    line += "W"
                else:
                    line += policy[col - 1][row - 1][LaserTankMap.UP]
            print(line)
        print("DOWN")
        for row in range(1, self.game_map.y_size - 1):
            line = ""
            for col in range(1, self.game_map.x_size - 1):
                if policy[col - 1][row - 1][LaserTankMap.DOWN] == -1:
                    line += "W"
                else:
                    line += policy[col - 1][row - 1][LaserTankMap.DOWN]
            print(line)
        print("LEFT")
        for row in range(1, self.game_map.y_size - 1):
            line = ""
            for col in range(1, self.game_map.x_size - 1):
                if policy[col - 1][row - 1][LaserTankMap.LEFT] == -1:
                    line += "W"
                else:
                    line += policy[col - 1][row - 1][LaserTankMap.LEFT]
            print(line)
        print("RIGHT")
        for row in range(1, self.game_map.y_size - 1):
            line = ""
            for col in range(1, self.game_map.x_size - 1):
                if policy[col - 1][row - 1][LaserTankMap.RIGHT] == -1:
                    line += "W"
                else:
                    line += policy[col - 1][row - 1][LaserTankMap.RIGHT]
            print(line)

    def print_values(self, values, policy):
        print("UP")
        for row in range(1, self.game_map.y_size - 1):
            line = ""
            for col in range(1, self.game_map.x_size - 1):
                if policy[col - 1][row - 1][LaserTankMap.UP] == -1:
                    line += "WWW"
                else:
                    line += str(round(values[col - 1][row - 1][LaserTankMap.UP], 1))
                line += " | "
            print(line)
        print("DOWN")
        for row in range(1, self.game_map.y_size - 1):
            line = ""
            for col in range(1, self.game_map.x_size - 1):
                if policy[col - 1][row - 1][LaserTankMap.DOWN] == -1:
                    line += "WWW"
                else:
                    line += str(round(values[col - 1][row - 1][LaserTankMap.DOWN], 1))
                line += " | "

            print(line)
        print("LEFT")
        for row in range(1, self.game_map.y_size - 1):
            line = ""
            for col in range(1, self.game_map.x_size - 1):
                if policy[col - 1][row - 1][LaserTankMap.LEFT] == -1:
                    line += "WWW"
                else:
                    line += str(round(values[col - 1][row - 1][LaserTankMap.LEFT], 1))
                line += " | "
            print(line)
        print("RIGHT")
        for row in range(1, self.game_map.y_size - 1):
            line = ""
            for col in range(1, self.game_map.x_size - 1):
                if policy[col - 1][row - 1][LaserTankMap.RIGHT] == -1:
                    line += "WWW"
                else:
                    line += str(round(values[col - 1][row - 1][LaserTankMap.RIGHT], 1))
                line += " | "
            print(line)

    def get_next_states(self, row, col, direction):
        if direction == LaserTankMap.UP:
            forward = (LaserTankMap.UP, row - 1, col)
            left = (LaserTankMap.LEFT, row, col)
            right = (LaserTankMap.RIGHT, row, col)
            forward_fails = ((LaserTankMap.UP, row, col), (LaserTankMap.UP, row, col + 1),
                             (LaserTankMap.UP, row, col - 1), (LaserTankMap.UP, row - 1, col - 1),
                             (LaserTankMap.UP, row - 1, col + 1))
        elif direction == LaserTankMap.LEFT:
            forward = (LaserTankMap.LEFT, row, col - 1)
            left = (LaserTankMap.DOWN, row, col)
            right = (LaserTankMap.UP, row, col)
            forward_fails = ((LaserTankMap.LEFT, row, col), (LaserTankMap.LEFT, row - 1, col),
                             (LaserTankMap.LEFT, row + 1, col), (LaserTankMap.LEFT, row - 1, col - 1),
                             (LaserTankMap.LEFT, row + 1, col - 1))
        elif direction == LaserTankMap.RIGHT:
            forward = (LaserTankMap.RIGHT, row, col + 1)
            left = (LaserTankMap.UP, row, col)
            right = (LaserTankMap.DOWN, row, col)
            forward_fails = ((LaserTankMap.RIGHT, row, col), (LaserTankMap.RIGHT, row - 1, col),
                             (LaserTankMap.RIGHT, row + 1, col), (LaserTankMap.RIGHT, row - 1, col + 1),
                             (LaserTankMap.RIGHT, row + 1, col + 1))
        else:
            forward = (LaserTankMap.DOWN, row + 1, col)
            left = (LaserTankMap.RIGHT, row, col)
            right = (LaserTankMap.LEFT, row, col)
            forward_fails = ((LaserTankMap.DOWN, row, col), (LaserTankMap.DOWN, row, col + 1),
                             (LaserTankMap.DOWN, row, col - 1), (LaserTankMap.DOWN, row + 1, col - 1),
                             (LaserTankMap.DOWN, row + 1, col + 1))

        forward = self.get_forward_end_position(forward[0], forward[1], forward[2], row, col)
        forward_fails_2 = []
        for i in range(len(forward_fails)):
            forward_fails_2.append(self.get_forward_end_position(forward_fails[i][0], forward_fails[i][1], forward_fails[i][2], row, col))
        forward_fails = tuple(forward_fails_2)
        return forward, forward_fails, left, right

    def get_forward_end_position(self, direction, row, col, init_row, init_col):
        cost = self.game_map.move_cost
        if self.game_map.grid_data[row][col] == LaserTankMap.OBSTACLE_SYMBOL:
            cost = self.game_map.collision_cost
            return direction, init_row, init_col, cost
        if self.game_map.grid_data[row][col] == LaserTankMap.WATER_SYMBOL:
            cost = self.game_map.game_over_cost
            return direction, init_row, init_col, cost

        if self.game_map.grid_data[row][col] == LaserTankMap.ICE_SYMBOL:
            if direction == LaserTankMap.UP:
                while self.game_map.grid_data[row][col] == LaserTankMap.ICE_SYMBOL:
                    if row < 1 or self.game_map.grid_data[row - 1][col] == LaserTankMap.OBSTACLE_SYMBOL:
                        cost = self.game_map.collision_cost
                        break
                    elif self.game_map.grid_data[row - 1][col] == LaserTankMap.WATER_SYMBOL:
                        cost = self.game_map.game_over_cost
                        return direction, init_row, init_col, cost
                    row -= 1

            elif direction == LaserTankMap.DOWN:
                while self.game_map.grid_data[row][col] == LaserTankMap.ICE_SYMBOL:
                    if row >= self.game_map.y_size or self.game_map.grid_data[row + 1][col] == LaserTankMap.OBSTACLE_SYMBOL:
                        cost = self.game_map.collision_cost
                        break
                    elif self.game_map.grid_data[row + 1][col] == LaserTankMap.WATER_SYMBOL:
                        cost = self.game_map.game_over_cost
                        return direction, init_row, init_col, cost
                    row += 1

            elif direction == LaserTankMap.LEFT:
                while self.game_map.grid_data[row][col] == LaserTankMap.ICE_SYMBOL:
                    if col < 1 or self.game_map.grid_data[row][col - 1] == LaserTankMap.OBSTACLE_SYMBOL:
                        cost = self.game_map.collision_cost
                        break
                    elif self.game_map.grid_data[row][col - 1] == LaserTankMap.WATER_SYMBOL:
                        cost = self.game_map.game_over_cost
                        return direction, init_row, init_col, cost
                    col -= 1

            else:
                while self.game_map.grid_data[row][col] == LaserTankMap.ICE_SYMBOL:
                    if col >= self.game_map.x_size or self.game_map.grid_data[row][
                            col + 1] == LaserTankMap.OBSTACLE_SYMBOL:
                        cost = self.game_map.collision_cost
                        break
                    elif self.game_map.grid_data[row][col + 1] == LaserTankMap.WATER_SYMBOL:
                        cost = self.game_map.game_over_cost
                        return direction, init_row, init_col, cost
                    col += 1

        if len(self.teleporters) >= 2:
            if (row, col) == self.teleporters[0]:
                return direction, self.teleporters[1][0], self.teleporters[1][1], cost
            elif (row, col) == self.teleporters[1]:
                return direction, self.teleporters[0][0], self.teleporters[0][1], cost

        return direction, row, col, cost

    # gets the expected value of a move forward. forward is a tuple (direction, row, col, cost) and forward_fails
    # is a tuple of 5 tuples of (direction, row, col, cost)
    def get_forward_value(self, forward, forward_fails, values):
        forward_value = 0
        for fail in forward_fails:
            direction, row, col, cost = fail
            forward_value += self.game_map.gamma * values[col - 1][row - 1][direction] + cost

        direction, row, col, cost = forward
        forward_value *= 0.2 * (1 - self.game_map.t_success_prob)
        forward_value += self.game_map.t_success_prob * (self.game_map.gamma * values[col - 1][row - 1][direction] + cost)
        return forward_value

    def run_value_iteration(self):
        """
        Build a value table and a policy table using value iteration, and store inside self.values and self.policy.
        """
        begin_time = time.time()
        values = [[[0 for _ in LaserTankMap.DIRECTIONS]
                   for __ in range(1, self.game_map.y_size - 1)]
                  for ___ in range(1, self.game_map.x_size - 1)]

        policy = [[[-1 for _ in LaserTankMap.DIRECTIONS]
                   for __ in range(1, self.game_map.y_size - 1)]
                  for ___ in range(1, self.game_map.x_size - 1)]

        max_delta = self.game_map.epsilon
        iterations = 0
        #self.game_map.time_limit = 1
        while time.time() - begin_time < self.game_map.time_limit and max_delta >= self.game_map.epsilon:# and iterations < 10:
            iterations += 1
            new_values = [[[0 for _ in LaserTankMap.DIRECTIONS]
                       for __ in range(1, self.game_map.y_size - 1)]
                      for ___ in range(1, self.game_map.x_size - 1)]
            max_delta = 0
            for row in range(1, self.game_map.y_size - 1):
                for col in range(1, self.game_map.x_size - 1):
                    if self.game_map.grid_data[row][col] != LaserTankMap.LAND_SYMBOL and \
                            self.game_map.grid_data[row][col] != LaserTankMap.ICE_SYMBOL:
                        continue
                    for direction in LaserTankMap.DIRECTIONS:
                        forward, forward_fails, left, right = self.get_next_states(row, col, direction)
                        left_value = self.game_map.gamma * values[left[2] - 1][left[1] - 1][left[0]] + self.game_map.move_cost
                        right_value = self.game_map.gamma * values[right[2] - 1][right[1] - 1][right[0]] + self.game_map.move_cost
                        forward_value = self.get_forward_value(forward, forward_fails, values)

                        max_value = max(left_value, right_value, forward_value)
                        if max_value == forward_value:
                            policy[col - 1][row - 1][direction] = LaserTankMap.MOVE_FORWARD
                        elif max_value == right_value:
                            policy[col - 1][row - 1][direction] = LaserTankMap.TURN_RIGHT
                        else:
                            policy[col - 1][row - 1][direction] = LaserTankMap.TURN_LEFT

                        if abs(max_value - values[col - 1][row - 1][direction]) > max_delta:
                            max_delta = abs(max_value - values[col - 1][row - 1][direction])
                        new_values[col - 1][row - 1][direction] = max_value
            values = new_values


        self.print_policy(policy)
        self.print_values(values, policy)

        print(str(time.time() - begin_time))
        print(iterations)

        # store the computed values and policy
        self.values = values
        self.policy = policy

    def run_policy_iteration(self):
        """
        Build a value table and a policy table using policy iteration, and store inside self.values and self.policy.
        """
        begin_time = time.time()
        # TODO delete self.game_map.initial_seed += 11
        values = [[[0 for _ in LaserTankMap.DIRECTIONS]
                   for __ in range(1, self.game_map.y_size - 1)]
                  for ___ in range(1, self.game_map.x_size - 1)]
        policy = [[[LaserTankMap.MOVE_FORWARD for _ in LaserTankMap.DIRECTIONS]
                   for __ in range(1, self.game_map.y_size - 1)]
                  for ___ in range(1, self.game_map.x_size - 1)]

        #max_delta = self.game_map.epsilon
        iterations = 0
        finished = False
        #self.game_map.time_limit = 1
        while time.time() - begin_time < self.game_map.time_limit and not finished:# and iterations < 10:#and max_delta >= self.game_map.epsilon:
            iterations += 1
            #max_delta = 0
            finished = True
            new_values = [[[0 for _ in LaserTankMap.DIRECTIONS]
                           for __ in range(1, self.game_map.y_size - 1)]
                          for ___ in range(1, self.game_map.x_size - 1)]
            for row in range(1, self.game_map.y_size - 1):
                for col in range(1, self.game_map.x_size - 1):
                    if self.game_map.grid_data[row][col] != LaserTankMap.LAND_SYMBOL and \
                            self.game_map.grid_data[row][col] != LaserTankMap.ICE_SYMBOL:
                        continue
                    for direction in LaserTankMap.DIRECTIONS:
                        forward, forward_fails, left, right = self.get_next_states(row, col, direction)
                        if policy[col - 1][row - 1][direction] == LaserTankMap.MOVE_FORWARD:
                            value = self.get_forward_value(forward, forward_fails, values)
                        elif policy[col - 1][row - 1][direction] == LaserTankMap.TURN_LEFT:
                            value = self.game_map.gamma * values[left[2] - 1][left[1] - 1][left[0]] + self.game_map.move_cost
                        elif policy[col - 1][row - 1][direction] == LaserTankMap.TURN_RIGHT:
                            value = self.game_map.gamma * values[right[2] - 1][right[1] - 1][right[0]] + self.game_map.move_cost
                        else:
                            return

                        #if abs(value - values[col - 1][row - 1][direction]) > max_delta:
                        #    max_delta = abs(value - values[col - 1][row - 1][direction])
                        new_values[col - 1][row - 1][direction] = value
            values = new_values

            for row in range(1, self.game_map.y_size - 1):
                for col in range(1, self.game_map.x_size - 1):
                    if self.game_map.grid_data[row][col] != LaserTankMap.LAND_SYMBOL and \
                            self.game_map.grid_data[row][col] != LaserTankMap.ICE_SYMBOL:
                        continue
                    for direction in LaserTankMap.DIRECTIONS:
                        forward, forward_fails, left, right = self.get_next_states(row, col, direction)
                        left_value = self.game_map.gamma * values[left[2] - 1][left[1] - 1][left[0]] + self.game_map.move_cost
                        right_value = self.game_map.gamma * values[right[2] - 1][right[1] - 1][right[0]] + self.game_map.move_cost
                        forward_value = self.get_forward_value(forward, forward_fails, values)

                        current_policy = policy[col - 1][row - 1][direction]
                        max_value = max(left_value, right_value, forward_value)
                        if max_value == forward_value:
                            policy[col - 1][row - 1][direction] = LaserTankMap.MOVE_FORWARD
                        elif max_value == right_value:
                            policy[col - 1][row - 1][direction] = LaserTankMap.TURN_RIGHT
                        else:
                            policy[col - 1][row - 1][direction] = LaserTankMap.TURN_LEFT

                        if current_policy != policy[col - 1][row - 1][direction]:
                            finished = False
                        new_values[col - 1][row - 1][direction] = max_value
            values = new_values

        self.print_policy(policy)
        self.print_values(values, policy)

        print(str(time.time() - begin_time))
        print(iterations)

        #
        # TODO
        # Write your Policy Iteration implementation here.
        #
        # When this method is called, you are allowed up to [state.time_limit] seconds of compute time. You should stop
        # iterating either when max_delta < epsilon, or when the time limit is reached, whichever occurs first.
        #

        # store the computed values and policy
        self.values = values
        self.policy = policy

    def get_offline_value(self, state):
        """
        Get the value of this state.
        :param state: a LaserTankMap instance
        :return: V(s) [a floating point number]
        """
        row = state.player_y
        col = state.player_x
        direction = state.player_heading
        return self.values[col - 1][row - 1][direction]

        #
        # TODO
        # Write code to return the value of this state based on the stored self.values
        #
        # You can assume that either run_value_iteration( ) or run_policy_iteration( ) has been called before this
        # method is called.
        #
        # When this method is called, you are allowed up to 1 second of compute time.
        #

        pass

    def get_offline_policy(self, state):
        """
        Get the policy for this state (i.e. the action that should be performed at this state).
        :param state: a LaserTankMap instance
        :return: pi(s) [an element of LaserTankMap.MOVES]
        """

        row = state.player_y
        col = state.player_x
        direction = state.player_heading
        return self.policy[col - 1][row - 1][direction]

        #
        # TODO
        # Write code to return the optimal action to be performed at this state based on the stored self.policy
        #
        # You can assume that either run_value_iteration( ) or run_policy_iteration( ) has been called before this
        # method is called.
        #
        # When this method is called, you are allowed up to 1 second of compute time.
        #

        pass

    def get_mcts_policy(self, state):
        """
        Choose an action to be performed using online MCTS.
        :param state: a LaserTankMap instance
        :return: pi(s) [an element of LaserTankMap.MOVES]
        """

        begin_time = time.time()

        root = MCTSTree(state, 0)

        while time.time() - begin_time < 0.95 * state.time_limit:
            leaf = root.select()
            if leaf.terminal:
                new_leaf = leaf
            else:
                new_leaf = leaf.expand()
            rewards = new_leaf.simulate()
            new_leaf.backpropogate(rewards)

        max_expected_value = float('-inf')
        policy = -1
        for action in root.actions:
            expected_value = root.calc_expected_value(root.actions[action][1])
            print(str(action) + " " + str(expected_value))
            if expected_value > max_expected_value:
                max_expected_value = expected_value
                policy = action

        return policy

        #
        # TODO
        # Write your Monte-Carlo Tree Search implementation here.
        #
        # Each time this method is called, you are allowed up to [state.time_limit] seconds of compute time - make sure
        # you stop searching before this time limit is reached.
        #

        pass







