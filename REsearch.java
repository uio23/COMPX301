// Oleksandr Kashpir ID:1637705
// Date last modified: 12/05/2025
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * A regular expression searcher that loads a FSM from standrad in and outputs to standard out
 * the lines from a specified file that contain a match for the regular expression represented 
 * in the FSM.
 *
 * @author Oleksandr Kashpir ID:1637705
 */
public class REsearch {
		// Make use of class fields to avoid passing around variables
	private static String filename;
	private static String[] lines;
	private static FSM fsm;
	private static final int STATE_ZERO = 0;
	private static final int SCAN = -2;

	/**
	 * Accepts a single filename argument. Through function calls:
	 * loads a FSM into <code>fsm</code>, reads lines from the specified file and
	 * outputs all of these lines with a match for the regular expression represented in <code>fsm</code>.
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

		loadFSM();
		readFile();
		outputMatchLines();
	}

	/**
	 * Creates a new <code>FSM</code> instance in <code>fsm</code> and populates it 
	 * with states specified by lines from standard in, with the format: 
	 * state number,state character,first next state,second next state.
	 */
	private static void loadFSM() {
		String line;
		String[] values;

		int stateN, next1, next2;
		String ch;

		fsm = new FSM();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
			// Every line is a state in the fsm
			line = reader.readLine();	
			while (line != null) {
				// Unpack the line
				values = line.split(",");

				stateN = Integer.valueOf(values[0]);
				ch = values[1];
				next1 = Integer.valueOf(values[2]);
				next2 = Integer.valueOf(values[3]);

				// Add values from line into a new state
				fsm.insertState(stateN, ch, next1, next2);

				line = reader.readLine();
			}

		} catch (IOException e) {
			System.out.printf("Error reading FSM from standrad in: %s", e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Reads the lines of a file specified by <code>filename</code>,
	 * and stores them in <code>lines</code>.
	 */
	private static void readFile() {
		ArrayList<String> linesList = new ArrayList<String>();

		try(BufferedReader reader = new BufferedReader(new FileReader(filename))) {
			String line = reader.readLine();

			// Read all the lines into an ArrayList
			while (line != null) {
				linesList.add(line);	
				line = reader.readLine();
			}
		}
		catch (IOException error) {
			// Don't necessarily have to exit if some lines fail to read
			System.err.printf("Error reading from file: %s", error.getMessage());
		}

		// Convert and store lines ArrayList as an array of Strings
		lines = linesList.toArray(new String[0]);
	}

	/**
	 * Searches <code>lines</code> for those that match with the regexp represented in <code>fsm</code>, 
	 * and outputs every line that matches to standard out.
	 */
	private static void outputMatchLines() {
		int base;
		boolean fsmTraversed;

		for (String line : lines) {
			base = 0;
			fsmTraversed = false;

			// Try traverse the fsm from an incrementing base in this line
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
	 * Attempts to find a path through the <code>fsm</code> given a <code>String</code>
	 * and a starting base in it. Uses a custom <code>DequeWithSCAN</code> for this algorythm.
	 *
	 * @param string  Sequence of plaintext characters to use for traversal
	 * @param base    starting index in <code>string</code>
	 * @return  			true if <code>fsm</code> could be traversed from the specified base;
	 * 								false otherwise
	 */
	private static boolean traverseFSM(String string, int base) {
		String ch;
		int next1, next2;
		int stateN;
		// Point starts at base
		int point = base;

		DequeWithSCAN deque = new DequeWithSCAN(SCAN);
		// Track visited states in an array with a boolean for
		// each state in the fsm
		// No states are visited to start with 
		boolean[] visited = new boolean[fsm.size()];

		// Start at the zero state
		deque.push(STATE_ZERO);
		while (true) {
			// Get a possible current state
			stateN = deque.pop();

			// If its the final state, success
			// ...because the fsm can be traversed from the given base in the given string.
			if (stateN == -1) {
				return true;		
			}

			// If the SCAN value was popped...
			if (stateN == SCAN) {
				// Possible next states are now the possible current states

				// If the deque has no Nodes but the SCAN node
				if (deque.size() <= 1) {
				// There are no new possible current states, so this is failure
				// ...because the character at point cannot be consumed in any valid way
					return false;
				}

				// Consume character
				point++;

				// Reset visited states for next character
				visited = new boolean[fsm.size()];

				// Consider new possible current states with next character
				continue;
			}

			// If this state was already visited as a possible current state
			// do not consider it again
			if (visited[stateN] == true) {
				continue;
			}

			// Consider this state
			visited[stateN] = true;
			ch = fsm.getCh(stateN);
			next1 = fsm.getNext1(stateN);
			next2 = fsm.getNext2(stateN);

			// If its a branch state, push on where we could be instead
			if (ch.equals("BR")) {
				deque.push(next1);

				// Only push on next2 if its a different state
				if (next1 != next2) {
					deque.push(next2);
				}
			}
			// Otherwise, provided that a character is available, match a wildcard or try match a literal
			else if (point < string.length()) {
				if (ch.equals("WC") || ch.charAt(0) == string.charAt(point)) {
					// If its a match, this state's next state (1 & 2 same for literal) is a possible next state
					deque.enqueue(next1);
				}
			}
		}
	}
}


/**
 * A deque implementation that stores integers split by a SCAN value.
 * The values are stored in doubly linked <code>Node</code> instances. There is always 
 * at least one <code>Node</code> in this deque, that stores the SCAN value,
 * and it is restored to the tail of the deque when popped off.
 * 
 * @author Oleksandr Kashpir ID:1637705
 */
class DequeWithSCAN{
	private Node head, tail;
	private int nodeCounter;
	private int SCAN;

	/**
	 * Initializes a new instance of <code>DequeWithSCAN</code>, and creates
	 * the SCAN <code>Node</code> with the specified SCAN value, making it the head and tail of
	 * the this deque
	 *
	 * @param scan  value to use for SCAN <code>Node</code>
	 */
	public DequeWithSCAN(int scan) {
		SCAN = scan;
		// Insert SCAN node with the scan value
		head = new Node(SCAN, null, null);
		tail = head;
		nodeCounter = 1;
	}

	/**
	 * Pushes a new <code>Node</code> containing the passed <code>value</code> 
	 * on to the top of this deque.
	 * The value must not be the reserved SCAN value of this deque, otherwise it will 
	 * not be pushed on.
	 * 
	 * @param value  the integer value to push
	 */
	public void push(int value) {
		// Verify value is not SCAN
		if (value == SCAN) {
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
	 * to the end of this deque.
	 * The value must not be the reserved SCAN value of this deque, otherwise it will 
	 * not be enqueued.
	 * 
	 * @param value  the integer value to enqueue
	 */
	public void enqueue(int value) {
		// Verify value is not SCAN
		if (value == SCAN) {
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
	 * Deletes the head <code>Node</code> of this deque and returns
	 * its value. Replaces SCAN node to the tail before returning its value.
	 * 
	 * @return  the value of the top <code>Node</code> of this deque
	 */
	public int pop(){
		// Store head value and forget its pointer
		int value = head.value;
		head = head.prev;

		// If scan node is popped, reinstatiate it at the tail of the deque
		if (value == SCAN) {
			Node b = new Node(SCAN, null, tail);
			tail.prev = b;
			tail = b;

			// If this is now the only node in the deque, it should also be the head
			if (head == null) {
				head = tail;
			}
		}
		// Decrease Node count if an actual value is popped instead
		else {
			nodeCounter--;
		}

		// If the deque had more than one node, the new new head is still pointing to the old head
		// Remove this pointer
		head.next = null;

		return value;
	}

	/**
	 * Returns the number of positive integers stored in this deque
	 *
	 * @return  the number of positive integers stored in this deque
	 */
	public int size() {
		return nodeCounter;
	}

	/**
	 * Returns the SCAN value this deque uses. 
	 * (Not needed by my implementation but just a thought)
	 *
	 * @return  the SCAN value this deque uses
	 */
	public int getSCAN() {
		return SCAN;
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
	 * @param ch      literal/special <code>String</code> of the state
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
