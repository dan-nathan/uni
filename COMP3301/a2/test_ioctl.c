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

	/* Open in non-blocking mode */
	flags = O_RDONLY | O_NONBLOCK;
	fd = open(dev_acct, flags);
	if (fd == -1) {
		err(1, "%s%s%s", dev_acct,
		    (flags & O_RDWR) ? " O_RDWR" : " O_RDONLY",
		    (flags & O_NONBLOCK) ? " O_NONBLOCK" : "");
	} else
		printf("Opened %s with fd %d \n", dev_acct, fd);

	val = 0;

	/* Set to non-blocking */
	ioctl(fd, FIONBIO, &val);

	printf("Result from ls: \n\n");

	/* Fork and exec child to ls */
        args[0] = "/bin/ls";
        args[1] = NULL;
        if (fork() == 0)
        	execv(args[0], args);
        else
        	wait(&status);

	printf("\n\n");


	ioctl(fd, FIONREAD, &val);
	printf("ioctl predicts %d bytes will be read\n", val);

	read_bytes = read(fd, buf, sizeof(buf));
	if (read_bytes == -1)
		err(1, "");

	printf("%d bytes were actually read \n", read_bytes);

	/* Set to nonblocking */
	val = 1;
	ioctl(fd, FIONBIO, &val);
	printf("Added nonblocking flag, next read should cause error \n");
	read_bytes = read(fd, buf, sizeof(buf));
	if (read_bytes == -1)
		err(1, "");

	printf("%d bytes where actually read \n", read_bytes);
	close(fd);

	return 0;
}
