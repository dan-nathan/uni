#include <stdio.h>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <err.h>


extern char **environ;

int main(int argc, char **argv) {
	char buf[65536];
	const char dev_acct[20] = "/dev/acct";
	char *args[2];
	int fd, flags, read_bytes, status, val;

	/* Open in blocking mode */
	flags = O_RDONLY;
	fd = open(dev_acct, flags);
	if (fd == -1) {
		err(1, "%s%s%s", dev_acct,
		    (flags & O_RDWR) ? " O_RDWR" : " O_RDONLY",
		    (flags & O_NONBLOCK) ? " O_NONBLOCK" : "");
	} else
		printf("Opened %s with fd %d \n", dev_acct, fd);

	printf("Result from ls: \n\n");
        args[0] = "/bin/ls";
        args[1] = NULL;
        if (fork() == 0)
        	execv(args[0], args);
        else
        	wait(&status);

	printf("\n\nA fork, exec and exit message would have been generated\n");
	printf("3 messages total in queue \n");

	read_bytes = read(fd, buf, sizeof(buf));
	if (read_bytes == -1)
		err(1, "");

	printf("%d bytes were read \n", read_bytes);

	printf("Closing and reopening, message queue should be cleared and");
	printf(" next read should block\n");

	close(fd);
	fd = open(dev_acct, flags);
	if (fd == -1) {
		err(1, "%s%s%s", dev_acct,
		    (flags & O_RDWR) ? " O_RDWR" : " O_RDONLY",
		    (flags & O_NONBLOCK) ? " O_NONBLOCK" : "");
	} else
		printf("Opened %s with fd %d \n", dev_acct, fd);

	read_bytes = read(fd, buf, sizeof(buf));
	if (read_bytes == -1) {
		err(1, "");
	}
	printf("%d bytes were read \n", read_bytes);
 	return 0;
}
