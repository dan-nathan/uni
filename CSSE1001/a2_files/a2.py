#!/usr/bin/env python3
"""
Assignment 2 - UNO++
CSSE1001/7030
Semester 2, 2018
"""

import random
#from a2_support import CardColour, TurnManager, UnoGame
__author__ = "Daniel Nathan 45373787"

# Write your classes here

class Card(object):
    """
    A class to store information about a card
    """
    
    def __init__(self, number, colour):
        """
        (None) Initialises the card

        Parameters:
            self (Card): The card being refered to
            number (int): The number on the card
            colour (a2_support.CardColour): The colour of the card
        """
        self._number = number
        self._colour = colour

    def __str__(self):
        """
        (str) Returns card info in the form 'Card([number], [colour])'

        Parameters:
            self (Card): The card being refered to
        """
        return "Card({0}, {1})".format(str(self.get_number()),self.get_colour().__str__()) #converts the number and colour to strings before inserting them into the output string

    def __repr__(self):
        """
        (str) Returns card info in the form 'Card([number], [colour])'

        Parameters:
            self (Card): The card being refered to
        """
        return "Card({0}, {1})".format(str(self.get_number()),self.get_colour().__str__())

    def get_number(self):
        """
        (int) Returns the number on the card

        Parameters:
            self (Card): The card being refered to
        """
        return self._number

    def get_colour(self):
        """
        (a2_support.CardColour) Returns the colour of the card

        Parameters:
            self (Card): The card being refered to
        """
        return self._colour

    def set_number(self, number):
        """
        (None) Sets the number on the card

        Parameters:
            self (Card): The card being refered to
        """
        self._number = number

    def set_colour(self, colour):
        """
        (None) Sets the colour on the card

        Parameters:
            self (Card): The card being refered to
        """
        self._colour = colour

    def get_pickup_amount(self):
        """
        (int) Returns the amount of cards the next player must pick up if this card is played

        Parameters:
            self (Card): The card being refered to
        """
        return 0 #all non-pickup cards will default to this, but for pickup cards the function will be overwritten

    def matches(self, card):
        """
        (Bool) Returns True iff the card can be placed on the putdown pile

        Parameters:
            self (Card): This card
            card (Card): The other card
        """
        if isinstance(self, Pickup4Card) or isinstance(card, Pickup4Card): #if one of the cards is a pickup 4
            return True
        elif isinstance(self, SkipCard) or isinstance(self, ReverseCard) or isinstance(self, Pickup2Card) or isinstance(card, SkipCard) or isinstance(card, ReverseCard) or isinstance(card, Pickup2Card): #if one of the cards is a skip, reverse or pickup 2
            return self.get_colour() == card.get_colour() #only a valid match with same colour
        else: #if it is a regular card
            return self.get_number() == card.get_number() or self.get_colour() == card.get_colour() #valid match with colour or number

    def play(self, player, game):
        """
        (None) Gives functionality to special cards (defined individually in subclasses)

        Parameters:
            self (Card): The card being refered to
            player (Player): The player whose turn it is
            game (a2_support.UnoGame): The game of Uno
        """
        return None

class SkipCard(Card):
    """
    A subclass of Card for Skip Cards
    """
    
    def __str__(self):
        """
        (str) Returns card info in the form 'SkipCard([number], [colour])'

        Parameters:
            self (Card): The card being refered to
        """
        return "SkipCard({0}, {1})".format(str(self.get_number()),self.get_colour().__str__())

    def __repr__(self):
        """
        (str) Returns card info in the form 'SkipCard([number], [colour])'

        Parameters:
            self (Card): The card being refered to
        """
        return "SkipCard({0}, {1})".format(str(self.get_number()),self.get_colour().__str__())

    def play(self, player, game):
        """
        (None) Enacts the effect of playing a skip card

        Parameters:
            self (Card): The card being refered to
            player (Player): The player whose turn it is
            game (a2_support.UnoGame): The game of Uno
        """
        game.skip() #the skip function is built into a2_support.UnoGame and is called here
        return None

