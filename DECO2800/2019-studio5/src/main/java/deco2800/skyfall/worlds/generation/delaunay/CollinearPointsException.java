package deco2800.skyfall.worlds.generation.delaunay;

import deco2800.skyfall.worlds.generation.WorldGenException;

/**
 * An exception thrown when trying to find the circumcentre of three collinear
 * points
 */
public class CollinearPointsException extends WorldGenException {

    public CollinearPointsException() {
    }

    public CollinearPointsException(String s) {
        super(s);
    }

}
