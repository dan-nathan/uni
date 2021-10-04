package deco2800.skyfall.worlds.generation;

import deco2800.skyfall.worlds.Tile;
import deco2800.skyfall.worlds.biomes.*;
import deco2800.skyfall.worlds.generation.delaunay.NotEnoughPointsException;
import deco2800.skyfall.worlds.generation.delaunay.WorldGenNode;

import deco2800.skyfall.worlds.world.World;
import deco2800.skyfall.worlds.world.WorldParameters;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds biomes from the nodes generated in the previous phase of the world generation.
 */
public class BiomeGenerator implements BiomeGeneratorInterface {

    public static final String UNABLE_TO_GENERATE_MORE_NODES = "Unable to generate more nodes";
    /**
     * The world this is generating biomes for
     */
    private final World world;

    /**
     * The `Random` instance being used for world generation.
     */
    private final Random random;

    /**
     * The number of nodes to be generated in each biome.
     */
    private final int[] biomeSizes;

    // String Constant
    private static final String OCEAN = "ocean";

    /**
     * The nodes generated in the previous phase of the world generation.
     */
    private final List<WorldGenNode> nodes;
    /**
     * The edges generated in the previous phase of the world generation.
     */
    private final List<VoronoiEdge> voronoiEdges;
    /**
     * The nodes that have already been assigned to
     */
    private HashSet<WorldGenNode> usedNodes;
    /**
     * The nodes that are currently adjacent to a free node.
     */
    private ArrayList<WorldGenNode> borderNodes;

    /**
     * The biomes generated during the generation process.
     */
    private ArrayList<BiomeInProgress> biomes;
    /**
     * The actual biomes to fill after generation.
     */
    private final List<AbstractBiome> realBiomes;

    /**
     * A map from a WorldGenNode to the BiomeInProgress that contains it.
     */
    private HashMap<WorldGenNode, BiomeInProgress> nodesBiomes;

    /**
     * The node on the center of the map
     */
    private WorldGenNode centerNode;

    // The number of lakes and rivers
    private int noLakes;
    private int[] lakeSizes;
    private int noRivers;

    /**
     * Creates a {@code BiomeGenerator} for a list of nodes (but does not start the generation).
     *
     * @param nodes           the nodes generated in the previous phase of the world generation
     * @param random          the random number generator used for deterministic generation
     * @param worldParameters A class that contains most the world parameters
     * @throws NotEnoughPointsException if there are not enough non-border nodes from which to form the biomes
     */
    public BiomeGenerator(World world, List<WorldGenNode> nodes, List<VoronoiEdge> voronoiEdges, Random random,
                          WorldParameters worldParameters)
            throws NotEnoughPointsException {
        Objects.requireNonNull(nodes, "nodes must not be null");
        Objects.requireNonNull(random, "random must not be null");
        Objects.requireNonNull(worldParameters.getBiomeSizes(), "biomeSizes must not be null");
        Objects.requireNonNull(worldParameters.getBiomes(), "realBiomes must not be null");
        for (AbstractBiome realBiome : worldParameters.getBiomes()) {
            Objects.requireNonNull(realBiome, "Elements of realBiome must not be null");
        }

        if (Arrays.stream(worldParameters.getBiomeSizesArray()).anyMatch(size -> size == 0)) {
            throw new IllegalArgumentException("All biomes must require at least one node");
        }

        if (nodes.stream().filter(node -> !node.isBorderNode()).count() < Arrays.stream(worldParameters.getBiomeSizesArray()).sum()) {
            throw new NotEnoughPointsException("Not enough nodes to build biomes");
        }

        this.world = world;
        this.nodes = nodes;
        this.voronoiEdges = voronoiEdges;
        this.random = random;
        this.biomeSizes = worldParameters.getBiomeSizesArray();
        this.realBiomes = worldParameters.getBiomes();
        this.centerNode = calculateCenterNode();
        this.noLakes = worldParameters.getNumOfLakes();
        this.noRivers = worldParameters.getNoRivers();
        this.lakeSizes = worldParameters.getLakeSizesArray();
    }

