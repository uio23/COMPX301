import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * A program that attempts to find a good solution to the NP-Complete box stacking problem under the touching faces and 
 * single-use constraints.
 * Touching faces: A cuboid may only go on top of another cuboid if its width and length are strictly smaller than those of 
 * the cuboid that it is going on top of.
 * Single-use: A box can only be present in the stack once, as a cuboid in the form of one of its rotations.
 *
 * This program uses dynamic programming to generate an initial stack from the boxes specified to it by a file,
 * and after removing duplicate rotations of a box by keeping the highest ones, it performs annealing on the 
 * stack using the specified initial temperature and cooling rate.
 *
 * @author Oleksandr Kashpir ID: 1637705
 */
public class NPCStack {
	/**
	 * Tries to maximise the height of a stack created from the boxes specified in a file, 
	 * under the touching faces and single-use constraint, through sequential function calls. 
	 *
	 * 1. After verifying the validity of command-line arguments, uses the specified file to initialise a <code>Stack</code> object.
	 * 2. Generates an initial stack with dynamic programming
	 * 3. Removes the shorter of rotational duplicates. 
	 * 4. Randomly changes some of the stack if <code>nerf</code> is specified.
	 * 5. Performs annealing on the stack to try find a better solution. 
	 * 6. Finally, displays the stack that is achieved after this annealing.
	 *
	 * @param args  the command-line arguments passed to this program
	 */
	public static void main(String[] args) {
		// Verify a valid number of command line arguments have been passed
		if (args.length < 3 || args.length > 4) {
			System.err.println("Usage: java NPCStack filename.txt initialTemp coolingRate [nerf]");
			System.exit(1);
		}

		String filename = args[0];

		// Try parse the numerical command line arguments, and exit the program if that fails
		int initialTemp = -1;
		double coolingRate = -1;
		try {
			initialTemp = Integer.parseInt(args[1]);
		}
		catch (NumberFormatException e) {
			System.err.println("Could not parse command line argument, initialTemp must be an integer");
			System.exit(1);
		}
		try {
			coolingRate = Double.parseDouble(args[2]);
		}
		catch (NumberFormatException e) {
			System.err.println("Could not parse command line argument, coolingRate must be a real number");
			System.exit(1);
		}

		// Initialize a stack with the cuboids of the boxes in the specified file
		Stack stack = loadBoxes(filename);

		// Ensure that the annealing parameters are withing their limits
		if (initialTemp <= 0 || initialTemp > stack.size / 3) {
			System.err.println("initialTemp must be 0 < initialTemp <= #boxes)");
			System.exit(1);
		}
		if (coolingRate < 0.1 || coolingRate > initialTemp) {
			System.err.println("coolingRate must be 0.1 <= coolingRate <= initialTemp");
			System.exit(1);
		}

		// Order the cuboids of the stack by there face areas
		stack.orderStack();
		// Select which cuboids to include in the initial stack, removing duplicates
		solveWithDP(stack);
		stack.removeDuplicates();

		// If the nerf flag is set, randomly change 1/4 of the boxes from the stack
		if (args.length > 3) {
			if (args[3].equals("nerf")) {
				int quarterOfBoxes = (int) stack.size / 12;
				stack.tryChanges(quarterOfBoxes);
				stack.applyChanges();
			}
		}

		// Perform annealing to find improvments to the initial stack, and fianlly display it
		annealing(stack, initialTemp, coolingRate);
		display(stack);
	}


	/**
	 * Provides the output of this program.
	 * Displays the cuboids included in the stack, from the top-most one down, 
	 * with four integers for each, specifying the width, length and height of the
	 * current cuboid as well as the height of the stack where this cuboid is the top-most one,
	 * in that order.
	 *
	 * @param stack  the stack to display the included cuboids of
	 */
	private static void display(Stack stack) {
		Cuboid cuboid;
		// Get the height of the stack
		int height = stack.evaluate(false);

		// Display every cuboid included in the stack, alongside the height at that level
		for (int i = stack.size - 1; i >= 0;  i--) {
			cuboid = stack.get(i);

			if (cuboid.included()) {
				System.out.format("%d %d %d %d\n", cuboid.w, cuboid.l, cuboid.h, height);
				height -= cuboid.h;
			}
		}
	}

