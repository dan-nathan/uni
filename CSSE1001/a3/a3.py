import tkinter as tk
import time as t
import math
import random

class HorizontalLine(tk.Frame):
    """
    A class for a grey horizontal line
    """
    def __init__(self, parent):
        """
        Sets the parameters of the line

        Parameters:
            parent (tk.Frame): the tk frame class
        """
        super().__init__(parent)
        self.config(bg = "#EEEEEE",
                    height = 2)

class TableHeader(tk.Frame):
    """
    A class for the headings of the queue
    """
    def __init__(self, parent):
        """
        Sets the parameters of the frame

        Parameters:
            parent (tk.Frame): the tk frame class
        """
        super().__init__(parent)
        self.config(bg = "white")

        self._number_header = tk.Label(self,
                                       bg = "white",
                                       font = "TkDefaultFont 10 bold",
                                       justify = tk.LEFT,
                                       width = 2,
                                       text = "#")
        self._number_header.pack(side = tk.LEFT)


        self._name_header = tk.Label(self,
                                     bg = "white",
                                     font = "TkDefaultFont 10 bold",
                                     justify = tk.LEFT,
                                     width = 5,
                                     text = "Name")
        self._name_header.pack(side = tk.LEFT)

        self._whitespace = tk.Frame(self,
                                    bg = "white",
                                    width = 55)
        self._whitespace.pack(side = tk.LEFT)

        self._questions_asked_header = tk.Label(self,
                                                bg = "white",
                                                font = "TkDefaultFont 10 bold",
                                                justify = tk.LEFT,
                                                width = 15,
                                                text = "Questions Asked")
        self._questions_asked_header.pack(side = tk.LEFT)

        self._time_header = tk.Label(self,
                                     bg = "white",
                                     font = "TkDefaultFont 10 bold",
                                     justify = tk.LEFT,
                                     width = 6,
                                     text = "Time")
        self._time_header.pack(side = tk.LEFT)

class QueuedRequest(tk.Frame):
    """
    A class to store the GUI elements of a request
    """
    def __init__(self, parent, name, questions_asked, time, parent_frame):
        """
        Sets up the frame

        Parameters:
            parent (tk.Frame): the tk frame class
            name (str): the name of the person making the request
            questions_asked (int): the number of quick/long questions the person has asked
            time (float): time.time() at the creation of the frame
            parent_frame (tk.Frame): the frame this is being packed into
        """
        super().__init__(parent)
        self.config(bg = "white")
        self._parent_frame = parent_frame
        self._name = name
        self._questions_asked = questions_asked
        self._initial_time = time

        self._number = tk.Label(self,
                                bg = "white",
                                text = "0")
        self._number.pack(side = tk.LEFT,
                          ipadx = 6)

        self._name_label = tk.Label(self,
                                    bg = "white",
                                    justify = tk.LEFT,
                                    text = name)
        self._name_label.pack(side = tk.LEFT)

        self._green_button_border = tk.Frame(self, #The button is put in a frame to allow a custom border colour
                                             bg = "#28C053",
                                             bd = 2)
        self._green_button_border.pack(side = tk.RIGHT)

        self._green_button = tk.Button(self._green_button_border,
                                       bg = "#A1E1AB",
                                       activebackground = "#A1E1AB",
                                       relief = tk.FLAT,
                                       font = "TkDefaultFont 1",
                                       command = self.resolve)
        self._green_button.pack(ipadx = 6,
                                ipady = 5)

        self._red_button_border = tk.Frame(self,
                                           bg = "#E94341",
                                           bd = 2)
        self._red_button_border.pack(side = tk.RIGHT)

        self._red_button = tk.Button(self._red_button_border,
                                     bg = "#F6A5A3",
                                     activebackground = "#F6A5A3",
                                     relief = tk.FLAT,
                                     font = "TkDefaultFont 1", #Reduces the minimum size of the button
                                     command = self.cancel)
        self._red_button.pack(ipadx = 6,
                              ipady = 5)

        self._time_label = tk.Label(self,
                                 bg = "white",
                                 width = 14,
                                 text = "a few seconds ago")
        self._time_label.pack(side = tk.RIGHT,
                              padx = 20)

        self._whitespace = tk.Frame(self,
                                    bg = "white",
                                    width = 93)
        self._whitespace.pack(side = tk.RIGHT)
        
        self._questions_asked_label = tk.Label(self,
                                 bg = "white",
                                 text = str(questions_asked))
        self._questions_asked_label.pack(side = tk.RIGHT)

    def cancel(self):
        """
        Deletes the request from the queue and subtracts the question asked from the user
        """
        self._parent_frame.users[self._name] -= 1 #Subtracts 1 from questions asked
        self.resolve()

    def resolve(self):
        """
        Resolves the request, deleting it from the queue
        """
        for request in self._parent_frame.current_requests:
            if self._name == request.get_name():
                self._parent_frame.current_requests.remove(request)
                self._parent_frame.order_queue() #Finds the correct user, removes them from the queue and reorders the queue
                break
        self.destroy() #Remove the info from the window

    def get_time(self):
        """
        (int) Returns the number of seconds since the request was made, rounded down
        """
        return math.floor(t.time() - self.get_initial_time())

    def get_name(self):
        """
        (str) Returns the name of the person making the request
        """
        return self._name

    def get_questions_asked(self):
        """
        (int) Returns the number of questions the person has asked
        """
        return self._questions_asked

    def get_initial_time(self):
        """
        (float) Returns the time.time() value at creation of the request
        """
        return self._initial_time

    def set_number(self, number):
        """
        Sets the number down the queue the request is
        """
        self._number.config(text = str(number))

    def tick(self):
        """
        Updates the time text
        """
        self._time_label.config(text = get_time_text(self.get_time()) + " ago")

