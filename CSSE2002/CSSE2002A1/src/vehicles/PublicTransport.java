package vehicles;

import java.util.ArrayList;
import java.util.List;

import exceptions.EmptyRouteException;
import exceptions.OverCapacityException;
import passengers.Passenger;
import routes.Route;
import stops.Stop;

/**
 * A base public transport vehicle in the transportation network.
 */
public abstract class PublicTransport extends Object {

    // The ID number of the vehicle
    private int id;

    // The maximum number of passengers the vehicle can have at any given time
    private int capacity;

    // The route the vehicle is on
    private Route route;

    // The stop the vehicle is at
    private Stop location;

    // The passengers on board the vehicle
    private List<Passenger> currentPassengers;

    /**
     * Creates a new public transport vehicle with the given id, capacity, and
     * route.
     * The vehicle should initially have no passengers on board, and should be
     * placed at the beginning of the given route
     * (given by Route.getStartStop()). If the route is empty, the current
     * location should be stored as null.
     * If the given capacity is negative, 0 should be stored as the capacity
     * instead (meaning no passengers will be allowed on board this vehicle).
     * @param id The identifying number of the vehicle.
     * @param capacity The maximum number of passengers allowed on board.
     * @param route The route the vehicle follows. Note that the given route
     *              should never be null (@require route != null), and thus will
     *              not be tested with a null value.
     */
    public PublicTransport(int id, int capacity, Route route) {
        this.id = id;
        this.capacity = capacity;
        this.route = route;
        this.currentPassengers = new ArrayList<>();
        // Class won't compile without catch statement for EmptyRouteException
        try {
            this.location = route.getStartStop();
        } catch (EmptyRouteException e) {
            // Squash
        }
    }

    /**
     * Returns the route this vehicle is on.
     * @return The route this vehicle is on.
     */
    public Route getRoute() {
        return this.route;
    }

    /**
     * Returns the id of this route.
     * @return The id of this route.
     */
    public int getId() {
        return this.id;
    }

    /**
     * Returns the current location of this vehicle.
     * @return The stop this vehicle is currently located at, or null if it is
     * not currently located at a stop.
     */
    public Stop getCurrentStop() {
        return this.location;
    }

    /**
     * Returns the number of passengers currently on board this vehicle.
     * @return The number of passengers in the vehicle.
     */
    public int passengerCount() {
        // Returns number of elements in passengers list
        return this.currentPassengers.size();
    }

    /**
     * Returns the maximum number of passengers allowed on this vehicle.
     * @return The maximum capacity of the vehicle.
     */
    public int getCapacity() {
        return this.capacity;
    }

    /**
     * Returns the type of this vehicle, as determined by the type of the route
     * it is on (i.e. The type returned by Route.getType()).
     * @return The type of this vehicle.
     */
    public String getType() {
        // Vehicle type is the same as route type
        return this.route.getType();
    }

    /**
     * Returns the passengers currently on-board this vehicle.
     * No specific order is required for the passenger objects in the returned
     * list.
     * Modifying the returned list should not result in changes to the internal
     * state of the class.
     * @return The passengers currently on the public transport vehicle.
     */
    public List<Passenger> getPassengers() {
        // Creates a new list to be returned so that the this.currentPassengers
        // can't be modified using getWaitingPassengers()
        List<Passenger> tempList = new ArrayList<>();
        // Iterates through this.currentPassengers, and copies each element to
        // tempList
        for (Passenger passenger : this.currentPassengers) {
            tempList.add(passenger);
        }
        return tempList;
    }

    /**
     * Adds the given passenger to this vehicle.
     * If the passenger is null, the method should return without adding it to
     * the vehicle.
     * If the vehicle is already at (or over) capacity, an exception should be
     * thrown and the passenger should not be added to the vehicle.
     * @param passenger The passenger boarding the vehicle.
     * @throws OverCapacityException If the vehicle is already at (or over)
     * capacity.
     */
    public void addPassenger(Passenger passenger) throws OverCapacityException {
        if (passenger == null) {
            return;
        }

        // If the vehicle is at or over capacity
        if (this.currentPassengers.size() >= this.capacity) {
            throw new OverCapacityException();
        }
        this.currentPassengers.add(passenger);
    }

    /**
     * Removes the given passenger from the vehicle.
     * If the passenger is null, or is not on board the vehicle, the method
     * should return false, and should not have any effect on the passengers
     * currently on the vehicle.
     * @param passenger The passenger disembarking the vehicle.
     * @return True if the passenger was successfully removed, false otherwise
     * (including the case where the given passenger was not on board the
     * vehicle to begin with).
     */
    public boolean removePassenger(Passenger passenger) {
        if (passenger == null) {
            // Remove fails if passenger is null, and the rest of the code is
            // run
            return false;
        }

        boolean onBoard = false;
        // Loops through passengers if the relevant passenger is on board
        for (Passenger i : this.currentPassengers) {
            if (i == passenger) {
                onBoard = true;
                break;
            }
        }

        if (!onBoard) {
            // Remove fails if passenger isn't on board
            return false;
        }
        this.currentPassengers.remove(passenger);
        return true;
    }

    /**
     * Empties the vehicle of all its current passengers, and returns all the
     * passengers who were removed.
     * No specific order is required for the passenger objects in the returned
     * list.
     * If there are no passengers currently on the vehicle, the method just
     * returns an empty list.
     * Modifying the returned list should not result in changes to the internal
     * state of the class.
     * @return The passengers who used to be on the bus.
     */
    public List<Passenger> unload() {
        // Uses this.getPassengers() instead of this.currentPassengers so that
        // a copy of this.currentPassengers is returned, and hence the original
        // this.currentPassengers cannot be modified by modifying the returned
        // list
        List<Passenger> tempList = this.getPassengers();
        this.currentPassengers = new ArrayList<Passenger>();
        return tempList;
    }

    /**
     * Updates the current location of the vehicle to be the given stop.
     * If the given stop is null, or is not on this public transport's route the
     * current location should remain unchanged.
     * @param stop The stop the vehicle has travelled to.
     */
    public void travelTo(Stop stop) {
        if (stop == null) {
            return;
        }

        // Checks if the desired stop is on the vehicle's route
        for (Stop location : this.route.getStopsOnRoute()) {
            if (stop == location) {
                this.location = stop;
                // If the stop is found, the rest of the route does not need
                // to be checked
                return;
            }
        }
    }

    /**
     * Creates a string representation of a public transport vehicle in the
     * format:
     * '{type} number {id} ({capacity}) on route {route}'
     * without the surrounding quotes, and where {type} is replaced by the type
     * of the vehicle, {id} is replaced by the id of the vehicle, {capacity} is
     * replaced by the maximum capacity of the vehicle, and {route} is replaced
     * by the route number of the route the vehicle is on. For example:
     * bus number 1 (30) on route 1
     * @return A string representation of the vehicle.
     */
    @Override
    public String toString() {
        // Concatenates info into desired format
        return this.getType() + " number " + this.id + " (" + this.capacity
                + ") on route " + this.route.getRouteNumber();
    }
}
