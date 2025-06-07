import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class NPCStack {
	public static void main(String[] args) {
		// Verify the valid number of command line arguments have been passed
		if (args.length != 3) {
			System.err.println("Usage: java NPCStack filename initialTemp coolingRate");
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

		Stack stack = loadBoxes(filename);

		// Ensure that the annealing parameters are withing their limits
		if (initialTemp <= 0 || initialTemp > stack.size) {
			System.err.println("initialTemp must be 0 < initialTemp <= #boxes)");
			System.exit(1);
		}
		if (coolingRate < 0.01 || coolingRate > initialTemp) {
			System.err.println("coolingRate must be 0.1 <= coolingRate <= initialTemp");
			System.exit(1);
		}

		stack.orderStack();
		solveWithDP(stack);
		display(stack);
		stack.reduceToSingleRotation();
		/*
		BitmapManager.makeChanges(bitmap, boxes, (int) bitmap.length / 6);
		for (Box box : boxes) {
			box.confirmBit();
		}
		for (int i = 0; i < stack.size; i++) {
			Box bb = stack.getFromStack(i);
				System.out.printf("%d %d %d\n", bb.w, bb.l, bb.h);
		}
		*/

		display(stack);
		System.out.println("performing annealing");
		annealing(stack, initialTemp, coolingRate);
		display(stack);
	}

	static public void display(Stack stack) {
		Box box;
		// Calculate the height of the bitmap stack
		int height = stack.evaluate(false);

		// Display every box in the bitmap from the highest one down, alongside the high at that level
		for (int i = stack.size - 1; i >= 0;  i--) {
			box = stack.getFromStack(i);
			if (box.included()) {
				System.out.format("%d %d %d %d\n", box.w, box.l, box.h, height);
				height -= box.h;
			}
		}
	}

	private static void annealing(Stack stack, int temp, double coolingRate) {
		double currentTemp = temp;
		int ceilingTemp = temp;

		while (ceilingTemp > 0) {
			// Make ceilingTemp many changes to 
			stack.tryChanges(ceilingTemp);

			// If this change improved the stack, set it to be the bitmap
			if (stack.evaluate(true) > stack.evaluate(false)) {
				System.out.println("IMPROVMENT");
				stack.applyChanges();
			}
			else {
				stack.dropChanges();
			}

			// Update the temperature and calculate its integer value
			currentTemp = currentTemp - coolingRate;
			ceilingTemp = (int)Math.ceil(currentTemp);
		}
	}

	private static Stack loadBoxes(String filename) {
		String line;
		String[] lineWords;
		int w, l, h;

		Stack stack = new Stack();

		try(BufferedReader reader = new BufferedReader(new FileReader(filename))) {
			// Create 3 boxes for every line in the file, skipping any invalid lines
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

				// Create a box for each of the 3 possible heights of this box
				stack.addBox(w, l, h);
			}
		}
		catch (IOException error) {
			// Exit the program with an error if the file could not be read completely/successfully
			System.err.printf("Error reading from file: %s\n", error.getMessage());
			System.exit(1);
		}


		// Return an array of the created box objects
		return stack;
	}


	private static void solveWithDP(Stack stack) {
		// For every box, stores the maximum stack heigh of the box, i.e. where this box is at the top
		int[] H = new int[stack.size];
		// For every box, store the index of the previous box in the tallest stack of this box
		int[] boxBelowI = new int[stack.size];

		int maxH = 0;
		int bestStackIndex = 0;

		Box currentBox, boxBelow;

		for (int i = 0; i < stack.size; i++) {
			currentBox = stack.getFromStack(i);

			// Initialise the maximum height of currentBox's stack to be the height of currentBox
			H[i] = currentBox.h;
			boxBelowI[i] = -1;

			// Test putting this box on every box below this one, which may be the top of its own stack
			for (int j = 0; j < i; j++) {
				boxBelow = stack.getFromStack(j);

				// Verify contraint for putting currentBox on top of this boxBelow
				if (currentBox.l < boxBelow.l && currentBox.w < boxBelow.w) {
					// If this also increases the high of the stack where currentBox is at the top
					if (H[j] + currentBox.h > H[i]) {
						// Update the maximum height of the stack with currentBox at the top
						H[i] = H[j] + currentBox.h;

						// Record that currentBox is going on top of this boxBelow
						boxBelowI[i] = j;
					}
				}
				
				// If the height achieved by currentBox's stack is the tallest so far,
				// record the idex of currentBox as the start of the tallest stack
				if (H[i] > maxH) {
					maxH = H[i];
					bestStackIndex = i;
				}
			}
		}

		for (int i : H) {
			System.out.print(i + " ");
		}
		System.out.println();
		for (int i : boxBelowI) {
			System.out.print(i + " ");
		}
		System.out.println();

		// Convert the solution to be a bitmap of the boxes included
		// by following the indicies in boxBelow for the best stack
		do {
			stack.getFromStack(bestStackIndex).include();
			bestStackIndex = boxBelowI[bestStackIndex];
		}
		while(bestStackIndex >= 0);
		stack.applyChanges();

	}
}
	
	
class Stack {
	ArrayList<Box> stack;
	int size;
	ArrayList<Integer> boxesInStack;
	Random random;

	public Stack() {
		stack = new ArrayList<Box>();
		size = 0;
		boxesInStack = new ArrayList<Integer>();
		random = new Random();
	}

