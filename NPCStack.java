import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

		Box[] boxes = loadBoxes(filename);
		Arrays.sort(boxes);

		// Ensure that the annealing parameters are withing their limits
		if (initialTemp <= 0 || initialTemp > boxes.length / 3) {
			System.err.println("initialTemp must be 0 < initialTemp <= 3*(N of boxes)");
			System.exit(1);
		}
		if (coolingRate < 0.01 || coolingRate > initialTemp) {
			System.err.println("coolingRate must be 0.1 <= coolingRate <=t");
			System.exit(1);
		}

		display(boxes);
		solveWithDP(boxes);

		for (int j = 0; j < boxes.length; j++) {
			//System.out.println(j + ": " + boxes[j].w + " " + boxes[j].l + " " + boxes[j].h);
		}

		display(boxes);
		BitmapManager.removeDuplicateBoxes(boxes);
		/*
		BitmapManager.makeChanges(bitmap, boxes, (int) bitmap.length / 6);
		for (Box box : boxes) {
			box.confirmBit();
		}
		*/
		display(boxes);
		System.out.println("performing annealing");
		boxes = annealing(boxes, initialTemp, coolingRate);
		display(boxes);
	}

	static public void display(Box[] boxes) {
		Box box;
		// Calculate the height of the bitmap stack
		int height = BitmapManager.envaluateBitmap(boxes, false);

		// Display every box in the bitmap from the highest one down, alongside the high at that level
		for (int i = boxes.length - 1; i >= 0;  i--) {
			box = boxes[i];
			if (box.stagedBit == 1) {
				System.out.format("%d %d %d %d\n", box.w, box.l, box.h, height);
				height -= box.h;
			}
		}

		for(Box b : boxes) {
			System.out.print(b.bit);
		}
		System.out.println();
	}

	private static Box[] annealing(Box[] boxes, int temp, double coolingRate) {
		double currentTemp = temp;
		int ceilingTemp = temp;

		while (ceilingTemp > 0) {
			// Make ceilingTemp many changes to 
			BitmapManager.makeChanges(boxes, ceilingTemp);

			// If this change improved the stack, set it to be the bitmap
			if (BitmapManager.envaluateBitmap(boxes, true) > BitmapManager.envaluateBitmap(boxes, false)) {
				System.out.println("IMPROVMENT");
				BitmapManager.confirmChanges(boxes);
			}
			else {
				BitmapManager.dropChanges(boxes);
			}

			// Update the temperature and calculate its integer value
			currentTemp = currentTemp - coolingRate;
			ceilingTemp = (int)Math.ceil(currentTemp);
		}

		return boxes;
	}

	private static Box[] loadBoxes(String filename) {
		String line;
		String[] lineWords;
		int w, l, h;
		Box r0, r1, r2;
		int i = 0;

		ArrayList<Box> boxes = new ArrayList<>();

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
				r0 = new Box(w, l, h, i++);
				r1 = new Box(h, w, l, i++);
				r2 = new Box(l, h, w, i++);
				r0.setRotation(r1);
				r1.setRotation(r2);
				r2.setRotation(r0);
				boxes.add(r0);
				boxes.add(r1);
				boxes.add(r2);
			}
		}
		catch (IOException error) {
			// Exit the program with an error if the file could not be read completely/successfully
			System.err.printf("Error reading from file: %s\n", error.getMessage());
			System.exit(1);
		}


		// Return an array of the created box objects
		return boxes.toArray(new Box[0]);
	}

	private static void solveWithDP(Box[] boxes) {
		// For every box, stores the maximum stack heigh of the box, i.e. where this box is at the top
		int[] H = new int[boxes.length];
		// For every box, store the index of the previous box in the tallest stack of this box
		int[] boxBelowI = new int[boxes.length];
		boxBelowI[0] = -1;

		int maxH = 0;
		int bestStackIndex = 0;

		Box currentBox, boxBelow;

		for (int i = 0; i < boxes.length; i++) {
			currentBox = boxes[i];
			// Initialise the maximum height of currentBox's stack to be the height of currentBox
			H[i] = currentBox.h;

			// Test putting this box on every box below this one, which may be the top of its own stack
			for (int j = 0; j < i; j++) {
				boxBelow = boxes[j];

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
		for (int i : boxBelowI) {
			System.out.print(i + " ");
		}

		// Convert the solution to be a bitmap of the boxes included
		// by following the indicies in boxBelow for the best stack
		do {
			boxes[bestStackIndex].set(1);
			bestStackIndex = boxBelowI[bestStackIndex];
		}
		while(bestStackIndex >= 0);
	}
}


class Box implements Comparable<Box> {
	public int h, l, w, index;
	public Box rotation;
	public int bit, stagedBit;

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
		bit = 0;
	}
	public void set(int b) {
		stagedBit = bit = b;
	}

	public void setRotation(Box r) {
		rotation = r;
	}
	public void stage(int b) {
		stagedBit = b;
	}
	public void set() {
		bit = stagedBit;
	}
	public void unstage() {
		stagedBit = bit;
	}

	/**
	 * This box is lesser than another box if its face area is greater
	 */
	public int compareTo(Box box) {
      return box.w * box.l - w * l;
	}
}

