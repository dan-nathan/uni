import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

/**
 * An iterator for the tree ADT that performs a preorder traversal
 */
public class TreeIterator<E> implements Iterator<E> {

    // The nodes that still need to be iterated through
    private Stack<Tree<E>> nodesToTraverse;

    /**
     * Constructs a new tree iterator from the root of the tree to iterate over
     *
     * You are welcome to modify this constructor but cannot change its signature
     * This method should have O(1) time complexity
     */
    public TreeIterator(Tree<E> root) {
        // Add the root to a stack of nodes to traverse
        this.nodesToTraverse = new Stack<>();
        this.nodesToTraverse.add(root);
    }

    /**
     * Determines if there are any more nodes to iterate through.
     *
     * This method simply determines if a stack is empty, and returns. Both of
     * these are O(1), giving this method O(1) time complexity in the worst (and
     * average) case.
     *
     * @return whether or not there are any more nodes to iterate through
     */
    @Override
    public boolean hasNext() {
        return !nodesToTraverse.empty();
    }

    /**
     * Returns the next element of the preorder traversal of the tree.
     *
     * The initial check for hasNext being false contains only O(1) operations.
     * The two variable assignments for top and children, as well as the return
     * statement are O(1). Adding each child node of top is O(1), and is done
     * c times where c is the number of children the node has. This makes this
     * method O(c). Calling this method for all n nodes in a tree will run the
     * for loop n times in total, meaning for n calls of this method is O(n),
     * resulting an an amortized time complexity of O(1).
     *
     */
    @Override
    public E next() throws NoSuchElementException {

        // Throw an exception if this is called when there isn't a next element
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }

        // Get the top node from the stack
        Tree<E> top = nodesToTraverse.pop();
        List<Tree<E>> children = top.getChildren();

        // Add children to stack in reverse order (so leftmost child ends up at
        // the top)
        for (int i = 1; i <= children.size(); i++) {
            nodesToTraverse.add(children.get(children.size() - i));
        }

        // Return the value of the node
        return top.getRoot();
    }

}
