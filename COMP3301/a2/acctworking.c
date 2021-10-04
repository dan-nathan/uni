// Ones that I'm unsure about
#include <sys/param.h>
#include <sys/tty.h>
#include <sys/mutex.h>
#include <sys/filio.h>
#include <sys/resource.h>


// Ones that I definitely need
#include <sys/types.h>
#include <sys/malloc.h>
#include <sys/time.h>
#include <sys/errno.h>
#include <sys/queue.h>
#include <sys/fcntl.h>
#include <sys/systm.h>
#include <sys/proc.h>

#include "acct.h"

struct messageEntry {
	TAILQ_ENTRY(messageEntry) entry;
	unsigned short type;
	void *message;
};


TAILQ_HEAD(messageList, messageEntry);
static struct messageList messages = TAILQ_HEAD_INITIALIZER(messages);


extern int hz;
extern int stathz;

static int open = 0;
static int closing = 0;
static int non_blocking;
static int seq_num;

// TODO check arguments
int
acctattach(dev_t dev, int flag, int mode, struct proc *p)
{
	return (0);
}

int
acctopen(dev_t dev, int flag, int mode, struct proc *p)
{
	/* There are no non-zero minor devices */
        if (minor(dev) != 0) {
                return (ENXIO);
	}

        /* This device is read-only */
        if (flag & FWRITE) {
                return (EPERM);
	}

	if (open) {
		// device busy
		return (EBUSY);
	}
	open = 1;
	seq_num = 0x01;

	if (flag & O_NONBLOCK) {
		non_blocking = 1;
	} else {
		non_blocking = 0;
	}

        return (0);
}

int
acctclose(dev_t dev, int flag, int mode, struct proc *p)
{
	closing = 1;
	wakeup(&messages);
	open = 0;
	return (0);
}

int
acctioctl(dev_t dev, u_long request, caddr_t data, int flags, struct proc *p)
{
	if (request == FIONREAD) {
		if (TAILQ_EMPTY(&messages)) {
			*(int *)data = 0;
		} else {
			struct messageEntry *message = TAILQ_FIRST(&messages);
			switch (message->type) {
			case ACCT_MSG_FORK:
				*(int *)data = ((struct acct_fork*)message->message)->ac_common.ac_len;
				break;
			case ACCT_MSG_EXEC:
				*(int *)data = ((struct acct_exec*)message->message)->ac_common.ac_len;
				break;
			case ACCT_MSG_EXIT:
				*(int *)data = ((struct acct_exit*)message->message)->ac_common.ac_len;
				break;
			default:
				return 1;
			}
		}
	}

	if (request == FIONBIO) {
		non_blocking = *(int *)data;
	}

	if (request == FIOASYNC) {
		return (EOPNOTSUPP);
	}
	// TODO check if

	return (0);
}

int
acctread(dev_t dev, struct uio *uio, int flags)
{
	int error;
	int size;
	int len;
	printf("here999 uio_offset %d uio_resid %d \n", (int)uio->uio_offset, (int)uio->uio_resid);

	if (non_blocking != 0) {
		return (EOPNOTSUPP);
	}

	// Block until there is a message to read
	while (TAILQ_EMPTY(&messages)) {
		tsleep(&messages, 0, "waiting to read", 0);
		if (closing != 0) {
			closing = 0;
			return (0);
		}
	}

	struct messageEntry *message = TAILQ_FIRST(&messages);
	switch (message->type) {
	case ACCT_MSG_FORK:
		size = ((struct acct_fork*)message->message)->ac_common.ac_len;
		break;
	case ACCT_MSG_EXEC:
		size = ((struct acct_exec*)message->message)->ac_common.ac_len;
		break;
	case ACCT_MSG_EXIT:
		size = ((struct acct_exit*)message->message)->ac_common.ac_len;
		break;
	default:
		return 1;
	}

        if (uio->uio_offset < 0)
                return (EINVAL);

	// TODO find better solution
	uio->uio_offset = 0;
        while (uio->uio_resid > 0) {
		if (uio->uio_offset >= size)
			break;

		len = size - uio->uio_offset;

		if (len > uio->uio_resid)
			len = uio->uio_resid;

                if ((error = uiomove(message->message + uio->uio_offset, len, uio)) != 0)
                        return (error);
        }

	// error - uio_offset starts at 128 instead of 0

	TAILQ_REMOVE(&messages, message, entry);
	free(message, M_DEVBUF, sizeof(struct messageEntry));

        return (0);
}

int
acctwrite(dev_t dev, struct uio *uio, int flags)
{
	return (EOPNOTSUPP);
}

int
acctpoll(dev_t dev, int i, struct proc *p)
{
	// TODO
	return (0);
}

int
acctkqfilter(dev_t dev, struct knote *k)
{
	// TODO
	return (0);
}

