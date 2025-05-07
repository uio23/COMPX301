import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class REsearch {
	// Define set of dynamically sized arrays for FSM representation
	private static ArrayList<String> type = new ArrayList<String>();
	private static ArrayList<Integer> next1 = new ArrayList<Integer>();
	private static ArrayList<Integer> next2 = new ArrayList<Integer>();
	private static ArrayList<Boolean> visited = new ArrayList<Boolean>();

	public static void main(String[] args) {
		// Verfiy a single argument is provided
		if (args.length < 1 || args.length > 1) {
			System.err.println("Usage: java REsearch filename.txt");
			System.exit(1);
		}
		// ...it is the filename
		String filename = args[0];

		// populate the FSM arrays by reading FSM specification from standard in
		readFSM();

		System.out.println(type);
		System.out.println(next1);
		System.out.println(next2);
		System.out.println(visited);
	}

	private static void readFSM() {
		String line;
		String[] values;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
			// Every line is a column of the FSM set of arrays
			line = reader.readLine();	
			while (line != null) {
				// Add values from line to their corresponding arrays
				values = line.split(",");
				type.add(values[1]);
				next1.add(Integer.valueOf(values[2]));
				next2.add(Integer.valueOf(values[3]));
				visited.add(false);

				line = reader.readLine();
			}

		} catch (IOException e) {
			System.out.printf("Error reading FSM from standrad in: %s", e.getMessage());
		}
	}
}


/**
 * A dequeue implementation that stores positive integers split by a SCAN value of -1.
 * The values are stored in doubly linked <code>Node</code> instances. There is always 
 * at least one <code>Node</code> in this dequeue, that stores the SCAN value (-1),
 * and it is restored to the bottom of the dequeue when popped off.
 * 
 * @author Oleksandr Kashpir ID:1637705
 */
class DequeueWithSCAN {
	// Track of head for push/pop and tail for enqueue
	Node head, tail;

	public DequeueWithSCAN() {
		// Insert SCAN node with the negative value -1
		head = new Node(-1, null, null);
		tail = head;
	}

	// TODO: Delete method
	public void disp() {
		while (tail != null) {
			System.out.print(tail.value + "->");
			tail = tail.next;
		}
		System.out.println();
	}

	/**
	 * Pushes a new <code>Node</code> with the passed value on to
	 * the top of this dequeue, if the value is positive.
	 * 
	 * @param value  the positive integer value to push
	 */
	public void push(int value) {
		// Verify value is positive
		if (value < 0) {
			return;
		}

		// The new node will be the head so it has no next node
		Node newNode = new Node(value, head, null);
		// Make node new head node
		head.next = newNode;
		head = newNode;
	}

	/**
	 * Enqueues a new <code>Node</code> with the passed value to 
	 * the end of this dequeue, if the value is positive.
	 * 
	 * @param value  the positive integer value to enqueue
	 */
	public void enqueue(int value) {
		// Verify value is positive
		if (value < 0) {
			return;
		}

		// The new Node will be the tail so it has no previous node
		Node newNode = new Node(value, null, tail);
		// Make node the new tail node
		tail.prev = newNode;
		tail = newNode;
	}

	/**
	 * Deletes the top <code>Node</code> of this dequeue and returns
	 * its value. Replaces SCAN node before returning its value (-1).
	 * 
	 * @return  the value of the top <code>Node</code> of this dequeue
	 */
	public int pop(){
		// Store head value and forget its pointer
		int value = head.value;
		head = head.prev;

		// If scan value is popped, add it to the bottom of the dequeue
		if (value == -1) {
			tail = new Node(-1, null, tail);

			// If this is now the only node in the dequeue, it should also be the head
			if (head == null) {
				head = tail;
			}
		}
		// If the dequeue had more than one node, the new new head is still pointing to the old head
		// Remove this pointer
		head.next = null;

		return value;
	}

	/**
	 * A doubly linked node that stores an integer value, and pointers to a the previous and next <code>Node</code>.
	 */
	private class Node {
		int value;
		Node next;
		Node prev;

		public Node(int value, Node prev, Node next) {
			this.value = value;
			this.next = next;
			this.prev = prev;
		}
	}
}
