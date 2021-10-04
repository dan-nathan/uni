#include <sys/socket.h>

#include <ctype.h>
#include <err.h>
#include <errno.h>
#include <event.h>
#include <fcntl.h>
#include <limits.h>
#include <netdb.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

struct eventEntry {
	TAILQ_ENTRY(eventEntry)	entry;
	struct 	event ev;
	int 	vni;
};

struct tunnel {
	int	fd;
	int	vni;
	int	tapNo;
};

struct timeoutEvent {
	struct	eventEntry e;
	struct	timeval tv;
};

struct serverEventInfo {
	struct	tunnel *tunnels;
	struct	timeoutEvent te;
	int	noTunnels;
};

struct tapEventInfo {
	struct 	timeoutEvent te;
	int 	fd;
	int 	vni;
};

TAILQ_HEAD(eventList, eventEntry);

__BEGIN_DECLS

__dead static void usage(void);

__dead static void event_timeout(int, short, void *);

static void event_read_from_tap(int, short, void *);

static void event_read_from_server(int, short, void *);

static int connect_to(const char *, const char *, int);

static int bind_to(const char *, const char *, int);

static void add_tunnel(struct serverEventInfo *, struct eventList *, char *);

__END_DECLS

__dead static void
usage(void) {
	extern char *__progname;
	fprintf(stderr, "usage: %s [-46d] [-l address] [-p port] -t 120 \
	    [-e /dev/tapX@vni] server [port]\n", __progname);
	exit(1);
}

__dead static void
event_timeout(int fd, short revents, void *conn)
{
	exit(0);
}

static void
event_read_from_tap(int fd, short revents, void *arg) {
	// TODO see if static can be removed
	struct tapEventInfo *tei;
	uint8_t buf[1500];
	ssize_t rlen, slen;

	rlen = read(fd, buf, sizeof(buf));
	switch (rlen) {
	case -1:
		err(1, "Failed to read from tap");
		return;
	case 0:
		errx(1, "socket closed?");
		/* NOTREACHED */
	default:
		break;
	}

	tei = (struct tapEventInfo *)arg;
	/* Append header info onto result */
	for (int i = rlen - 1; i >= 0; i--)
		buf[i + 8] = buf[i];
	buf[0] = 0;
	buf[1] = 0;
	buf[2] = 0x65;
	buf[3] = 0x58;
	buf[4] = tei->vni >> 16;
	buf[5] = tei->vni >> 8;
	buf[6] = tei->vni;
	buf[7] = 0;

	if (tei->vni == 4096) {
		// TODO return if packet is not IPv4
	} else if (tei->vni == 8192) {
		// TODO return if packet is not IPv6
	}

	slen = write(tei->fd, buf, rlen + 8);
	switch (slen) {
	case -1:
		err(1, "Failed to send");
		return;
	case 0:
		errx(1, "socket closed?");
		/* NOTREACHED */
	default:
		break;
	}

	/* Reset the timeout event */
    	event_del(&tei->te.e.ev);
    	event_add(&tei->te.e.ev, &tei->te.tv);
 }

static void
event_read_from_server(int fd, short revents, void *arg) {
	struct sockaddr_storage ss;
	struct serverEventInfo *sei;
	socklen_t sslen;
	uint8_t buf[1500];
	ssize_t rlen;
	uint32_t vni;
	uint16_t protocol;
	uint8_t optLen, version;
	bool validPacket = true;

	sslen = sizeof(ss);
	rlen = recvfrom(fd, buf, sizeof(buf), 0,
	    (struct sockaddr *)&ss, &sslen);
	switch (rlen) {
	case -1:
		switch (errno) {
		case EINTR:
		case EAGAIN:
			break;
		default:
			warn("recv");
			break;
		}

		/* try again later */
		return;
	case 0:
		errx(1, "socket closed?");
		/* NOTREACHED */
	default:
		break;
	}

	version = buf[0] >> 6;
	optLen = (buf[0] << 2) >> 2;
	protocol = (buf[2] << 8) + buf[3];
	if (version != 0 || rlen < 8 + optLen * 4 || protocol != 0x6558)
		validPacket = false;
	vni = (buf[4] << 16) + (buf[5] << 8) + buf[6];

	sei = (struct serverEventInfo *)arg;
	if (validPacket) {
		for (int i = 0; i < sei->noTunnels; i++) {
			if (sei->tunnels[i].vni == vni) {
				write(sei->tunnels[i].fd, buf + 8 + optLen * 4,
				    rlen - 8 - optLen * 4);
				break;
			}
		}
	}

	/* Reset the timeout event */
	event_del(&sei->te.e.ev);
	event_add(&sei->te.e.ev, &sei->te.tv);
}

