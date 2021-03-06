package passengers;

import stops.Stop;

/**
 * A passenger that pays concession fares. Concession fares require a
 * concession id.
 * Should meet the specification of Passenger.Passenger(String, Stop).
 */
public class ConcessionPassenger extends Passenger {

    // The passenger's concession ID number
    private int concessionId;

    // Whether or not the passenger's concession status is valid
    private boolean valid;

    /**
     * Construct a new concession fare passenger with the given name and
     * concessionId.
     * @param name The name of the passenger.
     * @param destination The name of the passenger.
     * @param concessionId Identifying number of the passenger's concession
     *                     card.
     */
    public ConcessionPassenger(String name, Stop destination, int concessionId) {
        super(name, destination);
        this.concessionId = concessionId;
        // It will be assumed that when created, the concession status will not
        // be expired
        this.valid = true;
    }

    /**
     * Sets the concession fare to be expired, and thus invalid. isValid()
     * returns false.
     */
    public void expire() {
        this.valid = false;
    }

    /**
     * Attempts to renew this concession passenger's fares with the given id.
     * concession ID
     * @param newId The ID of the renewed concession card.
     */
    public void renew(int newId) {
        this.valid = true;
        this.concessionId = newId;
    }

    /**
     * Returns true if and only if the stored concessionId is valid.
     * @return True if concession fares have not expired (are valid), false
     *         otherwise.
     */
    public boolean isValid() {
        // Check if concessionID is too short or a negative number
        if (!this.valid || Integer.toString(concessionId).length() < 6
                || concessionId < 0) {
            return false;
        }
        final String LEADING_DIGITS = "42";

        // Converts to a string, then checks that the first n characters match
        // the required LEADING_DIGITS where n is the number of relevant digits
        if (Integer.toString(concessionId).substring(0, LEADING_DIGITS.length())
                .equals(LEADING_DIGITS)) {
            return true;
        }
        return false;
    }
}