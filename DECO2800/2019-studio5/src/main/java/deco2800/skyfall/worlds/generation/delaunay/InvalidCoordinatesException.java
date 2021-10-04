package deco2800.skyfall.worlds.generation.delaunay;

/**
 * An exception thrown when a set of coordinates does not have 2 dimensions
 */
public class InvalidCoordinatesException extends RuntimeException {

    public InvalidCoordinatesException() {
    }

    public InvalidCoordinatesException(String s) {
        super(s);
    }

}
