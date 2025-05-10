import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class REsearch {
	private static String filename;
	private static String[] lines;
	private static FSM fsm = new FSM();

	public static void main(String[] args) {
		// Verfiy a single argument is provided
		if (args.length < 1 || args.length > 1) {
			System.err.println("Usage: java REsearch filename.txt");
			System.exit(1);
		}
		// ...it is the filename
		filename = args[0];

		// populate the FSM arrays by reading FSM specification from standard in
		readFSM();
		readFile();
		outputMatchLines();
	}

	private static void outputMatchLines() {
		int base;
		// For every line in the file
		for (String line : lines) {
			base = 0;
			// Traverse the FSM from every character in the line
			// until the FSM is fully traversed starting at some character
			// or the line ends
			while(!traverseFSM(line, base)) {
				base++;
				if (base >= line.length()) {
					break;
				}
			}
		}
	}

	private static boolean traverseFSM(String string, int step) {
		int next1, next2;
		String ch;
		int loc;

		DequeueWithSCAN dque = new DequeueWithSCAN();
		// No states are visited to start with 
		boolean[] visited = new boolean[fsm.size()];

		// Start at the zero state
		dque.push(0);
		while (true) {
			// Get a possible current state
			loc = dque.pop();

			// If one does not exist
			if (loc == -1) {
				// If there are no possible next states, the character
				// cannot be consumed and so we failed
				if (dque.size() <= 0) {
					return false;
				}

				// Consume character
				step++;

				// If the string runs out, we cannot consider the possible next
				// states, so we failed
				if (step >= string.length()) {
					return false;
				}

				// Reset visited states for next character
				visited = new boolean[fsm.size()];

				// Consider the possible next states as the possible current states
				continue;
			}

			// If we already considered this state, 
			// do not consider it again
			if (visited[loc] == true) {
				continue;
			}

			// Otherwise, ee are visiting this state
			visited[loc] = true;
			next1 = fsm.getNext1(loc);
			next2 = fsm.getNext2(loc);
			ch = fsm.getCh(loc);;

			// If its a branch state, push on where we could be instead
			if (ch.equals("BR")) {
				dque.push(next1);

				// Only push on next2 if its a different state
				if (next1 != next2) {
					dque.push(next2);
				}
			}
			// Otherwise, match wildcard or try match literal
			else if (ch.equals("WC") || ch.charAt(0) == string.charAt(step)) {
				// If its a match, a possible next state is this state's next state

				// If that's the final state, output it and we are done
				if (fsm.isFinal(next1)) {
					System.out.println(string);
					return true;
				}
				
				// Otherwise add it as a possible next state
				// (next1 and next2 are the same for a literal bc its not a branching state)
				dque.enqueue(next1);
			}
		}
	}

	private static void readFile() {
		ArrayList<String> linesList = new ArrayList<String>();

		try(BufferedReader reader = new BufferedReader(new FileReader(filename))) {
			String line = reader.readLine();
			while (line != null) {
				linesList.add(line);	
				line = reader.readLine();
			}
		}
		catch (IOException error) {
			String errorMess = String.format("Error reading from file: %s", error.getMessage());
			System.err.println(errorMess);
			System.exit(1);
		}

		lines = linesList.toArray(new String[0]);
	}

	private static void readFSM() {
		String line;
		String[] values;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
			// Every line is a column of the FSM set of arrays
			line = reader.readLine();	
			for (int i = 0; line != null; i++, line = reader.readLine()) {
				values = line.split(",");
				// Add values from line to their corresponding arrays
				fsm.addState(i, values[1], Integer.valueOf(values[2]), Integer.valueOf(values[3]));
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
	int nodeCounter;

	public DequeueWithSCAN() {
		// Insert SCAN node with the negative value -1
		head = new Node(-1, null, null);
		tail = head;
		nodeCounter = 0;
	}

	// TODO: Delete method
	public void disp() {
		Node ogTail = tail;
		while (tail != null) {
			System.out.print(tail.value + "->");
			tail = tail.next;
		}
		tail = ogTail;
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
		nodeCounter++;
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
		nodeCounter++;
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
			Node b = new Node(-1, null, tail);
			tail.prev = b;
			tail = b;

			// If this is now the only node in the dequeue, it should also be the head
			if (head == null) {
				head = tail;
			}
		}
		// Reduce size of dequeue if a none negative value was popped
		else {
			nodeCounter--;
		}
		// If the dequeue had more than one node, the new new head is still pointing to the old head
		// Remove this pointer
		head.next = null;

		return value;
	}

	public int size() {
		return nodeCounter;
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

class FSM {
	// Define set of dynamically sized arrays for FSM representation
	private ArrayList<String> chArr;
	private ArrayList<Integer> next1Arr = new ArrayList<Integer>();
	private ArrayList<Integer> next2Arr = new ArrayList<Integer>();

	public FSM() {
		chArr =  new ArrayList<String>();
		next1Arr = new ArrayList<Integer>();
		next2Arr = new ArrayList<Integer>();
	}
	
	public void addState(int stateN, String ch, int next1, int next2) {
		chArr.add(stateN, ch);
		next1Arr.add(stateN, next1);
		next2Arr.add(stateN, next2);
	}

	public String getCh(int stateN) {
		return chArr.get(stateN);
	}

	public int getNext1(int stateN) {
		return next1Arr.get(stateN);
	}

	public int getNext2(int stateN) {
		return next2Arr.get(stateN);
	}

	public int size() {
		return chArr.size();
	}

	/**
	 * Final state is the next state after the states of the FSM
	 */
	public boolean isFinal(int stateN) {
		return stateN == chArr.size();
	}
}
