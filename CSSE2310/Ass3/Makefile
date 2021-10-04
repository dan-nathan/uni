CFLAGS = -std=gnu99 -Wall -pedantic -g
.PHONY: all
.DEFAULT: all

all: 2310alice 2310bob 2310hub

util.o: util.c
	gcc $(CFLAGS) -c util.c -o util.o

player.o: player.c
	gcc $(CFLAGS) -c player.c -o player.o

alice.o: alice.c
	gcc $(CFLAGS) -c alice.c -o alice.o

bob.o: bob.c
	gcc $(CFLAGS) -c bob.c -o bob.o

hub.o: hub.c
	gcc $(CFLAGS) -c hub.c -o hub.o

2310alice: alice.o util.o player.o
	gcc $(CFLAGS) alice.o util.o player.o -o 2310alice

2310bob: bob.o util.o player.o
	gcc $(CFLAGS) bob.o util.o player.o -o 2310bob

2310hub: hub.o util.o
	gcc $(CFLAGS) hub.o util.o -o 2310hub
