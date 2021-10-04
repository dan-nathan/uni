import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class TreeTest {

    @Test(timeout=1000)
    public void testHasNext() {
        BinaryTree<Integer> root = new BinaryTree<>(1);

        Iterator<Integer> iter = root.iterator();
        assertTrue(iter.hasNext());
        iter.next();
        assertFalse(iter.hasNext());
    }

    @Test(timeout=1000)
    public void testNext() {
        BinaryTree<Integer> root = new BinaryTree<>(1);
        BinaryTree<Integer> left = new BinaryTree<>(2);
        BinaryTree<Integer> leftLeft = new BinaryTree<>(3);

        root.setLeft(left);
        left.setLeft(leftLeft);

        Iterator<Integer> iter = root.iterator();
        for (int i = 1; i <= 3; i++) {
            assertEquals(i, (int)iter.next());
        }
    }

    @Test(timeout=1000)
    public void testIsBST() {
        BinaryTree<Integer> tree = new BinaryTree<>(2);
        BinaryTree<Integer> left = new BinaryTree<>(1);
        BinaryTree<Integer> right = new BinaryTree<>(3);

        tree.setLeft(left);
        tree.setRight(right);
        left.setLeft(new BinaryTree<>(null));
        left.setRight(new BinaryTree<>(null));
        right.setLeft(new BinaryTree<>(null));
        right.setRight(new BinaryTree<>(null));
        assertTrue(BinaryTree.isBST(tree));
    }
}
