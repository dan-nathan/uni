#ifndef UTIL_H
#define UTIL_H
#include "util.h"
#endif

#define NUM_ARGS 5
#define NO_PLAYERS 1
#define PLAYER_NO 2
#define POSITION 3
#define HAND_SIZE 4
#define HIGHEST true
#define LOWEST false
#define CARD_MESSAGE_SIZE 3

// A enum giving meaningful names to errors/exit statuses
typedef enum {
    NO_ERROR = 0,
    INCORRECT_NO_ARGS = 1,
    INVALID_PLAYERS = 2,
    INVALID_POSITION = 3,
    INVALID_THRESHOLD = 4,
    INVALID_HAND_SIZE = 5,
    INVALID_MESSAGE = 6,
    UNEXPECTED_EOF = 7
} Status;

typedef struct PlayerInfo PlayerInfo;

// A struct representing info about the player, and the game
struct PlayerInfo {
    int (*strategy)(PlayerInfo*, GameInfo*);
    Card* hand;
    int noPlayers;
    int playerNo;
    int threshold;
    int handSize;
};

Status display_error_message(Status status);

Status setup_player(int argc, char** argv, PlayerInfo* playerInfo);

Status generate_hand(PlayerInfo* playerInfo, char* line);

void end_turn(PlayerInfo* playerInfo, GameInfo* gameInfo);

int search_suit(PlayerInfo* playerInfo, char suit, bool highLow);

int search_hand(PlayerInfo* playerInfo, const char* suitOrder, bool highLow);

void take_turn(PlayerInfo* playerInfo, GameInfo* gameInfo);

Status handle_card_played(PlayerInfo* playerInfo, char* line,
        GameInfo* gameInfo);

Status handle_new_round(PlayerInfo* playerInfo, char* line,
        GameInfo* gameInfo);

Status process_input(PlayerInfo* playerInfo, char* line,
        GameInfo* gameInfo);

Status play_game(PlayerInfo* playerInfo);