	/**
	 * Performs annealing on the given stack.
	 * Tries to change some number of boxes from the stack, equal to the current system
	 * temperature rounded up, and applies these changes if they make an improvement to
	 * the height of the stack.
	 * After each of these cycles, the temperature is reduced by the specified cooling rate.
	 * Exits once the rounded up temperature reaches 0.
	 *
	 * @param stack  				the stack to change in the course of this annealing
	 * @param initialTemp   the initial temperature of the annealing system, i.e. the maximum number of boxes
	 * 											that will be changed in a cycle
	 * @param coolingRate   the amount that will be subtracted from the system temperature after every cycle
	 */
	private static void annealing(Stack stack, int initialTemp, double coolingRate) {
		// actually current temperature will be a double since cooling rate may not be a whole number
		double currentTemp = initialTemp;
		int ceilingTemp = initialTemp;

		while (ceilingTemp > 0) {
			// Try ceilingTemp many changes to 
			stack.tryChanges(ceilingTemp);

			// If these changes improved the stack height, apply them
			if (stack.evaluate(true) > stack.evaluate(false)) {
				stack.applyChanges();
			}
			else {
				// Otherwise, discard the changes
				stack.cancelChange();
			}

			// Update the temperature and calculate its integer value
			currentTemp = currentTemp - coolingRate;
			ceilingTemp = (int)Math.ceil(currentTemp);
		}
	}

	/**
	 * Creates and returns an unsorted stack of cuboids from the boxes 
	 * specified in the file called <code>filename</code>.
	 *
	 * @return a stack of cuboids including 3 rotations for each box from the given file
	 */
	private static Stack loadBoxes(String filename) {
		String line;
		String[] lineWords;
		int w, l, h;

		// The stack is empty initialy
		Stack stack = new Stack();

		try(BufferedReader reader = new BufferedReader(new FileReader(filename))) {
			// Add a box specified by each valid line in the file
			while((line = reader.readLine()) != null) {
				lineWords = line.split(" ");

				// Verify that there are 3 values on this line
				if (lineWords.length != 3) {
					continue;
				}

				// Convert the 3 values to integers and verify they are all positive
				try {
					h = Integer.valueOf(lineWords[2]);
					l = Integer.valueOf(lineWords[1]);
					w = Integer.valueOf(lineWords[0]);
				}
				catch (NumberFormatException e) {
					continue;
				}
				if (h < 0 || l < 0 || w < 0) {
					continue;
				}

				// Add the 3 possible rotations of this box to the stack, as cuboids
				stack.addBox(w, l, h);
			}
		}
		catch (IOException error) {
			// Exit the program with an error if the file could not be read completely/successfully
			System.err.printf("Error reading from file: %s\n", error.getMessage());
			System.exit(1);
		}

		return stack;
	}

	/**
	 * Find the tallest stack possible from the given stack under the touching faces constraint
	 * using dynamic programming. Then include the cuboids from the stack that make up this solution.
	 *
	 * @param stack  the stack of cuboids to find the tallest touching faces stack for 
	 */
	private static void solveWithDP(Stack stack) {
		// For every cuboid, stores the maximum stack height of the cuboid, i.e. where this cuboid is at the top
		int[] H = new int[stack.size];
		// For every cuboid, store the index of the previous cuboid in the tallest stack of this cuboid
		int[] cuboidBelowI = new int[stack.size];

		int maxH = 0;
		int bestStackIndex = 0;

		Cuboid currentCuboid, cuboidBelow;

		for (int i = 0; i < stack.size; i++) {
			currentCuboid = stack.get(i);

			// Initialise the maximum height of currentCuboid's stack to be the height of currentCuboid
			H[i] = currentCuboid.h;
			cuboidBelowI[i] = -1;

			// Test putting this cuboid on every cuboid below this one, which may be the top of its own stack
			for (int j = 0; j < i; j++) {
				cuboidBelow = stack.get(j);

				// Verify contraint for putting currentCuboid on top of this cuboidBelow
				if (currentCuboid.l < cuboidBelow.l && currentCuboid.w < cuboidBelow.w) {
					// If this also increases the high of the stack where currentCuboid is at the top
					if (H[j] + currentCuboid.h > H[i]) {
						// Update the maximum height of the stack with currentCuboid at the top
						H[i] = H[j] + currentCuboid.h;

						// Record that currentCuboid is going on top of this cuboidBelow
						cuboidBelowI[i] = j;
					}
				}
				
				// If the height achieved by currentCuboid's stack is the tallest so far,
				// record the index of currentCuboid as the start of the tallest stack
				if (H[i] > maxH) {
					maxH = H[i];
					bestStackIndex = i;
				}
			}
		}

		// Represent the solution in the stack, including all the cuboids that make it up,
		// by following the indicies in cuboidBelowI for the best stack
		do {
			stack.get(bestStackIndex).include();
			bestStackIndex = cuboidBelowI[bestStackIndex];
		}
		while(bestStackIndex >= 0);
		stack.applyChanges();
	}
}
	
	
/**
 * An abstract stack that stores all the possible rotations of some set of boxes, as <code>Cuboid</code>s, and manages the inclusion
 * of these in an actual, valid stack that can verify the touching face constraint and can enforce the single-use condition by keeping
 * only the tallest rotations of each box.
 * The <code>Stack</code> also knows how to make changes to some number of boxes in it, keeping both of the aforementioned constraints.
 *
 * In this documentation, I refer to some box/rotation/cuboid being in the stack if it's tracked by an instance of <code>Stack</code>,
 * but I refer to something being "included" in the stack if it is actually part of the tallest stack sequence. Hopefully this distinction is 
 * contextually clear.
 *
 * @author Oleksandr Kashpir ID:1637705
 */
