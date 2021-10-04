#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define NO_PLAYERS 2
#define HAND_SIZE 6
#define NO_ADJACENCIES 4
#define LEFT 0
#define RIGHT 1
#define UP 2
#define DOWN 3
#define THIS_POSITION 4
#define HUMAN 'h'

/* A struct that represents a card in the game. Contains variables for the
 * card's number and suit */
struct Card {
    int number;
    char suit;
};

/* A struct that contains all of the relevant info needed to save the game */
struct SaveInfo {
    int boardWidth;
    int boardHeight;
    int cardsDrawn;
    int playerNo;
    char deckFileName[80];
    struct Card** hands;
    struct Card** board;
};

int error(int errorCode, struct SaveInfo saveInfo, struct Card* deck,
        int freeBoard, int freeDeck, int freeHands);

int play_game(struct SaveInfo saveInfo, struct Card* deck, int deckSize,
        char* playerTypes, int shouldDraw); 

void end_game(struct SaveInfo saveInfo, struct Card* deck);

char* read_line(FILE* file);

int string_to_int(char* text, int length);

int* split_line(char* line, int argsRequired);

void free_board(struct Card** board, int height);

void free_hands(struct Card** hands);

struct Card* generate_deck(FILE* deckFile, int deckSize);

char* card_to_string(struct Card card, FILE* output);

void draw_board(FILE* file, int boardWidth, int boardHeight,
        struct Card** board);

void reorder_hand(struct Card* hand, int cardPlayed);

int* find_empty_square(int playerNo, int boardWidth, int boardHeight,
        struct Card** board);

int take_turn(struct SaveInfo saveInfo, char playerType, 
        struct Card drawnCard);

void ai_turn(struct SaveInfo saveInfo);

int human_turn(struct SaveInfo saveInfo);

int check_position_validity(struct SaveInfo saveInfo, int row, int column,
        int cardNo);

int check_save(struct SaveInfo saveInfo, char* line);

struct Card* adjacent_cards(int row, int column, int boardWidth,
        int boardHeight, struct Card** board);

int exists_adjacent(struct Card* cards);

void save_game(char* saveFileName, struct SaveInfo saveInfo);

int* calculate_scores(int boardWidth, int boardHeight, struct Card** board);

int traverse_path(int row, int column, int boardWidth, int boardHeight,
        struct Card** board, char initialSuit, int pathLength);

int get_max_score(int* scores);

/* All I can say about the length of this function is I tried my hardest and 
 * I'm sorry */
