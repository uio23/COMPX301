import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class NPCStack {
	private static ArrayList<Box> boxes = new ArrayList<Box>();

	public static void main(String[] args) {
		if (args.length != 3) {
			System.err.println("Usage: java NPCStack filename initial_temp cooling_rate");
			System.exit(1);
		}

		String filename = args[0];
		loadBoxes(filename);
		displayBoxes();
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
	}

	private static void displayBoxes() {
		for (Box box : boxes) {
			System.out.println("-".repeat(box.w));
			for (int i = 0; i < box.h; i++) {
				System.out.print("|" + " ".repeat(box.w-2) + "|");
				if (i == (int)box.h/2) {
					System.out.println("~".repeat(box.l));
				}
				else{
					System.out.println();
				}
			}
			System.out.println("-".repeat(box.w));
		}
	}
}

class Box {
	public int h, l, w, id;

	public Box(int h, int l, int w, int id) {
		this.h = h;
		this.l = l;
		this.w = w;
		this.id = id;
	}
}