class ReverseCard(Card):
    """
    A subclass of Card for Reverse Cards
    """
    
    def __str__(self):
        """
        (str) Returns card info in the form 'ReverseCard([number], [colour])'

        Parameters:
            self (Card): The card being refered to
        """
        return "ReverseCard({0}, {1})".format(str(self.get_number()),self.get_colour().__str__())

    def __repr__(self):
        """
        (str) Returns card info in the form 'ReverseCard([number], [colour])'

        Parameters:
            self (Card): The card being refered to
        """
        return "ReverseCard({0}, {1})".format(str(self.get_number()),self.get_colour().__str__())

    def play(self, player, game):
        """
        (None) Enacts the effect of playing a reverse card

        Parameters:
            self (Card): The card being refered to
            player (Player): The player whose turn it is
            game (a2_support.UnoGame): The game of Uno
        """
        game.reverse()
        return None

class Pickup2Card(Card):
    """
    A subclass of Card for Picup 2 Cards
    """
    
    def __str__(self):
        """
        (str) Returns card info in the form 'Pickup2Card([number], [colour])'

        Parameters:
            self (Card): The card being refered to
        """
        return "Pickup2Card({0}, {1})".format(str(self.get_number()),self.get_colour().__str__())

    def __repr__(self):
        """
        (str) Returns card info in the form 'Pickup2Card([number], [colour])'

        Parameters:
            self (Card): The card being refered to
        """
        return "Pickup2Card({0}, {1})".format(str(self.get_number()),self.get_colour().__str__())

    def get_pickup_amount(self):
        """
        (int) Returns the amount of cards the next player must pick up if this card is played

        Parameters:
            self (Card): The card being refered to
        """
        return 2

    def play(self, player, game):
        """
        (None) Enacts the effect of playing a Pickup 2 card

        Parameters:
            self (Card): The card being refered to
            player (Player): The player whose turn it is
            game (a2_support.UnoGame): The game of Uno
        """
        drawn_cards = game.pickup_pile.pick(2) #draws the frist 2 cards off the pickup pile
        game._turns.peak(1).get_deck().add_cards(drawn_cards) #Adds the drawn cards to the next player's deck
        #Note, if PickupCard were to be made its own class with Pickup2Card and Pickup4Card
        #as subclasses, this play function could be implemented into that with self.get_pickup_amount()
        #replacing the 2s in this function.

class Pickup4Card(Card):
    """
    A subclass for Card for Pickup 4 Cards
    """
    
    def __str__(self):
        """
        (str) Returns card info in the form 'Pickup4Card([number], [colour])'

        Parameters:
            self (Card): The card being refered to
        """
        return "Pickup4Card({0}, {1})".format(str(self.get_number()),self.get_colour().__str__())

    def __repr__(self):
        """
        (str) Returns card info in the form 'Pickup4Card([number], [colour])'

        Parameters:
            self (Card): The card being refered to
        """
        return "Pickup4Card({0}, {1})".format(str(self.get_number()),self.get_colour().__str__())

    def get_pickup_amount(self):
        """
        (int) Returns the amount of cards the next player must pick up if this card is played

        Parameters:
            self (Card): The card being refered to
        """
        return 4

    def play(self, player, game):
        """
        (None) Enacts the effect of playing a Pickup 4 card

        Parameters:
            self (Card): The card being refered to
            player (Player): The player whose turn it is
            game (a2_support.UnoGame): The game of Uno
        """
        drawn_cards = game.pickup_pile.pick(4)
        game._turns.peak(1).get_deck().add_cards(drawn_cards)
    
