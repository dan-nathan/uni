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
	int fd, flags, read_bytes, status, val, write_bytes;

	printf("Attempting to open in write mode, should fail \n");
	flags = O_WRONLY;
	fd = open(dev_acct, flags);
	if (fd == -1) {
		/* Failed to open */
		printf("Successfully failed to open: \n");
	} else
		printf("Error - opened in write mode \n");

	printf("Attempting to open in read/write mode, should fail \n");
	flags = O_RDWR;
	fd = open(dev_acct, flags);
	if (fd == -1) {
		/* Failed to open */
		printf("Successfully failed to open: \n");
	} else {
		printf("Error - opened in write mode \n");
	}

	printf("Opening in read mode \n");
	flags = O_RDONLY;
	fd = open(dev_acct, flags);
	if (fd == -1) {
		err(1, "%s%s%s", dev_acct,
		    (flags & O_RDWR) ? " O_RDWR" : " O_RDONLY",
		    (flags & O_NONBLOCK) ? " O_NONBLOCK" : "");
	} else
		printf("Opened %s with fd %d \n", dev_acct, fd);

	/* Fork and exit immediately to create 2 messages */
        if (fork() == 0)
		exit(0);
	else
        	wait(&status);

	read_bytes = read(fd, buf, sizeof(buf));
	if (read_bytes == -1)
		err(1, "");
	printf("%d bytes were read \n", read_bytes);

	printf("Attempting to write, should fail\n");
	write_bytes = write(fd, buf, sizeof(buf));
	if (write_bytes == -1)
		printf("Failed to write\n");
	else
		printf("%d bytes were written \n", write_bytes);

 	return 0;
}
