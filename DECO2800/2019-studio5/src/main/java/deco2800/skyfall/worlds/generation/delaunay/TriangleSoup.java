package deco2800.skyfall.worlds.generation.delaunay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A collection of interconnected WorldGenTriangle
 * Code taken from https://github.com/jdiemke/delaunay-triangulator/blob/master/library/src/main/java/io/github/jdiemke/triangulation/TriangleSoup.java
 * @author Johannes Diemke
 */
class TriangleSoup {

    private List<WorldGenTriangle> triangleSoupList;

    private List<WorldGenNode> borderNodes;

    /**
     * Constructor of the triangle soup class used to create a new triangle soup
     * instance.
     */
    TriangleSoup() {
        this.triangleSoupList = new ArrayList<>();
        this.borderNodes = new ArrayList<>();
    }

    /**
     * Adds a triangle to this triangle soup.
     *
     * @param triangle The triangle to be added to this triangle soup
     */
    public void add(WorldGenTriangle triangle) {
        this.triangleSoupList.add(triangle);
    }

    /**
     * Removes a triangle from this triangle soup.
     *
     * @param triangle The triangle to be removed from this triangle soup
     */
    public void remove(WorldGenTriangle triangle) {
        this.triangleSoupList.remove(triangle);
    }

    /**
     * Returns the triangles from this triangle soup.
     *
     * @return The triangles from this triangle soup
     */
    List<WorldGenTriangle> getTriangles() {
        return this.triangleSoupList;
    }

    /**
     * Returns the triangle from this triangle soup that contains the specified
     * point or null if no triangle from the triangle soup contains the point.
     *
     * @param point
     *            The point
     * @return Returns the triangle from this triangle soup that contains the
     *         specified point or null
     */
    WorldGenTriangle findContainingTriangle(WorldGenNode point) {
        for (WorldGenTriangle triangle : triangleSoupList) {
            if (triangle.contains(point)) {
                return triangle;
            }
        }
        return null;
    }

    /**
     * Returns the neighbor triangle of the specified triangle sharing the same
     * edge as specified. If no neighbor sharing the same edge exists null is
     * returned.
     *
     * @param triangle The triangle
     * @param edge The edge
     * @return The triangles neighbor triangle sharing the same edge or null if
     *         no triangle exists
     */
    WorldGenTriangle findNeighbour(WorldGenTriangle triangle, WorldGenEdge edge) {
        for (WorldGenTriangle triangleFromSoup : triangleSoupList) {
            if (triangleFromSoup.isNeighbour(edge) && triangleFromSoup != triangle) {
                return triangleFromSoup;
            }
        }
        return null;
    }

    /**
     * Returns one of the possible triangles sharing the specified edge. Based
     * on the ordering of the triangles in this triangle soup the returned
     * triangle may differ. To find the other triangle that shares this edge use
     * the findNeighbour(WorldGenTriangle triangle, WorldGenEdge edge) method.
     *
     * @param edge The edge
     * @return Returns one triangle that shares the specified edge
     */
    WorldGenTriangle findOneTriangleSharing(WorldGenEdge edge) {
        for (WorldGenTriangle triangle : triangleSoupList) {
            if (triangle.isNeighbour(edge)) {
                return triangle;
            }
        }
        return null;
    }

    /**
     * Returns the edge from the triangle soup nearest to the specified point.
     *
     * @param point The point
     * @return The edge from the triangle soup nearest to the specified point
     */
    WorldGenEdge findNearestEdge(WorldGenNode point) {
        List<EdgeDistancePack> edgeList = new ArrayList<>();

        for (WorldGenTriangle triangle : triangleSoupList) {
            edgeList.add(triangle.findNearestEdge(point));
        }

        EdgeDistancePack[] edgeDistancePacks = new EdgeDistancePack[edgeList.size()];
        edgeList.toArray(edgeDistancePacks);

        Arrays.sort(edgeDistancePacks);
        return edgeDistancePacks[0].edge;
    }

    /**
     * Removes all triangles from this triangle soup that contain the specified
     * vertex.
     *
     * @param vertex The vertex
     */
    void removeTrianglesUsing(WorldGenNode vertex) {
        List<WorldGenTriangle> trianglesToBeRemoved = new ArrayList<>();

        for (WorldGenTriangle triangle : triangleSoupList) {
            if (triangle.hasVertex(vertex)) {
                trianglesToBeRemoved.add(triangle);
            }
        }
        triangleSoupList.removeAll(trianglesToBeRemoved);
    }

    /**
     * Assigns each node as a border and/or corner node if it is one
     * This method was not taken from the source of the rest of this class
     *
     * @author Daniel Nathan
     */
    void findBorderNodes() {
        for (WorldGenTriangle triangle : this.getTriangles()) {
            WorldGenEdge edgeAB = new WorldGenEdge(
                    triangle.getA(), triangle.getB());
            // Indicates whether each node is a border node
            boolean[] isBorder = {false, false, false};
            // If there is no triangle on the other side of the edge, both nodes
            // are border nodes
            if (findNeighbour(triangle, edgeAB) == null) {
                isBorder[0] = true;
                isBorder[1] = true;
            }
            WorldGenEdge edgeAC = new WorldGenEdge(
                    triangle.getA(), triangle.getC());
            if (findNeighbour(triangle, edgeAC) == null) {
                isBorder[0] = true;
                isBorder[2] = true;
            }
            WorldGenEdge edgeBC = new WorldGenEdge(
                    triangle.getB(), triangle.getC());
            if (findNeighbour(triangle, edgeBC) == null) {
                isBorder[1] = true;
                isBorder[2] = true;
            }

            // Record the relevant nodes as borders
            if (isBorder[0]) {
                this.borderNodes.add(triangle.getA());
                triangle.getA().setBorderNode(true);
            }
            if (isBorder[1]) {
                this.borderNodes.add(triangle.getB());
                triangle.getB().setBorderNode(true);
            }
            if (isBorder[2]) {
                this.borderNodes.add(triangle.getC());
                triangle.getC().setBorderNode(true);
            }
        }
    }

    /**
     * Returns a list of corner nodes
     * This method was not taken from the source of the rest of this class
     *
     * @author Daniel Nathan
     * @return the corner nodes
     */
    public List<WorldGenNode> getBorderNodes() {
        return this.borderNodes;
    }

}