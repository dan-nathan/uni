package deco2800.skyfall.worlds.generation.delaunay;

import java.util.Arrays;

/**
 * A triangle of WorldGenNodes. Code taken from
 * https://github.com/jdiemke/delaunay-triangulator/blob/master/library/src/main/java/io/github/jdiemke/triangulation/Triangle2D.java
 * <p>
 * author: Johannes Diemke
 */
class WorldGenTriangle {

    private WorldGenNode a;
    private WorldGenNode b;
    private WorldGenNode c;

    /**
     * Constructor of the 2D triangle class used to create a new triangle instance
     * from three 2D vectors describing the triangle's vertices.
     *
     * @param a The first vertex of the triangle
     * @param b The second vertex of the triangle
     * @param c The third vertex of the triangle
     */
    WorldGenTriangle(WorldGenNode a, WorldGenNode b, WorldGenNode c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    /**
     * Tests if a 2D point lies inside this 2D triangle. See Real-Time Collision
     * Detection, chap. 5, p. 206.
     *
     * @param point The point to be tested
     * @return Returns true iff the point lies inside this 2D triangle
     */
    public boolean contains(WorldGenNode point) {
        double pab = point.subtract(a).crossProduct(b.subtract(a));
        double pbc = point.subtract(b).crossProduct(c.subtract(b));

        if (!hasSameSign(pab, pbc)) {
            return false;
        }

        double pca = point.subtract(c).crossProduct(a.subtract(c));

        return hasSameSign(pab, pca);
    }

    /**
     * Tests if a given point lies in the circumcircle of this triangle. Let the
     * triangle ABC appear in counterclockwise (CCW) order. Then when det &gt; 0,
     * the point lies inside the circumcircle through the three points a, b and c.
     * If instead det &lt; 0, the point lies outside the circumcircle. When det = 0,
     * the four points are cocircular. If the triangle is oriented clockwise (CW)
     * the result is reversed. See Real-Time Collision Detection, chap. 3, p. 34.
     *
     * @param point The point to be tested
     * @return Returns true iff the point lies inside the circumcircle through the
     *         three points a, b, and c of the triangle
     */
    boolean isPointInCircumcircle(WorldGenNode point) {
        double a11 = a.getX() - point.getX();
        double a21 = b.getX() - point.getX();
        double a31 = c.getX() - point.getX();

        double a12 = a.getY() - point.getY();
        double a22 = b.getY() - point.getY();
        double a32 = c.getY() - point.getY();

        double a13 = (a.getX() - point.getX()) * (a.getX() - point.getX())
                + (a.getY() - point.getY()) * (a.getY() - point.getY());
        double a23 = (b.getX() - point.getX()) * (b.getX() - point.getX())
                + (b.getY() - point.getY()) * (b.getY() - point.getY());
        double a33 = (c.getX() - point.getX()) * (c.getX() - point.getX())
                + (c.getY() - point.getY()) * (c.getY() - point.getY());

        double det = a11 * a22 * a33 + a12 * a23 * a31 + a13 * a21 * a32 - a13 * a22 * a31 - a12 * a21 * a33
                - a11 * a23 * a32;

        if (isOrientedCCW()) {
            return det > 0.0d;
        }

        return det < 0.0d;
    }

    /**
     * Test if this triangle is oriented counterclockwise (CCW). Let A, B and C be
     * three 2D points. If det &gt; 0, C lies to the left of the directed line AB.
     * Equivalently the triangle ABC is oriented counterclockwise. When det &lt; 0,
     * C lies to the right of the directed line AB, and the triangle ABC is oriented
     * clockwise. When det = 0, the three points are colinear. See Real-Time
     * Collision Detection, chap. 3, p. 32
     *
     * @return Returns true iff the triangle ABC is oriented counterclockwise (CCW)
     */
    boolean isOrientedCCW() {
        double a11 = a.getX() - c.getX();
        double a21 = b.getX() - c.getX();

        double a12 = a.getY() - c.getY();
        double a22 = b.getY() - c.getY();

        double det = a11 * a22 - a12 * a21;

        return det > 0.0d;
    }

    /**
     * Returns true if this triangle contains the given edge.
     *
     * @param edge The edge to be tested
     * @return Returns true if this triangle contains the edge
     */
    boolean isNeighbour(WorldGenEdge edge) {
        return (a == edge.getA() || b == edge.getA() || c == edge.getA())
                && (a == edge.getB() || b == edge.getB() || c == edge.getB());
    }

    /**
     * Returns the vertex of this triangle that is not part of the given edge.
     *
     * @param edge The edge
     * @return The vertex of this triangle that is not part of the edge
     */
    WorldGenNode getNoneEdgeVertex(WorldGenEdge edge) {
        if (a != edge.getA() && a != edge.getB()) {
            return a;
        } else if (b != edge.getA() && b != edge.getB()) {
            return b;
        } else if (c != edge.getA() && c != edge.getB()) {
            return c;
        }

        return null;
    }

    /**
     * Returns true if the given vertex is one of the vertices describing this
     * triangle.
     *
     * @param vertex The vertex to be tested
     * @return Returns true if the Vertex is one of the vertices describing this
     *         triangle
     */
    boolean hasVertex(WorldGenNode vertex) {
        return (a == vertex || b == vertex || c == vertex);
    }

    /**
     * Returns an EdgeDistancePack containing the edge and its distance nearest to
     * the specified point.
     *
     * @param point The point the nearest edge is queried for
     * @return The edge of this triangle that is nearest to the specified point
     */
    EdgeDistancePack findNearestEdge(WorldGenNode point) {
        EdgeDistancePack[] edges = new EdgeDistancePack[3];

        edges[0] = new EdgeDistancePack(new WorldGenEdge(a, b),
                computeClosestPoint(new WorldGenEdge(a, b), point).subtract(point).magnitude());
        edges[1] = new EdgeDistancePack(new WorldGenEdge(b, c),
                computeClosestPoint(new WorldGenEdge(b, c), point).subtract(point).magnitude());
        edges[2] = new EdgeDistancePack(new WorldGenEdge(c, a),
                computeClosestPoint(new WorldGenEdge(c, a), point).subtract(point).magnitude());
        Arrays.sort(edges);
        return edges[0];
    }

    /**
     * Computes the closest point on the given edge to the specified point.
     *
     * @param edge  The edge on which we search the closest point to the specified
     *              point
     * @param point The point to which we search the closest point on the edge
     * @return The closest point on the given edge to the specified point
     */
    private WorldGenNode computeClosestPoint(WorldGenEdge edge, WorldGenNode point) {
        WorldGenNode ab = edge.getB().subtract(edge.getA());
        double t = point.subtract(edge.getA()).dotProduct(ab) / ab.dotProduct(ab);

        if (t < 0.0d) {
            t = 0.0d;
        } else if (t > 1.0d) {
            t = 1.0d;
        }

        return edge.getA().add(ab.scalarMultiply(t));
    }

    /**
     * Tests if the two arguments have the same sign.
     *
     * @param a The first floating point argument
     * @param b The second floating point argument
     * @return Returns true iff both arguments have the same sign
     */
    private boolean hasSameSign(double a, double b) {
        return Math.signum(a) == Math.signum(b);
    }

    @Override
    public String toString() {
        return "Triangle2D[" + a + ", " + b + ", " + c + "]";
    }

    /**
     * Calculates the coordinates circumcentre of the triangle. This method was not
     * taken from the source of the rest of this class
     *
     * @return the coordinates of the circumcentre
     * @throws CollinearPointsException if the three points of this triangle are
     *                                  collinear
     * @author Daniel Nathan
     */
    double[] circumcentre() throws CollinearPointsException {
        double ax = a.getX();
        double ay = a.getY();
        double bx = b.getX();
        double by = b.getY();
        double cx = c.getX();
        double cy = c.getY();

        // Check if the points are collinear

        // If they all have the same x value they are collinear
        if (ax == bx && ax == cx) {
            throw new CollinearPointsException();
        }

        // If two have the same x value and one doesn't, they aren't collinear
        // If they all have different x values, they are collinear if any two of
        // The gradients between them are equal
        if ((ax != bx && ax != cx) && ((by - ay) / (bx - ax) == (cy - ay) / (cx - ax))) {
            throw new CollinearPointsException();
        }

        // The coordinates to return. This method calculates the circumcentre by
        // finding the intersection between the normals of sides AB and AC
        // passing through the midpoint
        double[] coords = { 0, 0 };

        // The midpoints of the sides of the triangle
        double[] midAB = { (ax + bx) / 2, (ay + by) / 2 };
        double[] midAC = { (ax + cx) / 2, (ay + cy) / 2 };

        // See if two points have the same y value (if so, the gradient of the
        // normal is undefined)
        boolean sameYAB = false;
        boolean sameYAC = false;

        // else if is used because from the collinearity check, they can't all
        // have the same y value
        if (ay == by) {
            sameYAB = true;
            // The normal line will be vertical passing through the midpoint
            // meaning the x value is the same as that of the midpoint
            coords[0] = midAB[0];
        } else if (ay == cy) {
            sameYAC = true;
            coords[0] = midAC[0];
        }

        // The slope of the normal to the sides of the triangle.
        double normAB = 0;
        double normAC = 0;

        // Don't calculate the slopes if it will cause division by 0
        if (!sameYAB) {
            normAB = (bx - ax) / (ay - by);
        }
        if (!sameYAC) {
            normAC = (cx - ax) / (ay - cy);
        }

        if (sameYAB) {
            // Substitute into y-y0 = m(x-x0) for the normal to AC
            coords[1] = midAC[1] + normAC * (midAB[0] - midAC[0]);
        } else if (sameYAC) {
            // Substitute into y-y0 = m(x-x0) for the normal to AB
            coords[1] = midAB[1] + normAB * (midAC[0] - midAB[0]);
        } else {
            // Solution to simultaneous equations y - yab = mab(x - xab) and
            // y - yac = mac(x - xac)
            coords[0] = (normAB * midAB[0] - normAC * midAC[0] - midAB[1] + midAC[1]) / (normAB - normAC);
            coords[1] = midAB[1] + normAB * (coords[0] - midAB[0]);
        }

        return coords;
    }

    /**
     * Return the first node of this triangle
     *
     * @return the first node of this triangle
     */
    WorldGenNode getA() {
        return this.a;
    }

    /**
     * Return the second node of this triangle
     *
     * @return the second node of this triangle
     */
    WorldGenNode getB() {
        return this.b;
    }

    /**
     * Return the third node of this triangle
     *
     * @return the third node of this triangle
     */
    WorldGenNode getC() {
        return this.c;
    }

}