#include <sys/types.h>
#include <sys/malloc.h>
#include <sys/systm.h>
#include <sys/errno.h>
#include <sys/queue.h>
#include <sys/zones.h>
#include <sys/param.h>
#include <sys/ucred.h>
#include <sys/proc.h>
#include <sys/mount.h>
#include <sys/systm.h>
#include <sys/syscallargs.h>

/*
 * Initializes the list of zones and sets process0 to be in GLOBAL_ZONE
 */
void
zone_init(struct process *p)
{
	struct zone_entry *ze;

	TAILQ_INIT(&zones);
	/* Create the global zone */
	zone_count = 1;
	ze = malloc(sizeof(struct zone_entry), M_SUBPROC, M_ZERO | M_WAITOK);
	ze->z_id = GLOBAL_ZONE;
	TAILQ_INSERT_TAIL(&zones, ze, entry);

	/* Set the process to be in the global zone */
	p->ps_zone = ze->z_id;
}

/*
 * Attempts to create a zone and adds it to the list of zones if successful
 */
int
sys_zone_create(struct proc *p, void *v, register_t *retval)
{
	struct sys_zone_create_args /* {
		syscallarg(zoneid_t) z;
	}; */ *uap = v;
	struct zone_entry *ze;

	KERNEL_LOCK();
	/* If the current program isn't in the global zone */
	if (p->p_p->ps_zone != GLOBAL_ZONE) {
		*retval = -1;
		KERNEL_UNLOCK();
		return (EPERM);
	}

	/* If the user is not root */
	if (p->p_p->ps_ucred->cr_uid != 0) {
		*retval = -1;
		KERNEL_UNLOCK();
		return (EPERM);
	}

	/* If there are already too many existing zones */
	if (zone_count >= MAXZONES) {
		*retval = 1;
		KERNEL_UNLOCK();
		return (ERANGE);
	}

	TAILQ_FOREACH(ze, &zones, entry) {
		/* If there is an existing zone with this zone ID */
		if (ze->z_id == SCARG(uap, z)) {
			*retval = 1;
			KERNEL_UNLOCK();
			return (EBUSY);
		}
	}

	/* Create a new zone and add it to the list */
	ze = malloc(sizeof(struct zone_entry), M_SUBPROC, M_ZERO | M_WAITOK);
	ze->z_id = SCARG(uap, z);
	TAILQ_INSERT_TAIL(&zones, ze, entry);
	zone_count++;

	*retval = 0;
	KERNEL_UNLOCK();
	return (0);
}

/*
 * Attempts to destroy a zone and removes it from the list of zones if
 * successful
 */
int
sys_zone_destroy(struct proc *p, void *v, register_t *retval)
{
	struct sys_zone_destroy_args /* {
		syscallarg(zoneid_t) z;
	}; */ *uap = v;
	struct process *process;
	struct zone_entry *ze;

	KERNEL_LOCK();
	/* If the current program isn't in the global zone */
	if (p->p_p->ps_zone != GLOBAL_ZONE) {
		*retval = -1;
		KERNEL_UNLOCK();
		return (EPERM);
	}

	/* If the user is not root */
	if (p->p_p->ps_ucred->cr_uid != 0) {
		*retval = -1;
		KERNEL_UNLOCK();
		return (EPERM);
	}

	TAILQ_FOREACH(ze, &zones, entry) {
		if (ze->z_id == SCARG(uap, z)) {
			/*
			 * Loop through processes to see if any have this
			 * zone id, and fail if so.
			 */
			LIST_FOREACH(process, &allprocess, ps_list) {
				if (process->ps_zone == ze->z_id) {
					*retval = -1;
					KERNEL_UNLOCK();
					return (EBUSY);
				}
			}
			/* Remove the zone and clean up */
			TAILQ_REMOVE(&zones, ze, entry);
			free(ze, M_SUBPROC, sizeof(struct zone_entry));
			zone_count--;
			*retval = 0;
			KERNEL_UNLOCK();
			return (0);
		}
	}

	/* If this is reached the zone was not in the list (does not exist) */
	*retval = -1;
	KERNEL_UNLOCK();
	return (ESRCH);
}