int main(int argc, char** argv) {
    /* Save all saveable game info in one struct */
    struct SaveInfo saveInfo;
    int deckSize, shouldDraw;
    char players[NO_PLAYERS];
    /* deckFileName must be const to work with fopen, so a buffer is used */
    struct Card* deck;
    /* If the number of args is invalid */
    if (argc != 6 && argc != 4) {
        return error(1, saveInfo, 0, 0, 0, 0);
    }
    /* Initialise hands. A card is referenced by 
     * hands[player][handPostition] */
    saveInfo.hands = (struct Card**)malloc(sizeof(struct Card*) * NO_PLAYERS);
    for (int i = 0; i < NO_PLAYERS; i++) {
        saveInfo.hands[i] = 
                (struct Card*)calloc(HAND_SIZE, sizeof(struct Card));
    }
    /* Start game normally */
    if (argc == 6) {
        saveInfo.boardWidth = string_to_int(argv[2], (int)strlen(argv[2]));
        saveInfo.boardHeight = string_to_int(argv[3], (int)strlen(argv[3]));
        /* Check validity of board dimensions and player types. If dimensions 
         * are not valid integers, they will be -1, which is caught here */
        if ((strcmp(argv[4], "a") != 0 && strcmp(argv[4], "h") != 0)
                || (strcmp(argv[5], "a") != 0 && strcmp(argv[5], "h") != 0)
                || saveInfo.boardWidth < 3 || saveInfo.boardWidth > 100 
                || saveInfo.boardHeight < 3 || saveInfo.boardHeight > 100) {
            return error(2, saveInfo, 0, 0, 0, 1);
        }
        players[0] = argv[4][0];
        players[1] = argv[5][0];
        saveInfo.cardsDrawn = 0;

        /* Attempt to open deckFileName */
        sprintf(saveInfo.deckFileName, "%s", argv[1]);
        FILE* deckFile = fopen(saveInfo.deckFileName, "r");
        if (deckFile == 0) {
            return error(3, saveInfo, 0, 0, 0, 1);
        }

        /* Initialise the deck*/
        char* line = read_line(deckFile);
        /* If the file is empty */
        if (line == 0) {
            free(line);
            return error(3, saveInfo, 0, 0, 1, 1);
        }
        deckSize = string_to_int(line, (int)strlen(line));
        free(line);
        deck = generate_deck(deckFile, deckSize);
        /* generateDeck will return a null pointer if the file was not 
         * formatted correctly */
        if (deck == 0) {
            return error(3, saveInfo, deck, 0, 1, 1);
        }

        /* Allocate memory for board */
        saveInfo.board = (struct Card**)malloc(sizeof(struct Card*) 
                * saveInfo.boardHeight);
        for (int i = 0; i < saveInfo.boardHeight; i++) {
            saveInfo.board[i] = (struct Card*)calloc(saveInfo.boardWidth, 
                    sizeof(struct Card));
        }

        for (int i = 0; i < NO_PLAYERS; i++) {
            /* Add 5 cards to the hand */
            for (int j = 0; j < HAND_SIZE - 1; j++) {
                saveInfo.hands[i][j] = deck[saveInfo.cardsDrawn++];
            }
        }
        saveInfo.playerNo = 0;
        shouldDraw = 1;
    /* Load from save */
    } else {
        /* Check that player types are valid */
        if ((strcmp(argv[2], "a") != 0 && strcmp(argv[2], "h") != 0)
                || (strcmp(argv[3], "a") != 0 && strcmp(argv[3], "h") != 0)) {
            return error(2, saveInfo, 0, 0, 0, 1);
        }
        players[0] = argv[2][0];
        players[1] = argv[3][0];

        /* Attempt to open save file */
        FILE* saveFile = fopen(argv[1], "r");
        if (saveFile == 0) {
            return error(4, saveInfo, 0, 0, 0, 1);
        }

        /* Attempt to read the first line of arguments. line == 0 if EOF is
         * reached (prematurely in this case) */
        char* line = read_line(saveFile);
        if (line == 0) {
            fclose(saveFile);
            return error(4, saveInfo, 0, 0, 0, 1);
        }
        int* args = split_line(line, 4);
        free(line);
        int incorrectFirstLine = 0;	
        if (args == 0) {
            /* Indicate there should be a save file parsing error if there 
             * isn't a deck file parsing error*/
            incorrectFirstLine = 1;
        }
        saveInfo.boardWidth = args[0];
        saveInfo.boardHeight = args[1];
        saveInfo.cardsDrawn = args[2] - 1;
        saveInfo.playerNo = args[3] - 1;
        free(args);
        /* Check if the arguments are invalid */
        if (saveInfo.boardWidth < 3 || saveInfo.boardWidth > 100 
                || saveInfo.boardHeight < 3 || saveInfo.boardHeight > 100 
                || saveInfo.playerNo < 0 || saveInfo.playerNo > 1
                || saveInfo.cardsDrawn < 10) {
            incorrectFirstLine = 1;
        }

        line = read_line(saveFile);
        if (line == 0) {
            fclose(saveFile);
            return error(4, saveInfo, 0, 0, 0, 1);
        }
        sprintf(saveInfo.deckFileName, "%s", line);
        free(line);
        /* Attempt to open deckFileName */
        FILE* deckFile = fopen(saveInfo.deckFileName, "r");
        if (deckFile == 0) {
            fclose(saveFile);
            return error(3, saveInfo, 0, 0, 0, 1);
        }
        /* Initialise the deck*/
        line = read_line(deckFile);
        if (line == 0) {
            fclose(saveFile);
            fclose(deckFile);
            return error(3, saveInfo, 0, 0, 0, 1);
        }
        deckSize = string_to_int(line, (int)strlen(line));
        free(line);
        deck = generate_deck(deckFile, deckSize);
        /* generateDeck will return a null pointer if the file was not 
         * formatted correctly */
        if (deck == 0) {
            fclose(saveFile);
            return error(3, saveInfo, 0, 0, 0, 1);
        }

        /* Unable to parse deckfile error has priority over unable to parse 
         * savefile, so it is checked first*/
        if (incorrectFirstLine || saveInfo.cardsDrawn > deckSize) {
            fclose(saveFile);
            return error(4, saveInfo, deck, 0, 1, 1);
        }

        /* Add cards to hand */
        for (int i = 0; i < NO_PLAYERS; i++) {
            line = read_line(saveFile);
            if (line == 0) {
                fclose(saveFile);
                return error(4, saveInfo, deck, 0, 1, 1);
            }
            /* Makes sure there is the right number of cards. If i == playerNo 
             * there should is an additional card */
            if (strlen(line) != 
                    2 * (HAND_SIZE - 1 + (i == saveInfo.playerNo))) {
                fclose(saveFile);
                return error(4, saveInfo, deck, 0, 1, 1);
            }
            for (int j = 0; j < HAND_SIZE - 1 + (i == saveInfo.playerNo); 
                    j++) {
                struct Card card;
                /* Subtract ASCII value of '0' to convert char to int */
                card.number = line[2 * j] - '0';
                card.suit = line[2 * j + 1];
                if (card.number < 1 || card.number > 9 || card.suit < 'A'
                        || card.suit > 'Z') {
                    fclose(saveFile);
                    return error(4, saveInfo, deck, 0, 1, 1);
                }
                saveInfo.hands[i][j] = card;
            }	
            free(line);
        }

        /* Allocate memory for board */
        saveInfo.board = (struct Card**)malloc(sizeof(struct Card*) 
                * saveInfo.boardHeight);
        for (int i = 0; i < saveInfo.boardHeight; i++) {
            saveInfo.board[i] = (struct Card*)calloc(saveInfo.boardWidth, 
                    sizeof(struct Card));
        }
        /* Populate board with cards */
        for (int row = 0; row < saveInfo.boardHeight; row++) {
            line = read_line(saveFile);
            if (line == 0) {
                fclose(saveFile);
                return error(4, saveInfo, deck, 1, 1, 1);
            }
            /* Check that the line is the right length*/
            if (strlen(line) != saveInfo.boardWidth * 2) {
                fclose(saveFile);
                return error(4, saveInfo, deck, 1, 1, 1);
            }
            for (int column = 0; column < saveInfo.boardWidth; column++) {
                /* If there is a card in this position */
                if (line[2 * column] != '*') {
                    struct Card card;
                    /* Subtract ASCII value of '0' to convert char to int */
                    card.number = line[2 * column] - '0';
                    card.suit = line[2 * column + 1];
                    if (card.number < 1 || card.number > 9 || card.suit < 'A'
                            || card.suit > 'Z') {
                        fclose(saveFile);
                        return error(4, saveInfo, deck, 1, 1, 1);
                    }

                    saveInfo.board[row][column] = card;
                }
            }
            free(line);	
        }
        line = read_line(saveFile);
        /* If the file has more lines it shouldn't */
        if (line != 0) {
            fclose(saveFile);
            return error(4, saveInfo, deck, 1, 1, 1);
        }
        fclose(saveFile);
        /* When loading from a save file, the card has already been drawn */
        shouldDraw = 0;
    }

    if (deckSize < 11) {
        return error(5, saveInfo, deck, 1, 1, 1);
    }
    /* Check if the board is full (playerNo is irrelevent for this function
     * call) */
    int* emptySpace = find_empty_square(0, saveInfo.boardWidth, 
            saveInfo.boardHeight, saveInfo.board);
    int valid = emptySpace[0];
    free(emptySpace);
    /* The board is full if valid is -1, and an arbitrary space has a card on 
     * it */
    if (valid == -1 && saveInfo.board[0][0].number != 0) {
        return error(6, saveInfo, deck, 1, 1, 1);
    }

    /* Play the game, returning an exit status of 7 if end of input */
    if (play_game(saveInfo, deck, deckSize, players, shouldDraw) == 7) {
        return 7;
    }
    /* Calculate score and free memory */
    end_game(saveInfo, deck);
    return 0;
}