class Stack {
	ArrayList<Cuboid> stack;
	int size;
	ArrayList<Integer> idIndices;
	Random random;

	/**
	 * Initialises a stack for cuboids.
	 */
	public Stack() {
		stack = new ArrayList<Cuboid>();
		size = 0;
		idIndices = new ArrayList<Integer>();
		random = new Random();
	}

	/**
	 * Adds a box to the stack by adding its 3 possible rotations as cuboids.
	 *
	 * @param w  the width of the box
	 * @param l  the length of the box
	 * @param h  the height of the box
	 */
	public void addBox(int w, int l, int h) {
		// Create the rotations with the incremented current stack size as their IDs
		Cuboid r0 = new Cuboid(w, l, h, size);
		Cuboid r1 = new Cuboid(h, w, l, size+1);
		Cuboid r2 = new Cuboid(l, h, w, size+2);

		// Create a cyclic linked-list between these rotations
		r0.setRotation(r1);
		r1.setRotation(r2);
		r2.setRotation(r0);

		// Add the rotations to the stac
		stack.add(r0);
		stack.add(r1);
		stack.add(r2);

		// Without sorting, every cuboid has an index in the stack corresponding to its id
		idIndices.add(size);
		idIndices.add(size+1);
		idIndices.add(size+2);

		// Update the stack size
		size += 3;
	}

	/**
	 * Returns the cuboid at index <code>i</code> in the stack.
	 *
	 * @param i  the index of the cuboid to get
	 * @return   the cuboid at index <code>i</code> in the stack.
	 */
	public Cuboid get(int i) {
		return stack.get(i);
	}


	/**
	 * Orders the cuboids in this stack by their natural ordering,
	 * and rearranges the idIndices to reflect each cuboid's new position.
	 */
	public void orderStack() {
		// Sort with natural ordering of cuboid class
		stack.sort(null);

		// Update the index at every position of idIndices to reflect where 
		// each cuboid has moved in the stack from its original order 
		for (int stackI = 0; stackI < size; stackI++) {
			idIndices.set(stack.get(stackI).id, stackI);
		}
	}

	/**
	 * Removes any duplicate rotations of a box included in the stack, keeping the heighest ones.
	 */
	public void removeDuplicates() {
		Cuboid r0, r1, r2;

		for (Cuboid cuboid : stack) {
			// If this cuboid is included, remove it if either one of its rotations is also included and is higher or equal to it.
			if (cuboid.included()) {
				r0 = cuboid;
				r1 = cuboid.rotation;
				r2 = cuboid.rotation.rotation;

				if (r1.included() && r1.h >= r0.h) {
					r0.remove();	
				}
				if (r2.included() && r2.h >= r0.h) {
					r0.remove();	
				}
			}
		}

		applyChanges();	
	}

	/**
	 * Stages unique changes to up to <code>changesN</code> boxes, if that is possible without violating the 
	 * touching faces constraint.
	 *
	 * A change to a box is either the addition, removal or change of its rotation in the stack.
	 *
	 * @param changesN  how many boxes to try to change
	 */
	public void tryChanges(int changesN) {
		int selectedIndex;
		int selectionIndex = 0;
		boolean changeApplied = false;

		// Genereate a shuffled array of box indices
		int[] selections = generateSelections(size / 3);

		// Until the specified number of boxes have been changed
		while (changesN > 0) {
			// Pick the next box index
			selectedIndex = selections[selectionIndex];

			// If this box hasn't already been changed in this loop
			if (selections[selectionIndex] >= 0) {

				// Try make a change to the selected box
				if(tryChange(selectedIndex)) {
					// If a change can be made to the box

					// Mark that a change was succefully staged in this pass of the selection indices
					changeApplied = true;
					selections[selectionIndex] = -1;
					changesN--;
				}
			}
			// Point to the next selection index
			selectionIndex++;

			// If this exceeds the number of boxes in the stack
			if (selectionIndex >= selections.length) {
				// If no box could be changed in this pass of the selection indicies
				// no more boxes can be changed so return
				if (!changeApplied) {
					return;
				}

				// Otherwise, go back to the first selection index
				selectionIndex = 0;
				changeApplied = false;
			}
		}
	}

