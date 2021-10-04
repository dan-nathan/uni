#include <sys/types.h>
#include <sys/malloc.h>
#include <sys/systm.h>
#include <sys/errno.h>
#include <sys/queue.h>
#include <sys/atomic.h>
#include <sys/zones.h>
#include <sys/param.h>
#include <sys/ucred.h>
#include <sys/proc.h>
#include <sys/socket.h> // TODO this is janky as fuck
#include <sys/mount.h> // TODO this is janky as fuck
#include <sys/syscallargs.h>

void
zone_init(struct process *p)
{
	struct zone_entry *ze;
	struct process_entry *pe;
	TAILQ_INIT(&zones);
	zone_count = 1;
	ze = malloc(sizeof(struct zone_entry), M_SUBPROC, M_ZERO | M_WAITOK);
	ze->zone.z_id = 0;
	TAILQ_INIT(&(ze->zone.processes));
	pe = malloc(sizeof(struct process_entry), M_SUBPROC, M_ZERO | M_WAITOK);
	pe->process = p;
	TAILQ_INSERT_TAIL(&(ze->zone.processes), pe, entry);

	TAILQ_INSERT_TAIL(&zones, ze, entry);
	p->ps_zone = ze->zone.z_id;
}

int
sys_zone_create(struct proc *p, void *v, register_t *retval)
{
	struct sys_zone_create_args /* {
		syscallarg(zoneid_t) z;
	}; */ *uap = v;
	struct zone_entry *ze;

	// If the current program isn't in the global zone
	if (p->p_p->ps_zone != GLOBAL_ZONE) {
		*retval = -1;
		return (EPERM);
	}

	// If the user is not root
	if (p->p_p->ps_ucred->cr_uid != 0) {
		printf("zone_create not root \n");
		*retval = -1;
		return (EPERM);
	}

	if (zone_count >= MAXZONES) {
		*retval = 1;
		return (ERANGE);
	}

	TAILQ_FOREACH(ze, &zones, entry) {
		if (ze->zone.z_id == SCARG(uap, z)) {
			*retval = 1;
			return (EBUSY);
		}
	}

	ze = malloc(sizeof(struct zone_entry), M_SUBPROC, M_ZERO | M_WAITOK);
	ze->zone.z_id = SCARG(uap, z);
	TAILQ_INIT(&(ze->zone.processes));
	//ze->zone.processes = TAILQ_HEAD_INITIALIZER(ze->zone.processes);
	TAILQ_INSERT_TAIL(&zones, ze, entry);
	zone_count++;

	*retval = 0;
	return (0);
}

int
sys_zone_destroy(struct proc *p, void *v, register_t *retval)
{
	struct sys_zone_destroy_args /* {
		syscallarg(zoneid_t) z;
	}; */ *uap = v;
	struct process *process;
	struct zone_entry *ze;

	// If the current program isn't in the global zone
	if (p->p_p->ps_zone != GLOBAL_ZONE) {
		printf("zone_destroy not global \n");
		*retval = -1;
		return (EPERM);
	}

	// If the user is not root
	if (p->p_p->ps_ucred->cr_uid != 0) {
		printf("zone_destroy not root \n");
		*retval = -1;
		return (EPERM);
	}



	TAILQ_FOREACH(ze, &zones, entry) {
		if (ze->zone.z_id == SCARG(uap, z)) {

			LIST_FOREACH(process, &allprocess, ps_list) {
				printf("process %d zone %d\n", process->ps_pid,
				    ze->zone.z_id);
				if (process->ps_zone == ze->zone.z_id) {
					*retval = -1;
					return (EBUSY);
				}
			}
			/*
			if (TAILQ_EMPTY(&(ze->zone.processes)) == 0) {
				*retval = -1;
				return (EBUSY);
			}*/
			TAILQ_REMOVE(&zones, ze, entry);
			free(ze, M_SUBPROC, sizeof(struct zone_entry));
			zone_count--;
			*retval = 0;
			return (0);
		}
	}

	// If this is reached the zone does not exist
	*retval = -1;
	return (ESRCH);
}
int
sys_zone_enter(struct proc *p, void *v, register_t *retval)
{
	struct sys_zone_enter_args /* {
		syscallarg(zoneid_t) z;
	}; */ *uap = v;
	struct zone_entry *ze;
	// If the current program isn't in the global zone
	if (p->p_p->ps_zone != GLOBAL_ZONE) {
		printf("zone_enter not global\n");
		*retval = -1;
		return (EPERM);
	}

	// If the user is not root
	if (p->p_p->ps_ucred->cr_uid != 0) {
		printf("zone_enter not root \n");
		*retval = -1;
		return (EPERM);
	}

	TAILQ_FOREACH(ze, &zones, entry) {
		if (ze->zone.z_id == SCARG(uap, z)) {
			p->p_p->ps_zone = SCARG(uap, z);
			*retval = 0;
			return (0);
		}
	}

	*retval = -1;
	return (ESRCH);
	// check for fails
}

