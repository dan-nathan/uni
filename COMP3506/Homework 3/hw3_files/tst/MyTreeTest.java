import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class MyTreeTest {

    @Test
    public void insertTest() {
        Coordinates<Float, String> coords1 = new Coordinates<>(0f, 0f, "a");
        // Top right of a
        Coordinates<Float, String> coords2 = new Coordinates<>(2f, 2f, "b");
        // Overrides b
        Coordinates<Float, String> coords3 = new Coordinates<>(2f, 2f, "c");
        // Bottom left of c
        Coordinates<Float, String> coords4 = new Coordinates<>(1f, 1f, "d");
        // Bottom right of d
        Coordinates<Float, String> coords5 = new Coordinates<>(1.5f, 0.5f, "e");
        // Top left of c
        Coordinates<Float, String> coords6 = new Coordinates<>(1.5f, 3f, "f");
        // Directly left of d (should be put as top left)
        Coordinates<Float, String> coords7 = new Coordinates<>(0.5f, 1f, "g");
        // Directly down from c (should be put as bottom right)
        Coordinates<Float, String> coords8 = new Coordinates<>(2f, 1.5f, "h");
        PointQuadTree<Float, String> tree = new PointQuadTree<>(coords1);
        tree.insert(coords2);
        tree.insert(coords3);
        tree.insert(coords4);
        tree.insert(coords5);
        tree.insert(coords6);
        tree.insert(coords7);
        tree.insert(coords8);

        assertEquals(7, tree.size());
        assertEquals("a", tree.getRoot().getValue());
        // Check updated value and top right
        assertEquals("c", tree.getTopRight().getRoot().getValue());
        // Check bottom left
        assertEquals("d", tree.getTopRight().getBottomLeft().getRoot().getValue());
        // Check bottom right
        assertEquals("e", tree.getTopRight().getBottomLeft().getBottomRight().getRoot().getValue());
        // Check top left
        assertEquals("f", tree.getTopRight().getTopLeft().getRoot().getValue());
        // Check directly horizontal
        assertEquals("g", tree.getTopRight().getBottomLeft().getTopLeft().getRoot().getValue());
        // Check directly vertical
        assertEquals("h", tree.getTopRight().getBottomRight().getRoot().getValue());

    }

    @Test
    public void deleteTest() {
        // Root
        Coordinates<Float, String> coords1 = new Coordinates<>(0f, 0f, "a");
        // Top right of a
        Coordinates<Float, String> coords2 = new Coordinates<>(2f, 2f, "b");
        // Top left of a
        Coordinates<Float, String> coords3 = new Coordinates<>(-2f, 2f, "c");
        // Bottom left of b
        Coordinates<Float, String> coords4 = new Coordinates<>(1f, 1f, "d");
        // Bottom right of d
        Coordinates<Float, String> coords5 = new Coordinates<>(1.5f, 0.5f, "e");
        // Top left of b
        Coordinates<Float, String> coords6 = new Coordinates<>(1.5f, 3f, "f");
        PointQuadTree<Float, String> tree = new PointQuadTree<>(coords1);
        tree.insert(coords2);
        tree.insert(coords3);
        tree.insert(coords4);
        tree.insert(coords5);
        tree.insert(coords6);

        // Test with an entry that isn't in the tree
        Coordinates<Float, String> poppedCoords = tree.pop(new Coordinates<>(-1f, -1f, "z"));
        assertNull(poppedCoords);
        assertEquals(6, tree.size());

        // Test with a node that has 2 children, and a grandchild
        poppedCoords = tree.pop(coords2);
        assertEquals(5, tree.size());
        assertEquals("b", poppedCoords.getValue());
        Iterator<Coordinates<Float, String>> nodes = new TreeIterator<>(tree);
        while (nodes.hasNext()) {
            assertNotEquals("b", nodes.next().getValue());
        }

        // Test that the root is not affected by pop
        poppedCoords = tree.pop(coords1);
        assertEquals(5, tree.size());
        assertNull(poppedCoords);

        // Test removing leaf
        poppedCoords = tree.pop(coords3);
        assertEquals(4, tree.size());
        assertEquals("c", poppedCoords.getValue());
        nodes = new TreeIterator<>(tree);
        while (nodes.hasNext()) {
            assertNotEquals("c", nodes.next().getValue());
        }

    }
}
