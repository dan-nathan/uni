#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <pthread.h>
#include <netdb.h>
#include <signal.h>
#include <unistd.h>
#include <semaphore.h>

#define INVALID_INDEX -1
#define INVALID_ITEM_NAME ":"
#define INVALID_DEPOT_NAME ":"

// A enum giving meaningful names to errors/exit statuses
typedef enum {
    NO_ERROR = 0,
    INCORRECT_NO_ARGS = 1,
    INVALID_NAME = 2,
    INVALID_QUANTITY = 3
} Status;

// A enum representing operation types
typedef enum {
    DELIVER = 0,
    WITHDRAW = 1,
    TRANSFER = 2,
    INVALID_OPERATION_TYPE = 3
} OperationType;

// A struct representing an item with quantity
typedef struct {
    char* name;
    int quantity;
} Item;

// A struct representing a defered operation
typedef struct {
    Item item;
    unsigned int key;
    //int neighbourIndex;
    char* destination;
} Operation;

// A struct representing info about another depot
typedef struct {
    char* name;
    FILE* write;
    int port;
} Neighbour;

// A struct representing all info about the hub
typedef struct {
    char* name;
    int noItems;
    Item* items;
    int noNeighbours;
    Neighbour* neighbours;
    int noDeferedOperations;
    Operation* deferedOperations;
    unsigned short port;
    sem_t* lock;
} DepotInfo;

// A struct representing info to be passed into the client thread function
typedef struct {
    DepotInfo* depotInfo;
    char* port;
} ClientThreadInfo;

// A struct representing info to be passed into the server thread function
typedef struct {
    DepotInfo* depotInfo;
    int fd;
} ServerThreadInfo;

// connect_as_client and process_input call each other, so one needs to be
// defined earlier
void* connect_as_client(void* threadInfo);

// A flag to determine if SIGHUP has been received
bool receivedSighup;

/* Verify that a name has no invalid characters
 *
 * @param name - the name to verify
 * @return wheter or not the name is valid
 */
bool verify_name(char* name) {
    if (strlen(name) == 0) {
        return false;
    }
    const char* invalidCharacters = " \n\r:";
    char* character = name;
    while (*character != '\0') {
        // Check against each invalid character
        for (int i = 0; i < strlen(invalidCharacters); i++) {
            if (*character == invalidCharacters[i]) {
                return false;
            }
        }
        character++;
    }
    return true;
}

/* Reads a line from a file
 *
 * @param file - the file to read from
 * @return a line from the file, or NULL if EOF was encountered
 */
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

/* Converts a string to a positive int
 *
 * @param text - the string to convert
 * @return the integer value of the string, or -1 if the string isn't a valid
 *         positive integer
 */
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

/* Converts a string to an item name with quantity
 *
 * @param text - the string to convert
 * @return the item the string represents, or an invalid item if the string is
 *         formatted incorrectly
 */
Item string_to_item(char* text) {
    Item item;
    item.quantity = 0;
    item.name = INVALID_ITEM_NAME;
    char* subString = strtok(text, ":");
    int quantity = string_to_int(subString);
    // If quantity isn't a valid int, return an invalid item
    if (quantity <= 0) {
        return item;
    }
    subString = strtok(NULL, ":");
    if (subString == NULL) {
        return item;
    }
    // If the name is invalid return an invalid item
    if (!verify_name(subString)) {
        return item;
    }
    item.quantity = quantity;
    item.name = subString;
    subString = strtok(NULL, ":");
    if (subString != NULL) {
        item.quantity = 0;
        item.name = INVALID_ITEM_NAME;
        return item;
    }
    return item;
}

/* Displays an error message given an exit status
 *
 * @param status - the exit status
 * @return the exit status
 */
Status display_error_message(Status status) {
    const char* errorMessages[] = {"",
            "Usage: 2310depot name {goods qty}\n",
            "Invalid name(s)\n",
            "Invalid quantity\n"};
    fprintf(stderr, errorMessages[status]);
    return status;
}

/* Prints out the depot's goods and neighbours
 *
 * @param depotInfo - info about the depot
 */
