package routes;

import exceptions.EmptyRouteException;
import exceptions.IncompatibleTypeException;
import exceptions.TransportFormatException;
import stops.Stop;
import utilities.Writeable;
import vehicles.PublicTransport;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a route in the transportation network.
 *
 * <p>A route is essentially a collection of stops which public transport vehicles
 * can follow.
 */
public abstract class Route implements Writeable {
    // the name of the route
    private String name;

    // the number of the route
    private int routeNumber;

    // tracks where vehicles are currently located on the route
    private List<PublicTransport> vehicles;

    // the stops which make up the route
    private List<Stop> route;

    /**
     * Creates a new Route with the given name and number.
     *
     * <p>The route should initially have no stops or vehicles on it.
     *
     * <p>If the given name contains any newline characters ('\n') or carriage returns
     * ('\r'), they should be removed from the string before it is stored.
     *
     * <p>If the given name is null, an empty string should be stored in its place.
     *
     * @param name The name of the route.
     * @param routeNumber The route number of the route.
     */
    public Route(String name, int routeNumber) {
        this.name = name == null ? "" : name.replace("\n", "")
                .replace("\r", "");
        this.routeNumber = routeNumber;
        this.vehicles = new ArrayList<>();
        this.route = new ArrayList<>();
    }

    /**
     * Returns the name of the route.
     *
     * @return The route name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the number of the route.
     *
     * @return The route number.
     */
    public int getRouteNumber() {
        return routeNumber;
    }

    /**
     * Returns the stops which comprise this route.
     *
     * <p>The order of the stops in the returned list should be the same as the
     * order in which the stops were added to the route.
     *
     * <p>Modifying the returned list should not result in changes to the internal
     * state of the class.
     *
     * @return The stops making up the route.
     */
    public List<Stop> getStopsOnRoute() {
        return new ArrayList<>(route);
    }

    /**
     * Returns the first stop of the route (i.e. the first stop to be added to the
     * route).
     *
     * @return The start stop of the route.
     * @throws EmptyRouteException If there are no stops currently on the route
     */
    public Stop getStartStop() throws EmptyRouteException {
        if (route.isEmpty()) {
            throw new EmptyRouteException();
        }

        return route.get(0);
    }

    /**
     * Adds a stop to the route.
     *
     * <p>If the given stop is null, it should not be added to the route.
     *
     * <p>If this is the first stop to be added to the route, the given stop should
     * be recorded as the starting stop of the route. Otherwise, the given stop
     * should be recorded as a neighbouring stop of the previous stop on the route
     * (and vice versa) using the {@link Stop#addNeighbouringStop(Stop)} method.
     *
     * <p>This route should also be added as a route of the given stop (if the given
     * stop is not null) using the {@link Stop#addRoute(Route)} method.
     *
     * @param stop The stop to be added to this route.
     */
    public void addStop(Stop stop) {
        if (stop == null) {
            return;
        }

        stop.addRoute(this);
        route.add(stop);

        // return if this was the first stop
        if (route.size() == 1) {
            return;
        }

        Stop previous = route.get(route.size() - 2);
        previous.addNeighbouringStop(stop);
        stop.addNeighbouringStop(previous);
    }

    /**
     * Returns the public transport vehicles currently on this route.
     *
     * <p>No specific order is required for the public transport objects in the
     * returned list.
     *
     * <p>Modifying the returned list should not result in changes to the internal
     * state of the class.
     *
     * @return The vehicles currently on the route.
     */
    public List<PublicTransport> getTransports() {
        return new ArrayList<>(this.vehicles);
    }

    /**
     * Adds a vehicle to this route.
     *
     * <p>If the given transport is null, it should not be added to the route.
     *
     * <p>The method should check for the transport being null first, then for an
     * empty route, and then for incompatible types (in that order).</p>
     *
     * @param transport The vehicle to be added to the route.
     * @throws EmptyRouteException If there are not yet any stops on the route.
     * @throws IncompatibleTypeException If the given vehicle is of the incorrect
     *      type for this route. This depends on the type of the route, i.e. a
     *      BusRoute can only accept Bus instances.
     */
    public void addTransport(PublicTransport transport)
            throws EmptyRouteException, IncompatibleTypeException {
        if (transport == null) {
            return;
        }

        if (route.isEmpty()) {
            throw new EmptyRouteException();
        }

        if (!getType().equals(transport.getType())) {
            throw new IncompatibleTypeException();
        }

        vehicles.add(transport);
    }

    /**
     * Compares this stop with another object for equality.
     *
     * Two routes are equal if their names and route numbers are equal.
     *
     * {@inheritDoc}
     *
     * @param other The other object to compare for equality.
     * @return True if the objects are equal (as defined above), false otherwise
     *         (including if other is null or not an instance of the {@link Route}
     *         class.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Route)) {
            return false;
        }
        Route otherRoute = (Route) other;
        return name.equals(((Route) other).getName())
                && routeNumber == otherRoute.getRouteNumber();
    }

    @Override
    public int hashCode() {
        return routeNumber;
    }

    /**
     * Returns the type of this route.
     *
     * @return The type of the route (see subclasses)
     */
    public abstract String getType();

