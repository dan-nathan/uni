package vehicles;

import routes.Route;

/**
 * Represents a bus in the transportation network.
 */
public class Bus extends PublicTransport {

    // The registration number of the bus
    private String registrationNumber;

    /**
     * Creates a new Bus object with the given id, capacity, route, and
     * registration number.
     * Should meet the specification of
     * PublicTransport.PublicTransport(int, int, Route), as well as extending it
     * to include the following:
     * If the given registration number is null, an empty string should be
     * stored instead. If the registration number contains any newline
     * characters ('\n') or carriage returns ('\r'), they should be removed from
     * the string before it is stored.
     * @param id The identifying number of the bus.
     * @param capacity The maximum capacity of the bus.
     * @param route The route this bus is on.
     * @param registrationNumber The registration number of the bus.
     */
    public Bus(int id, int capacity, Route route, String registrationNumber) {
        super(id, capacity, route);
        if (registrationNumber == null) {
            this.registrationNumber = "";
        } else {
            // Removes \n  characters by replacing all instances of them with an
            // empty string, then does the same for \r
            this.registrationNumber = registrationNumber.replace("\n", "")
                    .replace("\r", "");
        }
    }

    /**
     * Returns the registration number of this bus.
     * @return The registration number of the bus.
     */
    public String getRegistrationNumber() {
        return this.registrationNumber;
    }
}
