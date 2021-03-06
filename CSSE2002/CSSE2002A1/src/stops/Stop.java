package stops;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import exceptions.NoNameException;
import passengers.Passenger;
import routes.Route;
import vehicles.PublicTransport;

/**
 * Represents a stop in the transportation network.
 * Stops are where public transport vehicles collect and drop off passengers,
 * and are located along one or more routes.
 */
public class Stop extends Object {

    // The name of the stop
    private String name;

    // The x and y coordinates of the stop
    private int x;
    private int y;

    // The routes that go through the stop
    private List<Route> routes;

    // The stops that are adjacent to this one on a route
    private List<Stop> neighbouringStops;

    // The passengers waiting at the stop
    private List<Passenger> passengers;

    // The vehicles stopped at the stop
    private List<PublicTransport> vehicles;

    /**
     * Creates a new Stop object with the given name and coordinates.
     * A stop should be created with no passengers, routes, or vehicles.
     * If the given name contains any newline characters ('\n') or carriage
     * returns ('\r'), they should be removed from the string before it is
     * stored.
     * @param name The name of the stop being created.
     * @param x The x coordinate of the stop being created.
     * @param y The y coordinate of the stop being created.
     * @throws NoNameException If the given name is null or empty.
     */
    public Stop(String name, int x, int y) {
        if (name == null) {
            throw new NoNameException();
        }
        this.name = name;
        this.x = x;
        this.y = y;
        this.routes = new ArrayList<>();
        this.neighbouringStops = new ArrayList<>();
        this.passengers = new ArrayList<>();
        this.vehicles = new ArrayList<>();
    }

    /**
     * Returns the name of this stop.
     * @return The name of the stop.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the x-coordinate of this stop.
     * @return The x-coordinate of the stop.
     */
    public int getX() {
        return this.x;
    }

    /**
     * Returns the y-coordinate of this stop.
     * @return The y-coordinate of the stop.
     */
    public int getY() {
        return this.y;
    }

    /**
     * Records that this stop is part of the given route. If the given route is
     * null, it should not be added to the stop.
     * @param route The route to be added.
     */
    public void addRoute(Route route) {
        if (route != null) {
            this.routes.add(route);
        }
    }

    /**
     * Returns the routes associated with this stop.
     * No specific order is required for the route objects in the returned list.
     * Modifying the returned list should not result in changes to the internal
     * state of the class.
     * @return The routes which go past the stop.
     */
    public List<Route> getRoutes() {
        // Creates a new list to be returned so that the this.routes
        // can't be modified using getRoutes()
        List<Route> tempList = new ArrayList<>();
        // Iterates through this.routes, and copies each element to
        // tempList
        for (Route route : this.routes) {
            tempList.add(route);
        }
        return tempList;
    }

    /**
     * Records the given stop as being a neighbour of this stop.
     * If the given stop is null, or if this stop is already recorded as a
     * neighbour, it should not be added as a neighbour, and the method should
     * return early.
     * @param neighbour The stop to add as a neighbour.
     */
    public void addNeighbouringStop(Stop neighbour) {
        if (neighbour != null) {
            // For loop to check if neighbour is already a neighbouring stop
            for (Stop stop : this.neighbouringStops) {
                if (stop == neighbour) {
                    return;
                }
            }
            // If the stop is already a neighbour this line of code will not be
            // reached
            this.neighbouringStops.add(neighbour);
        }
    }

    /**
     * Returns all of the stops adjacent to this one on any routes.
     * No specific order is required for the stop objects in the returned list.
     * Modifying the returned list should not result in changes to the internal
     * state of the class.
     * @return The neighbours of this stop.
     */
    public List<Stop> getNeighbours() {
        // Creates a new list to be returned so that the this.neighbouringStops
        // can't be modified using getNeighbours()
        List<Stop> tempList = new ArrayList<>();
        // Iterates through this.neighbouringStops, and copies each element to
        // tempList
        for (Stop stop : this.neighbouringStops) {
            tempList.add(stop);
        }
        return tempList;
    }

    /**
     * Places a passenger at this stop.
     * If the given passenger is null, it should not be added to the stop.
     * @param passenger The passenger to add to the stop.
     */
    public void addPassenger(Passenger passenger) {
        if (passenger != null) {
            this.passengers.add(passenger);
        }
    }

    /**
     * Returns the passengers currently at this stop.
     * The order of the passengers in the returned list should be the same as
     * the order in which the passengers were added to the stop.
     * Modifying the returned list should not result in changes to the internal
     * state of the class.
     * @return The passengers currently waiting at the stop.
     */
    public List<Passenger> getWaitingPassengers() {
        // Creates a new list to be returned so that the this.passengers can't
        // be modified using getWaitingPassengers()
        List<Passenger> tempList = new ArrayList<>();
        // Iterates through this.passengers, and copies each element to
        // tempList
        for (Passenger passenger : this.passengers) {
            tempList.add(passenger);
        }
        return tempList;
    }

