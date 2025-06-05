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
		if (coolingRate < 0.1 || coolingRate > initialTemp) {
			System.err.println("coolingRate must be 0.1 <= coolingRate <=t");
			System.exit(1);
		}

		int[] bitmap = solveWithDP(boxes);
		displayBitmap(bitmap);
		display(bitmap, boxes);
		BitmapManager.removeDuplicateBoxes(bitmap, boxes);
		BitmapManager.makeChanges(bitmap, boxes, (int) bitmap.length / 6);
		displayBitmap(bitmap);
		display(bitmap, boxes);
		System.out.println("performing annealing");
		annealing(bitmap, boxes, initialTemp, coolingRate);
		displayBitmap(bitmap);
		display(bitmap, boxes);
	}

	public static void displayBitmap(int[] bitmap) {
		for(int bit : bitmap) {
			System.out.print(bit);
		}
		System.out.println();
	}
	
	static public void display(int[] bitmap, Box[] boxes) {
		Box box;
		int height = 0;

		// Calculate the height of the bitmap stack
		for (int i = 0; i < bitmap.length;  i++) {
			if (bitmap[i] == 1) {
				height += boxes[i].h;
			}
		}

		// Display every box in the bitmap from the highest one down, alongside the high at that level
		for (int i = bitmap.length - 1; i >= 0;  i--) {
			if (bitmap[i] == 1) {
				box = boxes[i];
				System.out.format("%d %d %d %d\n", box.w, box.l, box.h, height);
				height -= box.h;
			}
		}
	}

	private static void annealing(int[] bitmap, Box[] boxes, int temp, double cooling) {
		int[] changedBitmap = bitmap.clone();
		double updatedTemp = temp;
		while (updatedTemp > 0) {
			BitmapManager.makeChanges(changedBitmap, boxes, (int)updatedTemp);

			if (BitmapManager.envaluateBitmap(changedBitmap, boxes) > BitmapManager.envaluateBitmap(bitmap, boxes)) {
				System.out.println("IMPROVMENT");
				bitmap = changedBitmap;
			}

			updatedTemp = updatedTemp - cooling;
		}
	}

	private static Box[] loadBoxes(String filename) {
		String line;
		String[] lineWords;
		int w, l, h;

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
				boxes.add(new Box(w, l, h));
				boxes.add(new Box(h, w, l));
				boxes.add(new Box(l, h, w));
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

	private static int[] solveWithDP(Box[] boxes) {
		// For every box, stores the maximum stack heigh of the box, i.e. where this box is at the top
		int[] H = new int[boxes.length];
		// For every box, store the index of the previous box in the tallest stack of this box
		int[] boxBelowI = new int[boxes.length];

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

		// Convert the solution to be a bitmap of the boxes included
		// by following the indicies in boxBelow for the best stack
		int[] bitmap = new int[boxes.length];
		do {
			bitmap[bestStackIndex] = 1;
			bestStackIndex = boxBelowI[bestStackIndex];
		}
		while(bestStackIndex > 0);

		return bitmap;
	}
}


class Box implements Comparable<Box> {
	public int h, l, w;

	public Box(int x, int y, int z) {
		if (x > y) {
			w = x;
			l = y;
		}
		else {
			w = y;
			l = x;
		}
		h = z;
	}

	/**
	 * This box is lesser than another box if its face area is greater
	 */
	public int compareTo(Box box) {
      return box.w * box.l - w * l;
	}
}