void handle_sighup(DepotInfo* depotInfo) {
    sem_wait(depotInfo->lock);
    printf("Goods:\n");
    for (int i = 0; i < depotInfo->noItems; i++) {
        if (depotInfo->items[i].quantity != 0) {
            printf("%s %d\n", depotInfo->items[i].name,
                    depotInfo->items[i].quantity);
        }
    }
    printf("Neighbours:\n");
    for (int i = 0; i < depotInfo->noNeighbours; i++) {
        printf("%s\n", depotInfo->neighbours[i].name);
    }
    fflush(stdout);
    receivedSighup = false;
    sem_post(depotInfo->lock);
}

/* Sets the SIGHUP flag to true
 *
 * @param signum - the type of signal
 */
void set_sighup_flag(int signum) {
    receivedSighup = true;
}

/* Sets up a handler for SIGHUP
 */
void setup_sighup_handler(void) {
    struct sigaction sig;
    // valgrind doesn't like sa_mask being uninitialised
    memset(&sig, 0, sizeof(struct sigaction));
    sig.sa_handler = set_sighup_flag;
    sig.sa_flags = SA_RESTART;
    sigaction(SIGHUP, &sig, 0);
}

/* Adds another depot as a neighbour of this one. Inserts it in the correct
 *         place in the list so that it is lexicographically ordered
 *
 * @param depotInfo - info about the depot
 * @param name - the name of the new neighbour
 * @param write - a FILE pointer that writes to the depot
 * @param port - the port the neighbour is on
 */
void add_neighbour(DepotInfo* depotInfo, char* name, FILE* write, int port) {
    // Find the index to insert at
    int index = depotInfo->noNeighbours;
    for (int i = 0; i < depotInfo->noNeighbours; i++) {
        // Check that this is not already a neighbour
        if (strcmp(depotInfo->neighbours[i].name, name) == 0) {
            free(name);
            sem_post(depotInfo->lock);
            return;
        }
        if (strcmp(depotInfo->neighbours[i].name, name) > 0) {
            index = i;
            break;
        }
    }

    // Increase the size of the neighbours array
    if (depotInfo->noNeighbours == 0) {
        depotInfo->neighbours = malloc(sizeof(Neighbour));
    } else {
        depotInfo->neighbours = realloc(depotInfo->neighbours,
                sizeof(Neighbour) * (depotInfo->noNeighbours + 1));
    }

    // Shift all the neighbours over so that this one can be inserted
    for (int i = depotInfo->noNeighbours - 1; i >= index; i--) {
        depotInfo->neighbours[i + 1] = depotInfo->neighbours[i];
    }
    // Add this neighbour
    depotInfo->neighbours[index].name = name;
    depotInfo->neighbours[index].write = write;
    depotInfo->neighbours[index].port = port;
    depotInfo->noNeighbours++;
}

/* Adds an item to track for this depot. Inserts in such that this list of
 *         items is lexicographically sorted
 *
 * @param depotInfo - info about the depot
 * @param item - the item to insert, with a quantity
 */
void add_item(DepotInfo* depotInfo, Item item) {
    // Find the index to insert at
    int index = depotInfo->noItems;
    for (int i = 0; i < depotInfo->noItems; i++) {
        if (strcmp(depotInfo->items[i].name, item.name) > 0) {
            index = i;
            break;
        }
    }

    // Increase the size of the items array
    depotInfo->items = realloc(depotInfo->items,
            sizeof(Item) * (depotInfo->noItems + 1));
    // Shift all the items over so that this one can be inserted
    for (int i = depotInfo->noItems - 1; i >= index; i--) {
        depotInfo->items[i + 1] = depotInfo->items[i];
    }
    // Add this item
    depotInfo->items[index] = item;
    depotInfo->noItems++;
}

/* Updates the quantity of an item, adding it to the list if it is not
 *         currently tracked
 *
 * @param depotInfo - info about the depot
 * @param item - the item name, with the change to quantity
 */
