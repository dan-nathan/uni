#include <sys/types.h>
#include <sys/sysctl.h>
#include <stdio.h>
#include <stdlib.h>
#include <zones.h>
#include <err.h>
#include <errno.h>

int
main(int argc, char *argv[])
{
	int mib[2], maxclusters;
	size_t len;

	scanf("%d", &len);

	mib[0] = CTL_KERN;
	mib[1] = KERN_CACHEPCT;
	len = sizeof(maxclusters);
	if (sysctl(mib, 2, &maxclusters, &len, NULL, 0) == -1)
		err(1, "sysctl");
	printf("max clusters = %d \n", maxclusters);

	int newmaxclusters = maxclusters + 1;
	size_t len2;
	len2 = sizeof(newmaxclusters);

	printf("max clusters stored at %p \n", newmaxclusters);

	if (sysctl(mib, 2, &maxclusters, &len, &newmaxclusters, len2) == -1)
		err(1, "sysctl");
	printf("max clusters = %d \n", newmaxclusters);
}