    /**
     * Calculates and returns the node which contains the point (0, 0).
     *
     * @return the node which contains (0, 0)
     */
    private WorldGenNode calculateCenterNode() {
        // Start the first biome at the node closest to the centre.
        WorldGenNode worldCenterNode = null;
        // Keep track of the squared distance, because it saves calls to the relatively expensive
        // Math.sqrt().
        double centerDistanceSquared = Double.POSITIVE_INFINITY;
        for (WorldGenNode node : nodes) {
            double x = node.getX();
            double y = node.getY();
            double newCenterDistanceSquared = x * x + y * y;
            if (newCenterDistanceSquared < centerDistanceSquared) {
                centerDistanceSquared = newCenterDistanceSquared;
                worldCenterNode = node;
            }
        }
        return worldCenterNode;
    }

    /**
     * Runs the generation process.
     */
    public void generateBiomes() throws DeadEndGenerationException {
        for (int i = 0; ; i++) {
            try {
                biomes = new ArrayList<>(biomeSizes.length + noLakes + 1);
                usedNodes = new HashSet<>(nodes.size());
                borderNodes = new ArrayList<>();
                nodesBiomes = new HashMap<>();
                growBiomes();
                growOcean();
                fillGaps();
                generateLakes(lakeSizes, noLakes);
                generateBeaches();
                generateRivers(noRivers, voronoiEdges);

                return;
            } catch (DeadEndGenerationException e) {
                // Remove tiles from the biomes so they can be reassigned on the next iteration.
                for (AbstractBiome biome : realBiomes) {
                    for (Tile tile : biome.getTiles()) {
                        tile.setBiome(null);
                    }
                    biome.getTiles().clear();
                }
                // Remove all biomes that were added to the list during generation.
                truncateList(realBiomes, biomeSizes.length);

                // If the generation reached a dead-end, try again.
                if (i >= 5) {
                    throw e;
                }
            }
        }
    }

    /**
     * Truncates a list until it is less than or equal to the desired size.
     *
     * @param list        the list to truncate
     * @param desiredSize the size to which to truncate
     * @param <T>         the type of the elements in the list
     */
    private <T> void truncateList(List<T> list, int desiredSize) {
        while (list.size() > desiredSize && !list.isEmpty()) {
            list.remove(list.size() - 1);
        }
    }

    /**
     * Spawns and expands the biomes to meet the required number of nodes.
     *
     * @throws DeadEndGenerationException if a biome which needs to grow has no border nodes
     */
    private void growBiomes() throws DeadEndGenerationException {
        for (int biomeID = 0; biomeID < biomeSizes.length; biomeID++) {
            BiomeInProgress biome = new BiomeInProgress(biomeID, realBiomes.get(biomeID));
            biomes.add(biome);

            if (biomeID == 0) {
                biome.addNode(centerNode);
            } else {
                // Pick a random point on the border to start the next biome from.
                WorldGenNode node = selectWeightedRandomNode(borderNodes, random);
                ArrayList<WorldGenNode> startNodeCandidates = node.getNeighbours().stream()
                        .filter(BiomeGenerator.this::nodeIsFree)
                        .collect(Collectors.toCollection(ArrayList::new));
                WorldGenNode startNode = startNodeCandidates.get(random.nextInt(startNodeCandidates.size()));
                biome.addNode(startNode);
            }

            biome.growBiome();
        }
    }

    /**
     * Grows the ocean biome from the outside of the world.
     */
    private void growOcean() {
        OceanBiome realBiome = new OceanBiome(random);
        realBiomes.add(realBiome);

        // All nodes on the outer edge of the map are ocean nodes.
        // Since the id is `biomeSizes.length`,
        BiomeInProgress ocean = new BiomeInProgress(biomeSizes.length, realBiome);
        biomes.add(ocean);
        for (WorldGenNode node : nodes) {
            if (node.isBorderNode()) {
                ocean.addNode(node);
            }
        }
        ocean.floodGrowBiome();
    }