/* Prints an error message and frees relevant memory 
 * Parameters: the type of error, board and hand info, deck, hands, and
 *         whether each of the board, hands and deck should be freed
 * Returns the error code it was parsed */
int error(int errorCode, struct SaveInfo saveInfo, struct Card* deck,
        int freeBoard, int freeDeck, int freeHands) {
    char errorMessage[80];
    switch (errorCode) {
        case 1:
            sprintf(errorMessage, "Usage: bark savefile p1type p2type\n");
            strcat(errorMessage, "bark deck width height p1type p2type");
            break;
        case 2:
            sprintf(errorMessage, "Incorrect arg types");
            break;
        case 3:
            sprintf(errorMessage, "Unable to parse deckfile");
            break;
        case 4:
            sprintf(errorMessage, "Unable to parse savefile");
            break;
        case 5:
            sprintf(errorMessage, "Short deck");
            break;
        case 6:
            sprintf(errorMessage, "Board full");
            break;
        case 7:
            sprintf(errorMessage, "End of input");
            break;
    }
    fprintf(stderr, "%s\n", errorMessage);

    /* Only free board, deck, hands if they have been malloced */
    if (freeBoard) {
        free_board(saveInfo.board, saveInfo.boardHeight);
    }
    if (freeDeck) {
        free(deck);
    }
    if (freeHands) {
        free_hands(saveInfo.hands);
    }
    /* Return errorCode back to main */
    return errorCode;
}