/*
 * Attempts switch the process to another zone
 */
int
sys_zone_enter(struct proc *p, void *v, register_t *retval)
{
	struct sys_zone_enter_args /* {
		syscallarg(zoneid_t) z;
	}; */ *uap = v;
	struct zone_entry *ze;

	KERNEL_LOCK();
	/* If the current program isn't in the global zone */
	if (p->p_p->ps_zone != GLOBAL_ZONE) {
		*retval = -1;
		KERNEL_UNLOCK();
		return (EPERM);
	}

	/* If the user is not root */
	if (p->p_p->ps_ucred->cr_uid != 0) {
		*retval = -1;
		KERNEL_UNLOCK();
		return (EPERM);
	}

	TAILQ_FOREACH(ze, &zones, entry) {
		/* If this is the zone we are looking for */
		if (ze->z_id == SCARG(uap, z)) {
			/* Enter he zone */
			p->p_p->ps_zone = SCARG(uap, z);
			*retval = 0;
			KERNEL_UNLOCK();
			return (0);
		}
	}

	/* If this is reached the zone was not in the list (does not exist) */
	*retval = -1;
	KERNEL_UNLOCK();
	return (ESRCH);
}

/*
 * Lists all zones if called in the global zone, or just the current zone if
 * not in the global zone
 */
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

	KERNEL_LOCK();
	/* Get nzs from userland */
	if (copyin(SCARG(uap, nzs), (void *)&no_zones, sizeof(size_t)) != 0) {
		*retval = -1;
		KERNEL_UNLOCK();
		return (EFAULT);
	}

	if (p->p_p->ps_zone != GLOBAL_ZONE) {
		if (no_zones < 1) {
			*retval = -1;
			KERNEL_UNLOCK();
			return (ERANGE);
		}
		/* Create a list with just the current zone */
		list = malloc(sizeof(zoneid_t), M_SUBPROC, M_ZERO | M_WAITOK);
		*list = p->p_p->ps_zone;
		no_zones = 1;
		/* Move the list to userland */
		copyout((void *)list, SCARG(uap, zs), sizeof(zoneid_t));
		copyout((void *)&no_zones, SCARG(uap, nzs), sizeof(size_t));
		*retval = -0;
		return (0);
	}

	/* If nzs is too small */
	if (no_zones < zone_count) {
		*retval = -1;
		KERNEL_UNLOCK();
		return (ERANGE);
	}

	list = malloc(sizeof(zoneid_t) * zone_count, M_SUBPROC,
	    M_ZERO | M_WAITOK);
	no_zones = 0;

	/* Add each zone to the array */
	TAILQ_FOREACH(ze, &zones, entry) {
		list[no_zones] = ze->z_id;
		no_zones++;
	}

	/* Copy the array and number of zones to userland */
	copyout((void *)list, SCARG(uap, zs), sizeof(zoneid_t) * no_zones);
	copyout((void *)&no_zones, SCARG(uap, nzs), sizeof(size_t));

	*retval = -0;
	KERNEL_UNLOCK();
	return (0);

}

/*
 * Determines if a given zone exists
 */
zoneid_t
sys_zone_lookup(struct proc *p, void *v, register_t *retval)
{
	struct sys_zone_lookup_args /* {
		syscallarg(zoneid_t) z;
	}; */ *uap = v;
	struct zone_entry *ze;

	KERNEL_LOCK();
	/* With an argument of -1 return the current zone */
	if (SCARG(uap, z) == -1) {
		*retval = p->p_p->ps_zone;
		KERNEL_UNLOCK();
		return (0);
	}

	/* If the current program isn't in the global zone */
	if (p->p_p->ps_zone != GLOBAL_ZONE) {
		*retval = -1;
		KERNEL_UNLOCK();
		return (ESRCH);
	}

	TAILQ_FOREACH(ze, &zones, entry) {
		/* If this is the zone we are looking for */
		if (ze->z_id == SCARG(uap, z)) {
			*retval = ze->z_id;
			return (0);
		}
	}

	/* If this is reached the zone was not in the list (does not exist) */
	*retval = -1;
	KERNEL_UNLOCK();
	return (ESRCH);
}