class TopFrame(tk.Frame):
    """
    A class for the top banner about plagarism
    """
    
    def __init__(self, parent):
        """
        Sets up the frame

        Parameters:
            parent (tk.Frame): the tk frame class
        """
        super().__init__(parent)
        self.bind("<Configure>", self.update_width) #Sets the function self.update_width to run when the frame dimensions are changed
        self.update() #Allows winfo_width() to return the actual width rather than 1

        self._whitespace = tk.Frame(self,
                                    bg = "#FEFCEC")
        self._whitespace.pack(side = tk.TOP,
                              pady = 7)

        self._important_label = tk.Label(self,
                                         bg = "#FEFCEC",
                                         fg = "#C59943",
                                         font = "TkDefaultFont 12 bold",
                                         text = "Important")
        self._important_label.pack(side = tk.TOP,
                                   anchor = tk.W,
                                   padx = 20)

        
        self._plagarism_label = tk.Label(self,
                                         bg = "#FEFCEC",
                                         fg = "black",
                                         wraplength = 1, #Placeholder, this is updated by update_width when more widgets are added to the window
                                         justify = tk.LEFT,
                                         text = "Individual assessment items must be solely your own work. While students are encouraged to have high-level conversations about problems they are trying to solve, you must not look at another student's code or copy from it. The university uses sophisticated anti-collusion measures to automatically detect similarity between assignment submissions.")
        self._plagarism_label.pack(side = tk.TOP,
                                   anchor = tk.W,
                                   padx = 20)

    def update_width(self, event):
        """
        Updates the wrapping width of the text

        Parameters:
            event (event): When the frame dimensions change
        """
        self._plagarism_label.config(wraplength = self.winfo_width() - 40) #Makes the label wrap to the window upon resizing. The -40 accounts for padding

