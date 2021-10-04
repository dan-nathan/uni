import sys
import platform
import time
import traceback
from laser_tank import LaserTankMap
from solver import Solver

# automatic timeout handling will only be performed on Unix
if platform.system() != 'Windows':
    import signal
    WINDOWS = False
else:
    WINDOWS = True

"""
Policy visualiser script.

Use this script to visualise the policy your code produces. You should avoid
modifying this file directly.

COMP3702 2020 Assignment 1 Support Code

Last updated by njc 13/10/20
"""

DEBUG_MODE = False      # set to True to disable time limit checks
TOLERANCE = 0.01

ACTION_LOOKUP = {LaserTankMap.MOVE_FORWARD: 0,
                 LaserTankMap.TURN_LEFT: 1,
                 LaserTankMap.TURN_RIGHT: 2,
                 LaserTankMap.SHOOT_LASER: 3}


class TimeOutException(Exception):
    pass


def timeout_handler(signum, frame):
    raise TimeOutException


def main(arglist):
    """
    Visualise the policy your code produces for the given map file.
    :param arglist: [map_file_name, mode]
    """

    if len(arglist) != 1:
        print("Running this file visualises the path your code produces for the given map file. ")
        print("Usage: policy_visualiser.py [map_file_name]")
        return

    input_file = arglist[0]
    game_map = LaserTankMap.process_input_file(input_file)
    solver = Solver(game_map)

    mark = 0

    # do offline computation
    if game_map.method == 'vi':
        if not WINDOWS:
            signal.signal(signal.SIGALRM, timeout_handler)
            signal.alarm(game_map.time_limit + 1)
        try:
            solver.run_value_iteration()
        except TimeOutException:
            print("/!\\ Ran overtime during run_value_iteration( )")
            sys.exit(mark)
        except:
            traceback.print_exc()
            print("/!\\ Crash occurred during run_value_iteration( )")
            sys.exit(mark)
        if not WINDOWS:
            signal.alarm(0)
    elif game_map.method == 'pi':
        if not WINDOWS:
            signal.signal(signal.SIGALRM, timeout_handler)
            signal.alarm(game_map.time_limit + 1)
        try:
            solver.run_policy_iteration()
        except TimeOutException:
            print("/!\\ Ran overtime during run_policy_iteration( )")
            sys.exit(mark)
        except:
            traceback.print_exc()
            print("/!\\ Crash occurred during run_policy_iteration( )")
            sys.exit(mark)
        if not WINDOWS:
            signal.alarm(0)

    # simulate an episode (using de-randomised transitions) and compare total reward to benchmark
    total_reward = 0
    state = game_map.make_clone()
    state.render()
    seed = hash(input_file)     # use file name as RNG seed
    for i in range(100):
        new_seed = seed + 1
        if not WINDOWS:
            signal.signal(signal.SIGALRM, timeout_handler)
            if game_map.method == 'mcts':
                signal.alarm(game_map.time_limit + 1)
            else:
                signal.alarm(1)
        try:
            if game_map.method == 'mcts':
                action = solver.get_mcts_policy(state)
            else:
                action = solver.get_offline_policy(state)
        except TimeOutException:
            if game_map.method == 'mcts':
                print("/!\\ Ran overtime during get_mcts_policy( )")
            else:
                print("/!\\ Ran overtime during get_offline_policy( )")
            sys.exit(mark)
        except:
            traceback.print_exc()
            if game_map.method == 'mcts':
                print("/!\\ get_mcts_policy( ) caused crash during evaluation")
            else:
                print("/!\\ get_offline_policy( ) caused crash during evaluation")
            sys.exit(mark)
        if not WINDOWS and not DEBUG_MODE:
            signal.alarm(0)
        r = state.apply_move(action, new_seed)
        print(action)
        state.render()
        total_reward += r
        if r == game_map.goal_reward or r == game_map.game_over_cost:
            break
        seed = new_seed

        time.sleep(0.5)


if __name__ == '__main__':
    main(sys.argv[1:])