void change_item(DepotInfo* depotInfo, Item item) {
    for (int i = 0; i < depotInfo->noItems; i++) {
        // If the item is already being kept track of, update the quantity
        if (strcmp(depotInfo->items[i].name, item.name) == 0) {
            depotInfo->items[i].quantity += item.quantity;
            return;
        }
    }
    // If the item in not being kept track of, add it
    add_item(depotInfo, item);
}

/* Adds a defered deliver or withdraw operation to keep track of
 *
 * @param depotInfo - info about the depot
 * @param item - the item name, with the change to quantity
 * @param key - the key of the operation
 * @param destination - the depot being delivered to if this is a transfer
 *         operaiton, null if a different operation type
 */
void add_operation(DepotInfo* depotInfo, Item item, unsigned int key,
        char* destination) {
    Operation operation;
    operation.item = item;
    operation.key = key;
    operation.destination = destination;
    sem_wait(depotInfo->lock);
    // Increase the size of the operations array
    if (depotInfo->noDeferedOperations == 0) {
        depotInfo->deferedOperations = malloc(sizeof(Operation));
    } else {
        depotInfo->deferedOperations = realloc(depotInfo->deferedOperations,
                sizeof(Operation) * (depotInfo->noDeferedOperations + 1));
    }
    depotInfo->deferedOperations[depotInfo->noDeferedOperations] = operation;
    depotInfo->noDeferedOperations++;
    sem_post(depotInfo->lock);
}

/* Processes a transfer message
 *
 * @param depotInfo - info about the depot
 * @param input - the message received
 */
void process_transfer(DepotInfo* depotInfo, char* input) {
    char* transferInfo = input + strlen("Transfer:");
    Item item;
    // quantity
    char* subString = strtok(transferInfo, ":");
    int quantity = string_to_int(subString);
    if (quantity <= 0) {
        return;
    }
    // item name
    subString = strtok(NULL, ":");
    if (subString == NULL) {
        return;
    }
    if (!verify_name(subString)) {
        return;
    }
    // Multiply by -1 to withdraw
    item.quantity = -1 * quantity;
    item.name = subString;
    // neighbour name
    subString = strtok(NULL, ":");
    if (subString == NULL) {
        return;
    }
    sem_wait(depotInfo->lock);
    for (int i = 0; i < depotInfo->noNeighbours; i++) {
        // If the neighbour name matches, send a deliver message
        if (strcmp(subString, depotInfo->neighbours[i].name) == 0) {
            fprintf(depotInfo->neighbours[i].write, "Deliver:%d:%s\n",
                    quantity, item.name);
            fflush(depotInfo->neighbours[i].write);
            change_item(depotInfo, item);
            break;
        }
    }
    sem_post(depotInfo->lock);
}

/* Executes defered operations of a given key
 *
 * @param depotInfo - info about the depot
 * @param key - the key of the operations to execute
 */
void execute(DepotInfo* depotInfo, unsigned int key) {
    sem_wait(depotInfo->lock);
    for (int i = 0; i < depotInfo->noDeferedOperations; ) {
        Operation operation = depotInfo->deferedOperations[i];
        if (operation.key == key) {
            if (operation.destination == 0) {
                // If the message is a deliver or withdraw, execute
                change_item(depotInfo, operation.item);
            } else {
                // If the message is a transfer, search for the correct
                // neighbour
                for (int j = 0; j < depotInfo->noNeighbours; j++) {
                    if (strcmp(depotInfo->neighbours[j].name,
                            depotInfo->deferedOperations[i].destination)
                            == 0) {
                        fprintf(depotInfo->neighbours[j].write,
                                "Deliver:%d:%s\n",
                                operation.item.quantity * -1,
                                operation.item.name);
                        fflush(depotInfo->neighbours[j].write);
                        // Only change item if neighbour name is valid
                        change_item(depotInfo, operation.item);
                        break;
                    }
                }
            }

            // Move the end operation to this position and check it again
            depotInfo->deferedOperations[i] = depotInfo
                    ->deferedOperations[depotInfo->noDeferedOperations - 1];
            depotInfo->noDeferedOperations--;
        } else {
            // Move to the next position in the array
            i++;
        }
    }
    sem_post(depotInfo->lock);
}