	public void addBox(int w, int l, int h) {
		Box r0 = new Box(w, l, h, size);
		Box r1 = new Box(h, w, l, size+1);
		Box r2 = new Box(l, h, w, size+2);
		r0.setRotation(r1);
		r1.setRotation(r2);
		r2.setRotation(r0);

		stack.add(r0);
		stack.add(r1);
		stack.add(r2);
		boxesInStack.add(size);
		boxesInStack.add(size+1);
		boxesInStack.add(size+2);
		size += 3;
	}

	public Box getFromStack(int i) {
		return stack.get(i);
	}

	public Box getRotation(int i, int r) {
		int rotationIndex = (i * 3) + r;
		return stack.get(boxesInStack.get(rotationIndex));
	}

	public Box[] getRotations(int i) {
		Box r0 = getRotation(i, 0);
		Box r1 = getRotation(i, 1);
		Box r2 = getRotation(i, 2);

		return new Box[] {r0, r1, r2};
	}

	public void orderStack() {
		// Sort with natural ordering of box class
		stack.sort(null);

		// Update all index in the stack
		for (int stackI = 0; stackI < size; stackI++) {
			boxesInStack.set(stack.get(stackI).index, stackI);
		}
	}

	public void reduceToSingleRotation() {
		Box r0, r1, r2;

		for (Box cuboid : stack) {
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

	private boolean validateChange(Box newCuboid) {
		Box previousCuboid = null;

		for (Box cuboid : stack) {
			if (cuboid.included(true)) {
				if (previousCuboid != null) {
					if (cuboid.index == newCuboid.index) {
						if (previousCuboid.w <= cuboid.w || previousCuboid.l <= cuboid.l) {
							return false;
						}
					}
					else if (previousCuboid.index == newCuboid.index) {
						if (previousCuboid.w <= cuboid.w || previousCuboid.l <= cuboid.l) {
							return false;
						}
					}
				}

				previousCuboid = cuboid;
			}
		}

		return true;
	}

	public void tryChanges(int changesN) {
		int[] selections = generateSelections(size / 3);
		int selectionIndex = 0;
		boolean changeApplied = false;
		int boxI;

		/*
		System.out.print("initial selections: ");
		for (int i : selections) {
			System.out.print(i + " ");
		}
		System.out.println();
		*/

		while (changesN > 0) {
			if (selections[selectionIndex] >= 0) {
				boxI = selections[selectionIndex];

				// Make change to selected box
				if(tryChange(getRotations(boxI))) {
					//System.out.println("Change " + changesN + " successful");
					changeApplied = true;
					selections[selectionIndex] = -1;
					changesN--;
				}
			}
			selectionIndex++;

			if (selectionIndex >= size / 3) {
				/*
				System.out.print("selections: ");
				for (int i : selections) {
					nSystem.out.print(i + " ");
				}
				System.out.println();
				*/

				if (!changeApplied) {
					//System.out.println(changesN + " many changed missed");
					return;
				}

				selectionIndex = 0;
				changeApplied = false;
			}
		}
	}

	private boolean tryChange(Box[] rotations) {
		int[] selections = generateSelections(3);
		int selectionIndex = 0;

		Box selectedBox;
		boolean flippedIncluded;

		while (selectionIndex < 3) {
			selectedBox = rotations[selections[selectionIndex]];

			flippedIncluded = !selectedBox.included();

			// Remove all rotations
			for (Box rotation : rotations) {
				rotation.remove();
			}

			// If the inverse of the selected box's inclusion is to include it,
			// do so and verify it doesn' violate the touching face condition
			if (!flippedIncluded) {
				return true;
			}
			selectedBox.include();
			if (validateChange(selectedBox)) {
				return true;
			}
			
			selectionIndex++;
			for (Box rotation : rotations) {
				rotation.cancel();
			}
		}

		return false;
	}



	public int evaluate(boolean unconfirmed) {
		Box cuboid;
		int score = 0;

		// Every included box contributes its height to the score
		for (int stackI = 0; stackI < size; stackI++) {
			cuboid = getFromStack(stackI);
			if (cuboid.included(unconfirmed)) {
				score += cuboid.h;

			}
		}

		return score;
	}

	public void applyChanges() {
		for (Box cuboid : stack) {
			cuboid.confirm();
		}
	}

	public void dropChanges() {
		for (Box cuboid : stack) {
			cuboid.cancel();
		}
	}


	private int[] generateSelections(int n) {

		int [] selections = new int[n];
		int temp, index;
		for (int i = 0; i < n; selections[i] = i++);

		// Shuffle
		for (int i = 0; i < n; i++) {
			index = random.nextInt(n);
			temp = selections[0];
			selections[0] = selections[index];
			selections[index] = temp;
		}

		return selections;
	}
}
	class Box implements Comparable<Box> {
		public int h, l, w, index;
		public Box rotation;
		private boolean included, stagedIncluded;

		public Box(int x, int y, int z, int index) {
			if (x > y) {
				w = x;
				l = y;
			}
			else {
				w = y;
				l = x;
			}
			h = z;
			this.index = index;

			included = stagedIncluded = false;
		}

		public void setRotation(Box r) {
			rotation = r;
		}

		public void include() {
			stagedIncluded = true;
		}

		public boolean included(boolean unconfirmed) {
			if (unconfirmed) {
				return stagedIncluded;
			}
			return included;
		}

		public void remove() {
			stagedIncluded = false;
		}

		public void confirm() {
			included = stagedIncluded;
		}

		public void cancel() {
			stagedIncluded = included;
		}

		public boolean included() {
			return included;
		}

		/**
	 * This box is lesser than another box if its face area is greater
	 */
		public int compareTo(Box box) {
			return box.w * box.l - w * l;
		}
	}
