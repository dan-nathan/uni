package routes;

import java.util.ArrayList;
import java.util.List;

import exceptions.EmptyRouteException;
import exceptions.IncompatibleTypeException;
import stops.Stop;
import vehicles.PublicTransport;

/**
 * Represents a route in the transportation network.
 * A route is essentially a collection of stops which public transport vehicles
 * can follow.
 */
public abstract class Route extends Object {

    // The route's name
    private String name;

    // The route's route number
    private int routeNumber;

    // The stops on the route
    private List<Stop> stops;

    // The vehicles traversing the route
    private List<PublicTransport> vehicles;

    /**
     * Creates a new Route with the given name and number.
     * The route should initially have no stops or vehicles on it.
     * If the given name contains any newline characters ('\n') or carriage
     * returns ('\r'), they should be removed from the string before it is
     * stored.
     * If the given name is null, an empty string should be stored in its place.
     * @param name The name of the route.
     * @param routeNumber The route number of the route.
     */
    public Route(String name, int routeNumber) {
        if (name == null) {
            this.name = "";
        } else {
            // Removes \n  characters by replacing all instances of them with an
            // empty string, then does the same for \r
            this.name = name.replace("\n", "").replace("\r", "");
        }

        this.routeNumber = routeNumber;
        this.stops = new ArrayList<>();
        this.vehicles = new ArrayList<>();
    }

    /**
     * Returns the name of the route.
     * @return The route name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the number of the route.
     * @return The route number.
     */
    public int getRouteNumber() {
        return this.routeNumber;
    }

    /**
     * RReturns the stops which comprise this route.
     * The order of the stops in the returned list should be the same as the
     * order in which the stops were added to the route.
     * Modifying the returned list should not result in changes to the internal
     * state of the class.
     * @return The stops making up the route.
     */
    public List<Stop> getStopsOnRoute() {
        // Creates a new list to be returned so that the this.stops can't be
        // modified using getStopsOnRoute()
        List<Stop> tempList = new ArrayList<>();
        // Iterates through this.stop, and copies each element to tempList
        for (Stop stop : this.stops) {
            tempList.add(stop);
        }
        return tempList;
    }

    /**
     * Returns the first stop of the route (i.e. the first stop to be added to
     * the route).
     * @return The start stop of the route.
     * @throws EmptyRouteException If there are no stops currently on the route
     */
    public Stop getStartStop() throws EmptyRouteException {
        // If the list of stops is empty
        if (this.stops.size() == 0) {
            throw new EmptyRouteException();
        }
        return this.stops.get(0);
    }

    /**
     * Adds a stop to the route.
     * If the given stop is null, it should not be added to the route.
     * If this is the first stop to be added to the route, the given stop should
     * be recorded as the starting stop of the route. Otherwise, the given stop
     * should be recorded as a neighbouring stop of the previous stop on the
     * route (and vice versa) using the Stop.addNeighbouringStop(Stop) method.
     * This route should also be added as a route of the given stop (if the
     * given stop is not null) using the Stop.addRoute(Route) method.
     * @param stop The stop to be added to this route.
     */
    public void addStop(Stop stop) {
        if (stop == null) {
            return;
        }

        // If this is not the first stop to be added
        if (this.stops.size() != 0) {
            // The current final element of stops
            Stop currentLastStop = this.stops.get(this.stops.size() - 1);
            // Adds the stop and currently last stop as neighbours
            stop.addNeighbouringStop(currentLastStop);
            currentLastStop.addNeighbouringStop(stop);
        }

        // Adds the stop to the route
        this.stops.add(stop);
        // Adds this route to the stop
        stop.addRoute(this);
    }

    /**
     * Returns the public transport vehicles currently on this route.
     * No specific order is required for the public transport objects in the
     * returned list.
     * Modifying the returned list should not result in changes to the internal
     * state of the class.
     * @return The vehicles currently on the route.
     */
    public List<PublicTransport> getTransports() {

        // Creates a new list to be returned so that the this.vehicles can't be
        // modified using getTransports()
        List<PublicTransport> tempList = new ArrayList<>();
        // Iterates through this.vehicles, and copies each element to tempList
        for (PublicTransport vehicle : this.vehicles) {
            tempList.add(vehicle);
        }
        return tempList;
    }

    /**
     * Adds a vehicle to this route.
     * If the given transport is null, it should not be added to the route.
     * The method should check for the transport being null first, then for an
     * empty route, and then for incompatible types (in that order).
     * @param transport The vehicle to be added to the route.
     * @throws EmptyRouteException If there are not yet any stops on the route.
     * @throws IncompatibleTypeException If the given vehicle is of the
     *                                   incorrect type for this route. This
     *                                   depends on the type of the route, i.e.
     *                                   a BusRoute can only accept Bus
     *                                   instances.
     */
    public void addTransport(PublicTransport transport)
            throws EmptyRouteException, IncompatibleTypeException {
        if (transport == null) {
            return;
        }

        // Checks if the route is empty
        if (this.stops.size() == 0) {
            throw new EmptyRouteException();
        }

        // Checks if the route and transport are of the same type
        if (this.getType() != transport.getType()) {
            throw new IncompatibleTypeException();
        }

        this.vehicles.add(transport);
    }

    /**
     * Compares this stop with another object for equality. Two routes are equal
     * if their names and route numbers are equal.
     * @param other The other object to compare for equality.
     * @return True if the objects are equal (as defined above), false otherwise
     * (including if other is null or not an instance of the Route class).
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Route)) {
            // If the other object is not a route, they cannot be equal
            return false;
        }

        // Checks to see if the names and route numbers match
        if (this.name == ((Route) other).getName()
                && this.routeNumber == ((Route) other).getRouteNumber()) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // Prime numbers used to minimise chance of different routes having the
        // same hash code
        return 13 * this.name.hashCode() + 31 * this.routeNumber;
    }

    /**
     * Returns the type of this route.
     * @return The type of the route (see subclasses)
     */
    public abstract String getType();

    /**
     * Creates a string representation of a route in the format:
     * '{type},{name},{number}:{stop0}|{stop1}|...|{stopN}' without the
     * surrounding quotes, and where {type} is replaced by the type of the
     * route, {name} is replaced by the name of the route, {number} is replaced
     * by the route number, and {stop0}|{stop1}|...|{stopN} is replaced by a
     * list of the names of the stops stops making up the route. For example:
     * bus,red,1:UQ Lakes|City|Valley
     * @return A string representation of the route.
     */
    @Override
    public String toString() {
        // Creates a string to return, and adds information to the start
        String returnString = this.getType() + "," + this.name + ","
                + this.routeNumber + ":";

        // Loop is used for stops as different routes can have different numbers
        // of stops
        for (Stop stop : this.stops) {
            returnString += stop.getName();
            // Checks if the stop is the index of the stop in the list
            // corresponds with the last in the list. The | character is only
            // added if this is not the case
            if (this.stops.indexOf(stop) != this.stops.size() - 1) {
                returnString += "|";
            }
        }

        return returnString;
    }
}