package passengers;

import org.junit.Test;
import static org.junit.Assert.*;
import stops.Stop;

public class PassengerTest {

    private Stop stop1;
    private Stop stop2;
    private Passenger namedPassenger1;
    private Passenger namedPassenger2;
    private Passenger anonPassenger1;
    private Passenger anonPassenger2;

    @org.junit.Before
    public void setUp() throws Exception {
        stop1 = new Stop("Stop 1", 0, 0);
        stop2 = new Stop("Stop 2", 1, 1);
        namedPassenger1 = new Passenger("John Doe");
        namedPassenger2 = new Passenger("Ja\nne\r\n \rDoe");
        anonPassenger1 = new Passenger("", stop1);
        anonPassenger2 = new Passenger(null, stop2);
    }

    @org.junit.After
    public void tearDown() throws Exception {
    }

    @Test
    public void getName() {

        // Check when name is a non-empty string
        assertEquals(namedPassenger1.getName(), "John Doe");

        // Check when name has \n and \r characters
        assertEquals(namedPassenger2.getName(), "Jane Doe");

        // Check when name is an empty string
        assertEquals(anonPassenger1.getName(), "");

        // Check when name is initialised as null
        assertEquals(anonPassenger2.getName(), "");
    }

    @Test
    public void setDestination() {

        // Check when stop is set to the current stop
        anonPassenger1.setDestination(stop1);
        assertEquals(anonPassenger1.getDestination(), stop1);

        //Check when stop is set to another stop
        anonPassenger1.setDestination(stop2);
        assertEquals(anonPassenger1.getDestination(), stop2);

        // Check when stop is set to null
        anonPassenger1.setDestination(null);
        assertNull(anonPassenger1.getDestination());
    }

    @Test
    public void getDestination() {

        // Check when destination is null
        assertNull(namedPassenger1.getDestination());

        // Check when destination is not null
        assertEquals(anonPassenger1.getDestination(), stop1);
    }

    @Test
    public void toStringTest() {

        // Check when name is an empty string
        assertEquals(anonPassenger1.toString(), "Anonymous passenger");

        // Check when name is a non-empty string
        assertEquals(namedPassenger1.toString(), "Passenger named John Doe");
    }
}