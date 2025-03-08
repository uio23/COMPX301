import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class XSort {
	public static void main(String[] args) {
		int runLength = Integer.parseInt(args[0]);
		int k = Integer.parseInt(args[1]);

		if (runLength > 1024) {
			System.out.println("Initial runs length passed too long. Must be between 64 and 1024");
			return;
		} else if (runLength < 1) {
			System.out.println("Initial runs length passed too short. Must be between 64 and 1024");
			return;
		}
		if (k != 0 && k != 2) {
			System.out.println("This is a 2-way balanced sort merge");
			return;
		}

		createInitialRuns(runLength, k);	
		System.out.println("Runs created");

		mergeRuns();
	}

	public static void mergeRuns() {
		String inputT1 = "tape1";
		String inputT2 = "tape2";
		String outputT1 = "tape3";
		String outputT2 = "tape4";
		String temp1, temp2;

		boolean sorted = false;

		while (!sorted) {
			sorted = merge(inputT1, inputT2, outputT1, outputT2);
			System.out.println("swiitching");
			temp1 = inputT1;
			temp2 = inputT2;

			inputT1 = outputT1;
			inputT2 = outputT2;
			outputT1 = temp1;
			outputT2 = temp2;
		}
	}
	
	public static String flush(BufferedReader reader, BufferedWriter writer, String string, String previousString) {
		try {
			System.out.println("Flushing from " + string);
			while (string != null) {
				if (string.compareTo(previousString) >= 0) {
					System.out.println(string);
					writer.write(string);
					writer.newLine();
					previousString = string;
					string = reader.readLine();
				}
				else {
					break;
				}
			}
		}
		catch (IOException error) {
			System.err.println(String.format("Exeption occured when flushing tape: %s",error.getMessage()));
		}

		return string;
	}

	public static boolean merge(String inputT1, String inputT2, String outputT1, String outputT2) {
		String outputT = outputT1;

		String string1, string2;
		String previousString = "";

		BufferedReader reader1, reader2;
		BufferedWriter writer, writer1, writer2;

		int runCounter = 0;
		boolean inputTEmpty = false;

		try {
			reader1 = new BufferedReader(new FileReader(inputT1));
			reader2 = new BufferedReader(new FileReader(inputT2));
			writer1 = new BufferedWriter(new FileWriter(outputT1));
			writer2 = new BufferedWriter(new FileWriter(outputT2));
			writer = writer1;
			string1 = reader1.readLine();
			string2 = reader2.readLine();

			while(!inputTEmpty) {
				// Truncate file
				System.out.println(String.format("Merging %s and %s into %s", inputT1, inputT2, outputT));
				runCounter++;
				while (true) {
					System.out.println(String.format("String 1: %s, String 2 %s, Prev %s", string1, string2, previousString));

					if (string1 == null) {
						string2 = flush(reader2, writer, string2, previousString);
						if (string2 == null) {
							inputTEmpty = true;
						}
						break;
					}
					else if (string2 == null) {
						string1 = flush(reader1, writer, string1, previousString);
						if (string1 == null) {
							inputTEmpty = true;
						}
						break;
					}


					if (string1.compareTo(string2) < 0) {
						if (string1.compareTo(previousString) < 0) {
							string2 = flush(reader2, writer, string2, previousString);
							break;
						}
						System.out.println("Wrote string 1");
						writer.write(string1);
						writer.newLine();

						previousString = string1;
						string1 = reader1.readLine();
					}
					else if (string1.compareTo(string2) >= 0) {
						if (string2.compareTo(previousString) < 0) {
							string1 = flush(reader1, writer, string1, previousString);
							break;
						}
						System.out.println("Wrote string 2");
						writer.write(string2);
						writer.newLine();

						previousString = string2;
						string2 = reader2.readLine();
					}
					else {
						// Need to start new run
						break;
					}
				}
				if (outputT == outputT1) {
					writer = writer2;
					outputT = outputT2;
				}
				else {
					writer = writer1;
					outputT = outputT1;
				}
				previousString = "";
			}
			writer1.close();
			writer2.close();
			reader1.close();
			reader2.close();
		}
		catch (IOException error) {
			System.err.println(error.getMessage());
		}

		if (runCounter < 2) {
			// we are done!
			return true;
		}
		return false;
	}

	public static void createInitialRuns(int runLength, int k) {
		String line;
		String[] run = new String[runLength];
		int actualRunLength = runLength;

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		if (k > 0) {
			k = 1;
		}

		try {
			// While there are lines coming in
			while (actualRunLength == runLength) {
				// Reset actual run length
				actualRunLength = 0;

				// While the run length hasn't been exceeded
				while (actualRunLength < runLength) {

					// Read the next line
					if ((line = reader.readLine()) != null) {
						// Append the current line to the current run
						run[actualRunLength] = line;
						// Increment the actual run length
						actualRunLength++;
					}
					else {
						// Once we read an empty line the stream is over
						break;
					}
				}
				// Heapsort complete run
				heapsort(run, actualRunLength);
				if (k == 0) {
					// Print it to standard out
					for (int i = 0; i < actualRunLength; i++) {
						System.out.println(run[i]);
					}
				} else {
					// Move this upper bound up to create more initial runs
					if (k > 2) {
						k = 1;
					}
					writeRunToTape(run, actualRunLength, k);
					k++;
				}
			}
		}
		catch (IOException error) {
			System.err.printf("Exeption occured when reading from standard out: {0}", error.getMessage());
		}
	}

	public static void writeRunToTape(String[] run, int runLength, int k) {
		String tapeName = "tape"+k;

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(tapeName, true))) {
			for (int i = 0; i<runLength; i++) {
				writer.write(run[i]);	
				writer.newLine();
			}
		} catch (IOException error) {
			System.err.printf("Exeption occured when writing to tape {0}: {1}", tapeName, error.getMessage());
		}
	}

	public static void upheap(String[] arr, int parentIndex, int heapLength) {
		int leftIndex;
		int rightIndex;
		int largestIndex = parentIndex;

		// Up to and including the top node
		while (parentIndex >= 0) {
			// Calc indicies of left and right children
			leftIndex = parentIndex*2+1;
			rightIndex = parentIndex*2+2;

			// If left child index out of bound
			if (leftIndex >= heapLength) {
				// ...this node is a leaf node that has nothing to upheap
				break;
			}

			// If the left child is larger than the current largest node (parent), it is the
			// new largest node
			if (arr[leftIndex].compareTo(arr[largestIndex]) > 0) {
				largestIndex = leftIndex;
			}
			// If the right child exists
			if (rightIndex < heapLength) {
				// If the right child is larger than the current largest node, it is the
				// new largest node
				if (arr[rightIndex].compareTo(arr[largestIndex]) > 0) {
					largestIndex = rightIndex;
				}
			}

			// If we changed the largest node from the parent
			if (largestIndex != parentIndex) {
				// Swap parent with largest child
				String parent = arr[parentIndex];
				arr[parentIndex] = arr[largestIndex];
				arr[largestIndex] = parent;
			}
			// Otherwise this node has nothing to upheap
			else {
				break;
			}
			// Upheap with the new parent
			parentIndex = largestIndex;
		}
	}

	public static void heapify(String[] arr, int heapLength) {
		// Starting at the last node that has children, upheap from every node going up
		for (int i = heapLength / 2; i >=0; i--) {
			upheap(arr, i, heapLength);	
		}
	}

	public static void heapsort(String[] arr, int heapLength) {
		String largest;

		// Heapify the array
		heapify(arr, heapLength);

		// While the heap part of the array has more than one node
		while (heapLength > 0) {
			// Swap the head node (max) with the last node in heap array
			largest = arr[0];
			arr[0] = arr[heapLength-1];
			arr[heapLength-1] = largest;
			// Reduce heap part of the array
			heapLength--;
			// Upheap from the new head node
			upheap(arr, 0, heapLength);
		}
	}

}
