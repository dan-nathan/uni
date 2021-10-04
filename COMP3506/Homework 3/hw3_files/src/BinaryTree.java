import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A binary tree, where each node contains at most two children
 * Each root node contains a value and references to its left and right children (if they exist)
 *
 * @param <E> the type of the tree's elements
 */
public class BinaryTree<E> implements Tree<E> {
    private E root; // the element sitting at this tree's root node
    private BinaryTree<E> left; // the left child (subtree)
    private BinaryTree<E> right; // the right child (subtree)

    /**
     * Constructs a new binary tree with a single node
     *
     * @param root the element to store at this tree's root
     */
    public BinaryTree(E root) {
        this.root = root;
        this.left = null;
        this.right = null;
    }

    @Override
    public int size() {
        return 1 + (left != null ? left.size() : 0) + (right != null ? right.size() : 0);
    }

    @Override
    public E getRoot() {
        return root;
    }

    @Override
    public boolean isLeaf() {
        return left == null && right == null;
    }

    @Override
    public List<Tree<E>> getChildren() {
        List<Tree<E>> children = new ArrayList<>();
        if (left != null) {
            children.add(left);
        }
        if (right != null) {
            children.add(right);
        }
        return children;
    }

    @Override
    public boolean contains(E elem) {
        if (root.equals(elem)) {
            return true;
        }
        return (left != null && left.contains(elem)) || (right != null && right.contains(elem));
    }

    @Override
    public Iterator<E> iterator() {
        return new TreeIterator<>(this);
    }

    /**
     * Sets the left child of this tree's root to the given subtree
     * Any existing left child will be overridden
     *
     * @param left the new left child
     */
    public void setLeft(BinaryTree<E> left) {
        this.left = left;
    }

    /**
     * Sets the right child of this tree's root to the given subtree
     * Any existing right child will be overridden
     *
     * @param right the new right child
     */
    public void setRight(BinaryTree<E> right) {
        this.right = right;
    }

    /**
     * @return the left child subtree of this tree's root node
     */
    public BinaryTree<E> getLeft() {
        return left;
    }

    /**
     * @return the right child subtree of this tree's root node
     */
    public BinaryTree<E> getRight() {
        return right;
    }

    /**
     * Determines whether the parameter tree is a binary search tree or not
     * This is determined by the definition of a binary search tree provided in the lectures
     *
     * every line of inOrderTraverse runs in constant time except for the
     * recursive calls. The method will end up running once for each node in the
     * tree, making it O(n) where n is the size of the tree. In isBST, all
     * operations are O(1) apart from inOrderTraverse and the for loop, which
     * runs n - 1 times. This makes the time complexity of the method in the
     * worst case (the tree is a BST, and all elements are checked) O(n).
     *
     * @param tree the tree to check
     * @return true if this tree is a BST, otherwise false
     */
    public static <T extends Comparable<T>> boolean isBST(BinaryTree<T> tree) {
        // Get a list of elements in the tree according to an in order traversal
        List<T> inOrderElements = new ArrayList<>();
        tree.inOrderTraverse(inOrderElements);
        // Return false if elements are not in increasing order
        for (int i = 0; i < inOrderElements.size() - 1; i++) {
            if (inOrderElements.get(i)
                    .compareTo(inOrderElements.get(i + 1)) > 0) {
                return false;
            }
        }
        return true;
    }

    /* Does an in order traversal of the tree, and adds elements to a list */
    private void inOrderTraverse(List<E> elements) {
        // Visit the left node if it isn't null
        if (this.getLeft() != null && this.getLeft().getRoot() != null) {
            this.getLeft().inOrderTraverse(elements);
        }
        // Add this node's value to the list of elements
        elements.add(this.getRoot());
        // Visit the right node if it isn't null
        if (this.getRight() != null && this.getRight().getRoot() != null) {
            this.getRight().inOrderTraverse(elements);
        }
    }
}
