/**
 * An entry for a PointQuadTree that is comparable by two fields
 * @param <T> The type of the key of the entry. There will be two fields of this
 * @param <V> The value of the entry
 */
public class Coordinates<T extends Comparable<T>, V> {

    private T x;
    private T y;
    private V value;

    public Coordinates(T x, T y, V value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public T getX() {
        return this.x;
    }

    public T getY() {
        return this.y;
    }

    public V getValue() {
        return this.value;
    }
}
