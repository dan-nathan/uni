#ifndef UTIL_H
#define UTIL_H
#include "util.h"
#endif

#define READ_PIPE 0
#define WRITE_PIPE 1

// A enum giving meaningful names to errors/exit statuses
typedef enum {
    NO_ERROR = 0,
    INCORRECT_NO_ARGS = 1,
    INVALID_THRESHOLD = 2,
    DECK_ERROR = 3,
    NOT_ENOUGH_CARDS = 4,
    CANNOT_START_PLAYER = 5,
    PLAYER_EOF = 6,
    INVALID_MESSAGE = 7,
    INVALID_CARD_CHOICE = 8,
    RECEIVED_SIGHUP = 9
} Status;

typedef struct HubInfo HubInfo;

// A struct that contains info that the hub needs to run the game
struct HubInfo {
    int threshold;
    int noPlayers;
    Card* deck;
    int deckSize;
    FILE*** pipes;
    int handSize;
    Card** hands;
    int leadPlayer;
    int* roundsWon;
    pid_t* childPIDs;
};

// A flag to determine if SIGHUP has been received
bool receivedSighup;

// Displays an error message given an exit status
// Parameters: The exit status
// Returns: The exit status
Status display_error_message(Status status) {
    const char* errorMessages[] = {"",
            "Usage: 2310hub deck threshold player0 {player1}\n",
            "Invalid threshold\n",
            "Deck error\n",
            "Not enough cards\n",
            "Player error\n",
            "Player EOF\n",
            "Invalid message\n",
            "Invalid card choice\n",
            "Ended due to signal\n"};
    fprintf(stderr, errorMessages[status]);
    return status;
}

// Sets the SIGHUP flag
// Parameters: signum - the type of signal
void handle_sighup(int signum) {
    receivedSighup = true;
}

// Sets up a handler for SIGHUP
void setup_sighup_handler(void) {
    struct sigaction sig;
    sig.sa_handler = handle_sighup;
    sig.sa_flags = SA_RESTART;
    // valgrind doesn't like sa_mask being uninitialised
    sigemptyset(&sig.sa_mask);
    sigaction(SIGHUP, &sig, 0);
}

// Handles the end of a game, including scoring
// Parameters: hubInfo - info about the hub, gameInfo - the game state
void end_game(HubInfo* hubInfo, GameInfo* gameInfo) {
    // Calculate and print the score for each player
    for (int i = 0; i < hubInfo->noPlayers; i++) {
        int score = hubInfo->roundsWon[i];
        if (gameInfo->dCardsWon[i] < hubInfo->threshold) {
            score -= gameInfo->dCardsWon[i];
        } else {
            score += gameInfo->dCardsWon[i];
        }
        printf("%d:%d", i, score);
        // Don't print a trailing space
        if (i != hubInfo->noPlayers - 1) {
            printf(" ");
        }
    }
    printf("\n");

    // Free memory
    free(gameInfo->dCardsWon);
    free(gameInfo->playedThisRound);
    free(hubInfo->roundsWon);
    free(hubInfo->hands);
    free(hubInfo->deck);
    for (int i = 0; i < hubInfo->noPlayers; i++) {
        for (int j = 0; j < 2; j++) {
            fclose(hubInfo->pipes[i][j]);
        }
        free(hubInfo->pipes[i]);
    }
    free(hubInfo->pipes);
    free(hubInfo->childPIDs);
}