    /**
     * Fills in unassigned nodes within the continent with adjacent biomes.
     */
    private void fillGaps() {
        while (!borderNodes.isEmpty()) {
            WorldGenNode growFrom = borderNodes.get(random.nextInt(borderNodes.size()));
            BiomeInProgress expandingBiome = nodesBiomes.get(growFrom);

            // Pick a node adjacent to the border node to grow to.
            ArrayList<WorldGenNode> growToCandidates = growFrom.getNeighbours().stream()
                    .filter(BiomeGenerator.this::nodeIsFree)
                    .collect(Collectors.toCollection(ArrayList::new));
            WorldGenNode growTo = growToCandidates.get(random.nextInt(growToCandidates.size()));

            expandingBiome.addNode(growTo);
            for (WorldGenNode adjacentNode : growTo.getNeighbours()) {
                if (usedNodes.contains(adjacentNode) && borderNodes.contains(adjacentNode) &&
                        !nodeIsBorder(adjacentNode)) {
                    borderNodes.remove(adjacentNode);
                }
            }
        }
    }

    /**
     * Converts the coastal region of the island into a beach biome.
     */
    private void generateBeaches() {
        LinkedHashMap<VoronoiEdge, BeachBiome> beachEdges = new LinkedHashMap<>();

        for (VoronoiEdge edge : voronoiEdges) {
            int oceanIndex = -1;
            if (realBiomes.get(nodesBiomes.get(edge.getEdgeNodes().get(0)).id).getBiomeName().equals(OCEAN)) {
                oceanIndex = 0;
            }
            if (realBiomes.get(nodesBiomes.get(edge.getEdgeNodes().get(1)).id).getBiomeName().equals(OCEAN)) {

                if (realBiomes.get(nodesBiomes.get(edge.getEdgeNodes().get(0)).id).getBiomeName().equals(OCEAN) ||
                        oceanIndex == 0) {
                    oceanIndex = -1;
                } else {
                    oceanIndex = 1;
                }
            }

            if (oceanIndex == -1) {
                continue;
            }

            // Sets the parent biome to the one that isn't the ocean
            AbstractBiome parentBiome = realBiomes.get(nodesBiomes.get(edge.getEdgeNodes().get(1 - oceanIndex)).id);
            BeachBiome beach = new BeachBiome(parentBiome, random);
            realBiomes.add(beach);
            beachEdges.put(edge, beach);
        }

        world.setBeachEdges(beachEdges);
    }

    /**
     * Randomly generate lakes in landlocked locations (ie not next to the ocean or another lake)
     *
     * @param lakeSizes The number of WorldGenNodes to make each lake out of
     * @param noLakes   The number of lakes to genereate
     * @throws DeadEndGenerationException If a valid position for a lake cannot be found
     */
    private void generateLakes(int[] lakeSizes, int noLakes) throws DeadEndGenerationException {
        List<List<WorldGenNode>> chosenNodes = new ArrayList<>();
        // A list of parent biomes for each lake
        List<BiomeInProgress> maxNodesBiomes = new ArrayList<>();
        // The nodes that have been flagged to be assigned as lakes, but haven't
        // yet
        List<WorldGenNode> tempLakeNodes = new ArrayList<>();
        // A biome for each lake
        List<BiomeInProgress> lakesFound = new ArrayList<>();
        for (int i = 0; i < noLakes; i++) {
            // Nodes found for this lake
            List<WorldGenNode> nodesFound = findPossibleLakeLocation(lakeSizes[i], tempLakeNodes);

            // Add the lake
            lakesFound.add(new BiomeInProgress(biomes.size() + i, null));
            chosenNodes.add(nodesFound);
            tempLakeNodes.addAll(nodesFound);

            BiomeInProgress maxNodesBiome = findParentBiomeForLake(nodesFound);
            maxNodesBiomes.add(maxNodesBiome);
        }

        for (int i = 0; i < lakesFound.size(); i++) {
            BiomeInProgress lake = lakesFound.get(i);
            biomes.add(lake);
            // Add the lake to the list of real biomes in the same position in
            // the list
            LakeBiome realBiome = new LakeBiome(realBiomes.get(maxNodesBiomes.get(i).id), random);
            realBiomes.add(realBiome);
            lake.realBiome = realBiome;
            for (WorldGenNode node : chosenNodes.get(i)) {
                // Update the BiomeInProgress that the node is in
                nodesBiomes.get(node).nodes.remove(node);
                lake.addNode(node);
            }
        }
    }

