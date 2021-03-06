package stops;

import exceptions.OverCapacityException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import exceptions.NoNameException;
import passengers.Passenger;
import routes.*;
import vehicles.Bus;

import static org.junit.Assert.*;

public class StopTest {
    private Stop stop1;
    private Stop stop2;
    private Stop stop3;

    @Before
    public void setUp() throws Exception {
        stop1 = new Stop("Stop 1", 0, 0);
        stop2 = new Stop("Stop 2", 1, 3);
        stop3 = new Stop("Stop 3", -2, -2);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getName() {

        // Fail test if unnamed stop does not throw a NoNameException
        try {
            Stop unnamed = new Stop(null, 0, 0);
            fail();
        } catch (NoNameException e) {
            // Squash exception
        }

        // Check when stop has name
        assertEquals(stop1.getName(),"Stop 1");

    }

    @Test
    public void getX() {

        // Check when x = 0
        assertEquals(stop1.getX(), 0);

        // Check when x is -ve
        assertEquals(stop2.getX(), 1);

        // Check when x is +ve
        assertEquals(stop3.getX(), -2);
    }

    @Test
    public void getY() {

        // Check when y = 0
        assertEquals(stop1.getY(), 0);

        // Check when y is -ve
        assertEquals(stop2.getY(), 3);

        // Check when y is +ve
        assertEquals(stop3.getY(), -2);
    }

    @Test
    public void addRoute() {

        BusRoute busRoute1 = new BusRoute("Bus Route 1", 1);
        BusRoute busRoute2 = new BusRoute("Bus Route 2", 2);

        // Check that no routes are added when the argument is null
        stop1.addRoute(null);
        assertEquals(stop1.getRoutes().size(), 0);

        // Check when valid routes are added
        stop1.addRoute(busRoute1);
        stop1.addRoute(busRoute2);
        assertEquals(stop1.getRoutes().get(0), busRoute1);
        assertEquals(stop1.getRoutes().get(1), busRoute2);
    }

    @Test
    public void getRoutes() {

        BusRoute busRoute1 = new BusRoute("Bus Route 1", 1);
        BusRoute busRoute2 = new BusRoute("Bus Route 2", 2);
        BusRoute busRoute3 = new BusRoute("Bus Route 3", 3);

        List<Route> testList = new ArrayList<>();

        // Check when routes list is empty
        assertTrue(stop1.getRoutes().equals(testList));

        // Check when there is 1 equal element in the list
        stop1.addRoute(busRoute1);
        testList.add(busRoute1);
        // Currently stop1.getRoutes() = [busRoute1], testList = [busRoute1]
        assertTrue(stop1.getRoutes().equals(testList));

        // Check when there is 1 non equal element in the list
        testList.remove(busRoute1);
        testList.add(busRoute2);
        // Currently stop1.getRoutes() = [busRoute1], testList = [busRoute2]
        assertFalse(stop1.getRoutes().equals(testList));

        // Check when there are 2 equal elements in the wrong order
        stop1.addRoute(busRoute2);
        testList.add(busRoute1);
        // Currently stop1.getRoutes() = [busRoute1, busRoute2],
        // testList = [busRoute2, busRoute1]
        assertFalse(stop1.getRoutes().equals(testList));

        // Check when there are 2 elements in the right order
        testList.remove(busRoute2);
        testList.add(busRoute2);
        // Currently stop1.getRoutes() = [busRoute1, busRoute2],
        // testList = [busRoute1, busRoute2]
        assertTrue(stop1.getRoutes().equals(testList));

        // Check when lists are different length
        testList.add(busRoute3);
        // Currently stop1.getRoutes() = [busRoute1, busRoute2],
        // testList = [busRoute1, busRoute2, busRoute3]
        assertFalse(stop1.getRoutes().equals(testList));

        // Check when elements are different
        testList.remove(busRoute2);
        // Currently stop1.getRoutes() = [busRoute1, busRoute2],
        // testList = [busRoute1, busRoute3]
        assertFalse(stop1.getRoutes().equals(testList));

        // Check that the internal state of the class cannot be modified
        int previousSize = stop1.getRoutes().size();
        stop1.getRoutes().add(busRoute3);
        assertTrue(stop1.getRoutes().size() == previousSize);
    }

    @Test
    public void addNeighbouringStop() {

        // Check when stop added is null
        stop1.addNeighbouringStop(null);
        assertEquals(stop1.getNeighbours().size(), 0);

        // Check a for valid stop
        stop1.addNeighbouringStop(stop2);
        assertEquals(stop1.getNeighbours().get(0), stop2);

        // Check for a duplicate stop
        stop1.addNeighbouringStop(stop2);
        assertEquals(stop1.getNeighbours().size(), 1);

        // Check for a 2nd value stop
        stop1.addNeighbouringStop(stop3);
        assertEquals(stop1.getNeighbours().get(1), stop3);

        // Check for a duplicate with 2 stops
        stop1.addNeighbouringStop(stop3);
        assertEquals(stop1.getNeighbours().size(), 2);
    }

    @Test
    public void getNeighbours() {

        List<Stop> testList = new ArrayList<>();

        // Check stop with no neighbours
        assertEquals(stop1.getNeighbours(), testList);

        // Check with 1 neighbour
        stop1.addNeighbouringStop(stop2);
        testList.add(stop2);
        assertEquals(stop1.getNeighbours(), testList);

        // Check with 2 neighbours
        stop1.addNeighbouringStop(stop3);
        testList.add(stop3);
        assertEquals(stop1.getNeighbours(), testList);

        // Check that the internal state of the class cannot be modified
        int previousSize = stop1.getNeighbours().size();
        stop1.getNeighbours().add(stop1);
        assertTrue(stop1.getNeighbours().size() == previousSize);

    }

    @Test
    public void addPassenger() {

        Passenger passenger1 = new Passenger("John Doe", stop3);
        Passenger passenger2 = new Passenger("Jane Doe", stop3);

        // Check when passenger is null
        stop1.addPassenger(null);
        assertEquals(stop1.getWaitingPassengers().size(), 0);

        // Check when valid routes are added
        stop1.addPassenger(passenger1);
        stop1.addPassenger(passenger2);
        assertEquals(stop1.getWaitingPassengers().get(0), passenger1);
        assertEquals(stop1.getWaitingPassengers().get(1), passenger2);
    }

    @Test
    public void getWaitingPassengers() {

        Passenger passenger1 = new Passenger("John Doe", stop3);
        Passenger passenger2 = new Passenger("Jane Doe", stop3);
        Passenger passenger3 = new Passenger("John Doe Jr.", stop3);

        List<Passenger> testList = new ArrayList<>();

        // Check when routes list is empty
        assertTrue(stop1.getWaitingPassengers().equals(testList));

        // Check when there is 1 equal element in the list
        stop1.addPassenger(passenger1);
        testList.add(passenger1);
        // Currently stop1.getWaitingPassengers() = [passenger1],
        // testList = [passenger2]
        assertTrue(stop1.getWaitingPassengers().equals(testList));

        // Check when there is 1 non equal element in the list
        testList.remove(passenger1);
        testList.add(passenger2);
        // Currently stop1.getWaitingPassengers() = [passenger1],
        // testList = [passenger2]
        assertFalse(stop1.getWaitingPassengers().equals(testList));

        // Check when there are 2 equal elements in the wrong order
        stop1.addPassenger(passenger2);
        testList.add(passenger1);
        // Currently stop1.getWaitingPassengers() = [passenger1, passenger2],
        // testList = [passenger2, passenger1]
        assertFalse(stop1.getWaitingPassengers().equals(testList));

        // Check when there are 2 elements in the right order
        testList.remove(passenger2);
        testList.add(passenger2);
        // Currently stop1.getWaitingPassengers() = [passenger1, passenger2],
        // testList = [passenger1, passenger2]
        assertTrue(stop1.getWaitingPassengers().equals(testList));

        // Check when lists are different length
        testList.add(passenger3);
        // Currently stop1.getWaitingPassengers() = [passenger1, passenger2],
        // testList = [passenger1, passenger2, passenger3]
        assertFalse(stop1.getWaitingPassengers().equals(testList));

        // Check when elements are different
        testList.remove(passenger2);
        // Currently stop1.getWaitingPassengers() = [passenger1, passenger2],
        // testList = [passenger1, passenger3]
        assertFalse(stop1.getWaitingPassengers().equals(testList));

        // Check that the internal state of the class cannot be modified
        int previousSize = stop1.getWaitingPassengers().size();
        stop1.getWaitingPassengers().add(passenger3);
        assertTrue(stop1.getWaitingPassengers().size() == previousSize);
    }

    @Test
    public void isAtStop() {

        BusRoute busRoute1 = new BusRoute("Bus Route 1", 1);
        Bus vehicle1 = new Bus(1, 30, busRoute1, "ABC123");

        // Check when vehicle is not at stop
        assertFalse(stop1.isAtStop(vehicle1));

        // Check when vehicle is at stop
        stop1.transportArrive(vehicle1);
        assertTrue(stop1.isAtStop(vehicle1));

        // Check when vehicle has departed
        stop1.transportDepart(vehicle1, stop2);
        assertFalse(stop1.isAtStop(vehicle1));

    }

    @Test
    public void getVehicles() {

        BusRoute busRoute1 = new BusRoute("Bus Route 1", 1);
        Bus vehicle1 = new Bus(1, 30, busRoute1, "ABC123");
        Bus vehicle2 = new Bus(2, 30, busRoute1, "ABC124");
        Bus vehicle3 = new Bus(3, 30, busRoute1, "ABC125");

        List<Bus> testList = new ArrayList<>();

        // Check when routes list is empty
        assertTrue(stop1.getVehicles().equals(testList));

        // Check when there is 1 equal element in the list
        stop1.transportArrive(vehicle1);
        testList.add(vehicle1);
        // Currently stop1.getVehicles() = [vehicle1], testList = [vehicle1]
        assertTrue(stop1.getVehicles().equals(testList));

        // Check when there is 1 non equal element in the list
        testList.remove(vehicle1);
        testList.add(vehicle2);
        // Currently stop1.getVehicles() = [vehicle1], testList = [vehicle2]
        assertFalse(stop1.getVehicles().equals(testList));

        // Check when there are 2 equal elements in the wrong order
        stop1.transportArrive(vehicle2);
        testList.add(vehicle1);
        // Currently stop1.getVehicles() = [vehicle1, vehicle2],
        // testList = [vehicle2, vehicle1]
        assertFalse(stop1.getVehicles().equals(testList));

        // Check when there are 2 elements in the right order
        testList.remove(vehicle2);
        testList.add(vehicle2);
        // Currently stop1.getVehicles() = [vehicle1, vehicle2],
        // testList = [vehicle1, vehicle2]
        assertTrue(stop1.getVehicles().equals(testList));

        // Check when lists are different length
        testList.add(vehicle3);
        // Currently stop1.getVehicles() = [vehicle1, vehicle2],
        // testList = [vehicle1, vehicle2, vehicle3]
        assertFalse(stop1.getVehicles().equals(testList));

        // Check when elements are different
        testList.remove(vehicle2);
        // Currently stop1.getVehicles() = [vehicle1, vehicle2],
        // testList = [vehicle1, vehicle3]
        assertFalse(stop1.getVehicles().equals(testList));

        // Check that the internal state of the class cannot be modified
        int previousSize = stop1.getVehicles().size();
        stop1.getVehicles().add(vehicle3);
        assertTrue(stop1.getVehicles().size() == previousSize);
    }

    @Test
    public void transportArrive() {

        BusRoute busRoute1 = new BusRoute("Bus Route 1", 1);
        Bus vehicle1 = new Bus(1, 30, busRoute1, "ABC123");
        Bus vehicle2 = new Bus(2, 30, busRoute1, "ABC124");
        Passenger passenger1 = new Passenger("John Doe", stop3);
        Passenger passenger2 = new Passenger("Jane Doe", stop3);

        try {
            vehicle2.addPassenger(passenger1);
            vehicle2.addPassenger(passenger2);
        } catch (OverCapacityException e) {
            fail();
        }

        // Check when null vehicle is added
        stop1.transportArrive(null);
        assertEquals(stop1.getVehicles().size(), 0);

        // Check when valid vehicle arrives with no passengers
        stop1.transportArrive(vehicle1);
        assertEquals(stop1.getVehicles().get(0), vehicle1);
        assertEquals(stop1.getWaitingPassengers().size(), 0);

        // Check when valid vehicle arrives with passengers
        stop1.transportArrive(vehicle2);
        assertEquals(stop1.getVehicles().get(1), vehicle2);
        assertEquals(stop1.getWaitingPassengers().size(), 2);

        // Check when duplicate vehicle arrives
        stop1.transportArrive(vehicle1);
        assertEquals(stop1.getVehicles().size(), 2);
        assertEquals(stop1.getWaitingPassengers().size(), 2);

    }

    @Test
    public void transportDepart() {

        BusRoute busRoute1 = new BusRoute("Bus Route 1", 1);
        Bus vehicle1 = new Bus(1, 30, busRoute1, "ABC123");
        Bus vehicle2 = new Bus(2, 30, busRoute1, "ABC124");
        Bus vehicle3 = new Bus(3, 30, busRoute1, "ABC125");
        Passenger passenger1 = new Passenger("John Doe", stop3);
        Passenger passenger2 = new Passenger("Jane Doe", stop3);

        stop1.transportArrive(vehicle1);
        stop1.transportArrive(vehicle2);
        try {
            vehicle2.addPassenger(passenger1);
            vehicle2.addPassenger(passenger2);
            stop1.addPassenger(passenger1);
            stop1.addPassenger(passenger2);
        } catch (OverCapacityException e) {
            fail();
        }

        // Check when vehicle is null
        stop1.transportDepart(null, stop2);
        assertEquals(stop1.getVehicles().size(), 2);
        assertEquals(stop1.getWaitingPassengers().size(), 2);

        // Check when next stop is null
        stop1.transportDepart(vehicle1, null);
        assertEquals(stop1.getVehicles().size(), 2);
        assertEquals(stop1.getWaitingPassengers().size(), 2);

        // Check with invalid vehicle
        stop1.transportDepart(vehicle3, stop2);
        assertEquals(stop1.getVehicles().size(), 2);
        assertEquals(stop1.getWaitingPassengers().size(), 2);

        // Check with valid vehicle
        stop1.transportDepart(vehicle1, stop2);
        assertEquals(stop1.getVehicles().size(), 1);
        assertEquals(stop1.getVehicles().get(0), vehicle2);
        assertEquals(stop1.getWaitingPassengers().size(), 2);

        // Check with valid vehicle with passengers
        stop1.transportDepart(vehicle2, stop2);
        assertEquals(stop1.getVehicles().size(), 0);
        assertEquals(stop1.getWaitingPassengers().size(), 0);

    }

    @Test
    public void distanceTo() {

        // Check with a null stop
        assertEquals(stop1.distanceTo(null), -1);

        // Check a stop's distance to itself is 0
        assertEquals(stop1.distanceTo(stop1), 0);

        // Check two different stops
        assertEquals(stop1.distanceTo(stop2), 4);

        // Check with a stop with negative coordinates
        assertEquals(stop2.distanceTo(stop3), 8);

        // Check commutativity
        assertEquals(stop2.distanceTo(stop3), stop3.distanceTo(stop2));

    }

    @Test
    public void equals() {

        Stop testStop1 = new Stop("Stop 1", 0, 0);
        Stop testStop2 = new Stop("Stop 2", 0, 0);
        Stop testStop3 = new Stop("Stop 1", 1, 0);
        Stop testStop4 = new Stop("Stop 1", 0, 1);
        Stop testStop5 = new Stop("Stop 1", 0, 0);
        BusRoute busRoute1 = new BusRoute("Bus Route 1", 1);
        BusRoute busRoute2 = new BusRoute("Bus Route 2", 2);

        // Check two identical stops
        assertTrue(stop1.equals(testStop1));

        // Check two stops with different name, x or y
        assertFalse(stop1.equals(testStop2));
        assertFalse(stop1.equals(testStop3));
        assertFalse(stop1.equals(testStop4));

        // Check when routes are different
        testStop1.addRoute(busRoute1);
        testStop1.addRoute(busRoute2);
        assertFalse(stop1.equals(testStop1));

        // Check when routes are the same
        stop1.addRoute(busRoute1);
        stop1.addRoute(busRoute2);
        assertTrue(stop1.equals(testStop1));

        // Check when routes are the same but in a different order
        testStop5.addRoute(busRoute2);
        testStop5.addRoute(busRoute1);
        assertTrue(stop1.equals(testStop5));

        // Check with duplicate routes
        testStop5.addRoute(busRoute2);
        stop1.addRoute(busRoute1);
        assertTrue(stop1.equals(testStop5));
    }

    @Test
    public void hashCodeTest() {

        Stop testStop1 = new Stop("Stop 1", 0, 0);
        Stop testStop2 = new Stop("Stop 2", 0, 0);
        Stop testStop3 = new Stop("Stop 1", 1, 0);
        Stop testStop4 = new Stop("Stop 1", 0, 1);
        Stop testStop5 = new Stop("Stop 1", 0, 0);
        BusRoute busRoute1 = new BusRoute("Bus Route 1", 1);
        BusRoute busRoute2 = new BusRoute("Bus Route 2", 2);

        // Check two identical stops
        assertEquals(stop1.hashCode(), testStop1.hashCode());

        // Check two stops with different name, x or y
        assertNotEquals(stop1.hashCode(), testStop2.hashCode());
        assertNotEquals(stop1.hashCode(), testStop3.hashCode());
        assertNotEquals(stop1.hashCode(), testStop4.hashCode());

        // Check when routes are different
        testStop1.addRoute(busRoute1);
        testStop1.addRoute(busRoute2);
        assertNotEquals(stop1.hashCode(), testStop1.hashCode());

        // Check when routes are the same
        stop1.addRoute(busRoute1);
        stop1.addRoute(busRoute2);
        assertEquals(stop1.hashCode(), testStop1.hashCode());

        // Check when routes are the same but in a different order
        testStop5.addRoute(busRoute2);
        testStop5.addRoute(busRoute1);
        assertEquals(stop1.hashCode(), testStop1.hashCode());

        // Check with duplicate routes
        testStop5.addRoute(busRoute2);
        stop1.addRoute(busRoute1);
        assertEquals(stop1.hashCode(), testStop1.hashCode());

    }

    @Test
    public void toStringTest() {

        // Check with +ve coordinates
        assertEquals(stop2.toString(), "Stop 2:1:3");

        // Check with -ve coordinates
        assertEquals(stop3.toString(), "Stop 3:-2:-2");
    }
}