class BitmapManager {
	public static void removeDuplicateBoxes(Box[] boxes) {
		Box r0, r1, r2;

		// For every box in the bitmap
		for (int i = 0; i < boxes.length; i++) {
			r0 = boxes[i];
			r1 = r0.rotation;
			r2 = r1.rotation;

			if (r1.bit == 1 && r1.h >= r0.h) {
				r0.set(0);
			}
			if (r2.bit == 1 && r2.h >= r0.h) {
				r0.set(0);
			}
		}
	}

	private static boolean validateChange(Box[] boxes, Box box) {
		int prev = -1;

		for (int i = 0; i < boxes.length; i++) {
			if (boxes[i].stagedBit == 1) {
				if (prev >= 0) {
					if (boxes[prev].w <= boxes[i].w || boxes[prev].l <= boxes[i].l) {
						return false;
					}
				}
				// Record this box as the previous box for the box above
				prev = i;
			}
		}

		// If no contraints are violated, this bitmap is valid
		return true;
	}

	public static int envaluateBitmap(Box[] boxes, boolean staged) {
		int score = 0;

		// Every included box contributes its height to the score
		for (int i = 0; i < boxes.length; i++) {
			if (staged) {
				if (boxes[i].stagedBit == 1) {
					score += boxes[i].h;
				}
			}
			else {
				if (boxes[i].bit == 1) {
					score += boxes[i].h;
				}
			}
		}

		return score;
	}
	
	private static int[] generateSelections(int n) {
		Random random = new Random();

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

	private static boolean makeChange(Box[] boxes, Box box) {
		int selectionIndex = 0;
		int[] selections = generateSelections(3);

		Box[] rotations = new Box[] {box, box.rotation, box.rotation.rotation};

		Box selectedBox;
		int flippedBit;

		while (selectionIndex < 3) {
			selectedBox = rotations[selections[selectionIndex]];

			flippedBit = (selectedBox.bit - 1) * -1;

			// Remove all rotations
			for (Box rotation : rotations) {
				rotation.stage(0);
			}

			selectedBox.stage(flippedBit);

			//System.out.println("Attempting to change box " + selectedBox.index + " to " + flippedBit);

			if (flippedBit == 1) {
				if (validateChange(boxes, selectedBox)) {
					return true;
				}
			}
			else {
				return true;
			}

			selectionIndex++;
			for (Box rotation : rotations) {
				rotation.unstage();
			}
		}

		return false;
	}


	public static void makeChanges(Box[] boxes, int changesN) {
		int[] selections = generateSelections(boxes.length / 3);
		int selectionIndex = 0;
		boolean changeApplied = false;

		//System.out.print("initial selections: ");
		for (int i : selections) {
			//System.out.print(i + " ");
		}
		//System.out.println();

		while (changesN > 0) {
			//NPCStack.display(boxes);
			if (selections[selectionIndex] == -1) {
				//System.out.println("Skipping change in selection at index " +selectionIndex);
			}
			for (int i = 0; i < boxes.length; i++) {
				if (boxes[i].index == selections[selectionIndex]) {
					if(makeChange(boxes, boxes[i])) {
						//System.out.println("Change " + changesN + " successful");
						changeApplied = true;
						selections[selectionIndex] = -1;
						changesN--;
					}
					break;
				}
			}
			selectionIndex++;

			if (selectionIndex >= boxes.length / 3) {
				//System.out.print("selections: ");
				for (int i : selections) {
					//nSystem.out.print(i + " ");
				}
				//System.out.println();
				if (!changeApplied) {
					//System.out.println(changesN + " many changed missed");
					return;
				}

				selectionIndex = 0;
				changeApplied = false;
			}
		}
	}

	public static void confirmChanges(Box[] boxes) {
		for (Box box : boxes) {
			box.set();
		}
	}
	public static void dropChanges(Box[] boxes) {
		for (Box box : boxes) {
			box.unstage();
		}
	}
}