static int
connect_to(const char *host, const char *port, int af) {
	struct addrinfo hints, *res, *res0;
	const char *cause;
	int error, s, save_errno;

	cause = NULL;
	memset(&hints, 0, sizeof(hints));
	hints.ai_family = af;
	hints.ai_socktype = SOCK_DGRAM;
	error = getaddrinfo(host, port, &hints, &res0);
	if (error)
		errx(1, "%s", gai_strerror(error));

	s = -1;
	for (res = res0; res; res = res->ai_next) {
		s = socket(res->ai_family, res->ai_socktype, res->ai_protocol);
		if (s == -1) {
			save_errno = errno;
			cause = "socket";
			continue;
		}

		if (connect(s, res->ai_addr, res->ai_addrlen) == -1) {
			save_errno = errno;
			cause = "connect";
			close(s);
			s = -1;
			continue;
		}
		break;  /* okay we got one */
	}

	if (s == -1)
		errc(1, save_errno, "%s", cause);

	freeaddrinfo(res0);
	return s;
}

static int
bind_to(const char *host, const char *port, int af) {
	struct addrinfo hints, *res, *res0;
	int error, s, serrno;
	const char *cause;

	cause = NULL;
	memset(&hints, 0, sizeof(hints));
	hints.ai_family = af;
	hints.ai_socktype = SOCK_DGRAM;
	hints.ai_flags = AI_PASSIVE;

	error = getaddrinfo(host, port, &hints, &res0);
	if (error != 0)
		errx(1, "host %s port %s: %s", host, port, gai_strerror(error));

	for (res = res0; res != NULL; res = res->ai_next) {
		s = socket(res->ai_family, res->ai_socktype | SOCK_NONBLOCK,
		    res->ai_protocol);
		if (s == -1) {
			serrno = errno;
			cause = "socket";
			continue;
		}

		if (bind(s, res->ai_addr, res->ai_addrlen) == -1) {
			serrno = errno;
			cause = "bind";
			close(s);
			continue;
		}
	}
	freeaddrinfo(res0);

	return s;
}

static void
add_tunnel(struct serverEventInfo *sei, struct eventList *events, char *tap) {
	struct eventEntry *e;
	char tapStr[12];
	int tapNo, vni;

	if (sscanf(tap, "/dev/tap%d@%d", &tapNo, &vni) != 2 || tapNo < 0
	    || tapNo > 99)
		usage();
	sprintf(tapStr, "/dev/tap%d", tapNo);

	for (int i = 0; i < sei->noTunnels; i++) {
		if (sei->tunnels[i].tapNo == tapNo) {
			/* If the tap is already open, update the vni */
			sei->tunnels[i].vni = vni;
			return;
		} else if (sei->tunnels[i].vni == vni) {
			/* If vni is already taken, update thetap being used */
			close(sei->tunnels[i].fd);
			sei->tunnels[i].fd = open(tapStr, O_RDWR | O_NONBLOCK
			    | O_CREAT);
			if (sei->tunnels[i].fd == -1)
    				err(1, "Failed to open %d", errno);
			sei->tunnels[i].tapNo = tapNo;
			return;
		}
	}

	/* Allocate memory for another tunnel */
	sei->noTunnels++;
	if (sei->noTunnels == 1)
		sei->tunnels = malloc(sizeof(struct tunnel));
	else {
		sei->tunnels = realloc(sei->tunnels,
		    sizeof(struct tunnel) * sei->noTunnels);
	}
	sei->tunnels[sei->noTunnels - 1].tapNo = tapNo;
	sei->tunnels[sei->noTunnels - 1].vni = vni;

	sei->tunnels[sei->noTunnels - 1].fd = open(tapStr, O_RDWR | O_NONBLOCK
	    | O_CREAT);
	if (sei->tunnels[sei->noTunnels - 1].fd == -1)
		err(1, "Failed to open %d", errno);

	e = malloc(sizeof(*e));
	if (e == NULL)
		err(1, NULL);
	e->vni = vni;
	event_set(&e->ev, sei->tunnels[sei->noTunnels - 1].fd, 0, NULL, NULL);
	TAILQ_INSERT_TAIL(events, e, entry);
}