// Handles the end of a round
// Parameters: hubInfo - info about the hub, gameInfo - the game state
void end_round(HubInfo* hubInfo, GameInfo* gameInfo) {
    char leadSuit = gameInfo->playedThisRound[gameInfo->leadPlayer].suit;
    int dCardsPlayed = 0;
    int winningPlayer = gameInfo->leadPlayer;
    // Print the text for cards
    printf("Cards=");
    // Loops through cards played, starting from lead player
    for (int i = gameInfo->leadPlayer; i < hubInfo->noPlayers; i++) {
        // Don't print the space directly after the equals sign
        if (i != gameInfo->leadPlayer) {
            printf(" ");
        }
        Card cardPlayed = gameInfo->playedThisRound[i];
        char* cardText = write_card(cardPlayed, true);
        // Print the card
        printf("%s", cardText);
        free(cardText);
        // If card is better than the previous best, set this player as winner
        if (cardPlayed.suit == leadSuit && cardPlayed.rank >
                gameInfo->playedThisRound[winningPlayer].rank) {
            winningPlayer = i;
        }
        if (cardPlayed.suit == 'D') {
            dCardsPlayed++;
        }
        // Set the cards played this round back to placeholders
        gameInfo->playedThisRound[i].suit = INVALID_SUIT;
        gameInfo->playedThisRound[i].suit = INVALID_RANK;
    }
    // Loop back to player 0 if necessary
    for (int i = 0; i < gameInfo->leadPlayer; i++) {
        Card cardPlayed = gameInfo->playedThisRound[i];
        char* cardText = write_card(cardPlayed, true);
        printf(" %s", cardText);
        free(cardText);
        if (cardPlayed.suit == leadSuit && cardPlayed.rank >
                gameInfo->playedThisRound[winningPlayer].rank) {
            winningPlayer = i;
        }
        if (cardPlayed.suit == 'D') {
            dCardsPlayed++;
        }
        gameInfo->playedThisRound[i].suit = INVALID_SUIT;
        gameInfo->playedThisRound[i].suit = INVALID_RANK;
    }
    printf("\n");
    // Records the player that won, and the amount of D cards won
    gameInfo->dCardsWon[winningPlayer] += dCardsPlayed;
    hubInfo->roundsWon[winningPlayer]++;
    gameInfo->leadPlayer = winningPlayer;
}

// Gives a message to a player process
// Parameters: playerNo - the player to give it to, message - the message to
//         send, hubInfo - info about the hub
void give_message(int playerNo, char* message, HubInfo* hubInfo) {
    // Print the message into the relevant pipe, then flush the pipe
    fputs(message, hubInfo->pipes[playerNo][WRITE_PIPE]);
    fflush(hubInfo->pipes[playerNo][WRITE_PIPE]);
}

// Gives all players a GAMEOVER message
// Parameters: hubInfo - info about the hub
void shutdown_players(HubInfo* hubInfo) {
    for (int i = 0; i < hubInfo->noPlayers; i++) {
        give_message(i, "GAMEOVER\n", hubInfo);
    }
}

// Gives all players a NEWROUND message
// Parameters: hubInfo - info about the hub, playerNo - the lead player
void new_round_message(HubInfo* hubInfo, int playerNo) {
    char message[20];
    sprintf(message, "NEWROUND%d\n", playerNo);
    for (int i = 0; i < hubInfo->noPlayers; i++) {
        give_message(i, message, hubInfo);
    }
}

// Gives all players a PLAYED message
// Parameters: playerNo - the player that played, card - the card played,
//         hubInfo - info about the hub
void send_played_card_message(int playerNo, Card card, HubInfo* hubInfo) {
    // The size a message can be is limited by the max value of an integer. 20
    // should suffice
    char message[20];
    sprintf(message, "PLAYED%d,", playerNo);
    // Represent the card as text, and append to the string
    char* cardText = write_card(card, false);
    strcat(message, cardText);
    strcat(message, "\n");

    // Give the message to the players
    for (int i = 0; i < hubInfo->noPlayers; i++) {
        // Don't give the message to the player that played
        if (i != playerNo) {
            give_message(i, message, hubInfo);
        }
    }
    free(cardText);
}

// Checks a card played is a legal move, and plays it if so
// Parameters: hubInfo - info about the hub, gameInfo - the game state,
//         playerNo - the player that played the card, card - the card played
// Returns: whether or not the move was legal
bool validate_card_choice(HubInfo* hubInfo, GameInfo* gameInfo,
        int playerNo, Card card) {
    Card leadCard = gameInfo->playedThisRound[gameInfo->leadPlayer];
    bool hasMatchingSuit = false;
    bool cardInHand = false;
    for (int i = 0; i < hubInfo->handSize; i++) {
        // Don't play 2 cards in 1 turn
        if (card_matches(card, hubInfo->hands[playerNo][i]) && !cardInHand) {
            cardInHand = true;
            // Remove the card from hand
            reorder_hand(hubInfo->hands[playerNo], i, hubInfo->handSize);
        }
        if (hubInfo->hands[playerNo][i].suit == leadCard.suit) {
            hasMatchingSuit = true;
        }
        if (cardInHand && hasMatchingSuit) {
            break;
        }
    }

    // Don't bother checking the suit if it is the first card this round
    if (gameInfo->leadPlayer == playerNo) {
        hasMatchingSuit = false;
    }

    // If the player had a matching suit, but played a different suit
    if (hasMatchingSuit && card.suit != leadCard.suit) {
        return false;
    }
    return cardInHand;
}

