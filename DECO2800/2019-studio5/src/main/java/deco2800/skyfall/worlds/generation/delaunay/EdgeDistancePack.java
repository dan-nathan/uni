package deco2800.skyfall.worlds.generation.delaunay;

/**
 * A class that contains a WorldGenEdge, and a distance to that edge from a
 * point Code is taken from
 * https://github.com/jdiemke/delaunay-triangulator/blob/master/library/src/main/java/io/github/jdiemke/triangulation/EdgeDistancePack.java
 *
 * @author Johannes Diemke
 */
class EdgeDistancePack implements Comparable<EdgeDistancePack> {

    WorldGenEdge edge;
    private double distance;

    /**
     * Constructor of the edge distance pack class used to create a new edge
     * distance pack instance from a 2D edge and a scalar value describing a
     * distance.
     *
     * @param edge     The edge
     * @param distance The distance of the edge to some point
     */
    EdgeDistancePack(WorldGenEdge edge, double distance) {
        this.edge = edge;
        this.distance = distance;
    }

    @Override
    public int compareTo(EdgeDistancePack o) {
        return Double.compare(this.distance, o.distance);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EdgeDistancePack)) {
            return false;
        }

        EdgeDistancePack edgeIn = (EdgeDistancePack) o;

        return (this.compareTo(edgeIn) == 0);
    }
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 3 * hash + (int)distance;
        hash = 3 * hash + this.edge.hashCode();
        return hash;
    }
}