/* Represent an operation type as an enum
 *
 * @param text - the operation type
 * @return an enum representation of the operation
 */
OperationType get_operation_type(char* text) {
    if (strcmp(text, "Deliver") == 0) {
        return DELIVER;
    } else if (strcmp(text, "Withdraw") == 0) {
        return WITHDRAW;
    } else if (strcmp(text, "Transfer") == 0) {
        return TRANSFER;
    }
    return INVALID_OPERATION_TYPE;
}

/* Processes a defer message
 *
 * @param depotInfo - info about the depot
 * @param input - the message received
 */
void process_defer(DepotInfo* depotInfo, char* input) {
    char* deferInfo = input + strlen("Defer:");
    char* subString = strtok(deferInfo, ":");
    int key = string_to_int(subString);
    if (key < 0) {
        return;
    }
    // Message type
    subString = strtok(NULL, ":");
    if (subString == NULL) {
        return;
    }
    OperationType operationType = get_operation_type(subString);
    if (operationType == INVALID_OPERATION_TYPE) {
        return;
    }
    // Quantity
    subString = strtok(NULL, ":");
    if (subString == NULL) {
        return;
    }
    Item item;
    item.quantity = string_to_int(subString);
    if (item.quantity <= 0) {
        return;
    }
    // Item name
    subString = strtok(NULL, ":");
    if (subString == NULL) {
        return;
    }
    if (!verify_name(subString)) {
        return;
    }
    if (operationType == WITHDRAW || operationType == TRANSFER) {
        item.quantity *= -1;
    }
    item.name = subString;
    // Destination (if this is not a transfer message it should be NULL)
    subString = strtok(NULL, ":");
    if ((subString == NULL && operationType == TRANSFER)
            || (subString != NULL && operationType != TRANSFER)) {
        return;
    }
    if (operationType == TRANSFER) {
        add_operation(depotInfo, item, (unsigned int) key, subString);
    } else {
        add_operation(depotInfo, item, (unsigned int) key, 0);
    }
}

/* Processes a connect message
 *
 * @param depotInfo - info about the depot
 * @param input - the message received
 */
void process_connect(DepotInfo* depotInfo, char* input) {
    ClientThreadInfo* threadInfo = malloc(sizeof(ClientThreadInfo));
    threadInfo->depotInfo = depotInfo;
    threadInfo->port = input + strlen("Connect:");
    int port = string_to_int(threadInfo->port);
    sem_wait(depotInfo->lock);
    // Prevent connecting to itself
    if (port == depotInfo->port) {
        sem_post(depotInfo->lock);
        return;
    }
    // Prevent connecting to the same port twice
    for (int i = 0; i < depotInfo->noNeighbours; i++) {
        if (depotInfo->neighbours[i].port == port) {
            sem_post(depotInfo->lock);
            return;
        }
    }
    // Don't unlock yet. Do it after connecting has finished to prevent
    // crash when multiple connect messages for the same port are sent quickly
    pthread_t threadId;
    pthread_create(&threadId, 0, connect_as_client, threadInfo);
}

/* Processes a message
 *
 * @param depotInfo - info about the depot
 * @param input - the message received
 */