    /**
     * Creates a string representation of a route in the format:
     *
     * <p>'{type},{name},{number}:{stop0}|{stop1}|...|{stopN}'
     *
     * <p>without the surrounding quotes, and where {type} is replaced by the type
     * of the route, {name} is replaced by the name of the route, {number} is replaced
     * by the route number, and {stop0}|{stop1}|...|{stopN} is replaced by a list of
     * the names of the stops stops making up the route. For example:
     *
     * <p>bus,red,1:UQ Lakes|City|Valley
     *
     * @return A string representation of the route.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(getType()).append(",");
        builder.append(name).append(",").append(routeNumber);
        builder.append(":");

        for (Stop stop : route) {
            builder.append(stop.getName()).append("|");
        }

        if (!route.isEmpty()) {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    /*
     * Helper method for decode. Takes routeString and splits it into an array
     * of strings: [type, name, routeNumber, stops], throwing an exception if
     * the string cannot be split as expected
     */
    private static String[] splitNonStops(String routeString)
            throws TransportFormatException {
        // splitComma is [type, name, other info]. A limit of -1 includes empty
        // strings to check if there is an extra , at the end of the string
        String[] splitComma = routeString.split(",", -1);
        // Throws an exception if there are an unexpected number of commas
        if (splitComma.length != 3) {
            throw new TransportFormatException();
        }
        for (int i = 0; i <= 1; i ++) {
            // If the type or name contain delimiters they shouldn't throw an
            // exception
            if (splitComma[i].contains(":") || splitComma[i].contains("|")) {
                throw new TransportFormatException();
            }
        }

        // Split the info that was not split by commas. splitColon is
        // [number, stop names[]]
        String[] splitColon = splitComma[2].split(":", -1);
        if (splitColon.length != 2) {
            throw new TransportFormatException();
        }

        // The route number doesn't have to be checked for extra | delimiters as
        // that will already throw an exception when checking that it can be
        // parsed as an integer

        String[] returnStrings = {splitComma[0], splitComma[1], splitColon[0],
                splitColon[1]};
        return returnStrings;
    }

    /*
     * Helper method for decode. Converts a string representing a list of stops
     * to an actual list of stops, throwing an exception if one of the stops is
     * invalid
     */
    private static List<Stop> splitStops(String stopListString,
            List<Stop> existingStops) throws TransportFormatException {

        // splitVLine is a list of stop names
        // Split by \\| rather than | as | is a special character and needs to
        // be escaped
        String[] splitVLine = stopListString.split("\\|", -1);
        List<Stop> routeStops = new ArrayList<>();
        // There is no split option that will both throw an exception when there
        // is an extra | at the end, and not throw an exception when there are
        // no stops, so the case where there is no text after the colon is
        // eliminated using an if statement
        List<Stop> returnStops = new ArrayList<>();
        if (!splitVLine[0].equals("")) {
            // If there is at least one stop, loop through each
            for (String stopString : splitVLine) {
                Stop equivalentStop = null;
                // Try to find a matching stop name in existingStops
                for (Stop stop : existingStops) {
                    if (stop.getName().equals(stopString)) {
                        equivalentStop = stop;
                        break;
                    }
                }
                // If a matching stop is not found, throw an exception,
                // Otherwise add it to the route
                if (equivalentStop == null) {
                    throw new TransportFormatException();
                }
                routeStops.add(equivalentStop);
            }
        }

        return routeStops;
    }

    /**
     * Creates a new route object based on the given string representation.
     * The format of the string should match that returned by the encode()
     * method.
     * The correct stops should also be added to the decoded route using the
     * addStop(Stop) method. If there are multiple stops in the existingStops
     * list which have the same name, then the first stop in the list with a
     * matching name should be used. This still applies if there are multiple
     * stops in the routeString with the same name (i.e. they should both use
     * the first stop in the list with a matching name).
     * Whilst parsing, if spaces (i.e. ' ') are encountered before or after
     * integers, (e.g. {number}), the spaces should simply be trimmed (for
     * example, using something like String.trim()). If spaces are encountered
     * before or after strings (e.g. {type}), the spaces should be considered
     * part of the string and not handled differently from any other character.
     * @param routeString The string to decode.
     * @param existingStops The stops which currently exist in the transport
     *                      network.
     * @return The decoded route object (a BusRoute, TrainRoute, or FerryRoute,
     *         depending on the type given in the string).
     * @throws TransportFormatException If the given string or existingStops
     *         list is null, or the string is incorrectly formatted (according
     *         to the encode() representation). This includes, but is not
     *         limited to:
     *         1. A route type that is not one of "bus", "train", or "ferry".
     *         2. The route number is not an integer value.
     *         3. The stop name given in the string does not match one of the
     *            given existingStops.
     *         4. Any extra delimiters (, : |) being encountered whilst parsing.
     *         5. Any of the parts of the string being missing.
     */
    public static Route decode(String routeString, List<Stop> existingStops)
            throws TransportFormatException {
        // Throws an exception if either parameter is null
        if (routeString == null || existingStops == null) {
            throw new TransportFormatException();
        }

        // Split routeString into [type, name, number, stops]
        String[] split = splitNonStops(routeString);
        // Rename elements of split for readability
        String type = split[0], name = split[1], stopListString = split[3];
        int routeNumber;
        try {
            // Removes leading and trailing spaces and attempts to covert to an
            // integer
            routeNumber = Integer.parseInt(split[2].trim());
        } catch (NumberFormatException e) {
            throw new TransportFormatException();
        }
        // Coverts
        List<Stop> routeStops = splitStops(stopListString, existingStops);

        Route returnRoute;
        switch (type) {
            case "bus":
                returnRoute = new BusRoute(name, routeNumber);
                break;
            case "ferry":
                returnRoute = new FerryRoute(name, routeNumber);
                break;
            case "train" :
                returnRoute = new TrainRoute(name, routeNumber);
                break;
            default:
                // Throw an exception if the route type is invalid
                throw new TransportFormatException();
        }
        // For each stop listed, add them to the route
        for (Stop stop : routeStops) {
            returnRoute.addStop(stop);
        }

        return returnRoute;
    }

    /**
     * Encodes this route as a string in the same format as specified in
     * toString().
     * Specified by encode in interface Writeable
     * @return This route encoded as a string.
     */
    public String encode() {
        return this.toString();
    }
}
