package network;

import exceptions.TransportFormatException;
import routes.Route;
import stops.Stop;
import vehicles.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the transportation network, and manages all of the various
 * components therein.
 */
public class Network extends Object {

    private List<Route> networkRoutes;
    private List<Stop> networkStops;
    private List<PublicTransport> networkVehicles;

    /**
     * Creates a new empty Network with no stops, vehicles, or routes.
     */
    public Network() {
        this.networkRoutes = new ArrayList<>();
        this.networkStops = new ArrayList<>();
        this.networkVehicles = new ArrayList<>();
    }

    /*
     * Helper method for Network(filename). Reads a number of lines, then reads
     * those lines, decoding them according to type and adding the to the
     * member list variables
     */
    private void readInfo (BufferedReader file, String type)
            throws TransportFormatException, IOException {
        String line = file.readLine();
        int numRows;
        try {
            // Sets the number of lines to be checked depending on the
            // first line's value is after spaces are removed and it is
            // parsed as an integer
            numRows = Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            throw new TransportFormatException();
        }
        // For each row that is expected to be of the relevant class
        for (int i = 0; i < numRows; i++) {
            line = file.readLine();
            switch(type) {
                case "Stop":
                    // Decodes the stop string and adds to the list of stops
                    // If the line is in the wrong format, the Stop.decode
                    // method will throw a TransportFormatException
                    this.networkStops.add(Stop.decode(line));
                    break;
                case "Route":
                    this.networkRoutes.add(Route
                            .decode(line, this.networkStops));
                    break;
                case "PublicTransport":
                    this.networkVehicles.add(PublicTransport
                            .decode(line, this.networkRoutes));
                    break;
            }
        }
    }

    /**
     * Creates a new Network from information contained in the file indicated by
     * the given filename. The file should be in the following format:
     * {number_of_stops}
     * {stop0:x0:y0}
     * ...
     * {stopN:xN:yN}
     * {number_of_routes}
     * {type0,name0,number0:stop0|stop1|...|stopM}
     * ...
     * {typeN,nameN,numberN:stop0|stop1|...|stopM}
     * {number_of_vehicles}
     * {type0,id0,capacity0,routeNumber,extra}
     * ...
     * {typeN,idN,capacityN,routeNumber,extra}
     * where {number_of_stops}, {number_of_routes}, and {number_of_vehicles} are
     * the number of stops, routes, and vehicles (respectively) in the network,
     * and where {stop0,x0,y0} is the encode() representation of a Stop,
     * {type0,name0,number0:stop0|stop1|...|stopM} is the encode()
     * representation of a Route, and {typeN,idN,capacityN,routeNumber,extra} is
     * the encode() representation of a PublicTransport.
     * Whilst parsing, if spaces (i.e. ' ') are encountered before or after
     * integers, (i.e. {number_of_stops}, {number_of_routes}, or
     * {number_of_vehicles}), the spaces should simply be trimmed
     * (for example, using something like String.trim()).
     *
     * For example:
     * 4
     * stop0:0:1
     * stop1:-1:0
     * stop2:4:2
     * stop3:2:-8
     * 2
     * train,red,1:stop0|stop2|stop1
     * bus,blue,2:stop1|stop3|stop0
     * 3
     * train,123,30,1,2
     * train,42,60,1,3
     * bus,412,20,2,ABC123
     *
     * The Network object created should have the stops, routes, and vehicles
     * contained in the given file.
     * If the given filename is null, an empty network (as defined by Network()
     * should be created instead.
     *
     * @param filename The name of the file to load the network from.
     * @throws IOException If any IO exceptions occur whilst trying to read from
     *         the file, or if the filename is null.
     * @throws TransportFormatException
     *         1. If any of the lines representing stops, routes, or vehicles
     *            are incorrectly formatted according to their respective decode
     *            methods (i.e. if their decode method throws an exception).
     *         2. If any of the integers are incorrectly formatted
     *            (i.e. cannot be parsed).
     *         3. If the {number_of_stops} does not match the actual number of
     *            lines representing stops present. This also applies to
     *            {number_of_routes} and {number_of_vehicles}.
     *         4. If there are any extra lines present in the file
     *            (the file may end with a single newline character, but there
     *            may not be multiple blank lines at the end of the file).
     *         5. If any other formatting issues are encountered whilst parsing
     *            the file.
     */
    public Network(String filename) throws IOException,
            TransportFormatException {
        // Creates the lists as specified in the constructor without parameters
        this();
        if (filename == null) {
            throw new IOException();
        }
        // Opens the file in read mode
        BufferedReader file = new BufferedReader(new FileReader(filename));

        // Read the number of lines, then decode that many lines and add to the
        // list. Repeats the same code for stops, then routes, then vehicles
        // using a for loop
        String[] order = {"Stop", "Route", "PublicTransport"};
        for (int i = 0; i < 3; i++) {
            readInfo(file, order[i]);
        }

        String finalLine = file.readLine();
        // If there are still additional lines after the expected ones
        if (finalLine != null) {
            if (finalLine.equals("")) {
                // If there is another line after the final blank line
                if (file.readLine() != null) {
                    throw new TransportFormatException();
                }
            } else {
                // If there is an additional line that isn't blank
                throw new TransportFormatException();
            }
        }
        // Closes the buffered reader
        file.close();
    }

