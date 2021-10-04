import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * A PointQuadTree is a two dimensional equivalent of a binary search tree.
 * Each node has four children, one above and to the left, one above and to the
 * right, one below and to the left, one below and to the right. This is useful
 * for partitioning 2D space, so that an object at a given position can be found
 * without having to store every position in an array, or similar data
 * structure. This allows memory complexity to be linear with the number of
 * elements, rather than the number of positions, which is more efficient for
 * sparsely populated regions.
 *
 * An example situation where this could be useful would be collision checking
 * in a simple program simulating motion of particles. To check if a given
 * position is occupied, the position can be searched for in the tree (with
 * O(log n) average case time complexity and O(n) worst case time complexity)
 * instead of looping through each particle to find if the coordinates match
 * (O(n) average and worst case time complexity), where n is the number of
 * particles. It would also be more memory efficient than an alternative such
 * as a 2D array if the array will be sparsely populated.
 *
 */
public class PointQuadTree<T extends Comparable<T>, V>
        implements Tree<Coordinates<T, V>> {

    // The entry at the root of this tree
    private Coordinates<T, V> root;

    // The subtrees corresponding to each child of the tree
    private PointQuadTree<T, V> topLeft;
    private PointQuadTree<T, V> topRight;
    private PointQuadTree<T, V> bottomLeft;
    private PointQuadTree<T, V> bottomRight;

    // The parent node of this node
    private PointQuadTree<T, V> parent;

    /**
     * Constructs a new PointQuadTree with a single node
     *
     * @param root the element to store at this tree's root
     */
    public PointQuadTree (Coordinates<T, V> root) {
        this.root = root;
        this.topLeft = null;
        this.topRight = null;
        this.bottomLeft = null;
        this.bottomRight = null;
        this.parent = null;
    }

    @Override
    public int size() {
        return 1 + (topLeft != null ? topLeft.size() : 0)
                + (topRight != null ? topRight.size() : 0)
                + (bottomLeft != null ? bottomLeft.size() : 0)
                + (bottomRight != null ? bottomRight.size() : 0);
    }

    @Override
    public Coordinates<T, V> getRoot() {
        return root;
    }

    /**
     * Sets the entry at the root of this tree
     *
     * @param coords the new entry for the root of this tree
     */
    public void setRoot(Coordinates<T, V> coords) {
        this.root = coords;
    }

    @Override
    public boolean isLeaf() {
        return topLeft == null && topRight == null && bottomLeft == null
                && bottomRight == null;
    }

    @Override
    public List<Tree<Coordinates<T, V>>> getChildren() {
        List<Tree<Coordinates<T, V>>> children = new ArrayList<>();
        if (topLeft != null) {
            children.add(topLeft);
        }
        if (topRight != null) {
            children.add(topRight);

        }
        if (bottomLeft != null) {
            children.add(bottomLeft);
        }
        if (bottomRight != null) {
            children.add(bottomRight);

        }

        return children;
    }

    @Override
    public boolean contains(Coordinates elem) {
        if (root.equals(elem)) {
            return true;
        }
        return (topLeft != null && topLeft.contains(elem))
                || (topRight != null && topRight.contains(elem))
                || (bottomLeft != null && bottomLeft.contains(elem))
                || (bottomRight != null && bottomRight.contains(elem));
    }

    @Override
    public Iterator<Coordinates<T, V>> iterator() {
        return new TreeIterator<>(this);
    }

    /**
     * Sets the parent of this node
     *
     * @param parent the node's parent
     */
    public void setParent(PointQuadTree<T, V> parent) {
        this.parent = parent;
    }

    /**
     * Sets the top left child of this tree's root to the given subtree
     * Any existing top left child will be overridden
     *
     * @param topLeft the new top left child
     */
    public void setTopLeft(PointQuadTree<T, V> topLeft) {
        this.topLeft = topLeft;
        if (topLeft != null) {
            topLeft.setParent(this);
        }
    }

    /**
     * Sets the top right child of this tree's root to the given subtree
     * Any existing top right child will be overridden
     *
     * @param topRight the new top right child
     */
    public void setTopRight(PointQuadTree<T, V> topRight) {
        this.topRight = topRight;
        if (topRight != null) {
            topRight.setParent(this);
        }
    }

    /**
     * Sets the bottom left child of this tree's root to the given subtree
     * Any existing bottom left child will be overridden
     *
     * @param bottomLeft the new bottom left child
     */
    public void setBottomLeft(PointQuadTree<T, V> bottomLeft) {
        this.bottomLeft = bottomLeft;
        if (bottomLeft != null) {
            bottomLeft.setParent(this);
        }
    }

    /**
     * Sets the bottom right child of this tree's root to the given subtree
     * Any existing bottom right child will be overridden
     *
     * @param bottomRight the new bottom right child
     */
    public void setBottomRight(PointQuadTree<T, V> bottomRight) {
        this.bottomRight = bottomRight;
        if (bottomRight != null) {
            bottomRight.setParent(this);
        }
    }

    /**
     * Returns the parent of this node
     *
     * @return the parent of this node
     */
    public PointQuadTree<T, V> getParent() {
        return parent;
    }

    /**
     * Returns the top left child subtree of this tree's root node
     *
     * @return the top left child subtree of this tree's root node
     */
    public PointQuadTree<T, V> getTopLeft() {
        return topLeft;
    }

    /**
     * Returns the top right child subtree of this tree's root node
     *
     * @return the top right child subtree of this tree's root node
     */
    public PointQuadTree<T, V> getTopRight() {
        return topRight;
    }

    /**
     * Returns the bottom left child subtree of this tree's root node
     *
     * @return the bottom left child subtree of this tree's root node
     */
    public PointQuadTree<T, V> getBottomLeft() {
        return bottomLeft;
    }

    /**
     * Returns the bottom right child subtree of this tree's root node
     *
     * @return the bottom right child subtree of this tree's root node
     */
    public PointQuadTree<T, V> getBottomRight() {
        return bottomRight;
    }

    /**
     * Method 1: Traverses the tree to find the correct place to insert a node.
     * If the node is directly vertical with an existing node, it will be
     * considered as being to the right. If the node is directly horizontal with
     * and existing node, it will be considered as being above. If a node
     * already exists at the given coordinates, the instance of Coordinates for
     * the node is updated.
     *
     * @param coords The coordinates of the node
     */
    public void insert(Coordinates<T, V> coords) {
        T x = coords.getX();
        T y = coords.getY();
        // Keep track of the current node when traversing the tree
        PointQuadTree<T, V> node = this;
        while (true) {
            if (x.compareTo(node.root.getX()) < 0) {
                // If the coordinates are to the bottom left of the current node
                if (y.compareTo(node.root.getY()) < 0) {
                    // If the node doesn't have a bottom left child, insert
                    if (node.getBottomLeft() == null) {
                        node.setBottomLeft(new PointQuadTree<>(coords));
                        return;
                    // If the node does have a bottom left child, move to it
                    } else {
                        node = node.getBottomLeft();
                    }
                // Left / top left
                } else {
                    if (node.getTopLeft() == null) {
                        node.setTopLeft(new PointQuadTree<>(coords));
                        return;
                    } else {
                        node = node.getTopLeft();
                    }
                }
            } else {
                // Bottom / bottom right
                if (y.compareTo(node.root.getY()) < 0) {
                    if (node.getBottomRight() == null) {
                        node.setBottomRight(new PointQuadTree<>(coords));
                        return;
                    } else {
                        node = node.getBottomRight();
                    }
                // Right / top / top right
                } else if (y.compareTo(node.root.getY()) > 0) {
                    if (node.getTopRight() == null) {
                        node.setTopRight(new PointQuadTree<>(coords));
                        return;
                    } else {
                        node = node.getTopRight();
                    }
                    // If there is already a node in this position, update its
                    // value
                } else {
                    node.setRoot(coords);
                    return;
                }
            }
        }
    }

    /**
     * Method 2: remove and return a given value from the tree. The root of the
     * tree cannot be removed, and all descendants of the removed node are
     * reassigned to be children of other nodes
     *
     * @param coords The coordinates to search for
     * @return The deleted node, or null if there is none, or the node being
     *         deleted is the root of the tree
     */
    public Coordinates<T, V> pop(Coordinates<T, V> coords) {
        T x = coords.getX();
        T y = coords.getY();
        PointQuadTree<T, V> node = this;
        // Represent the current node's relationship to it's parent (ie bl for
        // bottom left, br for bottom right, tl for top left, tr for top right)
        String parentRelationship = "";
        // Traverse the tree similarly to insert
        while (true) {
            if (x.compareTo(node.root.getX()) < 0) {
                if (y.compareTo(node.root.getY()) < 0) {
                    // If the node is not in the tree (in the expected position)
                    // then pop is unsuccessful
                    if (node.getBottomLeft() == null) {
                        return null;
                    // Go to the next node, and set parentRelationship
                    // accordingly
                    } else {
                        node = node.getBottomLeft();
                        parentRelationship = "bl";
                    }
                } else {
                    if (node.getTopLeft() == null) {
                        return null;
                    } else {
                        node = node.getTopLeft();
                        parentRelationship = "tl";
                    }
                }
            } else {
                if (y.compareTo(node.root.getY()) < 0) {
                    if (node.getBottomRight() == null) {
                        return null;
                    } else {
                        node = node.getBottomRight();
                        parentRelationship = "br";
                    }
                } else if (y.compareTo(node.root.getY()) > 0) {
                    if (node.getTopRight() == null) {
                        return null;
                    } else {
                        node = node.getTopRight();
                        parentRelationship = "tr";
                    }
                // If the node has been found
                } else {
                    // Delete and return the coordinates of the node
                    return node.deleteNode(this, parentRelationship);
                }
            }
        }
    }

    /* Delete this node from a tree, and insert all of its ancestors back into
    * the tree */
    private Coordinates<T, V> deleteNode(PointQuadTree<T, V> root,
            String parentRelationship) {
        switch (parentRelationship) {
            case "bl":
                this.getParent().setBottomLeft(null);
                break;
            case "br":
                this.getParent().setBottomRight(null);
                break;
            case "tl":
                this.getParent().setTopLeft(null);
                break;
            case "tr":
                this.getParent().setTopRight(null);
                break;
            default:
                // Root of tree - don't delete
                return null;
        }
        // Put the descendants of the node back into the tree
        Iterator<Coordinates<T, V>> descendants = new TreeIterator<>(this);
        while (descendants.hasNext()) {
            Coordinates<T, V> descendant = descendants.next();
            if (descendant == this.getRoot()) {
                continue;
            }
            root.insert(descendant);
        }
        // Return the coordinates of the node
        return this.getRoot();
    }
}
