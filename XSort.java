import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * An external balanced 2-wa sort merge implementation for sorting <code>String</code> lines read from standard in.
 * Can either output intial runs or sorted lines to standard out.
 *
 * @author Oleksandr Kashpir ID:1637705
 */
public class XSort {
	/**
	 * Program entry point. Verifes value of required first argument <code>maxRunLength</code> 
	 * is between 64 and 1024, and <code>k</code> is 2 if passed. 
	 * Calls function to create initial runs. 
	 * Calls function that will either output initial runs to standard out as the are generated with heapsort,
	 * if <code>k</code> isn't specified, 
	 * or will merge runs 2-way and output final run to standard out,
	 * if <code>k</code> is specified as 2.
	 *
	 * @param args  array of <code>String</code> arguments to the program. 
	 * 							<code>maxRunLength</code> is an integer required in first place,
	 * 							and must be between 64 and 1024 (inclusive).
	 * 							<code>k</code> is an optional integer in second place,
	 * 							and must be 2.
	 */
	public static void main(String[] args) {
		int maxRunLength = Integer.parseInt(args[0]);
		int k = Integer.parseInt(args[1]);

		// Verify passed maxRunLength is withing bounds (64-1024)
		if (maxRunLength >= 1024) {
			String errorMess = String.format("Initial runs length passed is too long: %d. Must be between 64 and 1024 (inclusive)", maxRunLength);
			System.err.println(errorMess);
			System.exit(1);
		} else if (maxRunLength <= 64) {
			String errorMess = String.format("Initial runs length passed is too short: %d. Must be between 64 and 1024 (inclusive)", maxRunLength);
			System.err.println(errorMess);
			System.exit(1);
		}

		// Verify passed k is 2
		if (k != 0 && k != 2) {
			String errorMess = String.format("This is a 2-way balanced sort merge, k must be 2 (%d given)", k);
			System.err.println(errorMess);
			System.exit(1);
		}

		// Pass k so this either outputs to standard out (if k==0) or into two files
		createInitialRuns(maxRunLength, k);	

		if (k != 0) {
			mergeRuns();
		}
	}

