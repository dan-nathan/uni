package exceptions;

/**
 * Exception thrown when a subclass of PublicTransport is added to a Route for a
 * different type of transport. e.g. Train added to BusRoute
 */
public class IncompatibleTypeException extends TransportException {

}