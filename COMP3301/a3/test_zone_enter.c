#include <stdio.h>
#include <zones.h>
#include <err.h>
#include <errno.h>

int
main(int argc, char *argv[])
{
	int result = zone_enter(1);
	if (result == 0) {
		printf("zone_enter worked \n");
	} else {
		err(1, "Zone enter failed");
	}
	result = zone_lookup(1);
	if (result == 0) {
		printf("zone_lookup worked \n");
	} else {
		err(1, "Zone lookup failed");
	}
	return (0);
}
