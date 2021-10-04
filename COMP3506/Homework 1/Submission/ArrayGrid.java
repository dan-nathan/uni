/**
 * A 2D grid implemented as an array.
 * Each (x,y) coordinate can hold a single item of type <T>.
 *
 * @param <T> The type of element held in the data structure
 */
public class ArrayGrid<T> implements Grid<T> {

	private int width;

	private int height;

	private T[][] data;
	/**
	 * Constructs a new ArrayGrid object with a given width and height.
	 *
	 * @param width The width of the grid
	 * @param height The height of the grid
     * @throws IllegalArgumentException If the width or height is less than or equal to zero
	 */
	public ArrayGrid(int width, int height) throws IllegalArgumentException {
	    if (width <= 0 || height <= 0) {
	    	throw new IllegalArgumentException();
		}
		this.width = width;
	    this.height = height;

	    // Create a 2D array with generic type
	    this.data = (T[][]) new Object[width][height];
	}

	public void add(int x, int y, T element) throws IllegalArgumentException {
		if (x >= this.width || y >= this.height) {
			throw new IllegalArgumentException();
		}
		this.data[x][y] = element;
	}

	public T get(int x, int y) throws IndexOutOfBoundsException {
		if (x >= this.width || y >= this.height) {
			throw new IndexOutOfBoundsException();
		}
		return this.data[x][y];
	}

	public boolean remove(int x, int y) throws IndexOutOfBoundsException {
		if (x >= this.width || y >= this.height) {
			throw new IndexOutOfBoundsException();
		}
		if (this.data[x][y] == null) {
			return false;
		}
		this.data[x][y] = null;
		return true;
	}

	public void clear() {
		// Loop through each column and row
		for (int i = 0; i < this.width; i++) {
			for (int j = 0; j < this.height; j++) {
				this.data[i][j] = null;
			}
		}
	}

	public void resize(int newWidth, int newHeight) throws IllegalArgumentException {
		// Create a temporary clone of this.data to store the data while it is
		// being resized
		T[][] temp = this.data.clone();
		this.data = (T[][]) new Object[newWidth][newHeight];

		// Loop through old width and height when copying over instead of new
		// width and height to ensure any lost data is detected
		for (int i = 0; i < this.width; i++) {
			for (int j = 0; j < this.height; j++) {
				if (temp[i][j] != null) {
					// If there is a non-null cell outside the new dimensions
					if (i > newWidth || j > newHeight) {
						throw new IllegalArgumentException();
					}
					// Copy over cell
					this.data[i][j] = temp[i][j];
				}
			}
		}

		// Record the new dimensions
		this.width = newWidth;
		this.height = newHeight;
	}

}