/* Handles the players taking turns 
 * Parameters: board and hand info, deck, deck size, whether each player is
 *         human or ai, whether or not a card should be drawn
 * Returns 0 if the game is still going, 1 if it has ended normally, -1 if it
 *         has ended due to an end of input error */
int play_game(struct SaveInfo saveInfo, struct Card* deck, int deckSize,
        char* playerTypes, int shouldDraw) {
    /* takeTurn returns 1 if the game is still going, and 0 if it should end
     * (exiting the loop) */
    do {
        /* End the game if the deck is empty */
        if (saveInfo.cardsDrawn == deckSize) {
            break;
        }
        struct Card drawnCard;
        if (shouldDraw == 0) {
            /* 'draw' the card that is already in the last position in their
            * hand */
            drawnCard = saveInfo.hands[saveInfo.playerNo][HAND_SIZE - 1];
            shouldDraw = 1;
        } else {
            drawnCard = deck[saveInfo.cardsDrawn];
        }

        /* Have the relevant player take a turn*/
        int gameOver = take_turn(saveInfo, playerTypes[saveInfo.playerNo],
                drawnCard);
        /* If end of input */
        if (gameOver == -1) {
            return error(7, saveInfo, deck, 1, 1, 1);
        } else if (gameOver == 1) {
            break;
        }

        saveInfo.cardsDrawn++;
        /* Change playerNo from 0 to 1 or vice versa */
        saveInfo.playerNo ^= 1;
    } while(1);
    return 0;
}

/* Handles a gameover with no errors (ie board is full or deck is empty) 
 * Parameters: board and hand info, deck */
void end_game(struct SaveInfo saveInfo, struct Card* deck) {
    /* Display the final board state */
    draw_board(stdout, saveInfo.boardWidth, saveInfo.boardHeight,
            saveInfo.board);

    int* scores = calculate_scores(saveInfo.boardWidth, saveInfo.boardHeight,
            saveInfo.board);
    printf("Player 1=%d Player 2=%d\n", scores[0], scores[1]);
    free(scores);

    /* Free memory for deck, board and hands */
    free_board(saveInfo.board, saveInfo.boardHeight);
    free_hands(saveInfo.hands);
    free(deck);
}

/* Reads a line from a file
 * Parameters: file
 * Returns the next line from the file */
char* read_line(FILE* file) {
    char* line = malloc(sizeof(char));
    int size = 0;
    while (1) {
        /* Read characters one by one and append them to the string until a
         * new line is encountered */
        int nextCharacter = fgetc(file);
        if (nextCharacter == '\n') {
            break;
        }
        /* Return a null pointer if EOF is encountered */
        if (nextCharacter == EOF) {
            free(line);
            return 0;
        }
        /* Resize array and put read character on the end */
        size++;
        line = (char*)realloc(line, (size + 1) * sizeof(char));
        line[size - 1] = nextCharacter;
    }
    line[size] = '\0';
    return line;
}

/* Converts a string to an int 
 * Parameters: the string, the length of the string
 * Returns the integer value, or -1 if there are invalid characters */
int string_to_int(char* text, int length) {
    for (int i = 0; i < length; i++) {
        if (text[i] < '0' || text[i] > '9') {
            return -1;
        }
    }
    return atoi(text);
}

/* Seperates a string of space seperated ints into an array of ints 
 * Parameters, the string, the number of ints expected 
 * Returns an array of ints if the string is fomatted correctly, or a null
 *         pointer otherwise */
int* split_line(char* line, int argsRequired) {
    int numArgs = 0;          
    int* args = (int*)malloc(sizeof(int) * argsRequired);                
    /* Split the string by spaces */            
    char* subString = strtok(line, " ");
    while (subString != NULL) {
        if (numArgs >= argsRequired) {                                
            /* Returns the null pointer if the format is not what is 
             * expected */
            return 0;
        }
        args[numArgs] = string_to_int(subString, (int)strlen(subString));
        numArgs++;
        /* Splits starting from the last split */
        subString = strtok(NULL, " ");
    }
    if (numArgs < argsRequired) {                        
        return 0;
    }
    return args; 
}

/* Creates a deck of cards from a deck file
 * Parameters: file, expected deck size
 * Returns the deck, or a null pointer if the file is incorrectly formatted */