int
sys_zone_list(struct proc *p, void *v, register_t *retval)
{
	struct sys_zone_list_args /* {
		syscallarg(zoneid_t *) zs;
		syscallarg(size_t *) nzs;
	}; */ *uap = v;
	struct zone_entry *ze;
	zoneid_t *list;
	size_t no_zones;

	printf("here1\n");
	printf("here1.5\n");
	if (copyin(SCARG(uap, nzs), (void *)&no_zones, sizeof(size_t)) != 0) {
		*retval = -1;
		return (EFAULT);
	}

	printf("here2\n");

	if (p->p_p->ps_zone != GLOBAL_ZONE) {
		printf("here3\n");
		if (no_zones < 1) {
			*retval = -1;
			return (ERANGE);
		}
		printf("here4\n");
		list = malloc(sizeof(zoneid_t), M_SUBPROC, M_ZERO | M_WAITOK);
		printf("here4.5\n");
		*list = p->p_p->ps_zone;
		no_zones = 1;
		printf("here5\n");
		copyout((void *)list, SCARG(uap, zs), sizeof(zoneid_t));
		copyout((void *)&no_zones, SCARG(uap, nzs), sizeof(size_t));
		printf("here6\n");
		*retval = -0;
		return (0);
	}

	if (no_zones < zone_count) {
		*retval = -1;
		return (ERANGE);
	}

	printf("here7\n");

	list = malloc(sizeof(zoneid_t) * zone_count, M_SUBPROC,
	    M_ZERO | M_WAITOK);
	printf("here8\n");

	no_zones = 0;
	TAILQ_FOREACH(ze, &zones, entry) {
		list[no_zones] = ze->zone.z_id;
		printf("here8.5 %d in position %zu\n", ze->zone.z_id, no_zones);
		no_zones++;
	}
	printf("here9 %p\n", list);

	copyout((void *)list, SCARG(uap, zs), sizeof(zoneid_t) * no_zones);
	copyout((void *)&no_zones, SCARG(uap, nzs), sizeof(size_t));
	printf("here10\n");

	*retval = -0;
	return (0);

}

zoneid_t
sys_zone_lookup(struct proc *p, void *v, register_t *retval)
{
	struct sys_zone_lookup_args /* {
		syscallarg(zoneid_t) z;
	}; */ *uap = v;
	struct zone_entry *ze;

	if (SCARG(uap, z) == -1) {
		*retval = p->p_p->ps_zone;
		return (0);
	}

	// If the current program isn't in the global zone
	if (p->p_p->ps_zone != GLOBAL_ZONE) {
		printf("zone_lookup not global \n");
		*retval = -1;
		return (ESRCH);
	}

	TAILQ_FOREACH(ze, &zones, entry) {
		if (ze->zone.z_id == SCARG(uap, z)) {
			*retval = ze->zone.z_id;
			return (0);
		}
	}

	*retval = -1;
	return (ESRCH);
}
