package deco2800.skyfall.worlds.generation;

import deco2800.skyfall.saving.AbstractMemento;
import deco2800.skyfall.saving.Saveable;
import deco2800.skyfall.worlds.Tile;
import deco2800.skyfall.worlds.biomes.OceanBiome;
import deco2800.skyfall.worlds.generation.delaunay.WorldGenNode;
import deco2800.skyfall.worlds.world.World;

import java.io.Serializable;
import java.util.*;

/**
 * A class to represent an edge of a polygon in a Voronoi Diagram
 */
public class VoronoiEdge implements Saveable<VoronoiEdge.VoronoiEdgeMemento> {
    private static long nextID = System.nanoTime();

    private World world;
    private long edgeID;

    // The coordinates of the two endpoints of the edge
    private double[] pointA;
    private double[] pointB;

    // The edges that share the respective vertex
    private List<VoronoiEdge> pointANeighbours;
    private List<VoronoiEdge> pointBNeighbours;

    // The nodes on either side of the edge (ie they share 2 vertices with the
    // line)
    private List<WorldGenNode> edgeNodes;

    // The nodes on the end points of the line (ie they share 1 vertex with the
    // line)
    private List<WorldGenNode> endNodes;

    // The tiles that this edge passes through
    private List<Tile> tiles;

    /**
     * Constructor for a VoronoiEdge
     *
     * @param pointA The first vertex of the edge
     * @param pointB The second vertex of the edge
     * @param world The world the edge is in
     */
    public VoronoiEdge(double[] pointA, double[] pointB, World world) {
        this.world = world;
        this.edgeID = nextID++;
        this.pointA = pointA;
        this.pointB = pointB;
        this.pointANeighbours = new ArrayList<>();
        this.pointBNeighbours = new ArrayList<>();
        this.edgeNodes = new ArrayList<>();
        this.endNodes = new ArrayList<>();
        this.tiles = new ArrayList<>();
    }

    /**
     * Constructor for a VoronoiEdge being loaded from a memento
     *
     * @param memento the memento of the edge
     */
    public VoronoiEdge(VoronoiEdgeMemento memento) {
        this.load(memento);
    }

