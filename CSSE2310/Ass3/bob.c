#ifndef PLAYER_H
#define PLAYER_H
#include "player.h"
#endif

// Determines which card to play given Bob's strategy
// Parameters: playerInfo - info about Bob, gameInfo - the game state
// Returns: the index of the card to play in Bob's hand
int strategy(PlayerInfo* playerInfo, GameInfo* gameInfo) {
    int cardNo;
    // If Bob is the lead player
    if (playerInfo->playerNo == gameInfo->leadPlayer) {
        cardNo = search_hand(playerInfo, "DHSC", LOWEST);
    } else {
        char leadSuit = gameInfo->playedThisRound[gameInfo->leadPlayer].suit;
        bool thresholdMet = false;
        bool dCardPlayed = false;
        // Check if any players have won (threshold - 2) D cards, or played a
        // D card this round
        for (int i = 0; i < playerInfo->noPlayers; i++) {
            if (gameInfo->dCardsWon[i] >= playerInfo->threshold - 2) {
                thresholdMet = true;
            }
            if (gameInfo->playedThisRound[i].suit == 'D') {
                dCardPlayed = true;
            }
            if (thresholdMet && dCardPlayed) {
                break;
            }
        }
        if (thresholdMet && dCardPlayed) {
            // If Bob has a card of the lead suit, play the highest ranked one
            cardNo = search_suit(playerInfo, leadSuit, HIGHEST);
            // If Bob doesn't have a card of the lead suit
            if (cardNo == -1) {
                cardNo = search_hand(playerInfo, "SCHD", LOWEST);
            }
        } else {
            // If Bob has a card of the lead suit, play the lowest ranked one
            cardNo = search_suit(playerInfo, leadSuit, LOWEST);
            // If Bob doesn't have a card of the lead suit
            if (cardNo == -1) {
                cardNo = search_hand(playerInfo, "SCDH", HIGHEST);
            }
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
    return (display_error_message(play_game(&playerInfo)));
}
