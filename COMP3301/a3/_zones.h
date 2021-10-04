#include <sys/types.h>
#include <sys/queue.h>

#define GLOBAL_ZONE 0

struct zone_entry {
	TAILQ_ENTRY(zone_entry)	entry;
	zoneid_t	z_id;
};

TAILQ_HEAD(zone_list, zone_entry);

#ifndef __ZONES_H_VARS
#define __ZONES_H_VARS

struct zone_list zones;
int zone_count;

#endif

void zone_init(struct process *);
