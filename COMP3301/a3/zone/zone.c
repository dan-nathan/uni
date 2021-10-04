/*	$OpenBSD$ */

/*
 * Copyright (c) 2015, 2019, 2020 The University of Queensland
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

#include <sys/types.h>
#include <sys/wait.h>

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <signal.h>
#include <unistd.h>
#include <err.h>
#include <errno.h>
#include <zones.h>

#ifndef nitems
#define nitems(_a) (sizeof(_a) / sizeof(_a[0]))
#endif

static int	zcreate(int, char *[]);
static int	zdestroy(int, char *[]);
static int	zexec(int, char *[]);
static int	zlist(int, char *[]);
static int	zlookup(int, char *[]);

__dead void usage(void);

struct task {
	const char *name;
	int (*task)(int, char *[]);
};

/* must be sorted alphanumerically */
static const struct task tasks[] = {
	{ "create",	zcreate },
	{ "destroy",	zdestroy },
	{ "exec",	zexec },
	{ "list",	zlist },
	{ "lookup",	zlookup },
};

static int	task_cmp(const void *, const void *);

static int
task_cmp(const void *a, const void *b)
{
	const struct task *ta = a;
	const struct task *tb = b;
	
	return (strcmp(ta->name, tb->name));
}

__dead void
usage(void)
{
	extern char *__progname;

	fprintf(stderr, "usage:\t%s create zoneid\n", __progname);
	fprintf(stderr, "\t%s destroy zoneid\n", __progname);
	fprintf(stderr, "\t%s list\n", __progname);
	fprintf(stderr, "\t%s lookup [zoneid]\n", __progname);
	fprintf(stderr, "\t%s exec zoneid command ...\n", __progname);

	exit(1);
}

int
main(int argc, char *argv[])
{
	struct task key, *t;

	if (argc < 2)
		usage();

	key.name = argv[1];
	t = bsearch(&key, tasks, nitems(tasks), sizeof(tasks[0]), task_cmp);
	if (t == NULL)
		usage();

	argc -= 2;
	argv += 2;

	return (t->task(argc, argv));
}

static zoneid_t
getzoneid(const char *zone)
{
	const char *errstr;
	zoneid_t z;

	z = strtonum(zone, 0, MAXZONEIDS, &errstr);
	if (errstr != NULL)
		errx(1, "zone id %s: %s", zone, errstr);

	return (z);
}

static int
zcreate(int argc, char *argv[])
{
	const char *zone;
	zoneid_t z;

	if (argc != 1)
		usage();

	zone = argv[0];

	z = getzoneid(zone);
	if (zone_create(z) == -1)
		err(1, "create %s", zone);

	return (0);
}

static int
zdestroy(int argc, char *argv[])
{
	const char *zone;
	zoneid_t z;

	if (argc != 1)
		usage();

	zone = argv[0];

	z = getzoneid(zone);
	if (zone_destroy(z) == -1)
		err(1, "destroy %s", zone);

	return (0);
}

static int
zexec(int argc, char *argv[])
{
	const char *zone;
	zoneid_t z;

	if (argc < 2)
		usage();

	zone = argv[0];
	z = getzoneid(zone);

	argc -= 1;
	argv += 1;

	if (zone_enter(z) == -1)
		err(1, "enter %s", zone);

	execvp(argv[0], argv);

	err(1, "exec %s", argv[0]);
	/* NOTREACHED */
}

static int
zlist(int argc, char *argv[])
{
	zoneid_t *zs = NULL;
	size_t nzs, i = 8;
	zoneid_t z;

	if (argc != 0)
		usage();

	for (;;) {
		nzs = i;

		zs = reallocarray(zs, nzs, sizeof(*zs));
		if (zs == NULL)
			err(1, "lookup");

		if (zone_list(zs, &nzs) == 0)
			break;

		if (errno != EFAULT)
			err(1, "list");

		i <<= 1;
	}

	printf("%8s %s\n", "ID", "NAME");

	for (i = 0; i < nzs; i++) {
		z = zs[i];
		printf("%8d\n", z);
	}

	free(zs);

	return (0);
}

static int
zlookup(int argc, char *argv[])
{
	zoneid_t z = -1;

	switch (argc) {
	case 1:
		z = getzoneid(argv[0]);
		break;
	case 0:
		break;
	default:
		usage();
	}

	z = zone_lookup(z);
	if (z == -1)
		err(1, "lookup %d", z);

	printf("%d\n", z);

	return (0);
}