// Gets a message from a player
// Parameters: hubInfo - info about the hub, gameInfo - the game state,
//         playerNo - the player to get info from
// Returns RECEIVED_SIGHUP if the SIGHUP flag is set, INVALID_MESSAGE if the
//         message is formatted incorrectly, INVALID_CARD_CHOICE if the move
//         made is illegal
Status get_input(HubInfo* hubInfo, GameInfo* gameInfo, int playerNo) {
    if (receivedSighup) {
        // Loop until pid is this pid (indicating end of array)
        for (int i = 0; ; i++) {
            pid_t pid = hubInfo->childPIDs[i];
            if (pid == getpid()) {
                break;
            }
            kill(pid, SIGKILL);
        }
        return RECEIVED_SIGHUP;
    }

    // Get a message from the player
    char* line = read_line(hubInfo->pipes[playerNo][READ_PIPE]);
    fflush(hubInfo->pipes[playerNo][READ_PIPE]);
    // IF EOF was encountered
    if (line == 0) {
        free(line);
        shutdown_players(hubInfo);
        return PLAYER_EOF;
    }
    const char playText[5] = "PLAY";
    if (strlen(line) < strlen(playText)) {
        shutdown_players(hubInfo);
        return INVALID_MESSAGE;
    }
    // If the first four characters aren't "PLAY"
    for (int i = 0; i < strlen(playText); i++) {
        if (line[i] != playText[i]) {
            shutdown_players(hubInfo);
            return INVALID_MESSAGE;
        }
    }
    // Chop "PLAY" from the start of the string
    Card cardPlayed = read_card(line + 4);
    free(line);
    // If the card isn't a valid card, the message is invlaid
    if (cardPlayed.suit == INVALID_SUIT) {
        return INVALID_MESSAGE;
    }
    gameInfo->playedThisRound[playerNo] = cardPlayed;
    // Check if the move was legal
    if (!validate_card_choice(hubInfo, gameInfo, playerNo, cardPlayed)) {
        shutdown_players(hubInfo);
        return INVALID_CARD_CHOICE;
    }
    // Tell the other players what was played
    send_played_card_message(playerNo, cardPlayed, hubInfo);
    return NO_ERROR;
}

// Plays a round of the game
// Parameters: hubInfo - info about the hub, gameInfo - the game state
// Returns: NO_ERROR if there are no errors, or the error code otherwise
Status play_round(HubInfo* hubInfo, GameInfo* gameInfo) {
    // Send a NEWROUND message to all players
    new_round_message(hubInfo, gameInfo->leadPlayer);

    // Sequentially read a message from each player, and process it
    for (int playerNo = gameInfo->leadPlayer; playerNo < hubInfo->noPlayers;
            playerNo++) {
        Status status = get_input(hubInfo, gameInfo, playerNo);
        if (status != NO_ERROR) {
            return status;
        }
    }
    for (int playerNo = 0; playerNo < gameInfo->leadPlayer; playerNo++) {
        Status status = get_input(hubInfo, gameInfo, playerNo);
        if (status != NO_ERROR) {
            return status;
        }
    }

    // Handle the end of a round
    end_round(hubInfo, gameInfo);
    return NO_ERROR;
}

// Plays rounds until the game is over
// Parameters: hubInfo - info about the hub
// Returns: NO_ERROR if there are no errors, or the error code otherwise
Status play_game(HubInfo* hubInfo) {
    // Set up a struct for game state
    GameInfo gameInfo;
    gameInfo.leadPlayer = 0;
    gameInfo.lastPlayer = -1;
    gameInfo.dCardsWon = calloc(hubInfo->noPlayers, sizeof(int));
    gameInfo.playedThisRound = malloc(sizeof(Card) * hubInfo->noPlayers);
    // Set all player's rounds won to 0
    hubInfo->roundsWon = calloc(hubInfo->noPlayers, sizeof(int));
    // Initialise the cards played this round with placeholders
    for (int i = 0; i < hubInfo->noPlayers; i++) {
        gameInfo.playedThisRound[i].suit = INVALID_SUIT;
        gameInfo.playedThisRound[i].rank = INVALID_RANK;
    }

    // Play rounds until the players are out of cards
    while (hubInfo->handSize > 0) {
        printf("Lead player=%d\n", gameInfo.leadPlayer);
        Status status = play_round(hubInfo, &gameInfo);
        if (status != NO_ERROR) {
            return status;
        }
        hubInfo->handSize--;
    }

    // Send GAMEOVER to the players
    shutdown_players(hubInfo);
    // Handle scoring
    end_game(hubInfo, &gameInfo);
    return NO_ERROR;
}

