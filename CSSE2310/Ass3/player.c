#ifndef PLAYER_H
#define PLAYER_H
#include "player.h"
#endif

// Displays an error message given an exit status
// Parameters: status - the exit status
// Returns: the status passed into the function
Status display_error_message(Status status) {
    const char* errorMessages[] = {"",
            "Usage: player players myid threshold handsize\n",
            "Invalid players\n",
            "Invalid position\n",
            "Invalid threshold\n",
            "Invalid hand size\n",
            "Invalid message\n",
            "EOF\n"};
    fprintf(stderr, errorMessages[status]);
    return status;
}

// Sets up the player given its command line arguments
// Parameters: argc - the number of command line arguments this player was set
//         up with, argv - the command line arguments, playerInfo - info about
//         the player
// Returns: NO_ERROR if the player was successfully set up, or the exit status
//         if arguments were invalid
Status setup_player(int argc, char** argv, PlayerInfo* playerInfo) {
    if (argc != NUM_ARGS) {
        return display_error_message(INCORRECT_NO_ARGS);
    }

    playerInfo->noPlayers = string_to_int(argv[NO_PLAYERS]);
    // If it isn't a number, string_to_int will return -1, covering this case
    if (playerInfo->noPlayers < 2) {
        return display_error_message(INVALID_PLAYERS);
    }

    playerInfo->playerNo = string_to_int(argv[PLAYER_NO]);
    if (playerInfo->playerNo < 0
            || playerInfo->playerNo >= playerInfo->noPlayers) {
        return display_error_message(INVALID_POSITION);
    }

    playerInfo->threshold = string_to_int(argv[POSITION]);
    if (playerInfo->threshold < 2) {
        return display_error_message(INVALID_THRESHOLD);
    }
    playerInfo->handSize = string_to_int(argv[HAND_SIZE]);
    if (playerInfo->handSize < 1) {
        return display_error_message(INVALID_HAND_SIZE);
    }

    playerInfo->hand = malloc(sizeof(Card) * playerInfo->handSize);

    fputc('@', stdout);
    fflush(stdout);
    return NO_ERROR;
}

// Stores the player's hand given to it by the hub
// Parameters: playerInfo - a struct for the hand to be stored in, line - the
//         input from the hub to construct a hand from
// Returns: NO_ERROR if the hand was generated, or INVALID_MESSAGE exit status
//         if line is formatted incorrectly
Status generate_hand(PlayerInfo* playerInfo, char* line) {
    const char handText[5] = "HAND";
    // If there isn't enouch characters for "HAND" and a number of cards
    if (strlen(line) < strlen(handText) + 1) {
        return INVALID_MESSAGE;
    }

    // If the first four characters aren't "HAND"
    for (int i = 0; i < strlen(handText); i++) {
        if (line[i] != handText[i]) {
            return INVALID_MESSAGE;
        }
    }

    // Chop "HAND" off the start of the line
    char* handInfo = line + strlen(handText);
    int handSize = atoi(handInfo);
    if (handSize != playerInfo->handSize) {
        return INVALID_MESSAGE;
    }

    int handSizeDigits = no_digits(handSize);
    // Chop the number of cards off the start of the line (starts with ,SR)
    char* cardsText = handInfo + handSizeDigits;
    if (strlen(cardsText) != 3 * handSize) {
        return INVALID_MESSAGE;
    }

    for (int i = 0; i < handSize; i++) {
        if (*cardsText != ',') {
            return INVALID_MESSAGE;
        }
        // Create an array consisting of suit and rank
        char cardText[CARD_MESSAGE_SIZE];
        for (int j = 0; j < CARD_MESSAGE_SIZE - 1; j++) {
            cardText[j] = cardsText[j + 1];
        }
        cardText[2] = '\0';
        // Convert text to card
        Card card = read_card(cardText);
        // If the card isn't formatted correctly
        if (card.suit == INVALID_SUIT) {
            return INVALID_MESSAGE;
        }
        playerInfo->hand[i] = card;
        // Move pointer to next card
        cardsText += CARD_MESSAGE_SIZE;
    }
    return NO_ERROR;
}

