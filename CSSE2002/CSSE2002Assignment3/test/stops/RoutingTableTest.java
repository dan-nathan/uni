package stops;

import network.Network;

import static org.junit.Assert.*;

public class RoutingTableTest {

    private Network network;
    private Stop stop1;
    private Stop stop2;
    private Stop stop3;
    private Stop stop4;
    private Stop stop5;

    @org.junit.Before
    public void setUp() throws Exception {
        network = new Network();
        stop1 = new Stop("Stop 1", 0, 0);
        stop2 = new Stop("Stop 2", 1, 2);
        stop3 = new Stop("Stop 3", -1, 1);
        stop4 = new Stop("Stop 4", 0, 4);
        stop5 = new Stop("Stop 5", 2, 3);
    }

    @org.junit.After
    public void tearDown() throws Exception {
    }

    @org.junit.Test
    public void addNeighbour() {
        stop1.getRoutingTable().addNeighbour(stop2);
        stop1.getRoutingTable().addNeighbour(stop3);
        stop2.getRoutingTable().addNeighbour(stop4);
        stop2.getRoutingTable().addNeighbour(stop5);
        assertTrue(stop1.getRoutingTable().getCosts().containsKey(stop1));
        assertEquals(0, (long) stop1.getRoutingTable().getCosts().get(stop1));
        assertTrue(stop1.getRoutingTable().getCosts().containsKey(stop2));
        assertEquals(3, (long) stop1.getRoutingTable().getCosts().get(stop2));
        assertTrue(stop1.getRoutingTable().getCosts().containsKey(stop3));
        assertEquals(2, (long) stop1.getRoutingTable().getCosts().get(stop3));
        assertTrue(stop2.getRoutingTable().getCosts().containsKey(stop4));
        assertEquals(3, (long) stop2.getRoutingTable().getCosts().get(stop4));
        assertTrue(stop2.getRoutingTable().getCosts().containsKey(stop5));
        assertEquals(2, (long) stop2.getRoutingTable().getCosts().get(stop5));

        // Test that addNeighbour will override the cost if it is less
        // Actual distance is 4
        stop1.getRoutingTable().addOrUpdateEntry(stop4, 5, stop4);
        assertTrue(stop1.getRoutingTable().getCosts().containsKey(stop4));
        assertEquals(5, (long) stop1.getRoutingTable().getCosts().get(stop4));
        stop1.getRoutingTable().addNeighbour(stop4);
        assertEquals(4, (long) stop1.getRoutingTable().getCosts().get(stop4));

        // Actual distance is 5
        stop1.getRoutingTable().addOrUpdateEntry(stop5, 4, stop5);
        assertTrue(stop1.getRoutingTable().getCosts().containsKey(stop5));
        assertEquals(4, (long) stop1.getRoutingTable().getCosts().get(stop5));
        stop1.getRoutingTable().addNeighbour(stop5);
        assertEquals(4, (long) stop1.getRoutingTable().getCosts().get(stop5));
    }

    @org.junit.Test
    public void addOrUpdateEntry() {
        stop1.getRoutingTable().addNeighbour(stop2);
        assertTrue(stop1.getRoutingTable().addOrUpdateEntry(stop4, 6, stop2));
        assertEquals(6, (long) stop1.getRoutingTable().getCosts().get(stop4));
        assertEquals(stop2, stop1.getRoutingTable().nextStop(stop4));

        // Lower cost route
        stop1.getRoutingTable().addOrUpdateEntry(stop4, 4, stop4);
        assertEquals(4, (long) stop1.getRoutingTable().getCosts().get(stop4));
        assertEquals(stop4, stop1.getRoutingTable().nextStop(stop4));

        // Higher cost route
        assertFalse(stop1.getRoutingTable().addOrUpdateEntry(stop4, 6, stop2));
        assertEquals(4, (long) stop1.getRoutingTable().getCosts().get(stop4));
        assertEquals(stop4, stop1.getRoutingTable().nextStop(stop4));

        // Same cost route
        assertFalse(stop1.getRoutingTable().addOrUpdateEntry(stop4, 4, stop2));
        assertEquals(stop4, stop1.getRoutingTable().nextStop(stop4));
    }

    @org.junit.Test
    public void costTo() {
        stop1.getRoutingTable().addNeighbour(stop2);
        assertEquals(3, stop1.getRoutingTable().costTo(stop2));

        // Test stop that is not connected
        assertEquals(Integer.MAX_VALUE, stop1.getRoutingTable().costTo(stop3));
    }

    @org.junit.Test
    public void getStop() {
        RoutingTable routingTable = new RoutingTable(stop1);
        assertEquals(routingTable.getStop(), stop1);
        assertEquals(stop2, stop2.getRoutingTable().getStop());
    }

    @org.junit.Test
    public void nextStop() {
        stop1.getRoutingTable().addOrUpdateEntry(stop5, 5, stop2);
        stop1.getRoutingTable().addOrUpdateEntry(stop4, 6, stop3);
        stop1.getRoutingTable().addOrUpdateEntry(stop2, 3, stop2);

        assertEquals(stop2, stop1.getRoutingTable().nextStop(stop5));
        assertEquals(stop3, stop1.getRoutingTable().nextStop(stop4));
        assertEquals(stop2, stop1.getRoutingTable().nextStop(stop2));

        assertNull(stop1.getRoutingTable().nextStop(null));
    }

    @org.junit.Test
    public void nextStopInvalid() {
        assertNull(stop1.getRoutingTable().nextStop(stop2));
        stop1.getRoutingTable().addNeighbour(stop2);
        assertNotNull(stop1.getRoutingTable().nextStop(stop2));
        assertNull(stop1.getRoutingTable().nextStop(stop3));
    }