struct Card* generate_deck(FILE* deckFile, int deckSize) {
    char* line;
    struct Card* deck = malloc(sizeof(struct Card) * deckSize);
    const int validLineLength = 2;
    for (int i = 0; i < deckSize; i++) {
        line = read_line(deckFile);
        /* If the end of file was reached too early */
        if (line == 0) {
            return 0;
        }
        /* If the line is the wrong length */
        if (strlen(line) != validLineLength) {
            return 0;
        }
        /* Convert char it int by subtracting ascii value for '0' */
        int number = line[0] - '0';
        char suit = line[1];
        free(line);
        /* Check that the number and suit are valid */
        if (number < 1 || number > 9 || suit < 'A' || suit > 'Z') {
            return 0;
        }
        struct Card card;
        card.number = number;
        card.suit = suit;
        deck[i] = card;
    }

    line = read_line(deckFile);
    /* if there are more cards than the 1st line indicates */
    if (line != 0) {
        free(line);
        return 0;
    }
    free(line);
    fclose(deckFile);
    return deck;
}

/* Creates a string representation of a card
 * Parameters: card, file to output to
 * Returns a string representation of the card */
char* card_to_string(struct Card card, FILE* output) {
    /* If a calloced struct is uninitialised, any ints it contains will be 0 */
    if (card.number == 0) {
        /* Blank spaces should be .. for stdout and ** for save files */
        if (output == stdout) {
            return "..";
        } else {
            return "**";
        }
    }
    char* returnString = malloc(sizeof(char) * 3);
    sprintf(returnString, "%d%c", card.number, card.suit);
    return returnString;
}

/* Prints the board state to the specified file (stdout or save file)
 * Parameters: file, board dimensions */
void draw_board(FILE* file, int boardWidth, int boardHeight,
        struct Card** board) {
    for (int row = 0; row < boardHeight; row++) {
        for (int column = 0; column < boardWidth; column++) {
            /* Convert the card to text and print it on the same row */
            char* cardText = card_to_string(board[row][column], file);
            fprintf(file, "%s", cardText);
            /* Note: there is a memory leak here because cardText is malloced
             * but not freed. If it is freed, a segmentation fault occurs,
             * which valgrind says is from an invalid free. I don't know what
             * is causing this */
        }
        /* Move to the next row */
        fprintf(file, "\n");
    }
}

/* Takes a turn 
 * Parameters: board and hand info, whether the player is human or ai, the
 *         card drawn
 * Returns 0 if the game is still going, 1 if it is over and -1 for end of 
 *         user input error */
int take_turn(struct SaveInfo saveInfo, char playerType, 
        struct Card drawnCard) {
    draw_board(stdout, saveInfo.boardWidth, saveInfo.boardHeight, 
            saveInfo.board);
    /* Draw a card */
    saveInfo.hands[saveInfo.playerNo][HAND_SIZE - 1] = drawnCard;
    /* Print out hand info */
    if (playerType == 'h') {
        printf("Hand(%d):", saveInfo.playerNo + 1);
    } else {
        printf("Hand:");
    }
    for (int i = 0; i < HAND_SIZE; i++) {
        printf(" %d%c", saveInfo.hands[saveInfo.playerNo][i].number, 
                saveInfo.hands[saveInfo.playerNo][i].suit);
    }
    printf("\n");
    /* Human player */
    if (playerType == HUMAN) {
        int validInput = 0;
        /* Keep prompting for input until a valid move is made */
        do {
            validInput = human_turn(saveInfo);
            /* End of user input */
            if (validInput == -1) {
                return -1;
            }
        } while(!validInput);
    } else {
        ai_turn(saveInfo);
    }

    /* Check if the board is full */
    int* emptySpace = find_empty_square(0, saveInfo.boardWidth, 
            saveInfo.boardHeight, saveInfo.board);
    int valid = emptySpace[0];
    free(emptySpace);
    /* The board is full if valid is -1, and an arbitrary space has a card on 
     * it */
    if (valid == -1 && saveInfo.board[0][0].number != 0) {
        return 1;
    }
    return 0;
}

/* Automatically make a move for an ai turn
 * Parameters: board and hand info */
void ai_turn(struct SaveInfo saveInfo) {
    /* Calculate card position */
    int* position = find_empty_square(saveInfo.playerNo,
            saveInfo.boardWidth, saveInfo.boardHeight, saveInfo.board);
    int row = position[0];
    int column;
    /* row is -1 if the board is empty */
    if (row == -1) {
        /* Subtract 1 to ensure result is rounded down */
        row = (saveInfo.boardHeight - 1) / 2;
        column = (saveInfo.boardWidth - 1) / 2;
    } else {
        column = position[1];
    }
    free(position);

    /* Place card and print the move made */
    saveInfo.board[row][column] = saveInfo.hands[saveInfo.playerNo][0];
    printf("Player %d plays %d%c in column %d row %d\n",
            saveInfo.playerNo + 1,
            saveInfo.hands[saveInfo.playerNo][0].number,
            saveInfo.hands[saveInfo.playerNo][0].suit, column + 1,
            row + 1);
    reorder_hand(saveInfo.hands[saveInfo.playerNo], 0);
}