// Prints the lead player, then the cards played this turn in order
// Parameters: playerInfo - info about the player, gameInfo - the game state
void end_turn(PlayerInfo* playerInfo, GameInfo* gameInfo) {
    char leadSuit = gameInfo->playedThisRound[gameInfo->leadPlayer].suit;
    int dCardsPlayed = 0;
    int winningPlayer = gameInfo->leadPlayer;
    // Print the lead player
    fprintf(stderr, "Lead player=%d:", gameInfo->leadPlayer);

    // Loops through cards played, starting from lead player
    for (int i = gameInfo->leadPlayer; i < playerInfo->noPlayers; i++) {
        Card cardPlayed = gameInfo->playedThisRound[i];
        char* cardText = write_card(cardPlayed, true);
        // Print the card
        fprintf(stderr, " %s", cardText);
        free(cardText);
        // If the card beats the currently best found card, set this player as
        // the current winning player
        if (cardPlayed.suit == leadSuit && cardPlayed.rank >
                gameInfo->playedThisRound[winningPlayer].rank) {
            winningPlayer = i;
        }
        if (cardPlayed.suit == 'D') {
            dCardsPlayed++;
        }
    }
    // Loop back to player 0 if necessary
    for (int i = 0; i < gameInfo->leadPlayer; i++) {
        Card cardPlayed = gameInfo->playedThisRound[i];
        char* cardText = write_card(cardPlayed, true);
        fprintf(stderr, " %s", cardText);
        free(cardText);
        if (cardPlayed.suit == leadSuit && cardPlayed.rank >
                gameInfo->playedThisRound[winningPlayer].rank) {
            winningPlayer = i;
        }
        if (cardPlayed.suit == 'D') {
            dCardsPlayed++;
        }
    }

    fprintf(stderr, "\n");
    gameInfo->dCardsWon[winningPlayer] += dCardsPlayed;
}

// Find a card of a particular suit in the player's hand
// Parameters: playerInfo - info about the player, suit - the suit to search
//         for, highLow - whether searching for highest or lowest of the suit
// Returns: The index of the card in hand, -1 if none of the suit were found
int search_suit(PlayerInfo* playerInfo, char suit, bool highLow) {
    Card* hand = playerInfo->hand;
    int handSize = playerInfo->handSize;
    // Set index to invalid value by default
    int chosenCard = -1;
    char rank = 0;
    for (int i = 0; i < handSize; i++) {
        if (hand[i].suit == suit) {
            // Choose any card of the suit if it's the first to be found
            if (rank == 0) {
                chosenCard = i;
                rank = hand[i].rank;
            } else if (highLow == HIGHEST && hand[i].rank > rank) {
                chosenCard = i;
                rank = hand[i].rank;
            } else if (highLow == LOWEST && hand[i].rank < rank) {
                chosenCard = i;
                rank = hand[i].rank;
            }
        }
    }
    return chosenCard;
}

// Find a card in the player's hand
// Parameters: playerInfo - info about the player, suitOrder - the order of
//         preference for suits, highLow - whether searching for highest or
//         lowest of the suit
// Returns: The index of the card in hand, -1 if none of the suits were found
int search_hand(PlayerInfo* playerInfo, const char* suitOrder, bool highLow) {
    int chosenCard = -1;
    // Loop through each suit, and search for a card from it
    for (int i = 0; i < strlen(suitOrder); i++) {
        char suit = suitOrder[i];
        chosenCard = search_suit(playerInfo, suit, highLow);
        // If a card of the suit has been found, don't bother checking the
        // other suits
        if (chosenCard != -1) {
            break;
        }
    }
    return chosenCard;
}