	/**
	 * Creates runs of <code>String</code> lines of length up to <code>maxRunLength</code> from standard in,
	 * sorts each one with heapsort and:
	 * If <code>k</code> == 0 outputs the resulting run to standard out.
	 * Else, alternately write the run to a tape (a file, named between 'tape1' - 'tape<code>k</code>').
	 *
	 * @param maxRunLength  the maximum length of a run
	 * @param k  						number of tapes to populate with runs.
	 * 											Function implemented to support any positive k
	 * 											<code>int</code>, however only value 2 is supported
	 * 											by the program
	 */
	public static void createInitialRuns(int maxRunLength, int k) {
		String line;
		String[] run = new String[maxRunLength];
		int actualRunLength = maxRunLength;
		int tapeIndex = 1;

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

			// While the last run was full, so there still may be more lines
			while (actualRunLength == maxRunLength) {
				actualRunLength = 0;

				while (actualRunLength < maxRunLength) {

					// Read the next line and if it isn't empty...
					if ((line = reader.readLine()) != null) {
						run[actualRunLength] = line;

						// Increment the actual run length
						actualRunLength++;
					}
					else {
						// Once we read an empty line the stream is over
						break;
					}
				}

				heapsort(run, actualRunLength);

				// Decide what to do with the run
				if (k == 0) {
					// Print run to standard out
					for (int i = 0; i < actualRunLength; i++) {
						System.out.println(run[i]);
					}
				} else {
					// Write run to tape
					writeRunToTape(run, actualRunLength, tapeIndex);
					tapeIndex++;

					// If the next tape's index would exceed k, go back to first tape
					if (tapeIndex > k) {
						tapeIndex = 1;
					}
				}
			// Possibly create next run...
			}
			reader.close();
		}
		catch (IOException error) {
			String errorMess = String.format("Exeption occured when reading from standard in: %s", error.getMessage());
			System.err.printf(errorMess);
		}
	}

	/**
	 * Writes <code>runLength</code> lines from the passed <code>run</code> array into
	 * a file called 'tape<code>k</code>'.
	 *
	 * @param run  the array to write from
	 * @param runLength  length of <code>run</code> that contanis non-empty <code>String</code>s
	 * @param k 			   index of file to write to
	 */
	public static void writeRunToTape(String[] run, int runLength, int k) {
		String tapeName = "tape"+k;

		// Try create a new BufferedWriiter by creating/opening-and-truncating file 'tapeName' in cwd
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(tapeName))) {
			// Write every non-empty run line
			for (int i = 0; i<runLength; i++) {
				writer.write(run[i]);	
				// This writes a newline charecter as defined by the system property line.separator
				writer.newLine();
			}
		} catch (IOException error) {
			String errorMess = String.format("Exeption occured when writing to tape %s: %s", tapeName, error.getMessage());
			System.err.printf(errorMess);
		}
	}

	/**
	 * Merges the runs alternatley from tape1 & tape2 into tape3 & tape4 files, and vice versa,
	 * until one of those files contains all the input <code>String<code> lines,
	 * ordered in ascending order.
	 */
	public static void mergeRuns() {
		String inputT1 = "tape1";
		String inputT2 = "tape2";
		String outputT1 = "tape3";
		String outputT2 = "tape4";
		String temp1, temp2;

		// Perform initial merge of tape1 & tape2
		boolean sorted = merge(inputT1, inputT2, outputT1, outputT2);

		// While the outcome of merging two files isn't fully sorted 
		while (!sorted) {
			// Perform the merge with the two input and out files switched
			temp1 = inputT1;
			temp2 = inputT2;

			inputT1 = outputT1;
			inputT2 = outputT2;

			outputT1 = temp1;
			outputT2 = temp2;

			sorted = merge(inputT1, inputT2, outputT1, outputT2);
		}
	}
	
	/**
	 * Writes passed <code>line</code> and subsequent lines from a single input file into a single output file until
	 * the current line is either empty or lexicographically less than the previous line (requires new run)
	 *
	 * @param reader  reader of the single input file
	 * @param writer  writer to the single output file
	 * @param line  	starting line to write
	 * @param previousLine  the previous line written to the output file
	 * @return 							the line that this function read but couldn't write to the output file
	 */
	public static String flush(BufferedReader reader, BufferedWriter writer, String line, String previousLine) {
		try {
			while (line != null) {
				if (line.compareTo(previousLine) >= 0) {
					writer.write(line);
					writer.newLine();
					previousLine = line;
					line = reader.readLine();
				}
				else {
					// Current line is lexicographically less than the previous line
					// ...time for a new run
					break;
				}
				// Read next line...
			}
		}
		catch (IOException error) {
			String errorMess = String.format("Exeption occured when flushing tape: %s",error.getMessage());
			System.err.println(errorMess);
		}

		return line;
	}

	/**
	 * Performs a merge cycle by lexicographically merging runs from the two input files 
	 * into the two output files, alternatley. 
	 * Output runs are at least doubled in size, as a run boundry is defined by when either
	 * the file ends or the next line is lexicographically less than the last written line.
	 *
	 * @return <code>true</code> if the function creates a single run;
	 * 				 <code>false</code> otherwise.
	 */
	public static boolean merge(String inputT1, String inputT2, String outputT1, String outputT2) {
		String outputT = outputT1;

		String line1, line2;
		// Any line will, lexicographically, be greater than this string
		// At worst, if its equal, everything will still work
		String smallestLine = "";

		String previousLine = smallestLine;

		BufferedReader reader1, reader2;
		BufferedWriter writer, writer1, writer2;

		int runCounter = 0;
		boolean inputTEmpty = false;

		try {
			reader1 = new BufferedReader(new FileReader(inputT1));
			reader2 = new BufferedReader(new FileReader(inputT2));

			// Create/open-and-truncate output files
			writer1 = new BufferedWriter(new FileWriter(outputT1));
			writer2 = new BufferedWriter(new FileWriter(outputT2));
			writer = writer1;

			line1 = reader1.readLine();
			line2 = reader2.readLine();

			while(!inputTEmpty) {
				runCounter++;
				while (true) {
					// If the end of first input file is reached
					if (line1 == null) {
						// flush the second input file
						line2 = flush(reader2, writer, line2, previousLine);
						// If in this way thhe end of the second input file is reached
						if (line2 == null) {
							// This merge cycle is done
							inputTEmpty = true;
						}
						break;
					}
					// Same logic 
					// ...applied to if the end of the second input file is reached
					else if (line2 == null) {
						line1 = flush(reader1, writer, line1, previousLine);
						if (line1 == null) {
							inputTEmpty = true;
						}
						break;
					}

					// If the line in the first input file is lexicographically less
					if (line1.compareTo(line2) < 0) {
						// If this line is less than the previous line
						if (line1.compareTo(previousLine) < 0) {
							// We need to start a new run after flushing all lines
							// from the second file that belong to this run
							line2 = flush(reader2, writer, line2, previousLine);
							break;
						}
						writer.write(line1);
						// This writes a newline charecter as defined by the system property line.separator
						writer.newLine();

						previousLine = line1;
						// Read the next line from the first input file
						line1 = reader1.readLine();
					}
					// Same logic
					// ...applied to if the line from the second input file is lexicograpgically less 
					// OR equal to the line from the first input file
					else if (line1.compareTo(line2) >= 0) {
						if (line2.compareTo(previousLine) < 0) {
							line1 = flush(reader1, writer, line1, previousLine);
							break;
						}
						writer.write(line2);
						writer.newLine();

						previousLine = line2;
						line2 = reader2.readLine();
					}
					else {
						// Code should break out before this point
						// but as a redundancy, ...need to start new run
						// TODO: REMOVE
						System.err.println("cat");
						break;
					}
					// Compare the two current lines again...
				}
				// Switch output file 
				// ...and update current output file indicator
				if (outputT == outputT1) {
					writer = writer2;
					outputT = outputT2;
				}
				else {
					writer = writer1;
					outputT = outputT1;
				}

				// Reset the previous line so any value is greater than it
				previousLine = smallestLine;

				// Start new run...
			}
			writer1.close();
			writer2.close();
			reader1.close();
			reader2.close();
		}
		catch (IOException error) {
			System.err.println(error.getMessage());
		}

		// If only one run was created, all lines are sorted
		if (runCounter < 2) {
			outputTape(outputT);
			return true;
		}
		return false;
	}

	/**
	 * Writes the lines from a file named <code>outputT</code> to standard out.
	 *
	 * @param outputT  name of file to read from
	 */
	public static void outputTape(String outputT) {
		String line;

		try {
			BufferedReader reader = new BufferedReader(new FileReader(outputT));

			// Read the next line and if it isn't empty...
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
			reader.close();
		}
		catch (IOException error) {
			String errorMess = String.format("An error occured when outputing sorted lines from %s: %s", outputT, error.getMessage());
			System.err.println(errorMess);
		}
	}

	//		----- Heapsort -----
	//		Functions implemented with reference to: https://www.interviewcake.com/concept/java/heapsort

	/**
	 * Bubbles down a value of <code>arr</code> that may be out of place in the array's heap structure,
	 * until its children are both less than it.
	 *
	 * @param arr  array that will be treated as the heap and 
	 * 						 may be modified as a result of the downheap
	 * @param parentIndex  index of the value to downheap
	 * @param heapSize		 length of the heap portion of <code>arr</code>
	 */
	public static void downheap(String[] arr, int parentIndex, int heapSize) {
		int leftChildIndex;
		int rightChildIndex;
		int largestIndex = parentIndex;

		// Down to and including the last node
		while (parentIndex < heapSize) {
			leftChildIndex = parentIndex*2+1;
			rightChildIndex = parentIndex*2+2;

			// If left child index out of bound
			if (leftChildIndex >= heapSize) {
				// ...this node is a leaf node that has nothing to downheap
				break;
			}

			// If the left child is larger than the current largest node (parent), it is the
			// new largest node
			if (arr[leftChildIndex].compareTo(arr[largestIndex]) > 0) {
				largestIndex = leftChildIndex;
			}
			// If the right child exists
			if (rightChildIndex < heapSize) {
				// If the right child is larger than the current largest node, it is the
				// new largest node
				if (arr[rightChildIndex].compareTo(arr[largestIndex]) > 0) {
					largestIndex = rightChildIndex;
				}
			}

			// If we changed the largest node from the parent
			if (largestIndex != parentIndex) {
				// Swap parent with largest child
				String parentValue= arr[parentIndex];
				arr[parentIndex] = arr[largestIndex];
				arr[largestIndex] = parentValue;
			}
			else {
				// Otherwise this node has nothing to downheap
				break;
			}

			// downheap the value from its new position
			parentIndex = largestIndex;
		}
	}

	/**
	 * Transforms <code>arr</code> into a lexicographical max-heap.
	 *
	 * @param arr  array that will be transformed into a heap
	 * @param arrayLength  length of <code>arr</code> that contains non-empty <code>String</code>s
	 */
	public static void heapify(String[] arr, int arrayLength) {
		// Starting at the last node that has children, downheap from every node going up
		for (int i = arrayLength / 2; i >=0; i--) {
			downheap(arr, i, arrayLength);	
		}
	}

	/**
	 * Sorts <code>String</code>s in <code>arr</code> lexicographically via <code>compareTo</code>,
	 * in ascending order.
	 *
	 * Process:
	 * Heapifies <code>arr</code> into a max-heap.
	 * By swapping the heap's head with the lowest node, reducing the heap size by 1 and
	 * then downheaping from the heap's head, elements are gradually appended to the end of the array 
	 * in descending order and the heap shrinks. When the heap size becomes 0 
	 * <code>arr</code> is sorted in ascending order.
	 *
	 * @param arr  array that will be heapsorted in ascending order
	 * @param arrayLength  length of <code>arr</code> that contains non-emtpy <code>String</code>s
	 */
	public static void heapsort(String[] arr, int arrayLength) {
		String largest;
		int heapSize = arrayLength;

		// Heapify the array
		heapify(arr, heapSize);

		// While the heap part of the array has more than one node
		while (heapSize > 0) {
			// Swap the head node (max) with the last node in heap array
			largest = arr[0];
			arr[0] = arr[heapSize-1];
			arr[heapSize-1] = largest;

			// Reduce heap part of the array
			heapSize--;
			// downheap the new heap head
			downheap(arr, 0, heapSize);
		}
	}
}
