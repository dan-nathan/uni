package network;

import exceptions.NoNameException;
import exceptions.TransportFormatException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import routes.*;
import stops.Stop;
import vehicles.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class NetworkTest {

    private Network network1;

    @Before
    public void setUp() throws Exception {
        network1 = new Network();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void addGetRoutes() {
        BusRoute busRoute1 = new BusRoute("Bus Route 1", 1);
        BusRoute busRoute2 = new BusRoute("Bus Route 2", 2);
        BusRoute busRoute3 = new BusRoute("Bus Route 3", 3);
        BusRoute busRoute4 = null;
        List<Route> testList = new ArrayList<>();

        // Check when stop added is null
        assertEquals(network1.getRoutes(), testList);
        network1.addRoute(null);
        network1.addRoute(busRoute4);
        assertEquals(network1.getRoutes().size(), 0);
        assertEquals(network1.getRoutes(), testList);

        // Check a for valid stop
        network1.addRoute(busRoute1);
        testList.add(busRoute1);
        assertEquals(network1.getRoutes(), testList);
        assertEquals(network1.getRoutes().size(), 1);

        // Check for a 2nd value stop
        network1.addRoute(busRoute2);
        testList.add(busRoute2);
        assertEquals(network1.getRoutes(), testList);
        assertEquals(network1.getRoutes().size(), 2);

        // Test that duplicates can be added
        network1.addRoute(busRoute2);
        testList.add(busRoute2);
        assertEquals(network1.getRoutes(), testList);
        assertEquals(network1.getRoutes().size(), 3);

        // Check ordering
        testList.remove(busRoute1);
        testList.add(busRoute1);
        // testList has now gone from [stop1, stop2, stop2]
        // to [stop2, stop2, stop1]
        assertNotEquals(testList, network1.getRoutes());

        // Check that the internal state of the class cannot be modified using
        // getStops
        int previousSize = network1.getRoutes().size();
        network1.getRoutes().add(busRoute3);
        assertEquals(network1.getRoutes().size(), previousSize);
    }

    @Test
    public void addGetStops() {
        Stop stop1 = new Stop("Stop 1", 0, 0);
        Stop stop2 = new Stop("Stop 2", 1, 3);
        Stop stop3 = new Stop("Stop 3", 4, 5);
        Stop stop4 = null;
        List<Stop> testList = new ArrayList<>();

        // Check when stop added is null
        assertEquals(network1.getStops(), testList);
        network1.addStop(null);
        network1.addStop(stop4);
        assertEquals(network1.getStops().size(), 0);
        assertEquals(network1.getStops(), testList);

        // Check a for valid stop
        network1.addStop(stop1);
        testList.add(stop1);
        assertEquals(network1.getStops(), testList);
        assertEquals(network1.getStops().size(), 1);

        // Check for a 2nd value stop
        network1.addStop(stop2);
        testList.add(stop2);
        assertEquals(network1.getStops(), testList);
        assertEquals(network1.getStops().size(), 2);

        // Test that duplicates can be added
        network1.addStop(stop2);
        testList.add(stop2);
        assertEquals(network1.getStops(), testList);
        assertEquals(network1.getStops().size(), 3);

        // Check ordering
        testList.remove(stop1);
        testList.add(stop1);
        // testList has now gone from [stop1, stop2, stop2]
        // to [stop2, stop2, stop1]
        assertNotEquals(testList, network1.getStops());

        // Check that the internal state of the class cannot be modified using
        // getStops
        int previousSize = network1.getStops().size();
        network1.getStops().add(stop3);
        assertEquals(network1.getStops().size(), previousSize);

        // Testing for addStops()

        network1 = new Network();
        testList = new ArrayList<>();

        // Test when adding null
        network1.addStops(testList);
        testList.add(stop4);
        network1.addStops(testList);
        testList.add(stop1);
        network1.addStops(testList);
        testList.remove(stop4);
        testList.add(stop4);
        testList.add(stop2);
        network1.addStops(testList);
        testList = new ArrayList<>();
        assertEquals(network1.getStops().size(), 0);
        assertEquals(network1.getStops(), testList);

        // Test when adding one valid element
        testList.add(stop1);
        network1.addStops(testList);
        assertEquals(network1.getStops().size(), 1);
        assertEquals(network1.getStops(), testList);

        // Test when adding multiple valid elements
        testList.remove(stop1);
        testList.add(stop2);
        testList.add(stop3);
        network1.addStops(testList);
        assertEquals(network1.getStops().size(), 3);
        assertEquals(network1.getStops().get(0), stop1);
        assertEquals(network1.getStops().get(1), stop2);
        assertEquals(network1.getStops().get(2), stop3);

    }

    @Test
    public void addGetVehicles() {
        BusRoute busRoute1 = new BusRoute("Bus Route 1", 1);
        FerryRoute ferryRoute1 = new FerryRoute("Ferry Route 1", 2);
        TrainRoute trainRoute1 = new TrainRoute("Train Route 1", 3);
        Bus bus1 = new Bus(1, 30, busRoute1, "ABC123");
        Bus bus2 = new Bus(2, 30, busRoute1, "ZYX987");
        Train train1 = new Train(3, 300, trainRoute1, 5);
        Ferry ferry1 = new Ferry(4, 100, trainRoute1, "City Cat");
        Bus bus3 = null;
        List<PublicTransport> testList = new ArrayList<>();

        // Check when stop added is null
        assertEquals(network1.getVehicles(), testList);
        network1.addVehicle(null);
        network1.addVehicle(bus3);
        assertEquals(network1.getVehicles().size(), 0);
        assertEquals(network1.getVehicles(), testList);

        // Check a for valid stop
        network1.addVehicle(bus1);
        testList.add(bus1);
        assertEquals(network1.getVehicles(), testList);
        assertEquals(network1.getVehicles().size(), 1);

        // Check for a 2nd value stop
        network1.addVehicle(bus2);
        testList.add(bus2);
        assertEquals(network1.getVehicles(), testList);
        assertEquals(network1.getVehicles().size(), 2);

        // Test that duplicates can be added
        network1.addVehicle(bus2);
        testList.add(bus2);
        assertEquals(network1.getVehicles(), testList);
        assertEquals(network1.getVehicles().size(), 3);

        // Test other vehicle types
        network1.addVehicle(train1);
        network1.addVehicle(ferry1);
        testList.add(train1);
        testList.add(ferry1);
        assertEquals(network1.getVehicles(), testList);
        assertEquals(network1.getVehicles().size(), 5);

        // Check ordering
        testList.remove(bus1);
        testList.add(bus1);
        // testList has now gone from [stop1, stop2, stop2, train1, ferry1]
        // to [stop2, stop2, train1, ferry1, stop1]
        assertNotEquals(testList, network1.getVehicles());

        // Check that the internal state of the class cannot be modified using
        // getStops
        int previousSize = network1.getVehicles().size();
        network1.getVehicles().add(train1);
        assertEquals(network1.getVehicles().size(), previousSize);
    }

    /*
     * Returns if two vehicles are the same, only considering information that
     * is encoded. A private method is written here as the equals method is not
     * overwritten in PublicTransport
     */
    private boolean vehicleEquals(PublicTransport vehicle1,
                                  PublicTransport vehicle2) {

        return vehicle1.getType().equals(vehicle2.getType())
                && vehicle1.getId() == vehicle2.getId()
                && vehicle1.getCapacity() == vehicle2.getCapacity()
                && vehicle1.getRoute().equals(vehicle2.getRoute());
    }

    /*
     * Same as vehicleEquals, but with a bus's registration number
     */
    private boolean busEquals(Bus bus1, Bus bus2) {
        return vehicleEquals(bus1, bus2)
                && bus1.getRegistrationNumber()
                .equals(bus2.getRegistrationNumber());
    }

    /*
     * Same as vehicleEquals, but with a ferry's ferry type
     */
    private boolean ferryEquals(Ferry ferry1, Ferry ferry2) {
        return vehicleEquals(ferry1, ferry2)
                && ferry1.getFerryType().equals(ferry2.getFerryType());
    }

    /*
     * Same as vehicleEquals, but with a trains's carriage count
     */
    private boolean trainEquals(Train train1, Train train2) {
        return vehicleEquals(train1, train2)
                && train1.getCarriageCount() == train2.getCarriageCount();
    }

    /*
     * Creates a file (to be read from)
     */
    private void createFile(String[] lines, String filename)
            throws IOException {

        BufferedWriter file = new BufferedWriter(new FileWriter(filename));
        for (String line : lines) {
            file.write(line);
            file.newLine();
        }
        file.close();
    }

    /*
     * Creates a file of valid data, with one modified (presumably incorrect)
     * line
     */
    private void createIncorrectFile(int lineNumber, String line,
            String filename) throws IOException {
        String[] lines = {"4",
                "Stop 1:0:0",
                "Stop 2:1:3",
                "Stop 3:4:5",
                "Stop 4:-2:-6",
                "4",
                "bus,Bus Route 1,1:Stop 1|Stop 2",
                "ferry,Ferry Route 1,2:Stop 1|Stop 3",
                "train,Train Route 1,3:Stop 1|Stop 2",
                "bus,Bus Route 2,4:Stop 1|Stop 2|Stop 3",
                "4",
                "bus,1,30,1,ABC123",
                "ferry,2,100,2,City Cat",
                "train,3,250,3,5",
                "bus,4,25,1,ZYX987"};
        try {
            lines[lineNumber - 1] = line;
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        }
        createFile(lines, filename);
    }

    /*
     * Load a file, and fail if a TransportFormatException is not thrown
     */
    private void testThrowTFE() throws IOException {
        Network network;
        try {
            network = new Network("testFile");
            fail();
        } catch (TransportFormatException e) {
            // Squash
        }
    }

    /*
     * Load a file, and fail if a TransportFormatException is thrown
     */
    private void testNoTFE() throws IOException {
        Network network;
        try {
            network = new Network("testFile");
        } catch (TransportFormatException e) {
            fail();
        }
    }

    @Test
    public void saveLoadValid1() {
        BusRoute busRoute1 = new BusRoute("Bus Route 1", 1);
        BusRoute busRoute2 = new BusRoute("Bus Route 2", 2);
        BusRoute busRoute3 = new BusRoute("Bus Route 3", 3);
        FerryRoute ferryRoute1 = new FerryRoute("Ferry Route 1", 4);
        TrainRoute trainRoute1 = new TrainRoute("Train Route 1", 5);
        Stop stop1 = new Stop("Stop 1", 0, 0);
        Stop stop2 = new Stop("Stop 2", 1, 3);
        Stop stop3 = new Stop("Stop 3", 4, 5);

        busRoute1.addStop(stop1);
        busRoute1.addStop(stop2);
        busRoute2.addStop(stop3);
        ferryRoute1.addStop(stop1);
        trainRoute1.addStop(stop1);

        Bus bus1 = new Bus(1, 30, busRoute1, "ABC123");
        Bus bus2 = new Bus(2, 30, busRoute2, "ZYX987");
        Bus bus3 = new Bus(3, 30, busRoute2, "AAA111");
        Ferry ferry1 = new Ferry(4, 100, ferryRoute1, "City Cat");
        Train train1 = new Train(5, 250, trainRoute1, 5);

        // Check that with valid inputs, no exceptions are thrown and the data
        // is correctly preserved
        network1.addRoute(busRoute1);
        network1.addRoute(busRoute2);
        network1.addRoute(ferryRoute1);
        network1.addRoute(trainRoute1);
        network1.addStop(stop1);
        network1.addStop(stop2);
        network1.addStop(stop3);
        network1.addVehicle(bus1);
        network1.addVehicle(ferry1);
        network1.addVehicle(bus2);
        network1.addVehicle(train1);
        network1.addVehicle(bus3);

        // Check there are no exceptions when writing
        try {
            network1.save("testFile");
        } catch (IOException e) {
            fail();
        }

        Network network2 = null;
        try {
            network2 = new Network("testFile");
        } catch (IOException | TransportFormatException e) {
            // Check there are no exceptions when reading
            fail();
        }

        // Check data is preserved and in correct order
        assertEquals(network2.getStops().get(0), stop1);
        assertEquals(network2.getStops().get(1), stop2);
        assertEquals(network2.getStops().get(2), stop3);
        assertEquals(network2.getStops().size(), 3);
        assertEquals(network2.getRoutes().get(0), busRoute1);
        assertEquals(network2.getRoutes().get(1), busRoute2);
        assertEquals(network2.getRoutes().size(), 4);
        // If vehicles are not entered into the list in the right order, the
        // vehicle types may not match the test, meaning they may throw a
        // ClassCastException. If this happens the solution has failed
        try {
            assertTrue(busEquals(((Bus) network2.getVehicles().get(0))
                    , bus1));
            assertTrue(ferryEquals(((Ferry) network2.getVehicles().get(1))
                    , ferry1));
            assertTrue(busEquals(((Bus) network2.getVehicles().get(2))
                    , bus2));
            assertTrue(trainEquals(((Train) network2.getVehicles().get(3))
                    , train1));
            assertTrue(busEquals(((Bus) network2.getVehicles().get(4))
                    , bus3));
        } catch (ClassCastException e) {
            fail();
        }
        assertEquals(network2.getVehicles().size(), 5);
    }

    @Test
    public void saveLoadValid2() {
        // test with nothing added
        try {
            network1.save("testFile");
        } catch (IOException e) {
            fail();
        }

        Network network2 = null;
        try {
            network2 = new Network("testFile");
        } catch (IOException | TransportFormatException e) {
            // Check there are no exceptions when reading
            fail();
        }
        assertEquals(network2.getVehicles().size(), 0);
        assertEquals(network2.getStops().size(), 0);
        assertEquals(network2.getRoutes().size(), 0);
    }

    @Test
    public void saveLoadInvalidCountLines() {
        // Try load with non-integer
        try {
            // Stop, Route, and Bus count lines for the default test file
            int[] countLines = {1, 6, 11};

            for (int lineNumber : countLines) {

                // Try the count line being not an integer
                createIncorrectFile(lineNumber, "Not an integer", "testFile");
                testThrowTFE();

                // Try integer then a string
                createIncorrectFile(lineNumber, "4 integer then not an integer"
                        , "testFile");
                testThrowTFE();

                // Try the count line being two integers
                createIncorrectFile(lineNumber, "4 1", "testFile");
                testThrowTFE();

                // Try with too many lines
                createIncorrectFile(lineNumber, "5", "testFile");
                testThrowTFE();

                // Try with not enough lines
                createIncorrectFile(lineNumber, "3", "testFile");
                testThrowTFE();

                // Try integer with spaces
                // (This one should not throw an exception)
                createIncorrectFile(lineNumber, "  4    ", "testFile");
                testNoTFE();

            }

        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void saveLoadInvalidStop() {
        // Try load with non-integer
        try {

            // Stop 4 is modified as it is not used by any of the routes,
            // meaning the exception will necessarily be caused by an
            // incorrectly formatted Stop
            // Test with missing field
            createIncorrectFile(5, "Stop 4:4", "testFile");
            testThrowTFE();

            // Test with non integer x coordinate
            createIncorrectFile(5, "Stop 4:1.3:4", "testFile");
            testThrowTFE();

            // Test with non-numeric x coordinate
            createIncorrectFile(5, "Stop 4:one:4", "testFile");
            testThrowTFE();

            // Test with non integer y coordinate
            createIncorrectFile(5, "Stop 4:1:1.3", "testFile");
            testThrowTFE();

            // Test with non-numeric y coordinate
            createIncorrectFile(5, "Stop 4:1:one", "testFile");
            testThrowTFE();

            // Test with extra colon
            createIncorrectFile(5, "Stop 4:1:1:", "testFile");
            testThrowTFE();

            // Test with extra colon and data
            createIncorrectFile(5, "Stop 4:1:1:String", "testFile");
            testThrowTFE();

            // Test with an empty x-coordinate
            createIncorrectFile(5, "Stop 4::1", "testFile");
            testThrowTFE();

            // Test with an empty y-coordinate
            createIncorrectFile(5, "Stop 4:1:", "testFile");
            testThrowTFE();

            // Test with spaces in integer fields
            createIncorrectFile(5, "Stop 4:  1   :     3 ", "testFile");
            testNoTFE();

            // Test with an empty name
            createIncorrectFile(5, ":1:1", "testFile");
            try {
                Network network2 = new Network("testFile");
                fail();
            } catch (NoNameException e) {
                // Squash
            } catch (TransportFormatException e) {
                fail();
            }

        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void saveLoadInvalidRoute() {
        // Try load with non-integer
        try {

            // Route 4 is modified as it is not used by any of the routes,
            // meaning the exception will necessarily be caused by an
            // incorrectly formatted Stop
            // Test with invalid type
            createIncorrectFile(10, "taxi,Bus Route 2,4:Stop 1|Stop 2|Stop 3"
                    , "testFile");
            testThrowTFE();

            // Test with invalid stop
            createIncorrectFile(10, "bus,Bus Route 2,4:Stop 5", "testFile");
            testThrowTFE();
            createIncorrectFile(10,
                    "bus,Bus Route 2,4:Stop 1|Stop 3|Stop 5|Stop 2",
                    "testFile");
            testThrowTFE();

            // Test with non integer route number
            createIncorrectFile(10,
                    "bus,Bus Route 2,four:Stop 1|Stop 2|Stop 3",
                    "testFile");
            testThrowTFE();
            createIncorrectFile(10,
                    "bus,Bus Route 2,:Stop 1|Stop 2|Stop 3","testFile");
            testThrowTFE();

            // Test with integer route number with spaces
            createIncorrectFile(10,
                    "bus,Bus Route 2,    4  :Stop 1|Stop 2|Stop 3",
                    "testFile");
            testNoTFE();

            // Test with extra ,
            createIncorrectFile(10, "bus,Bus Route 2,4,:Stop 1|Stop 2|Stop 3",
                    "testFile");
            testThrowTFE();
            createIncorrectFile(10, "bus,Bus Route 2,4:Stop 1|St,op 2|Stop 3",
                    "testFile");
            testThrowTFE();
            createIncorrectFile(10, "bus,Bus Route 2,4:Stop 1|Stop 2|Stop 3,",
                    "testFile");
            testThrowTFE();

            // Test with extra :
            createIncorrectFile(10, "bus,Bus R:oute 2,4:Stop 1|Stop 2|Stop 3",
                    "testFile");
            testThrowTFE();
            createIncorrectFile(10, "bus,Bus Route 2,4:Stop 1|Stop 2|Sto:p 3",
                    "testFile");
            testThrowTFE();
            createIncorrectFile(10, "bus,Bus Route 2,4:Stop 1|Stop 2|Stop 3:",
                    "testFile");
            testThrowTFE();

            // Test with extra |
            createIncorrectFile(10, "bus,Bus |Route 2,4:Stop 1|Stop 2|Stop 3",
                    "testFile");
            testThrowTFE();
            createIncorrectFile(10, "bus,Bus Route 2,4:Stop 1|Stop 2|Stop 3|",
                    "testFile");
            testThrowTFE();

            // Test with missing data
            createIncorrectFile(10, "Bus Route 2,4:Stop 1|Stop 2|Stop 3",
                    "testFile");
            testThrowTFE();
            createIncorrectFile(10, "bus,4:Stop 1|Stop 2|Stop 3", "testFile");
            testThrowTFE();
            createIncorrectFile(10, "bus,Bus Route 2:Stop 1|Stop 2|Stop 3",
                    "testFile");
            testThrowTFE();
            createIncorrectFile(10, "bus,Bus Route 2,4", "testFile");
            testThrowTFE();
            createIncorrectFile(10, "bus,Bus Route 2,4:Stop 1||Stop 2",
                    "testFile");
            testThrowTFE();

            // Test with no stops
            createIncorrectFile(10, "bus,Bus Route 2,4:", "testFile");
            testNoTFE();

            // Test with no name
            createIncorrectFile(10, "bus,,4:Stop 1|Stop 2|Stop 3", "testFile");
            testNoTFE();

        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void saveLoadInvalidVehicle() {
        // Try load with non-integer
        try {

            // Test with invalid type
            createIncorrectFile(15, "taxi,4,25,1,ZYX987", "testFile");
            testThrowTFE();

            // Test with non integer fields
            createIncorrectFile(15, "bus,four,25,1,ZYX987", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "bus,4,twentyfive,1,ZYX987", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "bus,4,25,one,ZYX987", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "bus,,25,1,ZYX987", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "bus,4,,1,ZYX987", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "bus,4,25,,ZYX987", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "train,4,25,1,five", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "train,4,25,1,", "testFile");
            testThrowTFE();

            // Test with spaces in integer fields
            createIncorrectFile(15, "bus,   4  ,25 ,  1,ZYX987", "testFile");
            testNoTFE();
            createIncorrectFile(15, "train,   4  ,25 ,  3,   5   ",
                    "testFile");
            testNoTFE();

            // Test with invalid Route number
            createIncorrectFile(15, "bus,4,25,5,ZYX987", "testFile");
            testThrowTFE();

            // Test with wrong type of Route
            createIncorrectFile(15, "bus,4,25,2,ZYX987", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "bus,4,25,3,ZYX987", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "ferry,4,25,1,City Cat", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "ferry,4,25,3,City Cat", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "train,4,25,1,5", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "train,4,25,2,5", "testFile");
            testThrowTFE();

            // Test for empty route exception (here bus1 & bus4 are added to an)
            // empty route
            createIncorrectFile(7, "bus,Bus Route 1,1:", "testFile");
            testThrowTFE();

            // Test for extra ,
            createIncorrectFile(15, "bus,4,25,1,ZYX987,", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "bus,4,25,1,ZYX9,87", "testFile");
            testThrowTFE();
            createIncorrectFile(15, ",bus,4,25,1,ZYX987", "testFile");
            testThrowTFE();

            // Test for missing data
            createIncorrectFile(15, "bus,4,25,1", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "bus,4,25,ZYX987", "testFile");
            testThrowTFE();
            createIncorrectFile(15, "4,25,1,ZYX987", "testFile");
            testThrowTFE();

            // Test for empty registration number/ferry type (shouldn't throw)
            // Exception
            createIncorrectFile(15, "bus,4,25,1,", "testFile");
            testNoTFE();
            createIncorrectFile(15, "ferry,4,25,2,", "testFile");
            testNoTFE();

        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void saveLoadExtraLines() {
        try {

            // Test with single blank line
            String[] file = {"1",
                    "Stop 1:1:1",
                    "1",
                    "bus,Bus Route 1,1:Stop 1",
                    "1",
                    "bus,1,30,1,ABC123",
                    ""};
            createFile(file, "testFile");
            testNoTFE();


            // Test with non empty final line
            file[6] = " ";
            createFile(file, "testFile");
            testThrowTFE();

            // Test with multiple blank lines
            String[] file2 = {"1",
                    "Stop 1:1:1",
                    "1",
                    "bus,Bus Route 1,1:Stop 1",
                    "1",
                    "bus,1,30,1,ABC123",
                    "",
                    ""};
            createFile(file2, "testFile");
            testThrowTFE();

            // Test with blank line in info
            String[] file3 = {"1",
                    "Stop 1:1:1",
                    "1",
                    "bus,Bus Route 1,1:Stop 1",
                    "",
                    "1",
                    "bus,1,30,1,ABC123",
                    ""};
            createFile(file3, "testFile");
            testThrowTFE();

            // Test with missing list of info
            String[] file4 = {"1",
                    "Stop 1:1:1",
                    "1",
                    "bus,Bus Route 1,1:Stop 1",
                    "1",
                    ""};
            createFile(file4, "testFile");
            testThrowTFE();

        } catch (IOException e) {
            fail();
        }
    }

}