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
		if (initialTemp <= 0 || initialTemp > boxes.length) {
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
		displayBitmap(bitmap);
		display(bitmap, boxes);
		annealing(bitmap, boxes, initialTemp, coolingRate);
		displayBitmap(bitmap);
		display(bitmap, boxes);
	}

	private static void displayBitmap(int[] bitmap) {
		for(int bit : bitmap) {
			System.out.print(bit);
		}
		System.out.println();
	}
	
	static private void display(int[] bitmap, Box[] boxes) {
		Box box;
		int height = 0;

		// Calculate the height of the bitmap stack
		for (int i = 0; i < bitmap.length;  i++) {
			if (bitmap[i] == 1) {
				height += boxes[i].h;
			}
		}

		// Display every box in the bitmap from the highest one down, alongside the high at that level
		for (int i = bitmap.length - 1; i >= 0;  i++) {
			if (bitmap[i] == 1) {
				box = boxes[i];
				System.out.format("%d %d %d %d\n", box.w, box.l, box.h, height);
				height -= box.h;
			}
		}
	}

	private static void annealing(int[] bitmap, Box[] boxes, int temp, double cooling) {
		int changesN;
		int changedBit;
		Random random = new Random();
		int[] changedBitmap = bitmap.clone();
		ArrayList<Integer> changedBits;
		while (temp > 0) {
			changesN = 0;
			changedBits = new ArrayList<Integer>(); 

			// Make temp many changes
			while(changesN < Math.ceil(temp)) {
				// Change different bits every time
				do {
					changedBit = random.nextInt(bitmap.length);
				}
				while (changedBits.contains(changedBit));

				// Flip bit
				changedBitmap[changedBit] = (bitmap[changedBit] - 1) * -1;

				// Undo invalid changes
				if (!validateBitmap(changedBitmap)) {
					// Flip bit back
					changedBitmap[changedBit] = (changedBitmap[changedBit] - 1) * -1;
					continue;
				}
				changesN++;
				changedBits.add(changedBit);
			}

			if (evaluateBitmap(changedBitmap) > evaluateBitmap(bitmap)) {
				System.out.println("HEY");
				bitmap = changedBitmap;
			}

			temp = temp - cooling;
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
}
