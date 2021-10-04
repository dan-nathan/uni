#ifndef PLAYER_H
#define PLAYER_H
#include "player.h"
#endif

// Determines which card to play given Alice's strategy
// Parameters: playerInfo - info about Alice, gameInfo - the game state
// Returns: the index of the card to play in Alice's hand
int strategy(PlayerInfo* playerInfo, GameInfo* gameInfo) {
    int cardNo;
    // If Alice is the lead player
    if (playerInfo->playerNo == gameInfo->leadPlayer) {
        cardNo = search_hand(playerInfo, "SCDH", HIGHEST);
    } else {
        char leadSuit = gameInfo->playedThisRound[gameInfo->leadPlayer].suit;
        // If Alice has a card of the lead suit, play the lowest ranked one
        cardNo = search_suit(playerInfo, leadSuit, LOWEST);
        // If Alice doesn't have a card of the lead suit
        if (cardNo == -1) {
            cardNo = search_hand(playerInfo, "DHSC", HIGHEST);
        }
    }
    return cardNo;
}

int main(int argc, char** argv) {
    PlayerInfo playerInfo;
    playerInfo.strategy = strategy;
    Status status = setup_player(argc, argv, &playerInfo);
    if (status != NO_ERROR) {
        return status;
    }
    return display_error_message(play_game(&playerInfo));
}
