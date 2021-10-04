#include <stdio.h>
#include <stdlib.h>
#include <zones.h>
#include <err.h>
#include <errno.h>

int
main(int argc, char *argv[])
{
	zoneid_t *zs;
	size_t nzs;
	nzs = 20;
	zs = malloc(sizeof(zoneid_t) * nzs);
	printf("%p\n", zs);
	int result = zone_list(zs, &nzs);
	if (result != 0) {
		err(1, "Zone enter failed");
	}

	int i;
	printf("number of zones - %d\n", nzs);
	printf("%p\n", zs);
	for (i = 0; i < nzs; i++) {
		printf("%d\n", zs[i]);
	}

	return (0);
}
