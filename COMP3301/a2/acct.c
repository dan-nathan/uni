#include <sys/types.h>
#include <sys/errno.h>
#include <sys/fcntl.h>
#include <sys/filio.h>
#include <sys/malloc.h>
#include <sys/mutex.h>
#include <sys/param.h>
#include <sys/proc.h>
#include <sys/queue.h>
#include <sys/resource.h>
#include <sys/systm.h>
#include <sys/time.h>
#include <sys/tty.h>

#include "acct.h"

struct message_entry {
	TAILQ_ENTRY(message_entry) entry;
	unsigned short type;
	void *message;
};

__BEGIN_DECLS

void acct_common(struct process *, struct acct_common *);

__END_DECLS

extern int hz;
extern int stathz;

TAILQ_HEAD(message_list, message_entry);
static struct message_list messages = TAILQ_HEAD_INITIALIZER(messages);
static struct mutex mtx;
static int non_blocking;
static int open;
static int seq_num;

int
acctattach(int num)
{
	open = 0;
	mtx_init(&mtx, 0);
	return (0);
}

int
acctopen(dev_t dev, int flag, int mode, struct proc *p)
{
	/* There are no non-zero minor devices */
	if (minor(dev) != 0)
		return (ENXIO);

	/* This device is read-only */
	if (flag & FWRITE)
		return (EPERM);

	/* This device has exclusive access */
	mtx_enter(&mtx);
	if (open) {
		mtx_leave(&mtx);
		return (EBUSY);
	}

	open = 1;
	seq_num = 0x01;
	mtx_leave(&mtx);

	if (flag & O_NONBLOCK)
		non_blocking = 1;
	else
		non_blocking = 0;

	return (0);
}

int
acctclose(dev_t dev, int flag, int mode, struct proc *p)
{
	struct message_entry *me;
	wakeup(&messages);

	/* Dump remaining messages */
	mtx_enter(&mtx);
	open = 0;
	while (!TAILQ_EMPTY(&messages)) {
		me = TAILQ_FIRST(&messages);
		TAILQ_REMOVE(&messages, me, entry);
		free(me, M_DEVBUF, sizeof(struct message_entry));
	}
	mtx_leave(&mtx);

	return (0);
}

int
acctioctl(dev_t dev, u_long request, caddr_t data, int flags, struct proc *p)
{
	struct message_entry *message;
	if (request == FIONREAD) {
		if (TAILQ_EMPTY(&messages))
			*(int *)data = 0;
		else {
			mtx_enter(&mtx);
			message = TAILQ_FIRST(&messages);
			mtx_leave(&mtx);
			switch (message->type) {
			case ACCT_MSG_FORK:
				*(int *)data = ((struct acct_fork*)
				    message->message)->ac_common.ac_len;
				break;
			case ACCT_MSG_EXEC:
				*(int *)data = ((struct acct_exec*)
				    message->message)->ac_common.ac_len;
				break;
			case ACCT_MSG_EXIT:
				*(int *)data = ((struct acct_exit*)
				    message->message)->ac_common.ac_len;
				break;
			default:
				return (EINVAL);
			}
		}
	}

	if (request == FIONBIO)
		non_blocking = *(int *)data;

	if (request == FIOASYNC)
		return (EOPNOTSUPP);

	return (0);
}

int
acctread(dev_t dev, struct uio *uio, int flags)
{
	struct message_entry *message;
	int error, len, size, wakeup_reason;

	/* Don't allow non-blocking I/O */
	if (non_blocking != 0)
		return (EOPNOTSUPP);

	/* Block until there is a message to read */
	while (TAILQ_EMPTY(&messages)) {
		wakeup_reason = tsleep(&messages, 0 | PCATCH,
		    "waiting to read", 0);

		/* If the wakeup reason was a signal */
		if (wakeup_reason != 0)
			return (0);
	}

	mtx_enter(&mtx);
	message = TAILQ_FIRST(&messages);
	mtx_leave(&mtx);
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
		return (EINVAL);
	}

	if (uio->uio_offset < 0)
		return (EINVAL);

	/* Ensure it is reading from the start of the message */
	uio->uio_offset = 0;
	while (uio->uio_resid > 0) {
		if (uio->uio_offset >= size)
			break;

		len = size - uio->uio_offset;

		if (len > uio->uio_resid)
			len = uio->uio_resid;

		if ((error = uiomove(message->message + uio->uio_offset,
		    len, uio)) != 0)
			return (error);
	}

	mtx_enter(&mtx);
	TAILQ_REMOVE(&messages, message, entry);
	mtx_leave(&mtx);
	free(message, M_DEVBUF, sizeof(struct message_entry));
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
	/* TODO */
	return (0);
}

int
acctkqfilter(dev_t dev, struct knote *k)
{
	/* TODO */
	return (0);
}