class SideFrame(tk.Frame):
    """
    A class for the body frames
    """

    def __init__(self, parent):
        """
        Sets up the frame

        Parameters:
            parent (tk.Frame): the tk frame class
        """
        super().__init__(parent)
        self._other_side_frame = None
        self._popup = None
        self._popup_open = False
        
        self.users = {} #{name: questions asked}
        self.current_requests = []

        self._top_frame = tk.Frame(self) #A large coloured box
        self._top_frame.pack(side = tk.TOP,
                             anchor = tk.NW,
                             fill = tk.X,
                             padx = 20,
                             pady = 20)

        self._top_label1 = tk.Label(self._top_frame,
                                    font = "TkDefaultFont 18 bold")
        self._top_label1.pack(side = tk.TOP,
                              anchor = tk.N,
                              padx = 110,
                              pady = 20)

        self._top_label2 = tk.Label(self._top_frame,
                                    fg = "#666666",
                                    font = "TkDefaultFont 10")
        self._top_label2.pack(side = tk.BOTTOM,
                              anchor = tk.S,
                              pady = 20)

        self._bullets = tk.Label(self,
                                 bg = "white",
                                 justify = tk.LEFT)
        self._bullets.pack(side = tk.TOP,
                           anchor = tk.W,
                           padx = 20)

        self._button_border = tk.Frame(self, #As in the QueuedRequest __init__, putting the button in a frame allows a custom border colour
                                       bd = 2)
        self._button_border.pack(side = tk.TOP,
                                 anchor = tk.N,
                                 pady = 15)

        self._button = tk.Button(self._button_border,
                                 fg = "white",
                                 relief = tk.FLAT,
                                 command = self.request_help)
        self._button.pack(ipadx = 10,
                          ipady = 5)

        self._hline1 = HorizontalLine(self)
        self._hline1.pack(fill = tk.X,
                          padx = 20)

        self._queue_status = tk.Label(self,
                                      bg = "white",
                                      text = "No students in queue",
                                      justify = tk.LEFT)
        self._queue_status.pack(anchor = tk.W,
                                pady = 12,
                                padx = 20)

        self._hline2 = HorizontalLine(self)
        self._hline2.pack(fill = tk.X,
                          padx = 20)

        self._table_header = TableHeader(self)
        self._table_header.pack(anchor = tk.W,
                                padx = 20,
                                pady = 5)

        self._hline3 = HorizontalLine(self)
        self._hline3.pack(fill = tk.X,
                          padx = 20)

    def request_help(self):
        """
        Opens a popup for name input
        """
        if self._popup_open == False and self._other_side_frame._popup_open == False: #If there is no popup open
            self._popup = tk.Toplevel(bg = "#DBF3D6") #Create popup
            self._popup.protocol("WM_DELETE_WINDOW", self.close) #Makes self.close run when the window is closed by the x button, so that self._popup_open can be reset to False
            self._popup._label1 = tk.Label(self._popup,
                               bg = "#DBF3D6",
                               text = "What is your name?")
            self._popup._label1.pack(side = tk.TOP)

            self._popup._label2 = tk.Label(self._popup,
                               bg = "#DBF3D6",
                               fg = "#E94341",
                               text = "")
            self._popup._label2.pack(side = tk.TOP)

            self._popup._text = tk.Entry(self._popup)
            self._popup._text.pack(side = tk.TOP,
                        padx = 5,
                        fill = tk.X)

            self._popup._confirm_button = tk.Button(self._popup,
                                         bg = "#D5EDF9",
                                         text = "Confirm",
                                         command = self.confirm)
            self._popup._confirm_button.pack(side = tk.LEFT,
                                  padx = 5,
                                  fill = tk.X,
                                  expand = True)

            self._popup._cancel_button = tk.Button(self._popup,
                                        bg = "#D5EDF9",
                                        text = "Cancel",
                                        command = self.close)
            self._popup._cancel_button.pack(side = tk.RIGHT,
                                 padx = 5,
                                 fill = tk.X,
                                 expand = True)

    def confirm(self):
        """
        Adds the inputted name to the queue if it is valid
        """
        if self._popup._text.get() == "":
            self._popup._label2.config(text = "Please enter a name") #Ensures the user does not enter an empty string
        else:
            already_queued = False
            name = self._popup._text.get()
            for request in self.current_requests + self._other_side_frame.current_requests:
                if name == request.get_name():
                    already_queued = True #Loops through all current requests to see if the user is already queued
                    break
            if already_queued:
                self._popup._label2.config(text = "You are already queued")
            else:
                self.add_to_queue(name) #If the name is valid, adds the user to the queue
                self.close() #Closes the popup

    def close(self):
        """
        Closes the popup
        """
        self._popup_open = False
        self._popup.destroy()

    def add_to_queue(self, name):
        """
        Adds the user to the queue
        """
        if name in self.users:
            self.users[name] += 1 #adds 1 to questions asked for that user
        else:
            self.users[name] = 0
        request = QueuedRequest(self, name, self.users[name], t.time(), self) #(name, questions asked, initial time, parent frame)
        self.current_requests.append(request) #adds request to list
        self.order_queue() #orders the queue

    def order_queue(self):
        """
        Orders the queue by questions asked and wait time
        """
        self.current_requests.sort(key = lambda x : (x.get_questions_asked(), x.get_initial_time())) #sorts by Questions asked, then by time descending
        for request in self.current_requests:
            request.pack_forget() #Removes all requests from the frame, but doesn't delete them
        queue_number = 1
        for request in self.current_requests:
            request.set_number(queue_number)
            request.pack(anchor = tk.W,
                         padx = 20,
                         fill = tk.X) #Puts requests back into the frame in the correct order with corresponding queue number
            queue_number += 1

    def set_other_side_frame(self, frame):
        """
        Stores the other side frame in a variable to access its requests
        """
        self._other_side_frame = frame