/* Removes the card just played from the hand, ensuring the cards are kept
 *         in order of being drawn
 * Parameters: hand, index of card played */
void reorder_hand(struct Card* hand, int cardPlayed) {
    /* Move all cards that were to the right of the card played one position
     * left */
    for (int i = cardPlayed; i < HAND_SIZE - 1; i++) {
        hand[i] = hand[i + 1];
    }
    hand[HAND_SIZE - 1].number = 0;
}

/* Handle user input for a turn
 * Parameters: board and deck info
 * Returns 0 if the user should be prompted again, 1 if they shouldn't 
 *         and -1 for end of input error */
int human_turn(struct SaveInfo saveInfo) {
    printf("Move? ");
    char* line = read_line(stdin);
    /* Indicate if it is the end of user input */
    if (line == 0) {
        return -1;
    }
    /* Prompt for input again if line is too short or it successfully saves */
    if (check_save(saveInfo, line)) {
        return 0;
    }
    const char argsRequired = 3;
    int* args = split_line(line, argsRequired);           
    free(line);               
    /* If split_line could not parse user input*/
    if (args == 0) {
        return 0;
    }
    /* Subtract 1 for consistent indexing between user input and program */
    int cardNo = args[0] - 1;
    int column = args[1] - 1;
    int row = args[2] - 1;
    free(args);
    /* Check that the chosen card is valid and position is on the board */ 
    if (cardNo < 0 || cardNo > HAND_SIZE - 1 || row < 0 
            || row >= saveInfo.boardHeight || column < 0 
            || column >= saveInfo.boardWidth) {
        return 0;
    }

    /* Attempt to place a card and return 1 if successfrul (or 0 otherwise) */
    return check_position_validity(saveInfo, row, column, cardNo);
}

/* Place a card if the positionn is valid 
 * Parameters: board and hand info, row, column, index of card in hand
 * Returns 1 if the position is valid, 0 otherwise */
int check_position_validity(struct SaveInfo saveInfo, int row, int column,
        int cardNo) {
    int* position = find_empty_square(0, saveInfo.boardWidth,
            saveInfo.boardHeight, saveInfo.board);
    int validPosition = 0;
    /* If the board is empty find_empty_square returns the position {-1, -1}
     * In this case all positions are valid */
    if (position[0] == -1) {
        validPosition = 1;
    } else {
        struct Card* neighbours = adjacent_cards(row, column,
                saveInfo.boardWidth, saveInfo.boardHeight, saveInfo.board);
        /* If the chosen position is empty and adjacent ot another card */
        if (saveInfo.board[row][column].number == 0
                && exists_adjacent(neighbours)) {
            validPosition = 1;
        }
        free(neighbours);
    }
    free(position);
    /* Place the card if valid */
    if (validPosition) {
        saveInfo.board[row][column] 
                = saveInfo.hands[saveInfo.playerNo][cardNo];
        reorder_hand(saveInfo.hands[saveInfo.playerNo], cardNo);
    }
    return validPosition;
}

/* Checks if the user input was to save
 * Parameters: board and deck info, input
 * Returns 0 if the input was for a move, 1 if the user should be prompted 
 *         for input again (ie if they saved or the input was invalid) */
int check_save(struct SaveInfo saveInfo, char* line) {
    const char saveCompare[5] = "SAVE";
    /* Check that there are at least 4 characters to compare to "SAVE" */
    if (strlen(line) < strlen(saveCompare)) {
        free(line);
        return 1;
    }
    /* Check if the input is to save (1st 4 characters are "SAVE") */
    int saving = 1;
    for (int i = 0; i < strlen(saveCompare); i++) {
        if (line[i] != saveCompare[i]) {
            saving = 0;
            break;
        }
    }
    /* If the user did a valid move instead of saving, don't propmt for input 
     * again */
    if (!saving) {
        return 0;
    }
    /* If no file name was given */
    if (strlen(line) < strlen(saveCompare) + 1) {
        printf("Unable to save\n");
    } else {
        /* Remove the 1st 4 characters of input by creating a pointer to the 
         * 5th */
        char* saveFileName = line + strlen(saveCompare);
        save_game(saveFileName, saveInfo);
    }
    free(line);
    /* Prompt for another move */
    return 1;
}

