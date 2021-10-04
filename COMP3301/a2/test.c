#include <stdio.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <err.h>


extern char **environ;

int main(int argc, char **argv) {
	const char dev_acct[20] = "/dev/acct";
	int flags = O_RDONLY | O_NONBLOCK;
	int fd = open(dev_acct, flags);
	if (fd == -1) {
		err(1, "%s%s%s", dev_acct,
		    (flags & O_RDWR) ? " O_RDWR" : " O_RDONLY",
		    (flags & O_NONBLOCK) ? " O_NONBLOCK" : "");
	} else {
		printf("fd %d \n", fd);
	}

	int status = 0;

	ioctl(fd, FIONBIO, &status);

	char *args[2];

        args[0] = "/bin/ls";        // first arg is the full path to the executable
        args[1] = NULL;             // list of args must be NULL terminated
        if ( fork() == 0 )
            execv( args[0], args ); // child: call execv with the path and the args
        else
            wait( &status );        // parent: wait for the child (not really necessary)

	ioctl(fd, FIONREAD, &status);
	printf("about to read %d", status);


	char buf[65536];
	int r = read(fd, buf, sizeof(buf));
	if (r == -1) {
		printf("error 1 \n");
		err(1, "a");
	}
	printf("read 1 : %d \n", r);
	close(fd);
	fd = open(dev_acct, flags ^ O_NONBLOCK);
	if (fd == -1) {
		err(1, "%s%s%s", dev_acct,
		    (flags & O_RDWR) ? " O_RDWR" : " O_RDONLY",
		    (flags & O_NONBLOCK) ? " O_NONBLOCK" : "");
	} else {
		printf("fd %d \n", fd);
	}
	r = read(fd, buf, sizeof(buf));
	if (r == -1) {
		printf("error 2 \n");
		err(1, "a");
	}
	printf("read 2 : %d \n", r);


    return 0;
}