// Takes a turn for this player, telling the hub what they played
// Parameters: playerInfo - info about the player, gameInfo - the game state
void take_turn(PlayerInfo* playerInfo, GameInfo* gameInfo) {
    // Get the strategy (depending on whether this is Alice or Bob)
    int (*strategy)(PlayerInfo*, GameInfo*) = playerInfo->strategy;
    // Find the card to play
    int cardNo = strategy(playerInfo, gameInfo);
    Card card = playerInfo->hand[cardNo];

    // Record the card in gameInfo
    gameInfo->playedThisRound[playerInfo->playerNo] = card;
    // Remove the card from hand
    reorder_hand(playerInfo->hand, cardNo, playerInfo->handSize);
    playerInfo->handSize--;

    // Message the hub the card played
    printf("PLAY%c%c\n", card.suit, card.rank);
    fflush(stdout);
    gameInfo->lastPlayer = playerInfo->playerNo;
}

// Handles another player playing a card, updating gameInfo
// Parameters: playerInfo - info about the player, line - the text representing
//         the move, gameInfo - the game state
// Returns: INVALID_MESSAGE exit status if line has incorrect format, or isn't
//         the expected player, NO_ERROR otherwise
Status handle_card_played(PlayerInfo* playerInfo, char* line,
        GameInfo* gameInfo) {
    // Return an error if a card is played before the start of the 1st round
    if (gameInfo->leadPlayer == -1) {
        return INVALID_MESSAGE;
    }
    // Get the player no (text before the comma)
    char* subString = strtok(line, ",");
    int playerNo = string_to_int(subString);
    // Return an error if the player number is not valid
    if (playerNo == -1 || playerNo >= playerInfo->noPlayers) {
        return INVALID_MESSAGE;
    }
    // If this isn't the first player this round
    if (gameInfo->lastPlayer != -1) {
        // If the player is out of order throw an error
        if (playerNo == 0
                && gameInfo->lastPlayer != playerInfo->noPlayers - 1) {
            return INVALID_MESSAGE;
        } else if (playerNo != 0 && playerNo != gameInfo->lastPlayer + 1) {
            return INVALID_MESSAGE;
        }
    } else if (playerNo != gameInfo->leadPlayer) {
        // The first play this round must come from the lead player
        return INVALID_MESSAGE;
    }
    // Return an error if a player plays a card for the 2nd time in one round
    if (gameInfo->playedThisRound[playerNo].suit != INVALID_SUIT) {
        return INVALID_MESSAGE;
    }
    subString = strtok(NULL, ",");
    Card card = read_card(subString);
    // Throw an error if the card couldn't be read properly
    if (card.suit == INVALID_SUIT) {
        return INVALID_MESSAGE;
    }
    gameInfo->playedThisRound[playerNo] = card;
    gameInfo->lastPlayer = playerNo;
    if (playerNo + 1 == playerInfo->playerNo || (playerInfo->playerNo == 0
            && playerNo == playerInfo->noPlayers - 1)) {
        if (playerInfo->playerNo != gameInfo->leadPlayer) {
            take_turn(playerInfo, gameInfo);
        }
    }
    // If that was the last play of the round
    if (gameInfo->lastPlayer == gameInfo->leadPlayer - 1
            || (gameInfo->leadPlayer == 0
            && gameInfo->lastPlayer == playerInfo->noPlayers - 1)) {
        end_turn(playerInfo, gameInfo);
    }
    return NO_ERROR;
}