class LeftFrame(SideFrame):
    """
    A class that extends SideFrame to set unique styling
    """
    
    def __init__(self, parent):
        """
        Sets up the styling

        Parameters:
            parent (SideFrame): the SideFrame class
        """
        super().__init__(parent)
        self._top_frame.config(bg = "#DBF3D6")
        self._top_label1.config(bg = "#DBF3D6",
                                fg = "#1F7B38",
                                text = "Quick Questions")
        self._top_label2.config(bg = "#DBF3D6",
                                text = "< 2 mins with a tutor")
        self._bullets.config(text = u"Some examples of quick questions: \n \u2022 Syntax errors \n \u2022 Interpreting error output \n \u2022 Assignment/MyPyTutor interpretation \n \u2022 MyPyTutor Submission issues")
        #u before a string allows unicode values to be used. \u2022 is the value for a bullet point
        self._button_border.config(bg = "#28C053")
        self._button.config(bg = "#9ADAA4",
                            activebackground = "#9ADAA4",
                            text = "Request Quick Help")

class RightFrame(SideFrame):
    """
    A class that extends SideFrame to set unique styling
    """
    
    def __init__(self, parent):
        """
        Sets up the styling

        Parameters:
            parent (SideFrame): the SideFrame class
        """
        super().__init__(parent)
        self._top_frame.config(bg = "#D5EDF9")
        self._top_label1.config(bg = "#D5EDF9",
                                fg = "#186F93",
                                text = "Long Questions")
        self._top_label2.config(bg = "#D5EDF9",
                                text = "> 2 mins with a tutor")
        self._bullets.config(text = u"Some examples of long questions: \n \u2022 Open ended questions \n \u2022 How to start a problem \n \u2022 How to improve code \n \u2022 Debugging \n \u2022 Assignment help")
        self._button_border.config(bg = "#31C1E3")
        self._button.config(bg = "#96D4E5",
                            activebackground = "#96D4E5",
                            text = "Request Long Help")

