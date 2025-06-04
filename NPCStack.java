import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class NPCStack {
	private static ArrayList<Box> boxes = new ArrayList<Box>();
	private static int[] bitmap;

	public static void main(String[] args) {
		if (args.length != 3) {
			System.err.println("Usage: java NPCStack filename initial_temp cooling_rate");
			System.exit(1);
		}

		String filename = args[0];
		loadBoxes(filename);
		generateInitialSubsequence();
		int sum = 0;
		for(int i = 0; i < bitmap.length; i++) {
			if (bitmap[i] == 1) {
				sum += boxes.get(i).h;
			}
		}
		System.out.println("Initial solution: " + sum);

		int temp = Integer.parseInt(args[1]);
		float cooling = Float.parseFloat(args[2]);
		annealing((float)temp, cooling);
		sum = 0;
		for(int i = 0; i < bitmap.length; i++) {
			if (bitmap[i] == 1) {
				sum += boxes.get(i).h;
			}
		}
		System.out.println("Annealing solution: " + sum);
	}
	
	static private void display() {
		Box box;
		int height = 0;

		for (int i = 0; i < bitmap.length; i++) {
			if (bitmap[i] == 1) {
				box = boxes.get(i);
				height += box.h;
				System.out.format("%d %d %d %d\n", box.w, box.l, box.h, height);
			}
		}
	}

	private static void annealing(float temp, float cooling) {
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

	private static void loadBoxes(String filename) {
		String line;
		String[] words;
		int h, l, w;
		int boxN = 0;

		try(BufferedReader reader = new BufferedReader(new FileReader(filename))) {
			while((line = reader.readLine()) != null) {
				words = line.split(" ");

				if (words.length != 3) {
					continue;
				}

				h = l = w = -1;
				try {
					h = Integer.valueOf(words[2]);
					l = Integer.valueOf(words[1]);
					w = Integer.valueOf(words[0]);
				}
				catch (NumberFormatException e) {
					continue;
				}

				if (h < 0 || l < 0 || w < 0) {
					continue;
				}

				boxes.add(new Box(h, l, w, boxN));
				if (h != l) {
					boxes.add(new Box(l, h, w, boxN));
				}
				if (h != w) {
					boxes.add(new Box(w, l, h, boxN));
				}
				boxN++;
			}
		}
		catch (IOException error) {
			String errorMess = String.format("Error reading from file: %s", error.getMessage());
			System.err.println(errorMess);
			System.exit(1);
		}
		bitmap = new int[boxes.size()];
	}

	private static void solveLDS() {
		boxes.sort(null);
		int[] maxH = new int[boxes.size()];
		int[] nextBox = new int[boxes.size()];
		Box currentBox, boxBelow;

		for (int i = 0; i < maxH.length; i++) {
			currentBox = boxes.get(i);
			maxH[i] = currentBox.h;
			for (int j = 0; j < i; j++) {
				boxBelow = boxes.get(j);

				if (currentBox.l < boxBelow.l && currentBox.w < boxBelow.w) {
					if (maxH[j] + currentBox.h > maxH[i]) {
						maxH[i] = maxH[j] + currentBox.h;
						nextBox[i] = j;
					}
				}
			}
		}
		
		getBitmapOfLDS(nextBox, maxH);
	}

	private static void getBitmapOfLDS(int[] nextBox, int[] H) {
		int maxHeightI = 0;
		for (int i = 0; i < nextBox.length; i++) {
			if(H[i] > H[maxHeightI]) {
				maxHeightI = i;
			}
		}
		System.out.println("DP solution: " + H[maxHeightI]);
		int currI = maxHeightI;
		do {
			bitmap[currI] = 1;
			currI = nextBox[currI];
		}
		while(currI > 0);
	}
	
	public static void generateInitialSubsequence() {
		solveLDS();
		removeDuplicateBoxUse();
	}

	private static void removeDuplicateBoxUse() {
		for (int boxI = 0; boxI < bitmap.length + 2; boxI+=3) {
			int maxH = 0;
			int maxHI = -1;
			for (int rotationI = 0; rotationI < 3; rotationI++) {
				if (boxI+rotationI >= bitmap.length) {
					break;
				}
				if (bitmap[boxI+rotationI] == 1 && boxes.get(boxI+rotationI).h > maxH) {
					maxH = boxes.get(boxI+rotationI).h;
					maxHI = boxI + rotationI;
				}
				bitmap[boxI+rotationI] = 0;
			}
			if (maxHI > 0) {
				bitmap[maxHI] = 1;
			}
		}
	}

	private static int evaluateBitmap(int[] mapToUse) {
		int score = 0;
		for (int i = 0; i < mapToUse.length; i++) {
			if (mapToUse[i] == 1) {
				score += boxes.get(i).h;
			}
		}

		return score;
	}

	private static boolean validateBitmap(int[] mapToUse) {
		int rotation;
		int boxSum = 0;
		int prev = -1;

		for (int i = 0; i < mapToUse.length; i++) {
			rotation = i % 3;
			if (rotation == 0) {
				if (boxSum > 1) {
					return false;
				}
				boxSum = 0;
			}
			// If this box violates the contraint
			if (mapToUse[i] == 1) {
				if (prev >= 0) {
					if (boxes.get(prev).w <= boxes.get(i).w || boxes.get(prev).l <= boxes.get(i).l) {
						return false;
					}
				}

				boxSum++;
				prev = i;
			}
		}
		if (boxSum > 1) {
			return false;
		}

		return true;
	}

}

class Box implements Comparable<Box> {
	public int h, l, w, id;

	public Box(int h, int l1, int l2, int id) {
		this.h = h;
		if (l1 > l2) {
			this.w = l1;
			this.l = l2;
		}
		else {
			this.l = l1;
			this.w = l2;
		}
		this.id = id;
	}

	/**
	 * This box is lesser than another box if its face area is greater
	 */
	public int compareTo(Box box) {
      return box.w * box.l - w * l;
	}
}