    /**
     * Finds a valid location for a lake.
     *
     * @param size          the size of the lake in nodes
     * @param tempLakeNodes the lake nodes
     * @return the list of nodes to comprise the lake
     * @throws DeadEndGenerationException if too many attempts fail
     */
    private List<WorldGenNode> findPossibleLakeLocation(int size, List<WorldGenNode> tempLakeNodes)
            throws DeadEndGenerationException {
        List<WorldGenNode> nodesFound = new ArrayList<>();
        int attempts = 0;
        while (true) {
            attempts++;
            // If there hasn't been a valid spot for a lake found after enough
            // attempts, assume there is no valid spot
            if (attempts > usedNodes.size()) {
                throw new DeadEndGenerationException(UNABLE_TO_GENERATE_MORE_NODES);
            }
            // Try to find a valid node to start a lake
            WorldGenNode chosenNode = nodes.get(random.nextInt(nodes.size()));
            if (!validLakeNode(chosenNode, tempLakeNodes)) {
                continue;
            }

            // Add the initial node
            nodesFound.clear();
            nodesFound.add(chosenNode);
            expandLakeNodes(size, tempLakeNodes, nodesFound);

            if (nodesFound.size() < size) {
                continue;
            }

            return nodesFound;
        }
    }

    /**
     * Expands the lake from a single node to the given size. The list of nodes provided should initially contain only
     * one node and should be modifiable. The result will be added to the list of nodes.
     *
     * @param size          the desired number of nodes
     * @param tempLakeNodes the temporary lake nodes
     * @param nodesFound    the list of nodes for the lake
     */
    private void expandLakeNodes(int size, List<WorldGenNode> tempLakeNodes, List<WorldGenNode> nodesFound) {
        // Find nodes to expand to
        for (int j = 1; j < size; j++) {
            ArrayList<WorldGenNode> growToCandidates = new ArrayList<>();
            // All neighbours of lake nodes that are valid via validLakeNode
            // are possible candidates to grow to
            for (WorldGenNode node : nodesFound) {
                for (WorldGenNode neighbour : node.getNeighbours()) {
                    if (validLakeNode(neighbour, tempLakeNodes) && !nodesFound.contains(neighbour)) {
                        growToCandidates.add(neighbour);
                    }
                }
            }
            // Don't attempt to add null
            if (growToCandidates.isEmpty()) {
                break;
            }
            // Add a random candidate
            WorldGenNode newNode = growToCandidates.get(random.nextInt(growToCandidates.size()));
            nodesFound.add(newNode);
        }
    }

    /**
     * Calculate the parent biome for a lake with the given nodes.
     *
     * @param nodesFound the nodes of the lake
     * @return the parent biome
     */
    private BiomeInProgress findParentBiomeForLake(List<WorldGenNode> nodesFound) {
        // Calculates how many nodes from each biome contribute to the lake
        // To determine the lake's parent biome
        HashMap<BiomeInProgress, Integer> nodesInBiome = new HashMap<>();
        for (WorldGenNode node : nodesFound) {
            BiomeInProgress biome = nodesBiomes.get(node);
            if (!nodesInBiome.containsKey(biome)) {
                nodesInBiome.put(biome, 1);
            } else {
                nodesInBiome.put(biome, nodesInBiome.get(biome) + 1);
            }
        }

        // Get the biome that contributes the most nodes
        BiomeInProgress maxNodesBiome = null;
        for (Map.Entry<BiomeInProgress, Integer> entry : nodesInBiome.entrySet()) {
            if (maxNodesBiome == null || entry.getValue() > nodesInBiome.get(maxNodesBiome)) {
                maxNodesBiome = entry.getKey();
            }
        }
        return maxNodesBiome;
    }