/* Finds and empty square with an adjacent card. Searches from top left or 
 *         bottom right depending on which player is searching
 * Parameters: player number, board dimensions, board
 * Returns the coordinates of an empty square adjacent to a non-empty square
 *         or {-1, -1} if such a square doesn't exist (ie the board is either 
 *         empty or full) */
int* find_empty_square(int playerNo, int boardWidth, int boardHeight, 
        struct Card** board) {
    int* coords = malloc(sizeof(int) * 2);
    if (playerNo == 0) {
        /* Check from top left */
        for (int row = 0; row < boardHeight; row++) {
            for (int column = 0; column < boardWidth; column++) {
                struct Card* neighbours = adjacent_cards(row, column, 
                        boardWidth, boardHeight, board);
                /* If the square is empty and has an adjacent card */
                if (board[row][column].number == 0 
                        && exists_adjacent(neighbours)) {
                    free(neighbours);
                    coords[0] = row;
                    coords[1] = column;
                    return coords;
                }
                free(neighbours);
            }
        }
    } else {
        /* Check from bottom right */
        for (int row = boardHeight - 1; row >= 0; row--) {
            for (int column = boardWidth - 1; column >= 0; column--) {
                struct Card* neighbours = adjacent_cards(row, column, 
                        boardWidth, boardHeight, board); 
                if (board[row][column].number == 0 
                        && exists_adjacent(neighbours)) {
                    free(neighbours);
                    coords[0] = row;
                    coords[1] = column;
                    return coords;
                }
                free(neighbours);
            }
        }
    }
    /* Set coordinates to -1 to flag the board as being empty or full */
    coords[0] = -1;
    coords[1] = -1;
    return coords;
}

/* Finds what the card is (if any) in each cardinal direction from a square
 * Parameters: row, column, board dimensions, board
 * Returns an array of the adjacent cards */
struct Card* adjacent_cards(int row, int column, int boardWidth, 
        int boardHeight, struct Card** board) {
    struct Card* cards = (struct Card*)malloc(sizeof(struct Card) * 4);
    /* Check to the left */
    if (column == 0) {
        /* Wrap around the board */
        cards[LEFT] = board[row][boardWidth - 1];
    } else {
        cards[LEFT] = board[row][column - 1];
    }
    /* Check to the right */
    if (column == boardWidth - 1) {
        cards[RIGHT] = board[row][0];
    } else {
        cards[RIGHT] = board[row][column + 1];
    }
    /* Check to the top */
    if (row == 0) {
        cards[UP] = board[boardHeight - 1][column];
    } else {
        cards[UP] = board[row - 1][column];
    }
    /* Check to the bottom */
    if (row == boardHeight - 1) {
        cards[DOWN] = board[0][column];
    } else {
        cards[DOWN] = board[row + 1][column];
    }
    return cards;
}

/* Finds if there is an adjacent card to a square
 * Parameters: array of adjacent cards
 * Returns 1 if there is at least one adjacent card, 0 otherwise */
int exists_adjacent(struct Card* cards) {
    for (int i = 0; i < NO_ADJACENCIES; i++) {
        if (cards[i].number != 0) {
            return 1;
        }
    }
    return 0;
}

/* Writes the relevant info to a save file 
 * Parameters: file name, board and hand info */
void save_game(char* saveFileName, struct SaveInfo saveInfo) {
    FILE* saveFile = fopen(saveFileName, "w");
    /* Apart form the board and deckFileName, lines will have at most 12 
     * characters (13 including \0) */
    const int maxValidLineLength = 13;
    char line[maxValidLineLength];
    /* Write first line of info */
    sprintf(line, "%d %d %d %d", saveInfo.boardWidth, saveInfo.boardHeight, 
            saveInfo.cardsDrawn + 1, saveInfo.playerNo + 1);
    fprintf(saveFile, "%s\n", line);
    /* Write the deck file name */
    fprintf(saveFile, "%s\n", saveInfo.deckFileName);
    for (int i = 0; i < NO_PLAYERS; i++) {
        for (int j = 0; j < HAND_SIZE - 1 + (i == saveInfo.playerNo); j++) {
            /* Write cards to file one at a time, then the \n */
            sprintf(line, "%d%c", saveInfo.hands[i][j].number, 
                    saveInfo.hands[i][j].suit);
            fprintf(saveFile, "%s", line);
        }
        fprintf(saveFile, "\n");
    }
    /* Write the board */
    draw_board(saveFile, saveInfo.boardWidth, saveInfo.boardHeight, 
            saveInfo.board);
    fclose(saveFile);
}

