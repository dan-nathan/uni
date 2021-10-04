import sys
import random
"""
laser_tank.py

This file contains a class representing Laser Tank game map with non-deterministic transitions. You should make use of
this class in your solver. You may modify this file if you wish.

Running this file directly with a valid testcase file given as an argument launches an interactive instance of the
Laser Tank game.

COMP3702 2020 Assignment 3 Support Code

Last updated by njc 16/08/20
"""


class LaserTankMap:
    """
    Instance of a LaserTank game map with non-deterministic transitions.

    You may use and modify this class however you want.
    """

    # input file symbols
    LAND_SYMBOL = ' '
    WATER_SYMBOL = 'W'
    OBSTACLE_SYMBOL = '#'
    BRIDGE_SYMBOL = 'B'
    BRICK_SYMBOL = 'K'
    ICE_SYMBOL = 'I'
    TELEPORT_SYMBOL = 'T'
    FLAG_SYMBOL = 'F'

    MIRROR_UL_SYMBOL = '1'
    MIRROR_UR_SYMBOL = '2'
    MIRROR_DL_SYMBOL = '3'
    MIRROR_DR_SYMBOL = '4'

    PLAYER_UP_SYMBOL = '^'  # note: player always starts on a land tile
    PLAYER_DOWN_SYMBOL = 'v'
    PLAYER_LEFT_SYMBOL = '<'
    PLAYER_RIGHT_SYMBOL = '>'

    ANTI_TANK_UP_SYMBOL = 'U'
    ANTI_TANK_DOWN_SYMBOL = 'D'
    ANTI_TANK_LEFT_SYMBOL = 'L'
    ANTI_TANK_RIGHT_SYMBOL = 'R'
    ANTI_TANK_DESTROYED_SYMBOL = 'X'

    VALID_SYMBOLS = [LAND_SYMBOL, WATER_SYMBOL, OBSTACLE_SYMBOL, BRIDGE_SYMBOL, BRICK_SYMBOL, ICE_SYMBOL,
                     TELEPORT_SYMBOL, FLAG_SYMBOL, MIRROR_UL_SYMBOL, MIRROR_UR_SYMBOL, MIRROR_DL_SYMBOL,
                     MIRROR_DR_SYMBOL, PLAYER_UP_SYMBOL, PLAYER_DOWN_SYMBOL, PLAYER_LEFT_SYMBOL, PLAYER_RIGHT_SYMBOL,
                     ANTI_TANK_UP_SYMBOL, ANTI_TANK_DOWN_SYMBOL, ANTI_TANK_LEFT_SYMBOL, ANTI_TANK_RIGHT_SYMBOL,
                     ANTI_TANK_DESTROYED_SYMBOL]

    # move symbols (i.e. output file symbols)
    MOVE_FORWARD = 'f'
    TURN_LEFT = 'l'
    TURN_RIGHT = 'r'
    SHOOT_LASER = 's'
    MOVES = [MOVE_FORWARD, TURN_LEFT, TURN_RIGHT, SHOOT_LASER]

    # directions
    UP = 0
    DOWN = 1
    LEFT = 2
    RIGHT = 3
    DIRECTIONS = [UP, DOWN, LEFT, RIGHT]

    # render characters
    MAP_GLYPH_TABLE = {LAND_SYMBOL: '   ', WATER_SYMBOL: 'WWW', OBSTACLE_SYMBOL: 'XXX', BRIDGE_SYMBOL: '[B]',
                       BRICK_SYMBOL: '[K]', ICE_SYMBOL: '-I-', TELEPORT_SYMBOL: '(T)', FLAG_SYMBOL: ' F ',
                       MIRROR_UL_SYMBOL: ' /|', MIRROR_UR_SYMBOL: '|\\ ', MIRROR_DL_SYMBOL: ' \\|',
                       MIRROR_DR_SYMBOL: '|/ ', ANTI_TANK_UP_SYMBOL: '[U]', ANTI_TANK_DOWN_SYMBOL: '[D]',
                       ANTI_TANK_LEFT_SYMBOL: '[L]', ANTI_TANK_RIGHT_SYMBOL: '[R]', ANTI_TANK_DESTROYED_SYMBOL: '[X]'}
    PLAYER_GLYPH_TABLE = {UP: '[^]', DOWN: '[v]', LEFT: '[<]', RIGHT: '[>]'}

    # symbols which are movable for each direction
    MOVABLE_SYMBOLS = {UP: [BRIDGE_SYMBOL, MIRROR_UL_SYMBOL, MIRROR_UR_SYMBOL, ANTI_TANK_UP_SYMBOL,
                            ANTI_TANK_LEFT_SYMBOL, ANTI_TANK_RIGHT_SYMBOL, ANTI_TANK_DESTROYED_SYMBOL],
                       DOWN: [BRIDGE_SYMBOL, MIRROR_DL_SYMBOL, MIRROR_DR_SYMBOL, ANTI_TANK_DOWN_SYMBOL,
                              ANTI_TANK_LEFT_SYMBOL, ANTI_TANK_RIGHT_SYMBOL, ANTI_TANK_DESTROYED_SYMBOL],
                       LEFT: [BRIDGE_SYMBOL, MIRROR_UL_SYMBOL, MIRROR_DL_SYMBOL, ANTI_TANK_UP_SYMBOL,
                              ANTI_TANK_DOWN_SYMBOL, ANTI_TANK_LEFT_SYMBOL, ANTI_TANK_DESTROYED_SYMBOL],
                       RIGHT: [BRIDGE_SYMBOL, MIRROR_UR_SYMBOL, MIRROR_DR_SYMBOL, ANTI_TANK_UP_SYMBOL,
                               ANTI_TANK_DOWN_SYMBOL, ANTI_TANK_RIGHT_SYMBOL, ANTI_TANK_DESTROYED_SYMBOL]
                       }

    def __init__(self, params):
        """
        Build a LaserTank map instance from the given grid data.
        :param params: dictionary containing map state information
        """
        self.x_size = params.x_size
        self.y_size = params.y_size
        self.t_success_prob = params.t_success_prob
        self.t_error_prob = 1 - params.t_success_prob
        self.gamma = params.gamma
        self.epsilon = params.epsilon
        self.move_cost = params.move_cost
        self.collision_cost = params.collision_cost
        self.game_over_cost = params.game_over_cost
        self.goal_reward = params.goal_reward
        self.method = params.method
        self.benchmark = params.benchmark
        self.time_limit = params.time_limit
        self.initial_seed = params.initial_seed
        self.grid_data = params.grid_data
        self.player_x = params.player_x
        self.player_y = params.player_y
        self.player_heading = params.player_heading
        self.flag_x = params.flag_x
        self.flag_y = params.flag_y

        # extract player position and heading if none given
        if self.player_x is None and self.player_y is None and self.player_heading is None:
            found = False
            for i in range(self.y_size):
                row = self.grid_data[i]
                for j in range(self.x_size):
                    if row[j] == self.PLAYER_UP_SYMBOL or row[j] == self.PLAYER_DOWN_SYMBOL or \
                            row[j] == self.PLAYER_LEFT_SYMBOL or row[j] == self.PLAYER_RIGHT_SYMBOL:
                        found = True
                        self.player_x = j
                        self.player_y = i
                        self.player_heading = {self.PLAYER_UP_SYMBOL: self.UP,
                                               self.PLAYER_DOWN_SYMBOL: self.DOWN,
                                               self.PLAYER_LEFT_SYMBOL: self.LEFT,
                                               self.PLAYER_RIGHT_SYMBOL: self.RIGHT}[row[j]]
                        # replace the player symbol with land tile
                        row[j] = self.LAND_SYMBOL
                        break
                if found:
                    break
            if not found:
                raise Exception("LaserTank Map Error: Grid data does not contain player symbol")
        elif self.player_x is None or self.player_y is None or self.player_heading is None:
            raise Exception("LaserTank Map Error: Incomplete player coordinates given")

    @staticmethod
    def process_input_file(filename):
        """
        Process the given input file and create a new map instance based on the input file.
        :param filename: name of input file
        """
        f = open(filename, 'r')

        t_success_prob = -1
        gamma = None
        epsilon = None
        move_cost = None
        collision_cost = None
        game_over_cost = None
        goal_reward = None
        method = None
        benchmark = None
        time_limit = None
        initial_seed = None
        rows = []
        i = 0
        for line in f:
            # skip optimal steps and time limit
            if i == 0:
                t_success_prob = float(line.strip().split(' ')[1])
            elif i == 1:
                gamma = float(line.strip().split(' ')[1])
            elif i == 2:
                epsilon = float(line.strip().split(' ')[1])
            elif i == 3:
                move_cost = float(line.strip().split(' ')[1])
            elif i == 4:
                collision_cost = float(line.strip().split(' ')[1])
            elif i == 5:
                game_over_cost = float(line.strip().split(' ')[1])
            elif i == 6:
                goal_reward = float(line.strip().split(' ')[1])
            elif i == 7:
                method = line.strip().split(' ')[1]
            elif i == 8:
                benchmark = float(line.strip().split(' ')[1])
            elif i == 9:
                time_limit = int(line.strip().split(' ')[1])
            elif i == 10:
                initial_seed = int(line.strip().split(' ')[1])
            elif len(line.strip()) > 0:
                rows.append(list(line.strip()))
            i += 1

        f.close()

        row_len = len(rows[0])
        for row in rows:
            assert len(row) == row_len, "LaserTank Map Error: Mismatch in row length"

        num_rows = len(rows)

        tp_count = 0
        player_count = 0
        flag_count = 0
        player_x = -1
        player_y = -1
        player_heading = -1
        flag_x = -1
        flag_y = -1
        for i in range(len(rows)):
            for j in range(len(rows[i])):
                symbol = rows[i][j]
                if symbol == LaserTankMap.TELEPORT_SYMBOL:
                    tp_count += 1
                elif symbol == LaserTankMap.PLAYER_UP_SYMBOL or \
                        symbol == LaserTankMap.PLAYER_DOWN_SYMBOL or \
                        symbol == LaserTankMap.PLAYER_LEFT_SYMBOL or \
                        symbol == LaserTankMap.PLAYER_RIGHT_SYMBOL:
                    player_count += 1
                    player_x = j
                    player_y = i
                    if symbol == LaserTankMap.PLAYER_UP_SYMBOL:
                        player_heading = LaserTankMap.UP
                    elif symbol == LaserTankMap.PLAYER_DOWN_SYMBOL:
                        player_heading = LaserTankMap.DOWN
                    elif symbol == LaserTankMap.PLAYER_LEFT_SYMBOL:
                        player_heading = LaserTankMap.LEFT
                    else:
                        player_heading = LaserTankMap.RIGHT
                    # replace the player symbol with land tile
                    rows[i][j] = LaserTankMap.LAND_SYMBOL
                elif symbol == LaserTankMap.FLAG_SYMBOL:
                    flag_count += 1
                    flag_x = j
                    flag_y = i
                elif symbol not in LaserTankMap.VALID_SYMBOLS:
                    raise Exception("LaserTank Map Error: Invalid symbol in input file")
        assert tp_count % 2 == 0, "LaserTank Map Error: Unmatched teleport symbol"
        assert tp_count < 3, "LaserTank Map Error: Too many teleport symbols"
        assert player_count > 0, "LaserTank Map Error: No initial player position given"
        assert player_count < 2, "LaserTank Map Error: More than one initial player position given"
        assert flag_count > 0, "LaserTank Map Error: No goal position given"
        assert flag_count < 2, "LaserTank Map Error: More than one goal position given"

        params = DotDict({'x_size': row_len,
                          'y_size': num_rows,
                          't_success_prob': t_success_prob,
                          'gamma': gamma,
                          'epsilon': epsilon,
                          'move_cost': move_cost,
                          'collision_cost': collision_cost,
                          'game_over_cost': game_over_cost,
                          'goal_reward': goal_reward,
                          'method': method,
                          'benchmark': benchmark,
                          'time_limit': time_limit,
                          'initial_seed': initial_seed,
                          'grid_data': rows,
                          'player_x': player_x,
                          'player_y': player_y,
                          'player_heading': player_heading,
                          'flag_x': flag_x,
                          'flag_y': flag_y})
        return LaserTankMap(params)

    def make_clone(self):
        """
        Create a deep copied clone of this LaserTankMap instance.
        :return: deep copy of the LaserTankMap
        """
        params = DotDict({'x_size': self.x_size,
                          'y_size': self.y_size,
                          't_success_prob': self.t_success_prob,
                          'gamma': self.gamma,
                          'epsilon': self.epsilon,
                          'move_cost': self.move_cost,
                          'collision_cost': self.collision_cost,
                          'game_over_cost': self.game_over_cost,
                          'goal_reward': self.goal_reward,
                          'method': self.method,
                          'benchmark': self.benchmark,
                          'time_limit': self.time_limit,
                          'initial_seed': self.initial_seed,
                          'grid_data': [row[:] for row in self.grid_data],
                          'player_x': self.player_x,
                          'player_y': self.player_y,
                          'player_heading': self.player_heading,
                          'flag_x': self.flag_x,
                          'flag_y': self.flag_y})
        return LaserTankMap(params)

    def apply_move(self, move, seed=None):
        """
        Apply a player move to the map.
        :param move: self.MOVE_FORWARD, self.TURN_LEFT, self.TURN_RIGHT or self.SHOOT_LASER
        :param seed: random seed used by tester to generate consistent random outcomes
        :return: the reward received for performing this action (a real number)
        """
        if seed is not None:
            random.seed(a=seed)

        if move == self.MOVE_FORWARD:
            # get coordinates for next cell
            if self.player_heading == self.UP:
                r = random.random()
                if r < self.t_success_prob:
                    next_y = self.player_y - 1
                    next_x = self.player_x
                elif r < self.t_success_prob + (self.t_error_prob * (1 / 5)):
                    next_y = self.player_y - 1
                    next_x = self.player_x - 1
                elif r < self.t_success_prob + (self.t_error_prob * (2 / 5)):
                    next_y = self.player_y - 1
                    next_x = self.player_x + 1
                elif r < self.t_success_prob + (self.t_error_prob * (3 / 5)):
                    next_y = self.player_y
                    next_x = self.player_x - 1
                elif r < self.t_success_prob + (self.t_error_prob * (4 / 5)):
                    next_y = self.player_y
                    next_x = self.player_x + 1
                else:
                    next_y = self.player_y
                    next_x = self.player_x

                if next_y < 0:
                    return self.collision_cost
            elif self.player_heading == self.DOWN:
                r = random.random()
                if r < self.t_success_prob:
                    next_y = self.player_y + 1
                    next_x = self.player_x
                elif r < self.t_success_prob + (self.t_error_prob * (1 / 5)):
                    next_y = self.player_y + 1
                    next_x = self.player_x - 1
                elif r < self.t_success_prob + (self.t_error_prob * (2 / 5)):
                    next_y = self.player_y + 1
                    next_x = self.player_x + 1
                elif r < self.t_success_prob + (self.t_error_prob * (3 / 5)):
                    next_y = self.player_y
                    next_x = self.player_x - 1
                elif r < self.t_success_prob + (self.t_error_prob * (4 / 5)):
                    next_y = self.player_y
                    next_x = self.player_x + 1
                else:
                    next_y = self.player_y
                    next_x = self.player_x

                if next_y >= self.y_size:
                    return self.collision_cost
            elif self.player_heading == self.LEFT:
                r = random.random()
                if r < self.t_success_prob:
                    next_y = self.player_y
                    next_x = self.player_x - 1
                elif r < self.t_success_prob + (self.t_error_prob * (1 / 5)):
                    next_y = self.player_y - 1
                    next_x = self.player_x - 1
                elif r < self.t_success_prob + (self.t_error_prob * (2 / 5)):
                    next_y = self.player_y + 1
                    next_x = self.player_x - 1
                elif r < self.t_success_prob + (self.t_error_prob * (3 / 5)):
                    next_y = self.player_y - 1
                    next_x = self.player_x
                elif r < self.t_success_prob + (self.t_error_prob * (4 / 5)):
                    next_y = self.player_y + 1
                    next_x = self.player_x
                else:
                    next_y = self.player_y
                    next_x = self.player_x

                if next_x < 0:
                    return self.collision_cost
            else:
                r = random.random()
                if r < self.t_success_prob:
                    next_y = self.player_y
                    next_x = self.player_x + 1
                elif r < self.t_success_prob + (self.t_error_prob * (1 / 5)):
                    next_y = self.player_y - 1
                    next_x = self.player_x + 1
                elif r < self.t_success_prob + (self.t_error_prob * (2 / 5)):
                    next_y = self.player_y + 1
                    next_x = self.player_x + 1
                elif r < self.t_success_prob + (self.t_error_prob * (3 / 5)):
                    next_y = self.player_y - 1
                    next_x = self.player_x
                elif r < self.t_success_prob + (self.t_error_prob * (4 / 5)):
                    next_y = self.player_y + 1
                    next_x = self.player_x
                else:
                    next_y = self.player_y
                    next_x = self.player_x

                if next_x >= self.x_size:
                    return self.collision_cost

            # handle special tile types
            if self.grid_data[next_y][next_x] == self.ICE_SYMBOL:
                # handle ice tile - slide until first non-ice tile or blocked
                if self.player_heading == self.UP:
                    for i in range(next_y, -1, -1):
                        if self.grid_data[i][next_x] != self.ICE_SYMBOL:
                            if self.grid_data[i][next_x] == self.WATER_SYMBOL:
                                # slide into water - game over
                                return self.game_over_cost
                            elif self.cell_is_blocked(i, next_x):
                                # if blocked, stop on last ice cell
                                next_y = i + 1
                                break
                            else:
                                next_y = i
                                break
                elif self.player_heading == self.DOWN:
                    for i in range(next_y, self.y_size):
                        if self.grid_data[i][next_x] != self.ICE_SYMBOL:
                            if self.grid_data[i][next_x] == self.WATER_SYMBOL:
                                # slide into water - game over
                                return self.game_over_cost
                            elif self.cell_is_blocked(i, next_x):
                                # if blocked, stop on last ice cell
                                next_y = i - 1
                                break
                            else:
                                next_y = i
                                break
                elif self.player_heading == self.LEFT:
                    for i in range(next_x, -1, -1):
                        if self.grid_data[next_y][i] != self.ICE_SYMBOL:
                            if self.grid_data[next_y][i] == self.WATER_SYMBOL:
                                # slide into water - game over
                                return self.game_over_cost
                            elif self.cell_is_blocked(next_y, i):
                                # if blocked, stop on last ice cell
                                next_x = i + 1
                                break
                            else:
                                next_x = i
                                break
                else:
                    for i in range(next_x, self.x_size):
                        if self.grid_data[next_y][i] != self.ICE_SYMBOL:
                            if self.grid_data[next_y][i] == self.WATER_SYMBOL:
                                # slide into water - game over
                                return self.game_over_cost
                            elif self.cell_is_blocked(next_y, i):
                                # if blocked, stop on last ice cell
                                next_x = i - 1
                                break
                            else:
                                next_x = i
                                break
            if self.grid_data[next_y][next_x] == self.TELEPORT_SYMBOL:
                # handle teleport - find the other teleporter
                tpy, tpx = (None, None)
                for i in range(self.y_size):
                    for j in range(self.x_size):
                        if self.grid_data[i][j] == self.TELEPORT_SYMBOL and i != next_y and j != next_x:
                            tpy, tpx = (i, j)
                            break
                    if tpy is not None:
                        break
                if tpy is None:
                    raise Exception("LaserTank Map Error: Unmatched teleport symbol")
                next_y, next_x = (tpy, tpx)
            else:
                # if not ice or teleport, perform collision check
                if self.cell_is_blocked(next_y, next_x):
                    return self.collision_cost

            # check for game over conditions
            if self.cell_is_game_over(next_y, next_x):
                return self.game_over_cost

            # no collision and no game over - update player position
            self.player_y = next_y
            self.player_x = next_x

        elif move == self.TURN_LEFT:
            # no collision or game over possible
            if self.player_heading == self.UP:
                self.player_heading = self.LEFT
            elif self.player_heading == self.DOWN:
                self.player_heading = self.RIGHT
            elif self.player_heading == self.LEFT:
                self.player_heading = self.DOWN
            else:
                self.player_heading = self.UP

        elif move == self.TURN_RIGHT:
            # no collision or game over possible
            if self.player_heading == self.UP:
                self.player_heading = self.RIGHT
            elif self.player_heading == self.DOWN:
                self.player_heading = self.LEFT
            elif self.player_heading == self.LEFT:
                self.player_heading = self.UP
            else:
                self.player_heading = self.DOWN

        elif move == self.SHOOT_LASER:
            # set laser direction
            if self.player_heading == self.UP:
                heading = self.UP
                dy, dx = (-1, 0)
            elif self.player_heading == self.DOWN:
                heading = self.DOWN
                dy, dx = (1, 0)
            elif self.player_heading == self.LEFT:
                heading = self.LEFT
                dy, dx = (0, -1)
            else:
                heading = self.RIGHT
                dy, dx = (0, 1)

            # loop until laser blocking object reached
            ly, lx = (self.player_y, self.player_x)
            while True:
                ly += dy
                lx += dx

                # handle boundary and immovable obstacles
                if ly < 0 or ly >= self.y_size or \
                        lx < 0 or lx >= self.x_size or \
                        self.grid_data[ly][lx] == self.OBSTACLE_SYMBOL:
                    # laser stopped without effect
                    return self.move_cost

                # handle movable objects
                elif self.cell_is_laser_movable(ly, lx, heading):
                    # check if tile can be moved without collision
                    if self.cell_is_blocked(ly + dy, lx + dx) or \
                            self.grid_data[ly + dy][lx + dx] == self.ICE_SYMBOL or \
                            self.grid_data[ly + dy][lx + dx] == self.TELEPORT_SYMBOL or \
                            self.grid_data[ly + dy][lx + dx] == self.FLAG_SYMBOL or \
                            (ly + dy == self.player_y and lx + dx == self.player_x):
                        # tile cannot be moved
                        return self.move_cost
                    else:
                        old_symbol = self.grid_data[ly][lx]
                        self.grid_data[ly][lx] = self.LAND_SYMBOL
                        if self.grid_data[ly + dy][lx + dx] == self.WATER_SYMBOL:
                            # if new bridge position is water, convert to land tile
                            if old_symbol == self.BRIDGE_SYMBOL:
                                self.grid_data[ly + dy][lx + dx] = self.LAND_SYMBOL
                            # otherwise, do not replace the old symbol
                        else:
                            # otherwise, move the tile forward
                            self.grid_data[ly + dy][lx + dx] = old_symbol
                        break

                # handle bricks
                elif self.grid_data[ly][lx] == self.BRICK_SYMBOL:
                    # remove brick, replace with land
                    self.grid_data[ly][lx] = self.LAND_SYMBOL
                    break

                # handle facing anti-tanks
                elif (self.grid_data[ly][lx] == self.ANTI_TANK_UP_SYMBOL and heading == self.DOWN) or \
                        (self.grid_data[ly][lx] == self.ANTI_TANK_DOWN_SYMBOL and heading == self.UP) or \
                        (self.grid_data[ly][lx] == self.ANTI_TANK_LEFT_SYMBOL and heading == self.RIGHT) or \
                        (self.grid_data[ly][lx] == self.ANTI_TANK_RIGHT_SYMBOL and heading == self.LEFT):
                    # mark anti-tank as destroyed
                    self.grid_data[ly][lx] = self.ANTI_TANK_DESTROYED_SYMBOL
                    break

                # handle player laser collision
                elif ly == self.player_y and lx == self.player_x:
                    return self.game_over_cost

                # handle facing mirrors
                elif (self.grid_data[ly][lx] == self.MIRROR_UL_SYMBOL and heading == self.RIGHT) or \
                        (self.grid_data[ly][lx] == self.MIRROR_UR_SYMBOL and heading == self.LEFT):
                    # new direction is up
                    dy, dx = (-1, 0)
                    heading = self.UP
                elif (self.grid_data[ly][lx] == self.MIRROR_DL_SYMBOL and heading == self.RIGHT) or \
                        (self.grid_data[ly][lx] == self.MIRROR_DR_SYMBOL and heading == self.LEFT):
                    # new direction is down
                    dy, dx = (1, 0)
                    heading = self.DOWN
                elif (self.grid_data[ly][lx] == self.MIRROR_UL_SYMBOL and heading == self.DOWN) or \
                        (self.grid_data[ly][lx] == self.MIRROR_DL_SYMBOL and heading == self.UP):
                    # new direction is left
                    dy, dx = (0, -1)
                    heading = self.LEFT
                elif (self.grid_data[ly][lx] == self.MIRROR_UR_SYMBOL and heading == self.DOWN) or \
                        (self.grid_data[ly][lx] == self.MIRROR_DR_SYMBOL and heading == self.UP):
                    # new direction is right
                    dy, dx = (0, 1)
                    heading = self.RIGHT
                # do not terminate laser on facing mirror - keep looping

            # check for game over condition after effect of laser
            if self.cell_is_game_over(self.player_y, self.player_x):
                return self.game_over_cost

        if self.grid_data[self.player_y][self.player_x] == self.FLAG_SYMBOL:
            return self.goal_reward
        else:
            return self.move_cost

    def render(self):
        """
        Render the map's current state to terminal
        """
        for r in range(self.y_size):
            line = ''
            for c in range(self.x_size):
                glyph = self.MAP_GLYPH_TABLE[self.grid_data[r][c]]

                # overwrite with player
                if r == self.player_y and c == self.player_x:
                    glyph = self.PLAYER_GLYPH_TABLE[self.player_heading]

                line += glyph
            print(line)

        print('\n' * (20 - self.y_size))

    def is_finished(self):
        """
        Check if the finish condition (player at flag) has been reached
        :return: True if player at flag, False otherwise
        """
        if self.grid_data[self.player_y][self.player_x] == self.FLAG_SYMBOL:
            return True
        else:
            return False

    def cell_is_blocked(self, y, x):
        """
        Check if the cell with the given coordinates is blocked (i.e. movement
        to this cell is not possible)
        :param y: y coord
        :param x: x coord
        :return: True if blocked, False otherwise
        """
        symbol = self.grid_data[y][x]
        # collision: obstacle, bridge, mirror (all types), anti-tank (all types)
        if symbol == self.OBSTACLE_SYMBOL or symbol == self.BRIDGE_SYMBOL or symbol == self.BRICK_SYMBOL or \
                symbol == self.MIRROR_UL_SYMBOL or symbol == self.MIRROR_UR_SYMBOL or \
                symbol == self.MIRROR_DL_SYMBOL or symbol == self.MIRROR_DR_SYMBOL or \
                symbol == self.ANTI_TANK_UP_SYMBOL or symbol == self.ANTI_TANK_DOWN_SYMBOL or \
                symbol == self.ANTI_TANK_LEFT_SYMBOL or symbol == self.ANTI_TANK_RIGHT_SYMBOL or \
                symbol == self.ANTI_TANK_DESTROYED_SYMBOL:
            return True
        return False

    def cell_is_game_over(self, y, x):
        """
        Check if the cell with the given coordinates will result in game
        over.
        :param y: y coord
        :param x: x coord
        :return: True if blocked, False otherwise
        """
        # check for water
        if self.grid_data[y][x] == self.WATER_SYMBOL:
            return True

        # check for anti-tank
        # up direction
        for i in range(y, -1, -1):
            if self.grid_data[i][x] == self.ANTI_TANK_DOWN_SYMBOL:
                return True
            # if blocked, can stop checking for anti-tank
            if self.cell_is_blocked(i, x):
                break

        # down direction
        for i in range(y, self.y_size):
            if self.grid_data[i][x] == self.ANTI_TANK_UP_SYMBOL:
                return True
            # if blocked, can stop checking for anti-tank
            if self.cell_is_blocked(i, x):
                break

        # left direction
        for i in range(x, -1, -1):
            if self.grid_data[y][i] == self.ANTI_TANK_RIGHT_SYMBOL:
                return True
            # if blocked, can stop checking for anti-tank
            if self.cell_is_blocked(y, i):
                break

        # right direction
        for i in range(x, self.x_size):
            if self.grid_data[y][i] == self.ANTI_TANK_LEFT_SYMBOL:
                return True
            # if blocked, can stop checking for anti-tank
            if self.cell_is_blocked(y, i):
                break

        # no water or anti-tank danger
        return False

    def cell_is_laser_movable(self, y, x, heading):
        """
        Check if the tile at coordinated (y, x) is movable by a laser with the given heading.
        :param y: y coord
        :param x: x coord
        :param heading: laser direction
        :return: True is movable, false otherwise
        """
        return self.grid_data[y][x] in self.MOVABLE_SYMBOLS[heading]

    def __eq__(self, other):
        """
        Compare player position and every tile.
        :param other: other laserTankMap instance
        :return: self == other
        """
        return self.player_x == other.player_x and self.player_y == other.player_y and \
               self.player_heading == other.player_heading and self.grid_data == other.grid_data

    def __hash__(self):
        """
        Flatten map, add player position, convert to tuple and hash
        :return: hash(self)
        """
        return hash((self.player_x, self.player_y, self.player_heading) +
                    tuple([item for sublist in self.grid_data for item in sublist]))