class BitmapManager {
	public static void removeDuplicateBoxes(int[] bitmap, Box[] boxes) {
		int i;
		int maxHeightRotation;
		int heighestRotationI;

		// For every box in the bitmap
		for (int boxI = 0; boxI < bitmap.length; boxI+=3) {
			maxHeightRotation = -1;
			heighestRotationI = -1;

			// For every rotation of a box
			for (int rotationI = 0; rotationI < 3; rotationI++) {
				// Calc index of this box's current rotation
				i = boxI+rotationI;

				// If this rotation is present in the bitmap and is the taller one...
				if (bitmap[i] == 1 && boxes[i].h > maxHeightRotation) {
					// Record this rotation as the tallest one
					maxHeightRotation = boxes[i].h;
					heighestRotationI = boxI + rotationI;
				}

				// Remove the rotation from the bitmap
				bitmap[i] = 0;
			}

			// Restore the heighest rotation of this box in the bitmap, if it was present
			if (heighestRotationI > 0) {
				bitmap[heighestRotationI] = 1;
			}
		}
	}

	public static boolean validateBitmap(int[] bitmap, Box[] boxes) {
		int rotationI;
		int rotationsSum = 0;
		int prev = -1;

		for (int i = 0; i < bitmap.length; i++) {
			rotationI = i % 3;

			// If this is the first rotation of a box, check that at most one rotation of
			// the previous box was included in the bitmap
			if (rotationI == 0) {
				// If more than one rotation of a box is included in the bitmap,
				// this bitmap contains a repeating box and is invalid
				if (rotationsSum > 1) {
					System.out.println("should never happen");
					return false;
				}
				// Reset the rotation sum for the new box
				rotationsSum = 0;
			}

			// If this box rotation is included
			if (bitmap[i] == 1) {
				// If the box below this one exists and has a width or length less than or equal to that of the current box,
				// the stacking constrain is violated and this bitmap is invalid
				if (prev >= 0) {
					if (boxes[prev].w <= boxes[i].w || boxes[prev].l <= boxes[i].l) {
						return false;
					}
				}

				// Account for the inclusion of this rotation
				rotationsSum++;
				// Record this box as the previous box for the box above
				prev = i;
			}
		}
		// Verify the last box was only included in one rotation
		if (rotationsSum > 1) {
			return false;
		}

		// If no contraints are violated, this bitmap is valid
		return true;
	}

	public static int envaluateBitmap(int[] bitmap, Box[] boxes) {
		int score = 0;

		// Every included box contributes its height to the score
		for (int i = 0; i < bitmap.length; i++) {
			if (bitmap[i] == 1) {
				score += boxes[i].h;
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

	private static boolean makeChange(int[] bitmap, Box[] boxes, int boxIndex) {
		int selectionIndex = 0;
		int[] selections = generateSelections(3);
		for (int i : selections) {
			System.out.print(i);
		}
		int r0 = bitmap[boxIndex];
		int r1 = bitmap[boxIndex+1];
		int r2 = bitmap[boxIndex+2];

		while (selectionIndex < 3) {
			int bitIndex = boxIndex + selections[selectionIndex];
			int flippedBit = (bitmap[bitIndex] - 1) * -1;

			// Remove all rotations
			bitmap[boxIndex] = bitmap[boxIndex+1] = bitmap[boxIndex+2] = 0;

			// Apply flipped bit
			bitmap[bitIndex] = flippedBit; 

			System.out.println("Attempting to change bit " + bitIndex);
			NPCStack.display(bitmap, boxes);
			NPCStack.displayBitmap(bitmap);
			if (validateBitmap(bitmap, boxes)) {
				return true;
			}
			selectionIndex++;
			bitmap[boxIndex] = r0;
			bitmap[boxIndex+1] = r1;
			bitmap[boxIndex+2] = r2;
		}

		return false;
	}


	public static void makeChanges(int[] bitmap, Box[] boxes, int changesN) {
		int[] selections = generateSelections(bitmap.length / 3);
		int selectionIndex = 0;

		while (changesN > 0) {
			System.out.println("Attempting to change box " + selections[selectionIndex] + " at " + selections[selectionIndex] * 3);
			if(makeChange(bitmap, boxes, selections[selectionIndex]*3)) {
				changesN--;
				System.out.println("Change succesful " + changesN);
			}
			selectionIndex++;
			if (selectionIndex >= bitmap.length/3) {
				return;
			}
		}
	}
}