void process_input(DepotInfo* depotInfo, char* input) {
    if (strncmp(input, "Deliver:", strlen("Deliver:")) == 0
            && strlen(input) > strlen("Deliver:")) {
        char* deliverInfo = input + strlen("Deliver:");
        Item item = string_to_item(deliverInfo);
        // If the message is valid
        if (strcmp(item.name, INVALID_ITEM_NAME) != 0) {
            sem_wait(depotInfo->lock);
            change_item(depotInfo, item);
            sem_post(depotInfo->lock);
        }
    } else if (strncmp(input, "Withdraw:", strlen("Withdraw:")) == 0
            && strlen(input) > strlen("Withdraw:")) {
        char* withdrawInfo = input + strlen("Withdraw:");
        Item item = string_to_item(withdrawInfo);
        // Negate quantity to subtract instead of add
        item.quantity *= -1;
        if (strcmp(item.name, INVALID_ITEM_NAME) != 0) {
            sem_wait(depotInfo->lock);
            change_item(depotInfo, item);
            sem_post(depotInfo->lock);
        }
    } else if (strncmp(input, "Transfer:", strlen("Transfer:")) == 0
            && strlen(input) > strlen("Transfer:")) {
        process_transfer(depotInfo, input);
    } else if (strncmp(input, "Defer:", strlen("Defer:")) == 0
            && strlen(input) > strlen("Defer:")) {
        process_defer(depotInfo, input);
    } else if (strncmp(input, "Execute:", strlen("Execute:")) == 0
            && strlen(input) > strlen("Execute:")) {
        char* keyText = input + strlen("Execute:");
        int key = string_to_int(keyText);
        if (key < 0) {
            return;
        }
        execute(depotInfo, key);
    } else if (strncmp(input, "Connect:", strlen("Connect:")) == 0
            && strlen(input) > strlen("Connect:")) {
        process_connect(depotInfo, input);
    }
}

/* Processes an IM message
 *
 * @param depotInfo - info about the depot
 * @param input - the message received
 * @param write - a FILE pointer writing to the depot that sent the message
 * @return whether or not the message was a valid IM message
 */
bool process_im_message(DepotInfo* depotInfo, char* input, FILE* write) {
    // If the message doesn't start with "IM:"
    if (strncmp(input, "IM:", strlen("IM:")) != 0
            || strlen(input) <= strlen("IM:")) {
        return false;
    }
    char* imInfo = input + 3;
    // port no
    char* subString = strtok(imInfo, ":");
    int port = string_to_int(subString);
    if (port < 0) {
        return false;
    }
    // name
    subString = strtok(NULL, ":");
    if (!verify_name(subString)) {
        return false;
    }
    add_neighbour(depotInfo, subString, write, port);
    return true;
}

/* Connects to a server
 *
 * @param threadInfo - a struct containing info about the depot and the port
 *         being connected to
 */
void* connect_as_client(void* threadInfo) {
    ClientThreadInfo* clientInfo = (ClientThreadInfo*)threadInfo;
    DepotInfo* depotInfo = clientInfo->depotInfo;
    char* port = clientInfo->port;

    // Connect to a server on the given port number
    struct addrinfo* ai = 0;
    struct addrinfo hints;
    memset(&hints, 0, sizeof(struct addrinfo));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_flags = AI_PASSIVE;
    int err = getaddrinfo("127.0.0.1", port, &hints, &ai);
    if (err) {
        freeaddrinfo(ai);
        sem_post(depotInfo->lock);
        return 0;
    }
    int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (connect(fd, (struct sockaddr*)ai->ai_addr, sizeof(struct sockaddr))) {
        sem_post(depotInfo->lock);
        return 0;
    }

    // Set up FILE pointers to read and write
    int fd2 = dup(fd);
    FILE* write = fdopen(fd, "w");
    FILE* read = fdopen(fd2, "r");
    // Send IM message
    fprintf(write, "IM:%u:%s\n", depotInfo->port, depotInfo->name);
    fflush(write);
    char* line = read_line(read);
    // If the first message sent isn't an IM message
    if (!process_im_message(depotInfo, line, write)) {
        fclose(write);
        fclose(read);
        free(line);
        sem_post(depotInfo->lock);
        return 0;
    }
    // Allow more connections/messages
    sem_post(depotInfo->lock);
    while (1) {
        char* line = read_line(read);
        // End the thread if EOF is encountered (ie the connection broke)
        if (line == 0) {
            while(1);
        }
        process_input(depotInfo, line);
    }
}

/* Handles a connection from a client
 *
 * @param threadInfo - a struct containing info about the depot a fild
 *         descriptor of the client
 */