class App(object):
    """
    A class for the main window
    """

    def __init__(self, master):
        """
        Sets up the styling

        Parameters:
            master (tk.Tk): the tk root object
        """
        self._master = master
        self._master.title("CSSE1001 Queue")
        self._master.resizable(False, True) #Disables resizing in the horizontal direction
        self._minesweeper = None
        self.minesweeper_open = False

        self._top_frame = TopFrame(self._master)
        self._top_frame.config(bg = "#FEFCEC")
        self._top_frame.pack(side = tk.TOP,
                             fill = tk.X,
                             ipady = 8)

        self._body_frame = tk.Frame(self._master)
        self._body_frame.pack(side = tk.TOP)

        self._left_frame = LeftFrame(self._body_frame)
        self._left_frame.config(bg = "white")
        self._left_frame.pack(side = tk.LEFT,
                              fill = tk.BOTH,
                              expand = True)

        self._right_frame = RightFrame(self._body_frame)
        self._right_frame.config(bg = "white")
        self._right_frame.pack(side = tk.RIGHT,
                               fill = tk.BOTH,
                               expand = True)
        
        self._left_frame.set_other_side_frame(self._right_frame)
        self._right_frame.set_other_side_frame(self._left_frame)

        self._whitespace = tk.Frame(self._master, #Adds space at the bottom of the window
                                    bg = "white",
                                    height = 50)
        self._whitespace.pack(side = tk.BOTTOM,
                              anchor = tk.S,
                              expand = True,
                              fill = tk.X)

        menubar = tk.Menu(self._master) 
        self._master.config(menu = menubar) #Adds menubar
        gamemenu = tk.Menu(menubar)
        menubar.add_cascade(label = "Games", menu = gamemenu)
        gamemenu.add_command(label = "Minesweeper", command = self.play_minesweeper) #Adds an option to open minesweeper under a games tab in the menubar

        self.tick() #Starts tick iterating

    def tick(self):
        """
        Updates the times displayed every second
        """
        total_request_time = [0, 0] #Quick requests, long requests
        for request in self._left_frame.current_requests:
            request.tick() #Upadates the time label for each request
            total_request_time[0] += request.get_time() #Gets the total wait time
        for request in self._right_frame.current_requests:
            request.tick()
            total_request_time[1] += request.get_time()
        num_requests = [len(self._left_frame.current_requests), len(self._right_frame.current_requests)]

        avg_request_time = [0, 0] #Placeholders

        for i in range(0, 2):
            if num_requests[i] != 0: #Prevents a division by zero error
                avg_request_time[i] = total_request_time[i]/num_requests[i] #Gets the average wait time

        if num_requests[0] == 0:
            self._left_frame._queue_status.config(text = "No students in queue")
        elif num_requests[0] == 1:
            self._left_frame._queue_status.config(text = "An average wait time of " + get_time_text(avg_request_time[0]) + " for 1 student")
        else:
            self._left_frame._queue_status.config(text = "An average wait time of " + get_time_text(avg_request_time[0]) + " for " + str(num_requests[0]) + " students")

        if num_requests[1] == 0:
            self._right_frame._queue_status.config(text = "No students in queue")
        elif num_requests[1] == 1:
            self._right_frame._queue_status.config(text = "An average wait time of " + get_time_text(avg_request_time[1]) + " for 1 student")
        else:
            self._right_frame._queue_status.config(text = "An average wait time of " + get_time_text(avg_request_time[1]) + " for " + str(num_requests[1]) + " students")
        
        self._master.after(1000, self.tick) #Runs the function again after around 1 second

    def play_minesweeper(self):
        """
        Opens minesweeper in a popup window
        """
        if self.minesweeper_open == False:
            self.minesweeper_open = True
            window = tk.Toplevel() #Opens a popup for minesweeper
            window.protocol("WM_DELETE_WINDOW", self.close_minesweeper) #Allows self.minesweeper_open to be set to False when the x button is clicked
            self._minesweeper = Minesweeper(window, self)

    def close_minesweeper(self):
        """
        Closes the minesweeper popup
        """
        self._minesweeper.exit()
        self.minesweeper_open = False

