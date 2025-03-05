import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class XSort {
	public static void main(String[] args) {
		int runLength = Integer.parseInt(args[0]);

		if (runLength > 1024) {
			System.out.println("Initial runs length passed too long. Must be between 64 and 1024");
			return;
		} else if (runLength < 64) {
			System.out.println("Initial runs length passed too short. Must be between 64 and 1024");
			return;
		}

		createInitialRuns(runLength);	
	}

	public static void createInitialRuns(int runLength) {
		String line;
		String[] run = new String[runLength];
		int actualRunLength = runLength;

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		try {
			// While there are lines coming in
			while (actualRunLength == runLength) {
				// Reset actual run length
				actualRunLength = 0;

				// While the run length hasn't been exceeded
				while (actualRunLength < runLength) {

					// Read the next line
					if ((line = reader.readLine()) != null) {
						// Append the current line to the current run
						run[actualRunLength] = line;
						// Increment the actual run length
						actualRunLength++;
					}
					else {
						// Once we read an empty line the stream is over
						break;
					}
				}
				// Heapsort complete run
				heapsort(run, actualRunLength);
				// Print it to standard out
				for (int i = 0; i < actualRunLength; i++) {
					System.out.println(run[i]);
				}
			}
		}
		catch (IOException error) {
			System.out.println("Error reading from standard input");
			System.out.println(error.getMessage());
		}
	}

	public static void upheap(String[] arr, int parentIndex, int heapLength) {
		int leftIndex;
		int rightIndex;
		int largestIndex = parentIndex;

		// Up to and including the top node
		while (parentIndex >= 0) {
			// Calc indicies of left and right children
			leftIndex = parentIndex*2+1;
			rightIndex = parentIndex*2+2;

			// If left child index out of bound
			if (leftIndex >= heapLength) {
				// ...this node is a leaf node that has nothing to upheap
				break;
			}

			// If the left child is larger than the current largest node (parent), it is the
			// new largest node
			if (arr[leftIndex].compareTo(arr[largestIndex]) > 0) {
				largestIndex = leftIndex;
			}
			// If the right child exists
			if (rightIndex < heapLength) {
				// If the right child is larger than the current largest node, it is the
				// new largest node
				if (arr[rightIndex].compareTo(arr[largestIndex]) > 0) {
					largestIndex = rightIndex;
				}
			}

			// If we changed the largest node from the parent
			if (largestIndex != parentIndex) {
				// Swap parent with largest child
				String parent = arr[parentIndex];
				arr[parentIndex] = arr[largestIndex];
				arr[largestIndex] = parent;
			}
			// Otherwise this node has nothing to upheap
			else {
				break;
			}
			// Upheap with the new parent
			parentIndex = largestIndex;
		}
	}

	public static void heapify(String[] arr, int heapLength) {
		// Starting at the last node that has children, upheap from every node going up
		for (int i = heapLength / 2; i >=0; i--) {
			upheap(arr, i, heapLength);	
		}
	}

	public static void heapsort(String[] arr, int heapLength) {
		String largest;

		// Heapify the array
		heapify(arr, heapLength);

		// While the heap part of the array has more than one node
		while (heapLength > 0) {
			// Swap the head node (max) with the last node in heap array
			largest = arr[0];
			arr[0] = arr[heapLength-1];
			arr[heapLength-1] = largest;
			// Reduce heap part of the array
			heapLength--;
			// Upheap from the new head node
			upheap(arr, 0, heapLength);
		}
	}

}
