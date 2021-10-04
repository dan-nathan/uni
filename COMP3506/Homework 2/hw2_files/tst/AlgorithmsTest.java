import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class AlgorithmsTest {

    @Test(timeout=1000)
    public void testSortQueueExample1() {
        Queue<Integer> queue = new LinkedList<>();
        queue.add(1);
        queue.add(3);
        queue.add(5);
        queue.add(4);
        queue.add(2);
        Algorithms.sortQueue(queue);
        assertEquals(1, (int)queue.remove());
        assertEquals(2, (int)queue.remove());
        assertEquals(3, (int)queue.remove());
        assertEquals(4, (int)queue.remove());
        assertEquals(5, (int)queue.remove());
        assertEquals(0, queue.size());
    }

    @Test(timeout=1000)
    public void testSortQueueExample2() {
        Queue<String> queue = new LinkedList<>();
        queue.add("a");
        queue.add("b");
        queue.add("c");
        queue.add("b");
        queue.add("a");
        Algorithms.sortQueue(queue);
        assertEquals("a", queue.remove());
        assertEquals("a", queue.remove());
        assertEquals("b", queue.remove());
        assertEquals("b", queue.remove());
        assertEquals("c", queue.remove());
    }

    @Test(timeout=10000)
    public void testSortQueueExample3() {
        Random random = new Random();
        for (int j = 0; j < 5000; j++) {
            Queue<Integer> queue = new LinkedList<>();
            List<Integer> nums = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                nums.add(i);
            }
            List<Integer> nums2 = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                int rand = random.nextInt(100 - i);
                queue.add(nums.get(rand));
                nums.remove(rand);
            }
            Algorithms.sortQueue(queue);
            for (int i = 0; i < 100; i++) {
                assertEquals((long) i, (long) queue.remove());
            }
        }

    }

    @Test(timeout=1000)
    public void testFindMissingNumberExample1() {
        int[] arr = {2, 4, 6, 8, 10, 12, 14, 18, 20};
        assertEquals(16, Algorithms.findMissingNumber(arr));
    }

    @Test(timeout=1000)
    public void testFindMissingNumberExample2() {
        int[] arr = {4, 1, -5};
        assertEquals(-2, Algorithms.findMissingNumber(arr));
    }
    @Test(timeout=1000)
    public void testFindMissingNumberExample4() {
        int[] arr = {4, 1, -5, -8};
        assertEquals(-2, Algorithms.findMissingNumber(arr));
    }
    @Test(timeout=1000)
    public void testFindMissingNumberExample5() {
        int[] arr = {1, 5};
        assertEquals(3, Algorithms.findMissingNumber(arr));
    }

    @Test(timeout=10000)
    public void testFindMissingNumberExample3() {
        Random random = new Random();
        for (int i = 0; i < 50000; i++) {
            int diff = random.nextInt(100) - 50;
            if (diff == 0) {
                continue;
            }
            int start = random.nextInt(10000) - 5000;
            List<Integer> nums = new ArrayList<>();
            for (int j = 0; j < random.nextInt(5) + 10; j++) {
                nums.add(start + j * diff);
            }
            int index = random.nextInt(nums.size() - 2) + 1;
            int expected = nums.get(index);
            nums.remove(index);
            int[] arr = new int[nums.size()];
            //System.out.println("\n\n\n\n\n\n\n\n\nLength" + arr.length);

            for (int j = 0; j < nums.size(); j++) {
                arr[j] = nums.get(j);
                //System.out.println(arr[j]);
            }
            assertEquals(expected, Algorithms.findMissingNumber(arr));
            //System.out.println(Algorithms.findMissingNumber(arr));
        }
    }

}