    /**
     * Checks whether the given public transport vehicle is at this stop or not.
     * @param transport The transport vehicle to check for.
     * @return True if the vehicle is at this stop, false otherwise.
     */
    public boolean isAtStop(PublicTransport transport) {
        boolean found = false;
        // Iterates through the vehicles at the stop to see if any match
        for (PublicTransport vehicle: this.vehicles) {
            if (transport == vehicle) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Returns the vehicles currently at this stop.
     * No specific order is required for the public transport objects in the
     * returned list.
     * Modifying the returned list should not result in changes to the internal
     * state of the class.
     * @return list of vehicles at the stop
     */
    public List<PublicTransport> getVehicles() {
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
     * Records a public transport vehicle arriving at this stop. There is no
     * limit on the number of vehicles that can be at a stop simultaneously.
     * If the given vehicle is already at this stop, or if the vehicle is null,
     * do nothing.
     * Otherwise, unload all of the passengers on the arriving vehicle
     * (using PublicTransport.unload()), and place them at this stop, as well as
     * recording the vehicle itself at this stop.
     * This method does not need to check whether this stop is on the given
     * transport's route, or whether the transport's route is a route of this
     * stop, and should also not update the location of the transport.
     * @param transport The public transport vehicle arriving at this stop.
     */
    public void transportArrive(PublicTransport transport) {
        if (transport == null) {
            return;
        }

        // Check if vehicle is already at the stop
        for (PublicTransport vehicle : this.vehicles) {
            if (vehicle == transport) {
                // Stops the rest of the code from running
                return;
            }
        }

        this.vehicles.add(transport);
        // Sets the location of each of the passengers of the vehicle to
        // this stop
        for (Passenger passenger : transport.getPassengers()) {
            this.addPassenger(passenger);
        }
        // Removes passengers from vehicle
        transport.unload();

    }

    /**
     * Records a public transport vehicle departing this stop and travelling to
     * a new stop.
     * This method should also update the vehicle's location to be the next stop
     * (using PublicTransport.travelTo(Stop)).
     * If the given vehicle is not at this stop, or if the vehicle is null, or
     * if the next stop is null, do nothing.
     * @param transport The transport currently leaving this stop.
     * @param nextStop The transport's next stop.
     */
    public void transportDepart(PublicTransport transport, Stop nextStop) {
        if (transport == null || nextStop == null) {
            // Does nothing if either input is null
            return;
        }

        boolean currentlyStopped = false;
        // Checks if the vehicle is actually at the stop
        for (PublicTransport vehicle : this.vehicles) {
            if (vehicle == transport) {
                currentlyStopped = true;
                break;
            }
        }

        if (currentlyStopped) {
            // Removes vehicle from the list of vehicles at this stop
            this.vehicles.remove(transport);
            // Moves the vehicle to the next stop
            transport.travelTo(nextStop);
            // Loops through passengers on vehicle to remove them from this stop
            for (Passenger passenger : transport.getPassengers()) {
                this.passengers.remove(passenger);
            }
        }
    }

    /**
     * Returns the Manhattan distance between this stop and the given other
     * stop.
     * Manhattan distance between two points, for example (x1, y1) and (x2, y2),
     * is calculated using the following formula:
     * abs(x1 - x2) + abs(y1 - y2)
     * where abs is a method that calculates the absolute value of a number.
     * @param stop The stop to calculate the Manhattan distance to.
     * @return The Manhattan distance between this stop and the given stop
     * (or -1 if the given stop is null)
     */
    public int distanceTo(Stop stop) {
        if (stop == null) {
            // Base case
            return -1;
        }
        // Uses absolute value to return the sum of the distances in each
        // direction
        return Math.abs(this.x - stop.x) + Math.abs(this.y - stop.y);
    }

    /*
     * Returns a set that corresponds with this.routes but without duplicates
     */
    private Set<Route> getNonDuplicateRoutes() {
        Set<Route> tempSet = new HashSet<>();
        // Loops through the routes associated with this stop
        for (Route route : this.routes) {
            boolean duplicate = false;
            // Checks route against all of the routes already added to tempSet
            for (Route tempSetRoutes : tempSet) {
                if (route == tempSetRoutes) {
                    // Marks the route as a duplicate
                    duplicate = true;
                    break;
                }
            }
            // If the route is not already in tempSet
            if (!duplicate) {
                tempSet.add(route);
            }
        }
        return tempSet;
    }

    /**
     * Compares this stop to the other object for equality.
     * Two stops are considered equal if they have the same name, x-coordinate,
     * y-coordinate, and routes. Routes may be in any order, as long as all of
     * this stop's routes are also associated with the other stop, and vice
     * versa. Duplicates of routes do not need to be considered in determining
     * equality (that is, if this stop has routes R1 and R2, and other has
     * routes R1, R2, and R1 again, their routes can still be considered equal,
     * ignoring duplicates).
     * @param other The other object to compare for equality.
     * @return True if the objects are equal (as defined above), false otherwise
     * (including if other is null or not an instance of the Stop class.
     */
    @Override
    public boolean equals(Object other) {
        // Cannot be equal if the other object is not a stop
        if (!(other instanceof Stop)) {
            return false;
        }

        // Returns true if all relevant fields are equal, and false if not
        return this.name.equals(((Stop) other).getName())
                && this.x == ((Stop) other).getX()
                && this.y == ((Stop) other).getY()
                && this.getNonDuplicateRoutes()
                .equals(((Stop) other).getNonDuplicateRoutes());
    }

    @Override
    public int hashCode() {
        // Prime numbers used to minimise chance of different routes having the
        // same hash code
        int result = 7 * this.name.hashCode() + 17 * this.x + 29 * this.y;
        for (Route route : this.getNonDuplicateRoutes()) {
            // All route hash codes are multiplied by this same number,
            // as order doesn't matter in the equals function, meaning different
            // orders should result in the same hash code
            result += 41 * route.hashCode();
        }
        return result;
    }

    /**
     * Creates a string representation of a stop in the format:
     * '{name}:{x}:{y}'
     * without the surrounding quotes, and where {name} is replaced by the name
     * of the stop, {x} is replaced by the x-coordinate of the stop, and {y} is
     * replaced by the y-coordinate of the stop.
     * @return A string representation of the stop.
     */
    @Override
    public String toString() {
        // Concatenates info into desired format
        return this.name + ":" + this.x + ":" + this.y;
    }

}