// Creates a deck from a file
// Parameters: hubInfo - info about the hub,
//         deckFileName - the name of the deck file
// Returns: DECK_ERROR if the file is formatted incorrectly, NO_ERROR otherwise
Status read_deck(HubInfo* hubInfo, char* deckFileName) {
    FILE* deckFile = fopen(deckFileName, "r");
    char* line;
    if (deckFile == 0) {
        return DECK_ERROR;
    }
    line = read_line(deckFile);
    // If EOF is encountered
    if (line == 0) {
        fclose(deckFile);
        return DECK_ERROR;
    }

    hubInfo->deckSize = string_to_int(line);
    free(line);
    // If the first line isn't a positive integer
    if (hubInfo->deckSize < 0) {
        return DECK_ERROR;
    }
    hubInfo->deck = malloc(sizeof(Card) * hubInfo->deckSize);
    const int validLineLength = 2;
    // Read each line
    for (int i = 0; i < hubInfo->deckSize; i++) {
        line = read_line(deckFile);
        // If the end of file was reached too early
        if (line == 0) {
            return DECK_ERROR;
        }
        // If the line is the wrong length
        if (strlen(line) != validLineLength) {
            return DECK_ERROR;
        }
        Card card = read_card(line);
        free(line);
        // If the line isn't a valid card
        if (card.suit == INVALID_SUIT) {
            return DECK_ERROR;
        }
        hubInfo->deck[i] = card;
    }

    line = read_line(deckFile);
    // if there are more cards than the 1st line indicates
    if (line != 0) {
        free(line);
        return DECK_ERROR;
    }
    free(line);
    fclose(deckFile);
    return NO_ERROR;
}

// Assigns a hand to a player
// Parameters: hubInfo - info about the hub,
//         playerNo - the player to give a hand to
void assign_hand(HubInfo* hubInfo, int playerNo) {
    Card* hand = hubInfo->deck + (playerNo * hubInfo->handSize);
    hubInfo->hands[playerNo] = hand;
    // 6 digits for HAND, \n, and \0, 3 for each card, and the number for hand
    // size
    int textSize = 6 + no_digits(hubInfo->handSize) + 3 * hubInfo->handSize;
    char* handMessage = malloc(sizeof(char) * textSize);
    sprintf(handMessage, "HAND%d", hubInfo->handSize);

    // Represent each card as a string, and append to the message
    for (int i = 0; i < hubInfo->handSize; i++) {
        char* cardString = write_card(hand[i], false);
        strcat(handMessage, ",");
        strcat(handMessage, cardString);
        free(cardString);
    }
    strcat(handMessage, "\n");
    give_message(playerNo, handMessage, hubInfo);
    free(handMessage);
}