int
main(int argc, char *argv[]) {
	struct serverEventInfo *sei;
	struct timeoutEvent *te;
	struct eventEntry *e;
	struct eventList events = TAILQ_HEAD_INITIALIZER(events);
	const char *errCause;
	char *destPort, *localAddress, *serverAddress, *srcPort;
	int addressFormat, destFd, opt, timeout;
	bool daemonise, specifiedSrcPort, timemoutSpecified;

	addressFormat = AF_UNSPEC;
	localAddress = NULL;
	destPort = "6081";
	srcPort = malloc(sizeof(char));
	daemonise = true;
	specifiedSrcPort = false;
	timemoutSpecified = false;

	te = malloc(sizeof(struct timeoutEvent));
	if (te == NULL)
		err(1, NULL);
	sei = malloc(sizeof(struct serverEventInfo));

	while ((opt = getopt(argc, argv, "46dl:p:t:e:")) != -1) {
		switch (opt) {
		case '4':
			addressFormat = AF_INET;
			break;
		case '6':
			addressFormat = AF_INET6;
			break;
		case 'd':
			daemonise = false;
			break;
		case 't':
			timeout = strtonum(optarg, INT_MIN, INT_MAX, &errCause);
			/* If optarg is not a vaild integer */
			if (errCause != NULL) {
				err(1, "%s", errCause);
			}
			te->tv.tv_sec = timeout;
			te->tv.tv_usec = 0;
			timemoutSpecified = true;
			break;
		case 'l':
			localAddress = optarg;
			break;
		case 'p':
			specifiedSrcPort = true;
			srcPort = optarg;
			break;
		case 'e':
			add_tunnel(sei, &events, optarg);
			break;
		default:
			usage();
			/* NOTREACHED */
		}
	}
	argc -= optind;
	argv += optind;

	/* If -t or -e were not used */
	if (!timemoutSpecified || TAILQ_EMPTY(&events))
		usage();

	switch (argc) {
		case 2:
			destPort = argv[1];
		case 1:
			serverAddress = argv[0];
			break;
		default:
			usage();
	}

	if (!specifiedSrcPort)
		strcpy(srcPort, destPort);
	if (daemonise)
		daemon(1, 1);

	event_init();
	if (timeout > 0) {
		evtimer_set(&te->e.ev, event_timeout, NULL);
		evtimer_add(&te->e.ev, &te->tv);
	}

	/* Connect to server and local address */
	destFd = connect_to(serverAddress, destPort, addressFormat);
	bind_to(localAddress, srcPort, addressFormat);

	/* Setup the event for reading from the server */
	e = malloc(sizeof(*e));
	if (e == NULL)
		err(1, NULL);
	sei->te = *te;
	event_set(&e->ev, destFd, EV_READ | EV_PERSIST, event_read_from_server,
	    sei);
	event_add(&e->ev, NULL);
	/* Setup events for reading from each tunnel */
	TAILQ_FOREACH(e, &events, entry) {
		struct tapEventInfo *tei = malloc(sizeof(struct tapEventInfo));
		tei->te = *te;
		tei->vni = e->vni;
		tei->fd = destFd;
		event_set(&e->ev, EVENT_FD(&e->ev), EV_READ | EV_PERSIST,
		    event_read_from_tap, tei);
		event_add(&e->ev, NULL);
	}
	event_dispatch();

	return 0;
}