    @org.junit.Test
    public void traverseNetwork() {
        // Test when stop has no neighbours
        assertEquals(1, stop1.getRoutingTable().traverseNetwork().size());
        assertEquals(stop1, stop1.getRoutingTable().traverseNetwork().get(0));

        // Test when stop has one neighbour
        stop1.addNeighbouringStop(stop2);
        assertEquals(2, stop1.getRoutingTable().traverseNetwork().size());
        assertTrue(stop1.getRoutingTable().traverseNetwork().contains(stop1));
        assertTrue(stop1.getRoutingTable().traverseNetwork().contains(stop2));

        // Test when stop has multiple branches
        stop1.addNeighbouringStop(stop3);
        stop2.addNeighbouringStop(stop4);
        stop2.addNeighbouringStop(stop5);
        assertEquals(5, stop1.getRoutingTable().traverseNetwork().size());
        assertTrue(stop1.getRoutingTable().traverseNetwork().contains(stop1));
        assertTrue(stop1.getRoutingTable().traverseNetwork().contains(stop2));
        assertTrue(stop1.getRoutingTable().traverseNetwork().contains(stop3));
        assertTrue(stop1.getRoutingTable().traverseNetwork().contains(stop4));
        assertTrue(stop1.getRoutingTable().traverseNetwork().contains(stop5));
    }

    @org.junit.Test
    public void transferEntries() {

        // Test transferring one stop
        stop1.addNeighbouringStop(stop2);
        stop2.addNeighbouringStop(stop1);
        stop1.addNeighbouringStop(stop3);
        stop3.addNeighbouringStop(stop1);
        stop1.getRoutingTable().transferEntries(stop2);

        assertEquals(3, stop1.getRoutingTable().traverseNetwork().size());
        assertEquals(3, stop2.getRoutingTable().traverseNetwork().size());
        assertTrue(stop2.getRoutingTable().traverseNetwork().contains(stop1));
        assertTrue(stop2.getRoutingTable().traverseNetwork().contains(stop2));
        assertTrue(stop2.getRoutingTable().traverseNetwork().contains(stop3));

        // Test distance is correct
        assertEquals(5, stop2.getRoutingTable().costTo(stop3));

        // Test transferring multiple stops
        stop3.addNeighbouringStop(stop4);
        stop4.addNeighbouringStop(stop3);
        stop3.addNeighbouringStop(stop5);
        stop5.addNeighbouringStop(stop3);
        stop1.getRoutingTable().addOrUpdateEntry(stop4, 6, stop3);
        stop1.getRoutingTable().addOrUpdateEntry(stop5, 7, stop3);
        stop1.getRoutingTable().transferEntries(stop2);

        assertEquals(5, stop2.getRoutingTable().traverseNetwork().size());
        assertTrue(stop2.getRoutingTable().traverseNetwork().contains(stop1));
        assertTrue(stop2.getRoutingTable().traverseNetwork().contains(stop2));
        assertTrue(stop2.getRoutingTable().traverseNetwork().contains(stop3));
        assertTrue(stop2.getRoutingTable().traverseNetwork().contains(stop4));
        assertTrue(stop2.getRoutingTable().traverseNetwork().contains(stop5));
        assertEquals(9, stop2.getRoutingTable().costTo(stop4));
        assertEquals(10, stop2.getRoutingTable().costTo(stop5));

        // Test distances are unchanged when larger
        assertFalse(stop1.getRoutingTable().transferEntries(stop3));
        assertEquals(4, stop3.getRoutingTable().costTo(stop4));
        assertEquals(5, stop3.getRoutingTable().costTo(stop5));
        assertEquals(5, stop3.getRoutingTable().costTo(stop2));

        // Test next stop is unchanged with the same distance
        // Although this cost value is incorrect, it will result in a cost of 4
        // when the distance to stop 3 is added
        stop1.getRoutingTable().addOrUpdateEntry(stop4, 2, stop5);
        assertFalse(stop1.getRoutingTable().transferEntries(stop3));
        assertEquals(4, stop3.getRoutingTable().costTo(stop4));
        assertEquals(stop4, stop3.getRoutingTable().nextStop(stop4));

        // Test when next stop is less cost
        stop1.getRoutingTable().addOrUpdateEntry(stop4, 1, stop5);
        assertTrue(stop1.getRoutingTable().transferEntries(stop3));
        assertEquals(3, stop3.getRoutingTable().costTo(stop4));
        // Path should go stop3 -> stop1 -> stop4 -> stop5
        assertEquals(stop1, stop3.getRoutingTable().nextStop(stop4));
    }

    @org.junit.Test
    public void synchronise() {
        stop1.addNeighbouringStop(stop2);
        stop2.addNeighbouringStop(stop1);
        stop2.addNeighbouringStop(stop4);
        stop4.addNeighbouringStop(stop2);
        assertEquals(3, stop1.getRoutingTable().getCosts().size());
        assertEquals(stop2, stop1.getRoutingTable().nextStop(stop4));
        assertTrue(stop1.getRoutingTable().getCosts().containsKey(stop4));

        stop1.addNeighbouringStop(stop5);
        stop5.addNeighbouringStop(stop1);
        assertEquals(4, stop4.getRoutingTable().getCosts().size());
        assertEquals(stop2, stop4.getRoutingTable().nextStop(stop5));
        assertTrue(stop5.getRoutingTable().getCosts().containsKey(stop4));

        stop1.addNeighbouringStop(stop3);
        stop3.addNeighbouringStop(stop1);
        assertEquals(5, stop4.getRoutingTable().getCosts().size());
        assertEquals(stop2, stop4.getRoutingTable().nextStop(stop3));
        assertTrue(stop3.getRoutingTable().getCosts().containsKey(stop4));
    }
}