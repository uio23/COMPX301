import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * A regular expression searcher that builds a FSM by reading it states as lines from
 * standard in, and searches through the lines of a specified file for matches with
 * the regular expression specified in the FSM, outputing each line that has such a
 * match to standard out.
 *
 * @author Oleksandr Kashpir ID:1637705
 */
public class REsearch {
	private static String filename;
	private static String[] lines;
	private static FSM fsm;

	private static final int STATE_ZERO = 0;

	/**
	 * Verifies that an argument has been passed and takes it as the <code>filename</code>.
	 * By making function calls, performs the following steps:
	 * 	1. Instantiates the <code>fsm</code> by reading states from standard in
	 * 	2. Reads lines of the file specified by the filename into <code>lines</code>
	 * 	3. Outputs all of these lines that match the regexp represented in the <code>fsm</code>
	 *
	 * 	@param args  the command-line arguments passed to this program
	 */
	public static void main(String[] args) {
		// Verfiy filename is provided
		if (args.length < 1 || args.length > 1) {
			System.err.println("Usage: java REsearch filename.txt");
			System.exit(1);
		}
		filename = args[0];

		// Make use of class fields to avoid passing around variables
		loadFSM();
		readFile();
		outputMatchLines();
	}

	/**
	 * Creates a new <code>FSM</code> instance and populates it with states specified
	 * by the lines of standard in, with the format: 
	 * state number, state character, first next state, second next state.
	 */
	private static void loadFSM() {
		String line;
		String[] values;

		int stateN, next1, next2;
		String ch;

		fsm = new FSM();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
			// Every line is a column in the FSM set of arrays
			line = reader.readLine();	
			while (line != null) {
				// Unpack the line
				values = line.split(",");
				stateN = Integer.valueOf(values[0]);
				ch = values[1];
				next1 = Integer.valueOf(values[2]);
				next2 = Integer.valueOf(values[3]);

				// Add values from line in a new state
				fsm.insertState(stateN, ch, next1, next2);

				line = reader.readLine();
			}

		} catch (IOException e) {
			System.out.printf("Error reading FSM from standrad in: %s", e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Reads the lines of a file specified by the filename passed to this program,
	 * and stores them in a class field
	 */
	private static void readFile() {
		ArrayList<String> linesList = new ArrayList<String>();

		// BufferedReader will close after try block
		try(BufferedReader reader = new BufferedReader(new FileReader(filename))) {
			String line = reader.readLine();

			// Read all the lines into an ArrayList
			while (line != null) {
				linesList.add(line);	
				line = reader.readLine();
			}
		}
		catch (IOException error) {
			System.err.printf("Error reading from file: %s", error.getMessage());
			// Don't necessarily have to exit if some lines fail to read
		}

		// Convert and store lines ArrayList as an array of Strings
		lines = linesList.toArray(new String[0]);
	}

	/**
	 * Searches every line in the lines read from the specified file for a
	 * match with the regexp represented in the <code>fsm</code>, and outputs
	 * every line that matches to standard out.
	 */
	private static void outputMatchLines() {
		int base;
		boolean fsmTraversed;

		for (String line : lines) {
			base = 0;
			fsmTraversed = false;

			// Try traverse the fsm at an incrementing index in this line
			// until the line runs out
			do {
				fsmTraversed = traverseFSM(line, base);

				base++;
				if (base >= line.length()) {
					break;
				}
			}
			while (!fsmTraversed);

			// If the while loop exited on a successful traversal,
			// output this line
			if (fsmTraversed) {
				System.out.println(line);
			}
		}
	}

	/**
	 * Attempts to find a path through the <code>fsm</code> given a sequence
	 * of characters and a base in it. Uses a custom Deque for this algorythm.
	 *
	 * @return  true if <code>fsm</code> could be traversed from the specified base;
	 * 					false otherwise
	 */
	private static boolean traverseFSM(String string, int base) {
		String ch;
		int next1, next2;
		int loc;
		// Point starts at base
		int point = base;

		DequeWithSCAN dque = new DequeWithSCAN();
		// Track visited states in an array with a boolean for
		// each state in the fsm
		// No states are visited to start with 
		boolean[] visited = new boolean[fsm.size()];

		// Start at the zero state
		dque.push(STATE_ZERO);
		while (true) {
			// Get a possible current state
			loc = dque.pop();

			// If no unconcidered possible current states exist
			if (loc == -1) {
				// Possible next states are the new possible current states

				// If there are no new possible current states, 
				// the character could not be consumed, 
				// so there is no way through and this is failure
				if (dque.size() <= 0) {
					return false;
				}

				// Consume character and point to next one
				point++;

				// If the string runs out, there is no next character to 
				// consider the new possible current states for,
				// so there is no way through and this is failure
				if (point >= string.length()) {
					return false;
				}

				// Reset visited states for next character
				visited = new boolean[fsm.size()];

				// Consider new possible current states for new character
				continue;
			}

			// If this state was already visited as a possible current state
			// do not consider it again
			if (visited[loc] == true) {
				continue;
			}

			// Otherwise, consider this state
			visited[loc] = true;
			ch = fsm.getCh(loc);
			next1 = fsm.getNext1(loc);
			next2 = fsm.getNext2(loc);

			// If its a branch state, push on where we could be instead
			if (ch.equals("BR")) {
				dque.push(next1);

				// Only push on next2 if its a different state number
				if (next1 != next2) {
					dque.push(next2);
				}
			}
			// Otherwise, match wildcard or try match literal
			else if (ch.equals("WC") || ch.charAt(0) == string.charAt(point)) {
				// If its a match, a possible next state is this state's next state

				// If that's the final state, 
				// we can reach the final state in the passed string
				// starting at the passed base,
				// and this is success
				if (fsm.isFinal(next1)) {
					return true;
				}
				
				// Otherwise add it as a possible next state
				// (next1 and next2 are the same for a literal because its not a branching state)
				dque.enqueue(next1);
			}
		}
	}
}


/**
 * A deque implementation that stores positive integers split by a SCAN value of -1.
 * The values are stored in doubly linked <code>Node</code> instances. There is always 
 * at least one <code>Node</code> in this deque, that stores the SCAN value (-1),
 * and it is restored to the tail of the deque when popped off.
 * 
 * @author Oleksandr Kashpir ID:1637705
 */
class DequeWithSCAN{
	// Track head for push/pop and tail for enqueue
	Node head, tail;
	int nodeCounter;

	public DequeWithSCAN() {
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
	 * Pushes a new <code>Node</code> containing the passed <code>value</code> 
	 * on to the top of this deque, if the value is positive.
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
	 * Enqueues a new <code>Node</code> containing the passed <code>value</code>
	 * to the end of this deque, if the value is positive.
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
	 * Deletes the top <code>Node</code> of this deque and returns
	 * its value. Replaces SCAN node before returning its value (-1).
	 * 
	 * @return  the value of the top <code>Node</code> of this dequeue
	 */
	public int pop(){
		// Store head value and forget its pointer
		int value = head.value;
		head = head.prev;

		// If scan node is popped, reinstatiate it at the end of the deque
		if (value == -1) {
			Node b = new Node(-1, null, tail);
			tail.prev = b;
			tail = b;

			// If this is now the only node in the deque, it should also be the head
			if (head == null) {
				head = tail;
			}
		}
		// Reduce size of deque if something other than the scan node is popped
		else {
			nodeCounter--;
		}

		// If the deque had more than one node, the new new head is still pointing to the old head
		// Remove this pointer
		head.next = null;

		return value;
	}

	/**
	 * Returns the number of positive integers stored in this dque
	 *
	 * @return  the number of positive integers stored in this deque
	 */
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


/**
 * A finite state machine implementation for regular expressions which stores 
 * its literal/branching states in a series of <code>ArrayList</code>s.
 *
 * @author Oleksandr Kashpir ID:1637705
 */
class FSM {
	// Define set of dynamically sized arrays for FSM representation
	private ArrayList<String> chArr;
	private ArrayList<Integer> next1Arr = new ArrayList<Integer>();
	private ArrayList<Integer> next2Arr = new ArrayList<Integer>();

	/**
	 * Initializes a new instance of <code>FSM</code> and intializes
	 * the <code>ArrayList</code>s for its states.
	 */
	public FSM() {
		chArr =  new ArrayList<String>();
		next1Arr = new ArrayList<Integer>();
		next2Arr = new ArrayList<Integer>();
	}
	
	/**
	 * Inserts a new state at <code>stateN</code> by storing its values
	 * in the respective underlying <code>ArrayList</code>s.
	 *
	 * @param stateN  index to insert state into
	 * @param ch      literal/special of the state
	 * @param next1   first next possible state
	 * @param next2   second next possible state
	 */
	public void insertState(int stateN, String ch, int next1, int next2) {
		chArr.add(stateN, ch);
		next1Arr.add(stateN, next1);
		next2Arr.add(stateN, next2);
	}

	/**
	 * Returns the literal/special <code>String</code> of the state at <code>stateN</code>.
	 *
	 * @param stateN  index of state to consider
	 * @return        the literal/special of the state
	 */
	public String getCh(int stateN) {
		return chArr.get(stateN);
	}

	/**
	 * Returns the first next possible state of the state at <code>stateN</code>.
	 *
	 * @param stateN  index of state to consider
	 * @return        the first next possible state
	 */
	public int getNext1(int stateN) {
		return next1Arr.get(stateN);
	}

	/**
	 * Returns the second next possible state of the state at <code>stateN</code>.
	 *
	 * @param stateN  index of state to consider
	 * @return        the second next possible state
	 */
	public int getNext2(int stateN) {
		return next2Arr.get(stateN);
	}

	/**
	 * Returns the number of states in this fsm 
	 *
	 * @return  the number of states in this fsm
	 */
	public int size() {
		return chArr.size();
	}

	/**
	 * Checks whether the passed state number is -1, which would indicate it is the final state.
	 *
	 * @return  true if the passed state number is -1;
	 *  				false otherwise
	 */
	public boolean isFinal(int stateN) {
		return stateN == -1;
	}
}
