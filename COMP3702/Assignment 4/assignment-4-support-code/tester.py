import sys
import platform
import traceback
import math
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

Use this script to test whether your implementation matches the episode reward benchmark. To ensure operation is
consistent with Gradescope, you should avoid modifying this file directly.

COMP3702 2020 Assignment 4 Support Code

Last updated by njc 17/10/20
"""

DEBUG_MODE = True      # set to True to disable time limit checks
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
        print("Running this file tests whether your code produces an approximately optimal policy for the given map "
              "file.")
        print("Usage: tester.py [map file name]")
        return

    input_file = arglist[0]
    game_map = LaserTankMap.process_input_file(input_file)
    simulator = game_map.make_clone()
    solver = Solver()

    mark = 0

    # do offline computation
    if game_map.method == 'q-learning':
        if not WINDOWS and not DEBUG_MODE:
            signal.signal(signal.SIGALRM, timeout_handler)
            signal.alarm(game_map.time_limit + 1)
        try:
            solver.train_q_learning(simulator)
        except TimeOutException:
            print("/!\\ Ran overtime during train_q_learning( )")
            sys.exit(OVERTIME)
        except:
            traceback.print_exc()
            print("/!\\ Crash occurred during train_q_learning( )")
            sys.exit(CRASH)
        if not WINDOWS and not DEBUG_MODE:
            signal.alarm(0)
    elif game_map.method == 'sarsa':
        if not WINDOWS and not DEBUG_MODE:
            signal.signal(signal.SIGALRM, timeout_handler)
            signal.alarm(game_map.time_limit + 1)
        try:
            solver.train_sarsa(simulator)
        except TimeOutException:
            print("/!\\ Ran overtime during train_sarsa( )")
            sys.exit(OVERTIME)
        except:
            traceback.print_exc()
            print("/!\\ Crash occurred during train_sarsa( )")
            sys.exit(CRASH)
        if not WINDOWS and not DEBUG_MODE:
            signal.alarm(0)

    # simulate an episode (using de-randomised transitions) and compare total reward to benchmark
    total_reward = 0
    num_trials = 50
    max_steps = 60
    for _ in range(num_trials):
        state = game_map.make_clone()
        for i in range(max_steps):
            if not WINDOWS and not DEBUG_MODE:
                signal.signal(signal.SIGALRM, timeout_handler)
                signal.alarm(1)
            try:
                action = solver.get_policy(state)
            except TimeOutException:
                print("/!\\ Ran overtime during get_policy( )")
                sys.exit(mark)
            except:
                traceback.print_exc()
                print("/!\\ get_policy( ) caused crash during evaluation")
                sys.exit(mark)
            if not WINDOWS and not DEBUG_MODE:
                signal.alarm(0)
            r, f = state.apply_move(action)
            total_reward += r
            if f:
                break
    total_reward /= num_trials

    # compute score based on how close episode reward is to optimum
    print(f"Avg Episode Reward = {str(total_reward)}, Benchmark = {str(game_map.benchmark)}")
    diff = game_map.benchmark - total_reward    # amount by which benchmark score is better
    if diff < 0:
        diff = 0
    if diff > 20:
        diff = 20
    below = math.ceil(diff / 2)
    mark = 10 - below

    if below == 0:
        print("Testcase passed, policy matches or exceeds benchmark")
    elif mark > 0:
        print(f"Testcase passed, {below} marks below solution quality benchmark")
    sys.exit(mark)


if __name__ == '__main__':
    main(sys.argv[1:])



