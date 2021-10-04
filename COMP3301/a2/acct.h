/*	$OpenBSD$ */

/*
 * Copyright (c) 2018, 2019 The University of Queensland
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

#ifndef __DEV_ACCT_H__
#define __DEV_ACCT_H__

#define ACCT_MSG_FORK	0
#define ACCT_MSG_EXEC	1
#define ACCT_MSG_EXIT	2

struct acct_common {
	unsigned short		ac_type;
	unsigned short		ac_len;
	unsigned int		ac_seq;

	char			ac_comm[16];	/* command name */
	struct timespec		ac_etime;	/* elapsed time */
	struct timespec		ac_btime;	/* starting time */
	pid_t			ac_pid;		/* process id */
	uid_t			ac_uid;		/* user id */
	gid_t			ac_gid;		/* group id */
	dev_t			ac_tty;		/* controlling tty */
	unsigned int		ac_flag;	/* accounting flags */
};

/*
 * fork info is mostly from the parent, but acct_fork gets passed the child.
 */
struct acct_fork {
	struct acct_common	ac_common;
	pid_t			ac_cpid;	/* child pid */
};

/*
 * exec exists mostly to show the new command name.
 */
struct acct_exec {
	struct acct_common	ac_common;
};

/*
 * basically a clone of the ACCOUNTING syscall
 */
struct acct_exit {
	struct acct_common	ac_common;
	struct timespec		ac_utime;	/* user time */
	struct timespec		ac_stime;	/* system time */
	uint64_t		ac_mem;		/* average memory usage */
	uint64_t		ac_io;		/* count of IO blocks */
};

#ifdef _KERNEL
void	acct_fork(struct process *);
void	acct_exec(struct process *);
void	acct_exit(struct process *);
#endif /* _KERNEL */

#endif /* __DEV_ACCT_H__ */
