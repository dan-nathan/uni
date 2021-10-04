import sys
import platform
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
Tester script.

Use this script to test whether your output files are valid solutions. You should avoid modifying this file directly.

COMP3702 2020 Assignment 3 Support Code

Last updated by njc 13/10/20
"""

DEBUG_MODE = False      # set to True to disable time limit checks
TOLERANCE = 0.01

CRASH = 255
OVERTIME = 254

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
    Test whether the given output file is a valid solution to the given map file.

    This test script uses a 'trapdoor function' approach to comparing your computed values and policy to a reference
    solution without revealing the reference solution - 3 different results are computed based on your values and policy
    and compared to the results computed for the reference solution.

    :param arglist: [map file name]
    """
    if len(arglist) != 1:
        print("Running this file tests whether your code produces an optimal policy for the given map file.")
        print("Usage: tester.py [map file name]")
        return

    input_file = arglist[0]
    game_map = LaserTankMap.process_input_file(input_file)
    solver = Solver(game_map)

    mark = 0

    # do offline computation
    if game_map.method == 'vi':
        if not WINDOWS and not DEBUG_MODE:
            signal.signal(signal.SIGALRM, timeout_handler)
            signal.alarm(game_map.time_limit + 1)
        try:
            solver.run_value_iteration()
        except TimeOutException:
            print("/!\\ Ran overtime during run_value_iteration( )")
            sys.exit(OVERTIME)
        except:
            traceback.print_exc()
            print("/!\\ Crash occurred during run_value_iteration( )")
            sys.exit(CRASH)
        if not WINDOWS and not DEBUG_MODE:
            signal.alarm(0)
    elif game_map.method == 'pi':
        if not WINDOWS and not DEBUG_MODE:
            signal.signal(signal.SIGALRM, timeout_handler)
            signal.alarm(game_map.time_limit + 1)
        try:
            solver.run_policy_iteration()
        except TimeOutException:
            print("/!\\ Ran overtime during run_policy_iteration( )")
            sys.exit(OVERTIME)
        except:
            traceback.print_exc()
            print("/!\\ Crash occurred during run_policy_iteration( )")
            sys.exit(CRASH)
        if not WINDOWS and not DEBUG_MODE:
            signal.alarm(0)

    # simulate an episode (using de-randomised transitions) and compare total reward to benchmark
    total_reward = 0
    state = game_map.make_clone()
    seed = game_map.initial_seed
    for i in range(int((game_map.benchmark / game_map.move_cost) * 2)):
        new_seed = seed + 1
        if not WINDOWS and not DEBUG_MODE:
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
        total_reward += r
        if r == game_map.goal_reward or r == game_map.game_over_cost:
            break
        seed = new_seed

    # compute score based on how close episode reward is to optimum
    print(f"Episode Reward = {str(total_reward)}, Benchmark = {str(game_map.benchmark)}")
    mark = 10
    below = 0
    for i in range(1, 11):
        if total_reward > (game_map.benchmark * (1 + (i / 20))):
            break
        else:
            mark -= 1
            below += 1

    if below == 0:
        print("Testcase passed, policy optimum")
    elif mark > 0:
        print(f"Testcase passed, {below} points below optimum")
    sys.exit(mark)


if __name__ == '__main__':
    main(sys.argv[1:])