class DotDict(dict):
    """
    This class provides dot.notation access to dictionary attributes.

    This class is used to represent the params object in the LaserTankMap constructor.

    You may use this class in your code if you wish.
    """
    __getattr__ = dict.get
    __setattr__ = dict.__setitem__
    __delattr__ = dict.__delitem__

    def __deepcopy__(self, memodict=None):
        return DotDict({key: value for key, value in dict(self).items()})


def main(arglist):
    """
    Run a playable game of LaserTank using the given filename as the map file.
    :param arglist: map file name
    """
    try:
        import msvcrt

        def windows_getchar():
            return msvcrt.getch().decode('utf-8')

        getchar = windows_getchar
    except ImportError:
        import tty
        import termios

        def unix_getchar():
            fd = sys.stdin.fileno()
            old_settings = termios.tcgetattr(fd)
            try:
                tty.setraw(sys.stdin.fileno())
                ch = sys.stdin.read(1)
            finally:
                termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)
            return ch

        getchar = unix_getchar

    if len(arglist) != 1:
        print("Running this file directly launches a playable game of LaserTank based on the given map file.")
        print("Usage: laser_tank.py [map_file_name]")
        return

    print("Use W to move forward, A and D to turn. Use (spacebar) to shoot. Press Q to quit." +
          "Press R to restart the map.")

    map_inst = LaserTankMap.process_input_file(arglist[0])
    map_inst.render()

    steps = 0

    while True:
        char = getchar()

        if char == 'q':
            return

        if char == 'r':
            map_inst = LaserTankMap.process_input_file(arglist[0])
            map_inst.render()

            steps = 0

        if char in ['w', 'a', 'd', ' ']:
            steps += 1
            a = {'w': LaserTankMap.MOVE_FORWARD,
                 'a': LaserTankMap.TURN_LEFT,
                 'd': LaserTankMap.TURN_RIGHT,
                 ' ': LaserTankMap.SHOOT_LASER}[char]

            ret = map_inst.apply_move(a)
            map_inst.render()

            if ret == map_inst.game_over_cost:
                print('Game Over!')
                return

            if map_inst.is_finished():
                print("Puzzle solved in " + str(steps) + " steps!")
                return


if __name__ == '__main__':
    main(sys.argv[1:])