/* Frees the memory malloced to store cards on the board
 * Parameters: board, board height */
void free_board(struct Card** board, int height) {
    for (int row = 0; row < height; row++) {
        free(board[row]);
    }
    free(board);
}

/* Frees the memory malloced to store cards in hand 
 * Parameters: hands */
void free_hands(struct Card** hands) {
    for (int i = 0; i < NO_PLAYERS; i++) {
        free(hands[i]);
    }
    free(hands);
}

/* Calculates the score for the longest valid path starting from each square
 *         on the board, then gets the maximum for each player 
 * Parameters: board dimensions, board
 * Returns an array of each player's score */
int* calculate_scores(int boardWidth, int boardHeight, struct Card** board) {
    /* Create an array of each player's score initialised as 0 */
    int* playerScores = (int*)malloc(sizeof(int) * NO_PLAYERS);
    for (int i = 0; i < NO_PLAYERS; i++) {
        playerScores[i] = 0;
    }
    /* Check each square for the maximum score of any path starting from it */
    for (int row = 0; row < boardHeight; row++) {
        for (int column = 0; column < boardWidth; column++) {
            if (board[row][column].number != 0) {
                char suit = board[row][column].suit;
                int score = traverse_path(row, column, boardWidth, boardHeight,
                        board, suit, 1);
                /* Convert suit to a number between 0 and 25 representing 
                 * position in the alphabet */
                suit -= 'A';
                /* If suit is even (A, C, E, etc.) compare with player 1's 
                 * score. If suit is odd compare with player 2's score */
                if (score > playerScores[((int)suit) % NO_PLAYERS]) {
                    playerScores[((int)suit) % NO_PLAYERS] = score;
                }
            }
        }
    }
    return playerScores;
}

/* Recursively calculates the maximum score of the paths starting from a
 *         square
 * Parameters: row, column, board dimensions, bord, suit of initial square,
 *         path length
 * Returns the maximum of this square's score, and any of the four around it 
 *         with a higher number than this square. For this square, the score 
 *         is 0 if the suit doesn't match the initial suit, and the path 
 *         length if it does match. */
int traverse_path(int row, int column, int boardWidth, int boardHeight, 
        struct Card** board, char initialSuit, int pathLength) {
    struct Card* adjacentCards = adjacent_cards(row, column, boardWidth, 
            boardHeight, board);
    /* Scores for to the left, to the right, above, below, this position */
    int scores[NO_ADJACENCIES + 1] = {0, 0, 0, 0, 0};
    if (board[row][column].suit == initialSuit) {
        scores[THIS_POSITION] = pathLength;
    }
    if (adjacentCards[LEFT].number > board[row][column].number) {
        if (column == 0) {
            scores[LEFT] = traverse_path(row, boardWidth - 1, boardWidth, 
                    boardHeight, board, initialSuit, pathLength + 1);
        } else {
            scores[LEFT] = traverse_path(row, column - 1, boardWidth, 
                    boardHeight, board, initialSuit, pathLength + 1);
        }
    }
    if (adjacentCards[RIGHT].number > board[row][column].number) {
        if (column == boardWidth - 1) {
            scores[RIGHT] = traverse_path(row, 0, boardWidth, boardHeight, 
                    board, initialSuit, pathLength + 1);
        } else {
            scores[RIGHT] = traverse_path(row, column + 1, boardWidth, 
                    boardHeight, board, initialSuit, pathLength + 1);
        }
    }
    if (adjacentCards[UP].number > board[row][column].number) {
        if (row == 0) {
            scores[UP] = traverse_path(boardHeight - 1, column, boardWidth, 
                    boardHeight, board, initialSuit, pathLength + 1);
        } else {
            scores[UP] = traverse_path(row - 1, column, boardWidth, 
                    boardHeight, board, initialSuit, pathLength + 1);
        }
    }
    if (adjacentCards[DOWN].number > board[row][column].number) {
        if (row == boardHeight - 1) {
            scores[DOWN] = traverse_path(0, column, boardWidth, boardHeight, 
                    board, initialSuit, pathLength + 1);
        } else {
            scores[DOWN] = traverse_path(row + 1, column, boardWidth, 
                    boardHeight, board, initialSuit, pathLength + 1);
        }
    }
    free(adjacentCards);
    return get_max_score(scores);
}

/* Calculates the maximum of 5 scores
 * Parameters: scores
 * Returns the maximum score */
int get_max_score(int* scores) {
    int maxScore = scores[0];
    for (int i = 1; i < NO_ADJACENCIES + 1; i++) {
        if (scores[i] > maxScore) {
            maxScore = scores[i];
        }
    }
    return maxScore;
}