class MinesweeperSquare(tk.Button):
    """
    A class for an individual square within the minesweeper game
    """
    
    def __init__(self, parent, parent_frame, row, column):
        """
        Initialises the square

        Parameters:
            parent (tk.Button): The tk button object
            parent_frame (tk.Frame): The game board the square belongs to
            row (int): The row the square is in
            column (int): The column the square is in
        """
        super().__init__(parent)
        self._parent_frame = parent_frame
        self._row = row
        self._column = column
        self._adjacent_mines = 0
        self._flagged = False
        self._clicked = False
        self.config(command = self.click,
                    bg = "#EEEEEE",
                    bd = 1,
                    width = 2,
                    height = 1,
                    disabledforeground = "black",
                    relief = tk.RIDGE)

    def click(self):
        """
        Reveals or flags the square depending on mode
        """
        if self._clicked == False:
            if self._parent_frame.mode == "reveal" and self._flagged == False: #Revealing an unflagged square
                self._clicked = True #Stops the button from being clicked again
                if self._adjacent_mines == -1:
                    self._parent_frame._parent_window.end_game(False) #If the clicked square was a mine, lose the game
                elif self._adjacent_mines == 0:
                    self._parent_frame.remaining_squares -= 1
                    for x in range(-1, 2):
                            if self._row + x >= 0 and self._row + x <= 8:
                                for y in range(-1, 2):
                                    if self._column + y >= 0 and self._column + y <= 8 and not (x == 0 and y == 0): #Checks all adjacent squares (in a 3x3 square), but does not try to check squares off the side of the board
                                        self._parent_frame._squares[self._row + x][self._column + y].click() #Automatically click adjacent square for a square with no adjacent mines

                else:
                    self._parent_frame.remaining_squares -= 1
                self.display_value() #Displays the number of adjacent mines
                if self._parent_frame.remaining_squares == 0:
                    self._parent_frame._parent_window.end_game(True) #If all non-mine squares have been revealed win the game
            elif self._parent_frame.mode == "flag":
                if self._flagged:
                    self._flagged = False
                    self.config(text = "")
                    self._parent_frame.flagged_squares -= 1
                    self._parent_frame._parent_window._mine_counter.config(text = "Mines left: " + str(10 - self._parent_frame.flagged_squares)) #Updates remaining mines counter
                elif self._parent_frame.flagged_squares < 10: #Will not flag if there are already 10 flagged squares
                    self._flagged = True
                    self.config(text = "O") #Marks the square
                    self._parent_frame.flagged_squares += 1
                    self._parent_frame._parent_window._mine_counter.config(text = "Mines left: " + str(10 - self._parent_frame.flagged_squares))

    def display_value(self):
        """
        Displays the number of adjacent mines and updates style to indicate that the square has been revealed
        """
        if self._adjacent_mines == -1: #Mine
            display_text = "X"
        elif self._adjacent_mines == 0:
            display_text = ""
        else:
            display_text = str(self._adjacent_mines)

        self.config(relief = tk.SOLID,
                    bg = "#CCCCCC",
                    state = tk.DISABLED, #Stops the square from being clicked again
                    text = display_text)

class GameFrame(tk.Frame):
    """
    A class for the minesweeper gameboard
    """
    
    def __init__(self, parent, parent_window):
        """
        Initialises the grid

        Parameters:
            parent (tk.Frame): The tk frame object 
            parent_frame (tk.Tk): The window the grid is in
        """
        super().__init__(parent)
        self._parent_window = parent_window
        self.remaining_squares = 71 #10/81 squares are mines, the rest must be revealed
        self.flagged_squares = 0
        self._mine_positions = []
        self._squares = []
        self.mode = "reveal"

        possible_squares = list(range(0, 81))
        for i in range(0, 10):
            self._mine_positions.append(possible_squares.pop(random.randint(0, len(possible_squares) - 1)))
            #Takes a random remaining number from possible_squares, removes it and appends it to self._mine_positions

        for i in range(0, 9): #9 x 9 grid
            row = []
            for j in range(0, 9):
                square = MinesweeperSquare(self, self, i, j)
                square.grid(row = i, column = j)
                square_number = 9*i + j #Gives the square a unique number from 0 - 80
                mine = False
                for number in self._mine_positions:
                    if number == square_number:
                        mine = True #Determine if the square is one assigned as a mine
                        break
                if mine:
                    square._adjacent_mines = -1 #Mines are indicated by an adjacent_mines value of -1
                row.append(square)
            self._squares.append(row)

        for i in range(0, 9): #Once all mines have been placed, loop through again to find the value of each square
            for j in range(0, 9):
                if self._squares[i][j]._adjacent_mines != -1: #if the square isn't a mine
                    adjacent_mines = 0
                    for x in range(-1, 2):
                        if i + x >= 0 and i + x <= 8: #Doesn't check outside the game board
                            for y in range(-1, 2): #Loops through adjacent squares
                                if j + y >= 0 and j + y <= 8 and not (x == 0 and y == 0):
                                    if self._squares[i + x][j + y]._adjacent_mines == -1: #If the square is a mine
                                        adjacent_mines += 1
                    self._squares[i][j]._adjacent_mines = adjacent_mines