    /**
     * Randomly generate rivers starting from lakes and ending at a lake or the ocean
     * <p>
     * Note: If the river width is not 0 there is a chance a river will terminate when meeting another river instead of
     * passing through it. Currently this is being treated as "it's not a bug it's a feature," as it still looks normal
     * and natural (arguably more natural than if the bug was fixed). I'm guessing the cause is that to get the biome of
     * the adjacent nodes, it gets the biome of node.getTiles().get(0), which can be a lake if some of the tiles have
     * already been overwritten by rivers. This method is still deterministic for a constant riverWidth
     *
     * @param noRivers The number of rivers to generate
     * @param edges    A list of edges that a river can use
     * @throws DeadEndGenerationException If not enough valid rivers can be found
     */
    private void generateRivers(int noRivers, List<VoronoiEdge> edges)
            throws DeadEndGenerationException {
        List<BiomeInProgress> lakes = new ArrayList<>();
        // A hash map cannot be used as that can cause non-deterministic
        // behaviour when there is a tile equidistant from two edges
        List<List<VoronoiEdge>> rivers = new ArrayList<>();
        List<List<AbstractBiome>> riverParentBiomes = new ArrayList<>();
        // Get a list of lake biomes
        for (BiomeInProgress biome : biomes) {
            // If the biome is a lake
            if (realBiomes.get(biome.id).getBiomeName().equals("lake")) {
                lakes.add(biome);
            }
        }
        // If there are no lakes, there can't be any rivers
        if (lakes.isEmpty()) {
            return;
        }
        for (int i = 0; i < noRivers; i++) {
            // Choose a random lake
            BiomeInProgress chosenLake = lakes.get(random.nextInt(lakes.size()));

            VoronoiEdge startingEdge = null;
            double[] startingVertex = null;
            int attempts = 0;
            while (true) {
                // If too many unsuccessful attempts are taken, assume that they
                // world layout does not allow a river to be created
                if (attempts > chosenLake.nodes.size() * 2) {
                    throw new DeadEndGenerationException(UNABLE_TO_GENERATE_MORE_NODES);
                }
                // Get a random node from the lake
                WorldGenNode node = chosenLake.nodes.get(random.nextInt(chosenLake.nodes.size()));
                attempts++;

                // Only allow the node if it is on the edge of the lake, and
                // has a protruding edge
                if (!hasNeighbourOfDifferentBiome(node, chosenLake)) {
                    continue;
                }
                startingEdge = edgeProtrudingFromBiome(edges, node, chosenLake);
                if (startingEdge == null) {
                    continue;
                }

                // Find which vertex the edge starts with
                if (node.getVertices().contains(startingEdge.getA())) {
                    startingVertex = startingEdge.getA();
                } else {
                    startingVertex = startingEdge.getB();
                }
                break;
            }

            // Generate the path for the river
            List<VoronoiEdge> riverEdges = VoronoiEdge.generatePath(startingEdge, startingVertex, random, 2);

            List<AbstractBiome> parentBiomes = new ArrayList<>();
            // Create a river biome and add all tiles for each edge
            for (VoronoiEdge edge : riverEdges) {
                AbstractBiome parentBiome = realBiomes.get(nodesBiomes
                        .get(edge.getEdgeNodes().get(random.nextInt(2))).id);
                parentBiomes.add(parentBiome);
            }
            rivers.add(riverEdges);
            riverParentBiomes.add(parentBiomes);
        }

        List<VoronoiEdge> allRiverEdges = new ArrayList<>();
        List<AbstractBiome> allParentBiomes = new ArrayList<>();

        for (int i = 0; i < rivers.size(); i++) {
            allRiverEdges.addAll(rivers.get(i));
            allParentBiomes.addAll(riverParentBiomes.get(i));
        }

        LinkedHashMap<VoronoiEdge, RiverBiome> nonDuplicateEdges = new LinkedHashMap<>();

        for (int i = 0; i < allRiverEdges.size(); i++) {
            if (!nonDuplicateEdges.containsKey(allRiverEdges.get(i))) {
                RiverBiome river = new RiverBiome(allParentBiomes.get(i), random);
                realBiomes.add(river);
                nonDuplicateEdges.put(allRiverEdges.get(i), river);
            }
        }

        world.setRiverEdges(nonDuplicateEdges);
    }