// Handles the start of a new round, updating gameInfo
// Parameters: playerInfo - info about the player, line - the text representing
//         the new lead player, gameInfo - the game state
// Returns: INVALID_MESSAGE exit status if line has incorrect format, isn't
//         a valid player, or the last round isn't over, NO_ERROR otherwise
Status handle_new_round(PlayerInfo* playerInfo, char* line,
        GameInfo* gameInfo) {
    int leadPlayer = string_to_int(line);
    // If the number at the end is invalid
    if (leadPlayer == -1 || leadPlayer >= playerInfo->noPlayers) {
        return INVALID_MESSAGE;
    }
    if (gameInfo->leadPlayer != -1) {
        // If any player hasn't played a card from last round, a NEWROUND
        // message is invalid
        for (int i = 0; i < playerInfo->noPlayers; i++) {
            Card card = gameInfo->playedThisRound[i];
            if (card.suit == INVALID_SUIT) {
                return INVALID_MESSAGE;
            }
        }
    }
    gameInfo->leadPlayer = leadPlayer;
    gameInfo->lastPlayer = -1;

    // Set all the cards played this round to invalid cards as placeholders
    for (int i = 0; i < playerInfo->noPlayers; i++) {
        gameInfo->playedThisRound[i].suit = INVALID_SUIT;
        gameInfo->playedThisRound[i].rank = INVALID_RANK;
    }
    // Take a turn if this player is the lead player
    if (leadPlayer == playerInfo->playerNo) {
        take_turn(playerInfo, gameInfo);
    }
    return NO_ERROR;
}

// Given a line of input, decides what to do with it
// Parameters: playerInfo - info about the player, line - the text received,
//         gameInfo - the game state
// Returns: INVALID_MESSAGE exit status if line has incorrect format, NO_ERROR
//         otherwise
Status process_input(PlayerInfo* playerInfo, char* line,
        GameInfo* gameInfo) {
    const char playedText[7] = "PLAYED";
    // Valid input is always at least 6 characters (for "PLAYED")
    if (strlen(line) < strlen(playedText)) {
        return INVALID_MESSAGE;
    }
    bool inputIsPlayed = true;
    // If the first six characters aren't "PLAYED"
    for (int i = 0; i < strlen(playedText); i++) {
        if (line[i] != playedText[i]) {
            inputIsPlayed = false;
            break;
        }
    }
    if (inputIsPlayed) {
        // Chop off "PLAYED" from the start of the string before running
        // handle_card_played
        return handle_card_played(playerInfo, line + strlen(playedText),
                gameInfo);
    } else {
        // Message is either NEWROUND or invalid
        const char newRoundText[9] = "NEWROUND";
        if (strlen(line) < strlen(newRoundText)) {
            return INVALID_MESSAGE;
        }
        // If the first 8 characters aren't "NEWROUND"
        for (int i = 0; i < strlen(newRoundText); i++) {
            if (line[i] != newRoundText[i]) {
                return INVALID_MESSAGE;
            }
        }
        return handle_new_round(playerInfo, line + strlen(newRoundText),
                gameInfo);
    }
}

// Starts reading input and giving output accordingly
// Parameters: playerInfo - info about the player
// Returns: The exit status from playing the game
Status play_game(PlayerInfo* playerInfo) {
    // Get input for hand
    char* line = read_line(stdin);
    if (line == NULL) {
        return UNEXPECTED_EOF;
    }

    Status status = generate_hand(playerInfo, line);
    free(line);
    if (status != NO_ERROR) {
        return status;
    }

    // Set up info about game state
    GameInfo gameInfo;
    gameInfo.playedThisRound = malloc(sizeof(Card) * playerInfo->noPlayers);
    gameInfo.leadPlayer = -1;
    gameInfo.lastPlayer = -1;
    // All players start off with 0 D Cards Won
    gameInfo.dCardsWon = calloc(playerInfo->noPlayers, sizeof(int));

    // Get input until "GAMEOVER" is read from hub
    while (true) {
        line = read_line(stdin);
        if (line == NULL) {
            free(line);
            return UNEXPECTED_EOF;
        }
        if (strcmp(line, "GAMEOVER") == 0) {
            free(line);
            break;
        }
        status = process_input(playerInfo, line, &gameInfo);
        free(line);
        if (status != NO_ERROR) {
            return status;
        }
    }

    free(playerInfo->hand);
    free(gameInfo.playedThisRound);
    free(gameInfo.dCardsWon);
    return NO_ERROR;
}
