#include <stdio.h>
#include <zones.h>

int
main(int argc, char *argv[])
{
	zoneid_t zid;
	size_t nzs = 1;
	printf("MAXZONES: %u, MAXZONEIDS: %u\n", MAXZONES, MAXZONEIDS);
	zone_create(1);
	zone_destroy(1);
	zone_enter(1);
	zone_list(&zid, &nzs);
	return (0);
}