void* connect_as_server(void* threadInfo) {
    ServerThreadInfo* serverInfo = (ServerThreadInfo*)threadInfo;
    DepotInfo* depotInfo = serverInfo->depotInfo;
    // Set up read and write FILE pointers
    int fd = serverInfo->fd;
    int fd2 = dup(fd);
    FILE* write = fdopen(fd, "w");
    FILE* read = fdopen(fd2, "r");
    char* line = read_line(read);
    if (!process_im_message(depotInfo, line, write)) {
        fclose(write);
        fclose(read);
        free(line);
        return 0;
    }
    fprintf(write, "IM:%u:%s\n", depotInfo->port, depotInfo->name);
    fflush(write);
    while (1) {
        char* line = read_line(read);
        if (line == 0) {
            while(1);
        }
        process_input(depotInfo, line);
    }
}

/* Sets up a server and starts listening for connections
 *
 * @param depotInfo - info about the depot
 */
void setup_server(DepotInfo* depotInfo) {
    struct addrinfo* ai = 0;
    struct addrinfo hints;
    memset(&hints, 0, sizeof(struct addrinfo));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_flags = AI_PASSIVE;
    // Set up server on an arbitrary unused port
    int err = getaddrinfo("127.0.0.1", 0, &hints, &ai);
    if (err) {
        freeaddrinfo(ai);
        fprintf(stderr, "%s\n", gai_strerror(err));
        return;
    }
    // Create a socket and bind it to a port
    int serv = socket(AF_INET, SOCK_STREAM, 0);
    if (bind(serv, (struct sockaddr*)ai->ai_addr, sizeof(struct sockaddr))) {
        return;
    }

    struct sockaddr_in ad;
    memset(&ad, 0, sizeof(struct sockaddr_in));
    socklen_t len = sizeof(struct sockaddr_in);
    if (getsockname(serv, (struct sockaddr*)&ad, &len)) {
        return;
    }
    depotInfo->port = ntohs(ad.sin_port);
    printf("%u\n", depotInfo->port);
    fflush(stdout);
    if (listen(serv, 10)) {
        return;
    }

    // Start accepting connections
    int fd;
    while (fd = accept(serv, 0, 0), fd >= 0) {
        ServerThreadInfo* threadInfo = malloc(sizeof(ServerThreadInfo));
        threadInfo->depotInfo = depotInfo;
        threadInfo->fd = fd;
        pthread_t threadId;
        pthread_create(&threadId, 0, connect_as_server, threadInfo);
    }
}

/* Waits for the sighup flag to be set, and takes action when it does
 *
 * @param threadInfo - info about the depot
 */
void* listen_for_sighup(void* threadInfo) {
    DepotInfo* depotInfo = (DepotInfo*)threadInfo;
    while (1) {
        if (receivedSighup) {
            handle_sighup(depotInfo);
        }
    }
}

int main(int argc, char** argv) {
    // If there are not enough arguments, or an item without a quantity
    if (argc < 2 || argc % 2 != 0) {
        return display_error_message(INCORRECT_NO_ARGS);
    }
    DepotInfo depotInfo;
    if (!verify_name(argv[1])) {
        return display_error_message(INVALID_NAME);
    }
    depotInfo.name = argv[1];
    depotInfo.noItems = 0;
    depotInfo.items = malloc(sizeof(Item) * depotInfo.noItems);
    // Set up a mutex using a semaphore
    sem_t lock;
    sem_init(&lock, 0, 1);
    depotInfo.lock = &lock;
    // Check validity of  all names before checking validity of quantities
    for (int i = 2; i < argc; i += 2) {
        if (!verify_name(argv[i])) {
            return display_error_message(INVALID_NAME);
        }
    }
    for (int i = 2; i < argc; i += 2) {
        int quantity = string_to_int(argv[i + 1]);
        if (quantity < 0) {
            return display_error_message(INVALID_QUANTITY);
        }
        Item item;
        item.name = argv[i];
        item.quantity = quantity;
        // Add the items to the depot. This is currently the only thread so the
        // Semaphore doesn't need to be used
        change_item(&depotInfo, item);
    }
    depotInfo.noNeighbours = 0;
    depotInfo.noDeferedOperations = 0;
    setup_sighup_handler();
    // Create a thread that polls for sighup
    pthread_t threadId;
    pthread_create(&threadId, 0, listen_for_sighup, &depotInfo);
    setup_server(&depotInfo);
}
