package vehicles;

import routes.Route;

public class Train extends PublicTransport {

    // The number of carriages the train has
    private int carriageCount;

    /**
     * Creates a new Train object with the given id, capacity, route, and
     * carriage count.
     * Should meet the specification of
     * PublicTransport.PublicTransport(int, int, Route), as well as extending it
     * to include the following:
     * If the given carriage count is less than or equal to zero, then 1 should
     * be stored instead.
     * @param id The identifying number of the train.
     * @param capacity The maximum capacity of the train.
     * @param route The route this train is on.
     * @param carriageCount The number of carriages this train has.
     */
    public Train(int id, int capacity, Route route, int carriageCount) {
        super(id, capacity, route);
        if (carriageCount <= 0) {
            this.carriageCount = 1;
        } else {
            this.carriageCount = carriageCount;
        }
    }

    /**
     * Returns the number of carriages this train has.
     * @return The number of carriages the train has.
     */
    public int getCarriageCount() {
        return this.carriageCount;
    }
}
