#!/usr/bin/env python3
"""
Assignment 1
CSSE1001/7030
Semester 2, 2018
"""

from a1_support import is_word_english, is_numeric
import re    #Used for splitting strings by whitespace and hyphens

__author__ = "Daniel Nathan 45373787"

# Write your functions here
def display_menu():
    """
        (None) Displays the main menu, propmting for input

        Parameters:
            None
    """
    selection = input("Please choose an option [e/d/a/q]: \n e) Encrypt some text \n d) Decrypt some text \n a) Automatically decrypt English text \n q) Quit \n")  #Prompts for user input
    if selection == "e":    #Encrypt
        phrase = input("Please enter some text to encrypt: \n")
        offset = input_offset_value()   #This is done in a separate function to avoid repeating code for checking if the input is valid in the decrypt option
        if offset == 0:
            print("The possible encryptions are:")
            for i in range(1, 26):
                print(str(i) + ": " + encrypt(phrase, i))   #Prints out the numbers from 1 - 25 with respective encryptions
            print("\n")
        else:
            print("The encrypted text is: \n" + encrypt(phrase, offset) + "\n")
        display_menu()  #Returns to the main menu
    elif selection == "d":  #Decrypt
        phrase = input("Please enter some text to decrypt: \n")
        offset = input_offset_value()
        if offset == 0:
            print("The possible decryptions are:")  #Prints out the numbers 1 - 25 with respective decryptions
            for i in range(1, 26):
                print(str(i) + ": " + encrypt(phrase, i))
            print("\n")
        else:
            print("The decrypted text is: \n" + decrypt(phrase, offset) + "\n")
        display_menu()
    elif selection == "a":  #Auto detect
        phrase = input("Please enter some encrypted text: \n")
        find_encryption_offsets(phrase)
        display_menu()
    elif selection == "q":  #Quit
        quit()  #Exits the program
    else:
        print("Invalid input")
        display_menu()
    return None

def input_offset_value():
    """
        (int) Propmts the user for an integer between 0 and 25 inclusive

        Parameters:
            None
    """
    invalid_input = True    #Initialised to True so that the while loop always runs at least once
    while invalid_input:    #Continues to prompt for input until the input is valid
        offset = input("Please enter a shift offset (1 - 25), or 0 for all: \n")
        if is_numeric(offset) == False:     #If the input isn't a number
            print("Invalid input: must be a whole number between 0 and 25")
        else:
            offset_number = float(offset)
            if offset_number > 25 or offset_number < 0 or int(offset_number) != offset_number: #If the input is an integer from 0 - 25. Tested separately so that if the input is not numeric the program does not crash
                print("Invalid input: must be a whole number between 0 and 25")
            else:
                invalid_input = False   #Exits the loop with the input value
    return int(offset_number)

def encrypt(phrase, offset):
    """
        (str) Encrypts the letters of a phrase by an offset

        Parameters:
            phrase (str): The phrase to encrypt
            offset (int): The amount of letters the phrase is offset by during encryption

        Examples:
            >>> encrypt('Hello World!', 5)
            Mjqqt Btwqi!
            >>> encrypt('abcde 12345', 1)
            bcdef 12345
    """
    encrypted_phrase = ""   #Empty string that will be populated with encrypted characters
    for character in phrase:
        unicode_value = ord(character)      #Gets the unicode value for the character
        if unicode_value >= 97 and unicode_value <= 122:    #Unicode values for lowercase a - z
            unicode_value += offset
            if unicode_value > 122:     #If it now falls outside the range of unicode values for a - z
                unicode_value -= 26         #loop back to the start of the alphbet
        elif unicode_value >= 65 and unicode_value <= 90:   #Unicode values for A - Z
            unicode_value += offset
            if unicode_value > 90:  
                unicode_value -= 26    #Loops back if unicode_value exceeds that of Z    
        encrypted_phrase += chr(unicode_value)  #Adds encrypted letter to the encrypted phrase
    return encrypted_phrase

def decrypt(phrase, offset):
    """
        (str) Decrypts the letters of a phrase by an offset

        Parameters:
            phrase (str): The phrase to decrypt
            offset (int): The amount of letters the phrase is offset by during decryption

        Examples:
            >>> encrypt('Mjqqt Btwqi!', 5)
            Hello World!
            Mjqqt Btwqi!
            >>> encrypt('bcdef 12345', 1)
            abcde 12345

        Note:
        This function is redundant; you can just directly call encrypt(phrase, 26 - offset). I have included it as the task sheet specifies that there must be a decrypt function. If I made a separate decrypt function from scratch, it would be the same as the encrypt functionbut it subtracts from the unicode value, then adds 26 if it's not in the valid range

    """
    return encrypt(phrase, 26 - offset) #Encrypting then decrypting by the same number will in effect encrypt by 26, looping back to the starting letters
    
def find_encryption_offsets(phrase):
    """
        (tuple) Finds the decryption offsets that will result in an English phrase, then prints them

        Parameters:
            phrase (str): The phrase to decrypt

        Examples:
            >>> find_encryption_offsets('ild')
                Reutrns (15, 17, 23)
                Prints
                    Multiple encryption offsets:
                    15: two
                    17: rum
                    23: log
            >>> find_encryption_offsets('eid')
                Returns ()
                Prints
                    No valid encryption offset
    """
    possible_offsets = {}   #Empty dictionary to be populated by valid offsets as the keys, and decrypted phrases as the values
    for i in range(1, 26):
        english_phrase = True   #Initialised as True, then if it is still True after checking it is a valid phrase
        decrypted_phrase = decrypt(phrase, i) 
        words = re.split("-|\s", decrypted_phrase)  #Splits string by whitespace and hyphens
        for word in words:  
            while len(word) != 0 and word[0].isalpha() == False:    #If the first character exists and is nonalphbetical
                word = word[1:len(word)]    #cuts off punctuation at the start of word for checking with word list
            while len(word) != 0 and word[len(word) - 1].isalpha() == False:
                word = word[0:len(word) - 1]    #cuts off punctuation at the end of the word
            contraction = False #Initialised as False
            for char in word:
                if char == "'":
                    contraction = True  #If there is an apostrophe, ignore the word
                    break
            if is_word_english(word.lower()) == False and contraction == False and word != "":  #If the word isn't on the list, isn't a contraction and isn't blank  
                english_phrase = False
                break
        if english_phrase:
            possible_offsets[i] = decrypted_phrase    #Add valid phrases to the dectionary
    if len(possible_offsets) == 0:  #If no decryptions were found
        print("No valid encryption offset")
    else:
        if len(possible_offsets) == 1:
            print("One encryption offset:")
        else:
            print("Multiple encryption offsets:")
        for key in possible_offsets:
            print(str(key) + ": " + possible_offsets[key])  #Print all valid decryptions with their offsets
    output_tuple = tuple(possible_offsets.keys())    #Converts keys to tuple for output
    return output_tuple

def main():
    """
        (None) Runs the progam

        Parameters:
            None
    """
    # Add your main code here
    display_menu()
    pass

##################################################
# !! Do not change (or add to) the code below !! #
#
# This code will run the main function if you use
# Run -> Run Module  (F5)
# Because of this, a "stub" definition has been
# supplied for main above so that you won't get a
# NameError when you are writing and testing your
# other functions. When you are ready please
# change the definition of main above.
###################################################

if __name__ == '__main__':
    main()

