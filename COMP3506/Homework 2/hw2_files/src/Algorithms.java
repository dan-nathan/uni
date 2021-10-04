import java.util.Queue;

/**
 * Write your solution to this assignment in this class
 */
public class Algorithms {

    /**
     * Write your implementation of the sortQueue algorithm here
     *
     * @param queue the queue to sort
     */
    public static <T extends Comparable<T>> void sortQueue(Queue<T> queue) {
        T selected;
        // Iterating this once for each element in the queue (-1 time) is
        // guaranteed to have it sorted by the end (at least from my tests).
        for (int i = 0; i < queue.size() - 1; i++) {
            // Take the element from the front of the queue
            selected = queue.remove();
            // Use size instead of size - 1 as size has been reduced by remove
            for (int j = 0; j < queue.size(); j++) {
                T front = queue.remove();
                // If the front element is bigger, it should be behind the
                // selected element in the queue.
                if (front.compareTo(selected) > 0) {
                    queue.add(selected);
                    selected = front;
                } else {
                    queue.add(front);
                }
            }
            // Add what is now the selected element back to the back of the queue
            queue.add(selected);
        }
    }

    /**
     * Write your implementation of the findMissingNumber algorithm here
     *
     * @param numbers the arithmetic sequence
     * @return the missing number in the sequence
     */
    public static int findMissingNumber(int[] numbers) {
        return fMNRecursion(numbers, 0, numbers.length);
    }

    private static int fMNRecursion(int[] numbers, int start, int end) {
        // If there are two numbers, the missing one is inbetween them
        if (end - start == 2) {
            return (numbers[start] + numbers[end - 1]) / 2;
        }
        // Get the difference between the first two elements, and the difference
        // between the last two elements
        int difference1 = Math.abs(numbers[start + 1] - numbers[start]);
        int difference2 = Math.abs(numbers[end - 1] - numbers[end - 2]);
        // If the difference is bigger, then there must be a missing number
        // between them (assuming a valid input was given). This missing number
        // is the average of the two numbers for an arithmetic sequence.
        if (difference1 > difference2) {
            return (numbers[start + 1] + numbers[start]) / 2;
        } else if (difference2 > difference1) {
            return (numbers[end - 1] + numbers[end - 2]) / 2;
        } else {
            // Remove numbers on either end to narrow the search
            return fMNRecursion(numbers, start + 1, end - 1);
        }
    }

}