class Deck(object):
    """
    A class to store information about a deck
    """
    
    def __init__(self, starting_cards = None):
        """
        (None) Initialises the deck

        Parameters:
            self (Deck): The deck being refered to
            starting_cards (list): A list of the cards that start in the deck
        """
        self.cards = starting_cards
        if self.cards is None:
            self.cards = [] #having an empty list instead of None prevents errors when using the deck as a list

    def get_cards(self):
        """
        (list) Returns the list of cards in the deck

        Parameters:
            self (Deck): The deck being refered to
        """
        return self.cards

    def get_amount(self):
        """
        (int) Returns the number of cards in the deck

        Parameters:
            self (Deck): The deck being refered to
        """
        return len(self.cards)

    def shuffle(self):
        """
        (None) Shuffles the deck

        Parameters:
            self (Deck): The deck being refered to
        """
        temp_card_list = []
        while len(self.cards) > 0: #each iteration a card is moved from self.cards to temp_card_list. This must stop when there are no cards remaining to move
            temp_card_list.append(self.cards.pop(random.randint(0, len(self.cards) - 1))) #removes a random remaining card from self._cards and adds it to temp_cards_list
        self.cards = temp_card_list

    def pick(self, amount = 1):
        """
        (list) Returns the top 'amount' of cards in the deck, and removes them from the deck

        Parameters:
            self (Deck): The deck being refered to
            amount (int): The number of cards removed from the deck
        """
        cards_picked = self.cards[-amount:] #the top 'amount' of cards
        self.cards = self.cards[:-amount] #removes the top 'amount' of cards from the deck
        return cards_picked

    def add_card(self, card):
        """
        (None) Adds a card to the top of the deck

        Parameters:
            self (Deck): The deck being refered to
            card (Card): The card added
        """
        self.cards.append(card)

    def add_cards(self, cards):
        """
        (None) Adds a list of cards to the top of the deck

        Parameters:
            self (Deck): The deck being refered to
            card (list): The cards added
        """
        for card in cards:
            self.cards.append(card)

    def top(self):
        """
        (None) Returns the last card in the deck

        Parameters:
            self (Deck): The deck being refered to
        """
        if len(self.cards) == 0:
            return None
        else:
            return self.cards[-1]

class Player(object):
    """
    A class to store information about a player
    """

    def __init__(self, name):
        """
        (None) Initialises the player

        Parameters:
            self (Player): The player being refered to
            name (str): The player's name
        """
        self._name = name
        self._deck = Deck() #gives the player an empty deck to be populated later

    def get_name(self):
        """
        (str) Returns the player's name

        Parameters:
            self (Player): The player being refered to
        """
        return self._name

    def get_deck(self):
        """
        (str) Returns the player's deck

        Parameters:
            self (Player): The player being refered to
        """
        return self._deck

    def is_playable(self):
        """
        (Bool) Determines if the player is a human. Only for use in subclasses

        Parameters:
            self (Player): The player being refered to
        """
        raise NotImplementedError

    def has_won(self):
        """
        (Bool) Determines if the player has won the game
        
        Parameters:
            self (Player): The player being refered to
        """
        return len(self._deck.get_cards()) == 0 #return True iff the player's deck has no cards

    def pick_card(self, putdown_pile):
        """
        (Card) Picks a card to put on the draw pile if the player is automated and has a playable card
        
        Parameters:
            self (Player): The player being refered to
        """
        raise NotImplementedError

class HumanPlayer(Player):
    """
    A subclass of Player for a controlable player
    """

    def is_playable(self):
        """
        (Bool) Determines if the player is a human. Only for use in subclasses

        Parameters:
            self (Player): The player being refered to
        """
        return True

    def pick_card(self, putdown_pile):
        """
        (Card) Picks a card to put on the draw pile if the player is automated and has a playable card
        
        Parameters:
            self (Player): The player being refered to
        """
        return None

class ComputerPlayer(Player):
    """
    A subclass of Player for an automated player
    """

    def is_playable(self):
        """
        (Bool) Determines if the player is a human. Only for use in subclasses

        Parameters:
            self (Player): The player being refered to
        """
        return False

    def pick_card(self, putdown_pile):
        """
        (Card) Picks a card to put on the draw pile if the player is automated and has a playable card
        
        Parameters:
            self (Player): The player being refered to
        """
        matching_cards = []
        for i in range(0, len(self._deck.get_cards()) - 1): #iterates through all cards in the player's deck keeping track of the index of the cards using i
            if self._deck.get_cards()[i].matches(putdown_pile.top()):
                matching_cards.append(i) #creates a list of playable cards
        if len(matching_cards) == 0:
            return None #doesn't return a card if non match
        else:
            chosen_card = self._deck.get_cards()[matching_cards[random.randint(0, len(matching_cards) - 1)]] #matching_cards contains the indexes of each playable card from self._deck.get_cards(). A random index from it is selected, then the corresponding card is chosen
            self._deck.get_cards().remove(chosen_card)
            return chosen_card
            
        #Note: This will return a random playable card. For a faster algorithm the first playable
        #card found can be returned instead using the code:
        #
        #for card in self._deck.get_cards():
        #    if card.matches(putdown_pile.top()):
        #        self._deck.get_cards().remove(card)
        #        return card

def main():
    print("Please run gui.py instead")


if __name__ == "__main__":
    main()