	/**
	 * Try change a box specified by <code>boxIndex</code>.
	 *
	 * A change to a box is either the addition, removal or change of its rotation in the stack.
	 *
	 * @param   the index of the box to change
	 * @return  <code>true</code> if a change to this box was successfully staged;
	 * 					<code>false</code> false, otherwise.
	 */
	private boolean tryChange(int boxIndex) {
		Cuboid selectedRotation;
		boolean flippedIncluded;
		int selectionIndex = 0;
	
		// Genereate a shuffled array of rotation indices 0-2
		int[] selections = generateSelections(3);
		Cuboid[] rotations = getRotations(boxIndex);

		// Until all rotations have been tried
		while (selectionIndex < 3) {
			// Select the rotation specified by the next index in selections
			selectedRotation = rotations[selections[selectionIndex]];

			// This will remove a box if its included rotation is selected, add it 
			// if none of its rotations are included, or change the rotation included
			// if a different rotation was already in the stack

			// Flip the inclusion of this rotation in the stack
			flippedIncluded = !selectedRotation.included();

			// Remove all rotations from the stack
			for (Cuboid rotation : rotations) {
				rotation.remove();
			}

			// If this change just removes a box, it will always be valid
			if (!flippedIncluded) {
				return true;
			}
			// Otherwise, only include a new/different rotation if it doesn't
			// violate the touching face constraint
			selectedRotation.include();
			if (validateChange(selectedRotation)) {
				return true;
			}
			
			// If flipping the selected rotation's inclusion would invalidate the stack,
			// try the next rotation from the selections array
			selectionIndex++;
			for (Cuboid rotation : rotations) {
				rotation.cancel();
			}
		}

		return false;
	}

	/**
	 * Validates the inclusion of <code>newCuboid</code> in the stack by insuring
	 * that replacing its other rotation/including it does not violate the touching faces constraint.
	 *
	 * @return  <code>true</code> if including the specified cuboid would not invalidate the stack;
	 * 					<code>false</code>, otherwise.
	 */
	private boolean validateChange(Cuboid newCuboid) {
		Cuboid cuboidBelow = null;

		for (Cuboid cuboid : stack) {
			// If the current cuboid is included and isn't the first one in the stack
			if (cuboid.included(true)) {
				if (cuboidBelow != null) {

					// If either the current cuboid or the cuboid below is the new cuboid, check it doesn't
					// violate the touching face constraint
					if (cuboid.id == newCuboid.id || cuboidBelow.id == cuboidBelow.id) {
						// If the current cuboid has a dimention greater or equal to the corresponding 
						// dimention of the cuboid below, this change is not valid
						if (cuboid.w >= cuboidBelow.w || cuboid.l >= cuboidBelow.l) {
							return false;
						}
					}
				}

				// This included cuboid will now be the cuboid below
				cuboidBelow = cuboid;
			}
		}

		return true;
	}

	/**
	 * Returns either the staged height or current height of this stack, formed by either its staged included or 
	 * actually included cuboids, respectively.
	 *
	 * @param unconfirmed   whether to return the staged stack height
	 * @return  						the staged or current height of this stack
	 */
	public int evaluate(boolean unconfirmed) {
		Cuboid cuboid;
		int totalHeight = 0;

		// Every cuboid that is either staged/actually included contributes its height to the total height
		for (int stackI = 0; stackI < size; stackI++) {
			cuboid = get(stackI);
			if (cuboid.included(unconfirmed)) {
				totalHeight += cuboid.h;
			}
		}

		return totalHeight;
	}

	/**
	 * Confirms the staged inclusion of every cuboid in this stack.
	 */
	public void applyChanges() {
		for (Cuboid cuboid : stack) {
			cuboid.confirm();
		}
	}

	/**
	 * Cancels the staged inclusion of every cuboid in this stack.
	 */
	public void cancelChange() {
		for (Cuboid cuboid : stack) {
			cuboid.cancel();
		}
	}