class Minesweeper(object):
    """
    A class to run the minesweeper game
    """
    def __init__(self, master, parent_app):
        """
        Initialises the window

        Parameters:
            master (tk.Tk): The tk root object
        """
        self._master = master
        self._master.title("Minesweeper")
        self._master.configure(bg = "#DBF3D6")
        self._master.resizable(False, False)
        self._parent_app = parent_app

        self._controls = tk.Frame(self._master,
                                  bg = "#DBF3D6")
        self._controls.pack(side = tk.TOP,
                            fill = tk.X,
                            padx = 5,
                            pady = 5)

        self._mode_switch = tk.Button(self._controls,
                                      bg = "#D5EDF9",
                                      activebackground = "#A8DAF3",
                                      bd = 1,
                                      relief = tk.RIDGE,
                                      text = "Current mode: Reveal. Switch to Flag",
                                      command = self.switch_mode)
        self._mode_switch.pack(side = tk.LEFT)

        self._exit = tk.Button(self._controls,
                               bg = "#D5EDF9",
                               activebackground = "#A8DAF3",
                               bd = 1,
                               relief = tk.RIDGE,
                               text = "Exit",
                               command = self.exit)
        self._exit.pack(side = tk.LEFT)

        self._mine_counter = tk.Label(self._master,
                                      text = "Mines left: 10",
                                      bg = "#DBF3D6")
        self._mine_counter.pack(side = tk.TOP,
                                fill = tk.X)

        self._game_grid = GameFrame(self._master, self) #Creates a grid linked to this window
        self._game_grid.pack(side = tk.TOP)

        self._whitespace = tk.Frame(self._master,
                                    bg = "#DBF3D6")
        self._whitespace.pack(side = tk.BOTTOM,
                              pady = 10)

    def switch_mode(self):
        """
        Toggles between reveal and flag modes
        """
        if self._game_grid.mode == "reveal":
            self._game_grid.mode = "flag"
            self._mode_switch.config(text = "Current mode: Flag. Switch to Reveal")
        else:
            self._game_grid.mode = "reveal"
            self._mode_switch.config(text = "Current mode: Reveal. Switch to Flag")

    def end_game(self, win):
        """
        Ends the game for a win or a loss

        Parameters:
            win (bool): Whether the game ended in a win (True) or loss (False)
        """
        if win:
            text_displayed = "Congratulations! You won!"
        else:
            text_displayed = "Unlucky! You lost."
        self._mine_counter.config(text = text_displayed)
        self._mode_switch.config(state = tk.DISABLED) #Disables the mode switch button
        for i in range(0, 9):
            for j in range(0, 9):
                if self._game_grid._squares[i][j]._clicked == False:
                    self._game_grid._squares[i][j].display_value() #Reveals the value of all unrevealed squares, if they are not already revealed
        
    def exit(self):
        """
        Exits the window
        """
        self._parent_app.minesweeper_open = False
        self._master.destroy()

def get_time_text(seconds):
    """
    (str) converts the input number of seconds to a string that represents the amount of time

    Examples:
        get_time_text(10) = "a few seconds"
        get_time_text(100) = "a minute"
        get_time_text(1000) = "16 minutes"
        get_time_text(5000) = "1 hour"
        get_time_text(15000) = "4 hours"

    """
    if seconds >= 7200:
        return str(math.floor(seconds/3600)) + " hours"
    elif seconds >= 3600:
        return "1 hour"
    elif seconds >= 120:
        return str(math.floor(seconds/60)) + " minutes"
    elif seconds >= 60:
        return "a minute"
    else:
        return "a few seconds"

root = tk.Tk()
app = App(root)
root.mainloop()