// Creates a player's child process and sets up pipes for it
// Parameters: hubInfo - info about the hub, playerNo - the player to set up,
//         args - the command line arguments for the player
// Returns: CANNOT_START_PLAYER if they can't be set up, NO_ERROR otherwise
Status create_process(HubInfo* hubInfo, int playerNo, char* args[]) {
    int fdParentRead[2], fdParentWrite[2];
    // If piping fails
    if (pipe(fdParentRead) < 0 || pipe(fdParentWrite) < 0) {
        return CANNOT_START_PLAYER;
    }

    pid_t childPID = fork();
    if (!childPID) { // Child
        // Close the reading end of the pipe child will use to write
        close(fdParentRead[0]);
        // Close the writing end of the pipe child will use to read
        close(fdParentWrite[1]);
        // Set child to use stdin and stdout for pipes
        dup2(fdParentWrite[0], STDIN_FILENO);
        dup2(fdParentRead[1], STDOUT_FILENO);

        // Send child stderr to output to nowhere
        int errFile = open("/dev/null", O_WRONLY);
        dup2(errFile, STDERR_FILENO);

        // Make the child run the player program
        execvp(args[0], args);
        // Exit if exec fails. Parent will realise when trying to read @
        exit(1);
    } else { // Parent
        hubInfo->childPIDs[playerNo] = childPID;
        // Close write end of read pipe
        close(fdParentRead[1]);
        // Close read end of write pipe
        close(fdParentWrite[0]);
        // Wrap the fd's in FILE pointers, and store in hubInfo
        FILE* readPipe = fdopen(fdParentRead[0], "r");
        FILE* writePipe = fdopen(fdParentWrite[1], "w");
        hubInfo->pipes[playerNo][READ_PIPE] = readPipe;
        hubInfo->pipes[playerNo][WRITE_PIPE] = writePipe;
        // Get @ from player
        int buffer = fgetc(hubInfo->pipes[playerNo][READ_PIPE]);
        fflush(hubInfo->pipes[playerNo][READ_PIPE]);
        if (buffer != '@') {
            return CANNOT_START_PLAYER;
        }
        assign_hand(hubInfo, playerNo);
    }
    return NO_ERROR;
}

// Creates a player
// Parameters: hubInfo - info about the hub, playerNo - the player to set up,
//         playerType - the name of the player program
// Returns: CANNOT_START_PLAYER if they can't be set up, NO_ERROR otherwise
Status create_player(HubInfo* hubInfo, int playerNo, char* playerType) {
    char* args[6];
    for (int i = 1; i < 5; i++) {
        // No valid numeric argument should be more than 11 characters
        args[i] = malloc(sizeof(char) * 12);
    }
    args[0] = playerType;
    sprintf(args[1], "%d", hubInfo->noPlayers);
    sprintf(args[2], "%d", playerNo);
    sprintf(args[3], "%d", hubInfo->threshold);
    sprintf(args[4], "%d", hubInfo->handSize);
    // Add NULL to end of args vector
    args[5] = 0;
    Status status = create_process(hubInfo, playerNo, args);
    if (status != NO_ERROR) {
        return status;
    }
    for (int i = 1; i < 5; i++) {
        free(args[i]);
    }
    return NO_ERROR;
}

// Creates all players
// Parameters: hubInfo - info about the hub,
//         players - the name of the player programs
// Returns: CANNOT_START_PLAYER if they can't be set up, NO_ERROR otherwise
Status create_players(HubInfo* hubInfo, char** players) {
    // Store pids in global variable so that SIGHUP handler can use them
    hubInfo->childPIDs = malloc(sizeof(pid_t) * (hubInfo->noPlayers + 1));
    // pipes[playerNo][read/write] is format of array
    hubInfo->pipes = malloc(sizeof(FILE**) * hubInfo->noPlayers);
    for (int i = 0; i < hubInfo->noPlayers; i++) {
        hubInfo->pipes[i] = malloc(sizeof(FILE*) * 2);
        Status status = create_player(hubInfo, i, players[i]);
        if (status != NO_ERROR) {
            return status;
        }
    }

    // Put own pid as flag for the end of the array
    hubInfo->childPIDs[hubInfo->noPlayers] = getpid();
    return NO_ERROR;
}

int main(int argc, char** argv) {
    HubInfo hubInfo;
    receivedSighup = false;
    Status status;
    if (argc < 5) {
        return display_error_message(INCORRECT_NO_ARGS);
    }

    int threshold = string_to_int(argv[2]);
    if (threshold < 2) {
        return display_error_message(INVALID_THRESHOLD);
    }

    hubInfo.threshold = threshold;
    status = read_deck(&hubInfo, argv[1]);
    if (status != 0) {
        return display_error_message(status);
    }

    // The first 3 arguments are not players, the rest are
    hubInfo.noPlayers = argc - 3;
    if (hubInfo.deckSize < hubInfo.noPlayers) {
        return display_error_message(NOT_ENOUGH_CARDS);
    }

    hubInfo.handSize = hubInfo.deckSize / hubInfo.noPlayers;
    hubInfo.hands = malloc(sizeof(Card*) * hubInfo.noPlayers);
    setup_sighup_handler();
    // Skip the arguments that are not players
    status = create_players(&hubInfo, argv + 3);
    if (status != 0) {
        return display_error_message(status);
    }
    return display_error_message(play_game(&hubInfo));
}