    /**
     * Find if an edge node for the edge appears in all sub-lists of previousNodes
     * (and is therefore invalid)
     *
     * @param previousNodes A list of lists of nodes to compare an edge with
     * @param edge the edge to compare
     * @return true if the node is valid, false otherwise
     */
    private static boolean validNeighbour (List<List<WorldGenNode>> previousNodes, VoronoiEdge edge) {
        // If the path is less than 2 edges long, the edge is valid
        if (previousNodes.size() < 2) {
            return true;
        }
        // Check each edge Node
        for (WorldGenNode node : edge.edgeNodes) {
            boolean legalNode = false;
            // If the edge Node appears in every list of previousNodes, it is
            // invalid
            for (List<WorldGenNode> nodes : previousNodes) {
                if (!nodes.contains(node)) {
                    legalNode = true;
                    break;
                }
            }
            if (!legalNode) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a random continuous path of edges, terminating at a lake or ocean
     *
     * @param startEdge The first edge of the path
     * @param startVertex The first vertex of the path
     * @param random The random seed for the path
     * @param maxTimesOnNode The maximum number of consecutive times the path can
     *                       be adjacent to one node
     * @return A list of edges that comprise the path
     * @throws DeadEndGenerationException If the path cannot continue for invalid
     *         reasons (ie an edge doesn't have any valid neighbour edges)
     */
    public static List<VoronoiEdge> generatePath(VoronoiEdge startEdge, double[] startVertex, Random random, int maxTimesOnNode)
            throws DeadEndGenerationException {

        EdgeVertex ev = new EdgeVertex();
        ev.edge = startEdge;
        ev.vertex = startVertex;

        // The list of edges found so far
        List<VoronoiEdge> path = new ArrayList<>();
        // The edge nodes for the last 2 edges added
        List<List<WorldGenNode>> previousNodes = new ArrayList<>(maxTimesOnNode);

        // Add the initial edge to the path
        path.add(ev.edge);
        previousNodes.add(ev.edge.getEdgeNodes());

        while (true) {
            if (generatePathIteration(ev, random, maxTimesOnNode, path, previousNodes)) {
                break;
            }
        }
        return path;
    }

    // Class containing an edge and a vertex
    private static class EdgeVertex {
        VoronoiEdge edge;
        double[] vertex;
    }

    private static boolean generatePathIteration(EdgeVertex ev, Random random, int maxTimesOnNode, List<VoronoiEdge> path, List<List<WorldGenNode>> previousNodes) throws DeadEndGenerationException {
        boolean validNeighbour = false;
        // Get the edges that are adjacent via the other vertex
        List<VoronoiEdge> tempEdges = new ArrayList<>(ev.edge.getVertexSharingEdges(ev.edge.otherVertex(ev.vertex)));
        while (!validNeighbour) {
            // If none of the adjacent edges are valid to add, or valid to
            // terminate the path
            if (tempEdges.isEmpty()) {
                throw new DeadEndGenerationException("");
            }
            // Get a random neighbour from tempEdges
            VoronoiEdge neighbour = tempEdges.get(random.nextInt(tempEdges.size()));
            validNeighbour = validNeighbour(previousNodes, neighbour);
            if (!validNeighbour) {
                // Don't check the same edge again
                tempEdges.remove(neighbour);
                continue;
            }

            // Check if the path has reached a lake or ocean
            if (path.size() == 1 && checkOneEdgePathCompletion(neighbour)) {
                return true;
            }

            // Add the edge if it's valid
            path.add(neighbour);
            if (checkMultipleEdgePathCompletion(neighbour)) {
                return true;
            }

            // Flag the while loop to end
            validNeighbour = true;
            // Move to the next edge
            ev.vertex = ev.edge.otherVertex(ev.vertex);
            ev.edge = neighbour;

            // Update the list of previous nodes
            if (previousNodes.size() == maxTimesOnNode) {
                previousNodes.remove(0);
            }
            previousNodes.add(ev.edge.getEdgeNodes());
        }
        return false;
    }

    private static boolean checkOneEdgePathCompletion(VoronoiEdge neighbour) {
        boolean endOfPath = true;
        for (WorldGenNode node : neighbour.endNodes) {
            if (node == null) {
                return true;
            }
            // Get the biome of the end node
            String biomeName = node.getBiome().getBiomeName();
            // If the path is already at the ocean
            if (biomeName.equals(OceanBiome.NAME)) {
                return true;
            }

            // If either end node isn't a lake the path has not ended
            if (!biomeName.equals("lake")) {
                endOfPath = false;
            }
        }
        // If both of the end nodes are lakes, the path is already complete
        return endOfPath;
    }

    private static boolean checkMultipleEdgePathCompletion(VoronoiEdge neighbour) {
        for (WorldGenNode node : neighbour.endNodes) {
            if (node == null) {
                return true;
            }
            String biomeName = node.getBiome().getBiomeName();
            // If the new edge ends with the ocean or a lake
            if (biomeName.equals(OceanBiome.NAME) || biomeName.equals("lake")) {
                return true;
            }
        }
        // Repeat for edge nodes as a fail-safe
        for (WorldGenNode node : neighbour.edgeNodes) {
            if (node == null) {
                return true;
            }
            String biomeName = node.getBiome().getBiomeName();
            // If the new edge ends with the ocean or a lake
            if (biomeName.equals(OceanBiome.NAME) || biomeName.equals("lake")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the edges that are adjacent to this edge via the given vertex
     *
     * @param vertex The vertex
     * @return a list of edges adjacent to this edge via the given vertex
     */
    public List<VoronoiEdge> getVertexSharingEdges(double[] vertex) {
        if (Arrays.equals(vertex, this.pointA)) {
            return this.pointANeighbours;
        }
        if (Arrays.equals(vertex, this.pointB)) {
            return this.pointBNeighbours;
        }
        return new ArrayList<>();
    }

    /**
     * Assign the end nodes and neighbouring edges of each edge
     *
     * @param edges The edges to assign
     */
    public static void assignNeighbours(List<VoronoiEdge> edges) {
        // Loop through every edge
        for (int i = 0; i < edges.size(); i++) {
            assignNeighboursSingle(edges, i);
        }
    }

    private static void assignNeighboursSingle(List<VoronoiEdge> edges, int i) {

        // Assign the end nodes of the edge
        // Start by choosing an arbitrary edge node
        WorldGenNode node = edges.get(i).getEdgeNodes().get(0);
        for (WorldGenNode neighbour : node.getNeighbours()) {
            // Don't add the other edge node as an end node
            if (edges.get(i).getEdgeNodes().contains(neighbour)) {
                continue;
            }

            // Add the node as an end node if it shares a vertex with the edge
            for (double[] nodeVertex : neighbour.getVertices()) {
                if (Arrays.equals(edges.get(i).getA(), nodeVertex) || Arrays.equals(edges.get(i).getB(), nodeVertex)) {
                    edges.get(i).addEndNode(neighbour);
                    break;
                }
            }
        }

        // Loop through each other edge
        for (int j = i + 1; j < edges.size(); j++) {
            VoronoiEdge edgeA = edges.get(i);
            VoronoiEdge edgeB = edges.get(j);
            addToNeighbourList(edgeA, edgeB);
        }
    }

    private static void addToNeighbourList(VoronoiEdge edgeA, VoronoiEdge edgeB) {
        // Figure out which vertex the edges are adjacent by if any, and
        // add them to the corresponding list of neighbours for that vertex
        if (Arrays.equals(edgeA.getA(), edgeB.getA())) {
            edgeA.addANeighbour(edgeB);
            edgeB.addANeighbour(edgeA);
        } else if (Arrays.equals(edgeA.getA(), edgeB.getB())) {
            edgeA.addANeighbour(edgeB);
            edgeB.addBNeighbour(edgeA);
        } else if (Arrays.equals(edgeA.getB(), edgeB.getA())) {
            edgeA.addBNeighbour(edgeB);
            edgeB.addANeighbour(edgeA);
        } else if (Arrays.equals(edgeA.getB(), edgeB.getB())) {
            edgeA.addBNeighbour(edgeB);
            edgeB.addBNeighbour(edgeA);
        }
    }

    /**
     * Get the square of the length of this edge
     *
     * @return The square of the length of this edge
     */
    public double getSquareOfLength() {
        double dx = this.pointA[0] - this.pointB[0];
        double dy = this.pointA[1] - this.pointB[1];
        return dx * dx + dy * dy;
    }

    /**
     * Add a neighbour that shares vertex A with this edge
     *
     * @param other the other edge
     */
    public void addANeighbour(VoronoiEdge other) {
        this.pointANeighbours.add(other);
    }

    /**
     * Add a neighbour that shares vertex B with this edge
     *
     * @param other the other edge
     */
    public void addBNeighbour(VoronoiEdge other) {
        this.pointBNeighbours.add(other);
    }

    /**
     * Add a node as an edge node of this edge
     *
     * @param node the node
     */
    public void addEdgeNode(WorldGenNode node) {
        this.edgeNodes.add(node);
    }

    /**
     * Add a node as an end node of this edge
     *
     * @param node the node
     */
    public void addEndNode(WorldGenNode node) {
        this.endNodes.add(node);
    }

    /**
     * Get the edge nodes of this edge
     *
     * @return the edge nodes of this edge
     */
    public List<WorldGenNode> getEdgeNodes() {
        return this.edgeNodes;
    }

    /**
     * Get the end nodes of this edge
     *
     * @return the end nodes of this edge
     */
    public List<WorldGenNode> getEndNodes() {
        return this.endNodes;
    }

    /**
     * Given one of the vertices of the edge, get the other vertex
     *
     * @param vertex The vertex for which to find the non-matching vertex
     * @return null if vertex isn't one of the vertices of this edge, the
     *         non-matching vertex otherwise
     */
    public double[] otherVertex(double[] vertex) {
        if (Arrays.equals(this.pointA, vertex)) {
            return this.pointB;
        }
        if (Arrays.equals(this.pointB, vertex)) {
            return this.pointA;
        }
        return new double[] {};
    }

    /**
     * Get vertex A for this edge
     *
     * @return vertex A
     */
    public double[] getA() {
        return this.pointA;
    }

    /**
     * Get vertex B for this edge
     *
     * @return vertex B
     */
    public double[] getB() {
        return this.pointB;
    }

    /**
     * Add a tile to this edge
     *
     * @param tile the tile to add
     */
    public void addTile(Tile tile) {
        this.tiles.add(tile);
    }

    /**
     * Get the tiles of this edge
     *
     * @return a list of tiles for this edge
     */
    public List<Tile> getTiles() {
        return this.tiles;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public long getID() {
        return this.edgeID;
    }

    @Override
    public VoronoiEdgeMemento save() {
        return new VoronoiEdgeMemento(this);
    }

    @Override
    public void load(VoronoiEdgeMemento memento) {
        this.pointA = new double[] {memento.ax, memento.ay};
        this.pointB = new double[] {memento.bx, memento.by};

        this.edgeID = memento.edgeID;
    }

    public static class VoronoiEdgeMemento implements AbstractMemento, Serializable {
        private long edgeID;

        // The coordinates of the two vertices of this edge
        private double ax;
        private double ay;
        private double bx;
        private double by;

        /**
         * Constructor for a new VoronoiEdgeMemento
         *
         * @param edge the edge this is for
         */
        public VoronoiEdgeMemento(VoronoiEdge edge)  {
            this.edgeID = edge.edgeID;

            this.ax = edge.getA()[0];
            this.ay = edge.getA()[1];
            this.bx = edge.getB()[0];
            this.by = edge.getB()[1];
        }
    }
}
