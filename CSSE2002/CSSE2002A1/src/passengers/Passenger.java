package passengers;

import stops.Stop;

/**
 * A base passenger in the transport network.
 */
public class Passenger extends Object {

    // The passenger's name
    private String name;

    // The passenger's desired destination
    private Stop destination;

    /**
     * Construct a new base passenger with the given name and destination.
     * Should meet the specification of Passenger(String), as well as storing
     * the given destination stop.
     * @param name
     * @param destination
     */
    public Passenger(String name, Stop destination) {
        if (name == null) {
            this.name = "";
        } else {
            // Removes \n  characters by replacing all instances of them with an
            // empty string, then does the same for \r
            this.name = name.replace("\n", "").replace("\r", "");
        }

        this.destination = destination;
    }

    /**
     * Construct a new base passenger with the given name, and without a
     * destination.
     * If the given name is null, an empty string should be stored instead.
     * If the given name contains any newline characters ('\n') or carriage
     * returns ('\r'), they should be removed from the string before it is
     * stored.
     * @param name The name of the passenger.
     */
    public Passenger(String name) {
        // Calls the other constructor with destination set to null
        this(name, null);
    }

    /**
     * Returns the name of the passenger.
     * @return The name of the passenger.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the destination of the passenger.
     * A value of null for the given stop simply indicates that the passenger
     * has no destination.
     * @param destination The intended destination of the passenger, or null if
     *                    the passenger has no destination.
     */
    public void setDestination(Stop destination) {
        this.destination = destination;
    }

    /**
     * Gets the destination of the passenger.
     * The intended destination of the passenger, or null if the passenger has
     * no destination.
     * @return the passenger's destination
     */
    public Stop getDestination() {
        return this.destination;
    }

    /**
     * Creates a string representation of the passenger in the format:
     * 'Passenger named {name}' without surrounding quotes and with {name}
     * replaced by the name of the passenger instance. For example:
     * Passenger named Agatha
     * If the passenger's name is empty, the method should instead return the
     * following:
     * Anonymous passenger
     * @return A string representation of the passenger.
     */
    @Override
    public String toString() {
        // If a name is not provided
        if (name.equals("")) {
            return "Anonymous passenger";
        }
        return "Passenger named " + this.name;
    }

}