    /**
     * Finds whether or not a node has a neighbour with a different biome to it
     *
     * @param node      The node to check
     * @param nodeBiome The biome of the node
     * @return whether or not the node has a neighbour with a different biome to it
     */
    private boolean hasNeighbourOfDifferentBiome(WorldGenNode node, BiomeInProgress nodeBiome) {
        // For each neighbour of the node, if it isn't in nodeBiome, return true
        for (WorldGenNode neighbour : node.getNeighbours()) {
            for (BiomeInProgress biome : biomes) {
                if (biome.nodes.contains(neighbour) && biome != nodeBiome) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds an edge protruding from a biome (one vertex is in the biome and the other is not)
     *
     * @param edges a list of edges to check
     * @param node  a node on the edge of the biome
     * @param biome the biome the edge is protruding from
     * @return A VoronoiEdge that has exactly one vertex in the biome, null if there is no such edge
     */
    private VoronoiEdge edgeProtrudingFromBiome(List<VoronoiEdge> edges, WorldGenNode node, BiomeInProgress biome) {
        for (VoronoiEdge edge : edges) {
            // If the edge is adjacent to the biome
            if (edge.getEndNodes().contains(node)) {
                boolean protruding = true;
                for (WorldGenNode edgeNode : edge.getEdgeNodes()) {
                    // If an edgeNode is in the biome, the edge is going along
                    // the border of the biome instead of protruding from it
                    // (not what we want)
                    if (biome.nodes.contains(edgeNode)) {
                        protruding = false;
                        break;
                    }
                }

                if (protruding) {
                    return edge;
                }
            }
        }
        return null;
    }

    /**
     * Returns whether the node is adjacent to any free nodes.
     *
     * @param node the node to check
     * @return whether the node is adjacent to any free nodes
     */
    private boolean nodeIsBorder(WorldGenNode node) {
        return node.getNeighbours().stream().anyMatch(this::nodeIsFree);
    }

    /**
     * Returns whether the node is free to be expanded into.
     *
     * @param node the node to check
     * @return whether the node is free to be expanded into.
     */
    private boolean nodeIsFree(WorldGenNode node) {
        return !node.isBorderNode() && !usedNodes.contains(node);
    }

    /**
     * Returns whether the node is a valid lake node
     *
     * @param node          the node to check
     * @param tempLakeNodes the nodes that have already been assigned as lakes
     * @return whether the node is a valid lake node
     */
    private boolean validLakeNode(WorldGenNode node, List<WorldGenNode> tempLakeNodes) {
        // Don't allow the node the player spawns in to be a lake
        if (node == centerNode || tempLakeNodes.contains(node)) {
            return false;
        }

        // Don't allow nodes that are already in other lakes
        List<WorldGenNode> neighbours = node.getNeighbours();
        for (WorldGenNode neighbour : neighbours) {
            if (tempLakeNodes.contains(neighbour)) {
                return false;
            }
        }

        return !(biomeLoop(node, neighbours));
    }

    private boolean biomeLoop(WorldGenNode node, List<WorldGenNode> neighbours) {
        // Loop through each biome to find which one the node is in
        for (int i = 0; i < biomes.size(); i++) {
            String biomeName = realBiomes.get(i).getBiomeName();
            boolean invalidBiome = (biomeName.equals(OCEAN) || biomeName.equals("lake"));
            // If the node is in a lake or ocean, don't allow it
            if (biomes.get(i).nodes.contains(node) && invalidBiome) {
                return true;
            }
            // Don't allow nodes that are adjacent to the ocean or other lakes
            for (WorldGenNode nodeToTest : neighbours) {
                if (biomes.get(i).nodes.contains(nodeToTest) && invalidBiome) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Selects a random node from the provided list weighted towards nodes closer to (0, 0).
     *
     * @param nodes  the nodes from which to select
     * @param random the RNG used for the selection
     * @return a random node from the provided list
     */
    private static WorldGenNode selectWeightedRandomNode(List<WorldGenNode> nodes, Random random) {
        double sum = nodes.stream().mapToDouble(node -> Math.pow(0.99, node.distanceTo(0, 0))).sum();
        double target = random.nextDouble() * sum;

        for (WorldGenNode node : nodes) {
            sum -= Math.pow(0.99, node.distanceTo(0, 0));
            if (sum < target) {
                return node;
            }
        }
        return nodes.get(nodes.size() - 1);
    }

    /**
     * Represents a single biome during the biome-generation process. This is separate from {@link AbstractBiome}
     * because it contains extra data that is not needed after the generation process.
     */
    private class BiomeInProgress {
        /**
         * The ID of the biome.
         */
        private int id;

        /**
         * The nodes contained within this biome.
         */
        private ArrayList<WorldGenNode> nodes;

        /**
         * The nodes on the border of the biome (for growing).
         */
        private ArrayList<WorldGenNode> borderNodes;

        /**
         * The biome associated with this {@code BiomeInProgress}.
         */
        private AbstractBiome realBiome;

        /**
         * Constructs a new {@code BiomeInProgress} with the specified id.
         *
         * @param id the id of the biome (to check the biome size)
         */
        private BiomeInProgress(int id, AbstractBiome realBiome) {
            this.id = id;
            this.realBiome = realBiome;

            nodes = new ArrayList<>();
            borderNodes = new ArrayList<>();
        }

        /**
         * Expands the biome outwards randomly until it is the required size.
         *
         * @throws DeadEndGenerationException if a biome which needs to grow has no border nodes
         */
        private void growBiome() throws DeadEndGenerationException {
            for (int remainingNodes = biomeSizes[id] - nodes.size(); remainingNodes > 0; remainingNodes--) {
                if (borderNodes.isEmpty()) {
                    throw new DeadEndGenerationException(UNABLE_TO_GENERATE_MORE_NODES);
                }

                // Pick a border node to grow from.
                WorldGenNode growFrom = borderNodes.get(random.nextInt(borderNodes.size()));

                // Pick a node adjacent to the border node to grow to.
                ArrayList<WorldGenNode> growToCandidates = growFrom.getNeighbours().stream()
                        .filter(BiomeGenerator.this::nodeIsFree)
                        .collect(Collectors.toCollection(ArrayList::new));
                WorldGenNode growTo = growToCandidates.get(random.nextInt(growToCandidates.size()));

                addNode(growTo);
            }
        }

        /**
         * Expands a biome to fill all contiguous nodes that are not already used.
         */
        private void floodGrowBiome() {
            while (!borderNodes.isEmpty()) {
                // It doesn't matter which node is grown from.
                WorldGenNode growFrom = borderNodes.get(0);

                for (WorldGenNode node : growFrom.getNeighbours()) {
                    if (nodeIsFree(node)) {
                        addNode(node);
                    }
                }
            }
        }

        /**
         * Adds the node to the nodes and updates the border-node states of nodes accordingly.
         *
         * @param node the node to add to the biome
         */
        private void addNode(WorldGenNode node) {
            // Add the new node to the nodes in this biome
            nodes.add(node);
            nodesBiomes.put(node, this);
            usedNodes.add(node);

            node.setBiome(realBiome);

            // Reassess the border-node status of the surrounding nodes.
            for (WorldGenNode adjacentNode : node.getNeighbours()) {
                if (usedNodes.contains(adjacentNode) && BiomeGenerator.this.borderNodes.contains(adjacentNode) &&
                        !nodeIsBorder(adjacentNode)) {
                    nodesBiomes.get(node).borderNodes.remove(adjacentNode);
                    BiomeGenerator.this.borderNodes.remove(adjacentNode);
                }
            }

            // Check if the current node is a border node.
            if (nodeIsBorder(node)) {
                borderNodes.add(node);
                BiomeGenerator.this.borderNodes.add(node);
            }
        }
    }
}