void
acct_common(struct process *p, struct acct_common *ac)
{
	int i;
	/* Copy p->ps_comm string into ac->ac_comm */
	for (i = 0; i < 16; i++) {
		ac->ac_comm[i] = p->ps_comm[i];
		if (p->ps_comm[i] == '\0')
			break;
	}
	ac->ac_etime = p->ps_tu.tu_runtime;
	ac->ac_btime = p->ps_start;
	ac->ac_pid = p->ps_pid;
	ac->ac_uid = p->ps_ucred->cr_uid;
	ac->ac_gid = p->ps_ucred->cr_gid;
	/* If the controlling terminal is NULL leave field as 0 */
	if (p->ps_pgrp->pg_session->s_ttyp != NULL)
		ac->ac_tty = p->ps_pgrp->pg_session->s_ttyp->t_dev;
	ac->ac_flag = p->ps_acflag;
}

void
acct_exit(struct process *p)
{
	struct message_entry *me;
	struct acct_exit *ae;
	int freq;

	if (open == 0)
		return;

	/* Zero allocated memory */
	me = malloc(sizeof(*me), M_DEVBUF, M_ZERO | M_WAITOK);
	if (me == NULL)
		return;

	me->type = ACCT_MSG_EXIT;

	ae = malloc(sizeof(*ae), M_DEVBUF, M_ZERO | M_WAITOK);
	if (ae == NULL)
		return;
	ae->ac_common.ac_type = ACCT_MSG_EXIT;
	ae->ac_common.ac_len = sizeof(struct acct_exit);
	mtx_enter(&mtx);
	ae->ac_common.ac_seq = seq_num;
	seq_num = seq_num << 1;
	if (seq_num == 0)
		seq_num = 0x01;
	mtx_leave(&mtx);


	/* Fill out info for acct_common struct*/
	acct_common(p, &ae->ac_common);

	/* acct_exit info */
	freq = stathz ? stathz : hz;
	ae->ac_utime.tv_sec = p->ps_tu.tu_uticks / freq;
	ae->ac_utime.tv_nsec = (p->ps_tu.tu_uticks * 1000000000 / freq)
	    % 1000000000;
	ae->ac_stime.tv_sec = p->ps_tu.tu_sticks / freq;
	ae->ac_stime.tv_nsec = (p->ps_tu.tu_sticks * 1000000000 / freq)
	    % 1000000000;
	ae->ac_mem = p->ps_ru->ru_ixrss + p->ps_ru->ru_idrss
	    + p->ps_ru->ru_isrss;
	ae->ac_io = p->ps_ru->ru_inblock + p->ps_ru->ru_oublock;

	/* Add message to the list */
	me->message = (void*)ae;
	mtx_enter(&mtx);
	TAILQ_INSERT_TAIL(&messages, me, entry);
	mtx_leave(&mtx);
	wakeup(&messages);
}

void
acct_exec(struct process *p)
{
	struct message_entry *me;
	struct acct_exec *ae;

	if (open == 0)
		return;

	me = malloc(sizeof(*me), M_DEVBUF, M_ZERO | M_WAITOK);
	if (me == NULL)
		return;

	me->type = ACCT_MSG_EXEC;

	ae = malloc(sizeof(*ae), M_DEVBUF, M_ZERO | M_WAITOK);
	if (ae == NULL)
		return;
	ae->ac_common.ac_type = ACCT_MSG_EXEC;
	ae->ac_common.ac_len = sizeof(struct acct_exec);
	mtx_enter(&mtx);
	ae->ac_common.ac_seq = seq_num;
	seq_num = seq_num << 1;
	if (seq_num == 0)
		seq_num = 0x01;
	mtx_leave(&mtx);

	acct_common(p, &ae->ac_common);

	me->message = (void*)ae;
	mtx_enter(&mtx);
	TAILQ_INSERT_TAIL(&messages, me, entry);
	mtx_leave(&mtx);
	wakeup(&messages);
}

void
acct_fork(struct process *p)
{
	struct message_entry *me;
	struct acct_fork *af;

	if (open == 0)
		return;

	me = malloc(sizeof(*me), M_DEVBUF, M_ZERO | M_WAITOK);
	if (me == NULL)
		return;

	me->type = ACCT_MSG_FORK;

	af = malloc(sizeof(*af), M_DEVBUF, M_ZERO | M_WAITOK);
	if (af == NULL)
		return;
	af->ac_common.ac_type = ACCT_MSG_FORK;
	af->ac_common.ac_len = sizeof(struct acct_fork);
	mtx_enter(&mtx);
	af->ac_common.ac_seq = seq_num;
	seq_num = seq_num << 1;
	if (seq_num == 0)
		seq_num = 0x01;
	mtx_leave(&mtx);

	acct_common(p, &af->ac_common);

	/* p refers to child process */
	af->ac_cpid = p->ps_pid;
	if (p->ps_pptr == NULL) {
		/* Failsafe - parent should never be NULL */
		af->ac_common.ac_pid = 0;
	} else
		af->ac_common.ac_pid = p->ps_pptr->ps_pid;

	me->message = (void*)af;
	mtx_enter(&mtx);
	TAILQ_INSERT_TAIL(&messages, me, entry);
	mtx_leave(&mtx);
	wakeup(&messages);
}
