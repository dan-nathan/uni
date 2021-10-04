#ifndef UTIL_H
#define UTIL_H
#include "util.h"
#endif

// Reads a line from a file
// Parameters: file - the file to read from
// Returns: a line from the file, or NULL if EOF was encountered
char* read_line(FILE* file) {
    char* line = malloc(sizeof(char));
    int size = 0;
    while (1) {
        // Read characters one by one and append them to the string until a
        // new line is encountered
        int nextCharacter = fgetc(file);
        if (nextCharacter == '\n') {
            break;
        }
        // Return a null pointer if EOF is encountered
        if (nextCharacter == EOF) {
            free(line);
            return 0;
        }
        // Resize array and put read character on the end
        size++;
        line = (char*)realloc(line, (size + 1) * sizeof(char));
        line[size - 1] = nextCharacter;
    }
    line[size] = '\0';
    return line;
}

// Converts a string to a positive int
// Parameters: text - the string to convert
// Returns the integer value of the string, or -1 if the string isn't a valid
//         positive integer
int string_to_int(const char* text) {
    int length = strlen(text);
    // If the string is blank
    if (length == 0) {
        return -1;
    }
    for (int i = 0; i < length; i++) {
        if (text[i] < '0' || text[i] > '9') {
            return -1;
        }
    }
    return atoi(text);
}

// Gets the number of digits of a positive number
// Parameters: num - the number to get the number of digits for
// Returns: the number of digits
int no_digits(int num) {
    int noDigits = 0;
    while (num != 0) {
        noDigits++;
        num /= 10;
    }
    return noDigits;
}

// Creates a card from a string
// Parameters: text - the string to convert to the card
// Returns: the card the string represents, or an invalid card if the string
//         isn't a valid representation of a card
Card read_card(const char* text) {
    // Create an uninitialised card
    Card card = {INVALID_SUIT, INVALID_RANK};
    // If the text to convert to a card is formatted incorrectly
    if (strlen(text) != 2) {
        return card;
    }
    char suit = text[0];
    char rank = text[1];
    // If the suit is invalid
    if (!(suit == 'D' || suit == 'H' || suit == 'S' || suit == 'C')) {
        return card;
    }
    // If the rank is invalid
    if ((rank < '0' || rank > '9') && (rank < 'a' || rank > 'f')) {
        return card;
    }
    card.suit = suit;
    card.rank = rank;
    return card;
}

// Represents a card as a string
// Parameters: card - the card to convert to a string, dot - whether or not
//         a . character should be between the suit and rank
// Returns: a string representation of a card
char* write_card(Card card, bool dot) {
    char* cardText;
    if (!dot) {
        cardText = malloc(sizeof(char) * 3);
        cardText[0] = card.suit;
        cardText[1] = card.rank;
        cardText[2] = '\0';
    } else {
        cardText = malloc(sizeof(char) * 4);
        cardText[0] = card.suit;
        cardText[1] = '.';
        cardText[2] = card.rank;
        cardText[3] = '\0';
    }
    return cardText;
}

// Determines if two cards match
// Parameters: a - the first card, b - the second card
// Returns: true if the cards have the same rank and suit
bool card_matches(Card a, Card b) {
    return a.suit == b.suit && a.rank == b.rank;
}

// Removes a card from a hand, shifting all subsequent cards to the left to
//         ensure there isn't a blank space in the middle of the hand
// Parameters: hand - the hand of cards, chosenCard - the index of the card to
//         remove, handSize - the number of cards currently in hand
void reorder_hand(Card* hand, int chosenCard, int handSize) {
    for (int i = chosenCard; i < handSize - 1; i++) {
        hand[i] = hand[i + 1];
    }
}