static void
acct_common(struct process *p, struct acct_common *ac) {
	int i = 0;
	while (i < 16) {
		ac->ac_comm[i] = p->ps_comm[i];
		if (p->ps_comm[i] == '\0') {
			break;
		}
		i++;
	}

	// TODO check
	ac->ac_etime = p->ps_tu.tu_runtime;

	// TODO check starting time
	ac->ac_btime = p->ps_start;

	ac->ac_pid = p->ps_pid;

	// TODO check if real (cr_uid, cr_gid) or effective (cr_ruid, cr_rgid) uid, gid
	ac->ac_uid = p->ps_ucred->cr_uid;
	ac->ac_gid = p->ps_ucred->cr_gid;

	if (p->ps_pgrp->pg_session->s_ttyp != NULL) {
		ac->ac_tty = p->ps_pgrp->pg_session->s_ttyp->t_dev;
	}
	ac->ac_flag = p->ps_acflag;

	// elapsed time
	// TODO check this
	/*struct tusage ps_tu = p->ps_tu;
	uint64_t ticks = ps_tu.tu_uticks + ps_tu.tu_sticks + ps_tu.tu_iticks;
	int freq = stathz ? stathz : hz;
	printf("here10 commname %s ticks %d freq %d \n", ae->ac_common.ac_comm, (int)ticks, freq);
	uint64_t nanoseconds = ticks * 1000000000 / freq;
	ae->ac_common.ac_etime.tv_sec = nanoseconds / 1000000000;
	ae->ac_common.ac_etime.tv_nsec = nanoseconds % 1000000000;
	*/

}

void
acct_exit(struct process *p)
{
	if (!open) {
		return;
	}
	struct messageEntry *me;
	// TODO check M_DEVBUF
	me = malloc(sizeof(*me), M_DEVBUF, M_ZERO | M_WAITOK);
	if (me == NULL)
		return;

	me->type = ACCT_MSG_EXIT;


	struct acct_exit *ae = malloc(sizeof(*ae), M_DEVBUF, M_ZERO | M_WAITOK);
	if (ae == NULL)
		return;
	ae->ac_common.ac_type = ACCT_MSG_EXIT;
	ae->ac_common.ac_len = sizeof(struct acct_exit);
	ae->ac_common.ac_seq = seq_num;
	seq_num = seq_num << 1;
	if (seq_num == 0) {
		seq_num = 0x01;
	}

	acct_common(p, &ae->ac_common);


	int freq = stathz ? stathz : hz;

	//ae info
	ae->ac_utime.tv_sec = p->ps_tu.tu_uticks / freq;
	ae->ac_utime.tv_nsec = (p->ps_tu.tu_uticks * 1000000000 / freq) % 1000000000;
	ae->ac_stime.tv_sec = p->ps_tu.tu_sticks / freq;
	ae->ac_stime.tv_nsec = (p->ps_tu.tu_sticks * 1000000000 / freq) % 1000000000;


	long total_memory = p->ps_ru->ru_ixrss + p->ps_ru->ru_idrss + p->ps_ru->ru_isrss;
	/*long nanoseconds = p->ps_tu.tu_runtime.tv_nsec + 1000000000 * p->ps_tu.tu_runtime.tv_sec;
	if (nanoseconds == 0) {
		ae->ac_mem = 0;
	} else {
		ae->ac_mem = (uint64_t)((float)total_memory / (float)(nanoseconds/1000000000));
	}*/
	// TODO check what exactly average memory means
	ae->ac_mem = (uint64_t)total_memory;
	ae->ac_io = p->ps_ru->ru_inblock + p->ps_ru->ru_oublock;

	me->message = (void*)ae;
	TAILQ_INSERT_TAIL(&messages, me, entry);

	// Indicate that there is a message to read
	wakeup(&messages);
}

void
acct_exec(struct process *p)
{
	if (!open) {
		return;
	}
	struct messageEntry *me;
	// TODO check M_DEVBUF
	me = malloc(sizeof(*me), M_DEVBUF, M_ZERO | M_WAITOK);
	if (me == NULL)
		return;

	me->type = ACCT_MSG_EXEC;


	struct acct_exec *ae = malloc(sizeof(*ae), M_DEVBUF, M_ZERO | M_WAITOK);
	if (ae == NULL)
		return;
	ae->ac_common.ac_type = ACCT_MSG_EXEC;
	ae->ac_common.ac_len = sizeof(struct acct_exec);
	ae->ac_common.ac_seq = seq_num;
	seq_num = seq_num << 1;
	if (seq_num == 0) {
		seq_num = 0x01;
	}

	acct_common(p, &ae->ac_common);

	me->message = (void*)ae;
	TAILQ_INSERT_TAIL(&messages, me, entry);

	// Indicate that there is a message to read
	wakeup(&messages);
}

void
acct_fork(struct process *p)
{
	if (!open) {
		return;
	}
	struct messageEntry *me;
	// TODO check M_DEVBUF
	me = malloc(sizeof(*me), M_DEVBUF, M_ZERO | M_WAITOK);
	if (me == NULL)
		return;

	me->type = ACCT_MSG_FORK;


	struct acct_fork *af = malloc(sizeof(*af), M_DEVBUF, M_ZERO | M_WAITOK);
	if (af == NULL)
		return;
	af->ac_common.ac_type = ACCT_MSG_FORK;
	af->ac_common.ac_len = sizeof(struct acct_fork);
	af->ac_common.ac_seq = seq_num;
	seq_num = seq_num << 1;
	if (seq_num == 0) {
		seq_num = 0x01;
	}

	acct_common(p, &af->ac_common);

	af->ac_cpid = p->ps_pid;
	if (p->ps_pptr == NULL) {
		/* failsafe - parent should never be NULL */
		af->ac_common.ac_pid = 0;
	} else {
		af->ac_common.ac_pid = p->ps_pptr->ps_pid;
	}

	me->message = (void*)af;
	TAILQ_INSERT_TAIL(&messages, me, entry);

	// Indicate that there is a message to read
	wakeup(&messages);
}
// Major: 24
/*
	Kernel generates message using acct_exit, acct_exec or acct_fork
*/
