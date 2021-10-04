#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <unistd.h>
#include <signal.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <fcntl.h>

int main(int argc, char** argv) {
    printf("@PLAYD1\n");
    fflush(stdout);
    printf("PLAYD:\n");
    fflush(stdout);
}