    /**
     * Adds the given route to the network.
     * If the given route is null, it should not be added to the network.
     * @param route The route to add to the network.
     */
    public void addRoute(Route route) {
        if (route != null) {
            this.networkRoutes.add(route);
        }
    }

    /**
     * Adds the given stop to the transportation network.
     * If the given stop is null, it should not be added to the network.
     * @param stop The stop to add to the network.
     */
    public void addStop(Stop stop) {
        if (stop != null) {
            this.networkStops.add(stop);
        }
    }

    /**
     * Adds multiple stops to the transport network.
     * If any of the stops in the given list are null, none of them should be
     * added (i.e. either all of the stops are added, or none are).
     * @param stops The stops to add to the network.
     */
    public void addStops(List<Stop> stops) {
        boolean foundNull = false;
        // First loop through all stops to see if any are null
        for (Stop stop : stops) {
            if (stop == null) {
                foundNull = true;
                break;
            }
        }
        // If none of the stops are null, loop through again and add them
        if (!foundNull) {
            for (Stop stop : stops) {
                this.addStop(stop);
            }
        }
    }

    /**
     * Adds the given vehicle to the network.
     * If the given vehicle is null, it should not be added to the network.
     * @param vehicle The vehicle to add to the network.
     */
    public void addVehicle(PublicTransport vehicle) {
        if (vehicle != null) {
            this.networkVehicles.add(vehicle);
        }
    }

    /**
     * Gets all the routes in this network.
     * Modifying the returned list should not result in changes to the internal
     * state of the class.
     * @return All the routes in the network.
     */
    public List<Route> getRoutes() {
        // Returns a new list so that modifying the returned list doesn't
        // impact the list with the class
        return new ArrayList<>(this.networkRoutes);
    }

    /**
     * Gets all of the stops in this network.
     * Modifying the returned list should not result in changes to the internal
     * state of the class.
     * @return All the stops in the network.
     */
    public List<Stop> getStops() {
        return new ArrayList<>(this.networkStops);
    }

    /**
     * Gets all of the vehicles in this network.
     * Modifying the returned list should not result in changes to the internal
     * state of the class.
     * @return All the vehicles in the transportation network.
     */
    public List<PublicTransport> getVehicles() {
        return new ArrayList<>(this.networkVehicles);
    }

    /**
     * Saves this network to the file indicated by the given filename.
     * The file should be written with the same format as described in the
     * Network(String) constructor.
     * The stops should be written to the file in the same order in which they
     * were added to the network. This also applies to the routes and the
     * vehicles.
     * @param filename The name of the file to save the network to.
     * @throws IOException If there are any IO errors whilst writing to the
     *         file.
     */
    public void save(String filename) throws IOException {
        if (filename == null) {
            throw new IOException();
        }
        // Opens the file in write mode
        BufferedWriter file = new BufferedWriter(new FileWriter(filename));

        int numRows;
        // Gets the number of stops, converts it to an string then writes it to
        // to the file
        numRows = this.networkStops.size();
        file.write(String.valueOf(numRows));
        file.newLine();

        // Loops through the stops, encoding and writing to the file
        for (Stop stop : this.networkStops) {
            file.write(stop.encode());
            file.newLine();
        }

        // Repeat this process for routes
        numRows = this.networkRoutes.size();
        file.write(String.valueOf(numRows));
        file.newLine();
        for (Route route : this.networkRoutes) {
            file.write(route.encode());
            file.newLine();
        }

        // Repeat for vehicles
        numRows = this.networkVehicles.size();
        file.write(String.valueOf(numRows));
        file.newLine();
        for (PublicTransport vehicle : this.networkVehicles) {
            file.write(vehicle.encode());
            file.newLine();
        }

        file.close();
    }
}
