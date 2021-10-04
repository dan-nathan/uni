#ifndef STD
#define STD
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <unistd.h>
#include <signal.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <fcntl.h>
#endif

#define INVALID_SUIT '\0'
#define INVALID_RANK '\0'

typedef struct {
    char suit;
    char rank;
} Card;

// A struct representing info about what has happened so far this round
typedef struct {
    Card* playedThisRound;
    int leadPlayer;
    int* dCardsWon;
    int lastPlayer;
} GameInfo;

char* read_line(FILE* file);

int string_to_int(const char* text);

char* int_to_string(int num);

int no_digits(int num);

Card read_card(const char* text);

char* write_card(Card card, bool dot);

bool card_matches(Card a, Card b);

void reorder_hand(Card* hand, int chosenCard, int handSize);
