package deco2800.skyfall.worlds.generation.delaunay;

/**
 * A class representing the edge between two WorldGenNodes. Code is taken from
 * https://github.com/jdiemke/delaunay-triangulator/blob/master/library/src/main/java/io/github/jdiemke/triangulation/Edge2D.java
 *
 * @author Johannes Diemke
 */
class WorldGenEdge {
    private WorldGenNode a;
    private WorldGenNode b;

    /**
     * Constructor of the 2D edge class used to create a new edge instance from
     * two 2D vectors describing the edge's vertices.
     *
     * @param a The first vertex of the edge
     * @param b The second vertex of the edge
     */
    WorldGenEdge(WorldGenNode a, WorldGenNode b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Return the first node of this edge
     *
     * @return the first node of this edge
     */
    public WorldGenNode getA() {
        return this.a;
    }

    /**
     * Return the second node of this edge
     *
     * @return the second node of this edge
     */
    public WorldGenNode getB() {
        return this.b;
    }
}