	/**
	 * Returns the cuboid specified by its box's index <code>i</code> and its rotation <code>r</code>.
	 * A box's index is how many boxes were added before it, such that 
	 * <code>size</code> / 3 is the index of the last box.
	 *
	 * @param   the index of the rotation's box
	 * @param   the rotation number, from 0-2
	 * @return  the cuboid specified by its box's index <code>i</code> and its rotation <code>r</code>
	 */
	private Cuboid getRotation(int i, int r) {
		// Calculate the id of this cuboid
		int id = (i * 3) + r;

		// Return a cuboid from the stack at the index specified by the IDth idIndicies value 
		return stack.get(idIndices.get(id));
	}

	/**
	 * Returns the rotations of a box specified by its index <code>i</code>.
	 * A box's index is how many boxes were added before it, such that 
	 * <code>size</code> / 3 is the index of the last box.
	 *
	 * @return  the rotations of a box specified by its index <code>i</code>
	 */
	private Cuboid[] getRotations(int i) {
		Cuboid r0 = getRotation(i, 0);
		Cuboid r1 = getRotation(i, 1);
		Cuboid r2 = getRotation(i, 2);

		return new Cuboid[] {r0, r1, r2};
	}

	/**
	 * Generates an array of integers from 0 up to <code>n</code> (exclusive)
	 * and returns this array, shuffled.
	 *
	 * @param n  size of array to generate/upper limit to the integers in it
	 * @return   A shuffled array of integers from 0 up to and excluding <code>n</code>
	 */
	private int[] generateSelections(int n) {
		int temp, index;

		// Create the array of integers from 0 to n
		int [] selections = new int[n];
		for (int i = 0; i < n; selections[i] = i++);

		// Shuffle it by swapping a random number with the number in the first position,
		// n many times
		for (int i = 0; i < n; i++) {
			index = random.nextInt(n);
			temp = selections[0];
			selections[0] = selections[index];
			selections[index] = temp;
		}

		return selections;
	}
}


/**
 * A representation of a particular rotation of a particular box, instances of which are stored in a <code>Stack</code> and
 * make up the 'stack' of boxes it represents with included cuboids.
 *
 * i.e., for the purposes of this documentation, a cuboid is any rotation of any box.
 *
 * @author Oleksandr Kashpir ID: 1637705
 */
class Cuboid implements Comparable<Cuboid> {
	public int h, l, w, id;
	public Cuboid rotation;
	private boolean included, stagedIncluded;

	/**
	 * Initialises a <code>Cuboid</code> object with the specified dimensions and index.
	 * The greater of <code>x</code> and <code>y</code> is always taken to be the cuboid's width.
	 * A cuboid is initially considered not included.
	 *
	 * @param x   the width or length of this cuboid
	 * @param y   the width of length of this cuboid
	 * @param z   the height of this cuboid
	 * @param id  the unique integer value associated with this cuboid
	 */
	public Cuboid(int x, int y, int z, int id) {
		if (x > y) {
			w = x;
			l = y;
		}
		else {
			w = y;
			l = x;
		}

		h = z;
		this.id = id;
		// Initialy this cuboid is not included
		included = stagedIncluded = false;
	}

	/**
	 * Sets the reference to one of this cuboid's rotations.
	 *
	 * For the purposes of my implementation, its suffices for each
	 * cuboid to be aware of just one of its rotations, as
	 * each box forms a cyclic linked-list of its rotations this way.
	 */
	public void setRotation(Cuboid r) {
		rotation = r;
	}

	/**
	 * Stages this cuboid to be marked as included.
	 */
	public void include() {
		stagedIncluded = true;
	}
	
	/**
	 * Stages this cuboid to be marked as not included.
	 */
	public void remove() {
		stagedIncluded = false;
	}

	/**
	 * Confirms the staged inclusion of this cuboid.
	 */
	public void confirm() {
		included = stagedIncluded;
	}

	/**
	 * Cancels the staged inclusion of this cuboid.
	 */
	public void cancel() {
		stagedIncluded = included;
	}

	/**
	 * Returns the confirmed inclusion of this cuboid.
	 *
	 * @return  the confirmed inclusion of this cuboid
	 */
	public boolean included() {
		return included;
	}

	/**
	 * Returns either the confirmed or staged inclusion of this cuboid.
	 *
	 * @param unconfirmed   whether to return the staged inclusion of this cuboid
	 * @return  the staged inclusion of this cuboid, if <code>unconfirmed</code>;
	 * 					the confirmed inclusion of this cuboid, otherwise.
	 */
	public boolean included(boolean unconfirmed) {
		if (unconfirmed) {
			return stagedIncluded;
		}
		return included;
	}

	/**
	 * Specified the natural ordering of cuboids.
	 * This cuboid is lesser than another cuboid if its face area is greater.
	 */
	public int compareTo(Cuboid cuboid) {
		return (cuboid.w * cuboid.l) - (w * l);
	}
}
