import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * A Knuth-Morris-Pratt string search implementation for finding occurances of a substring in the lines of a file.
 * Can either output skip table of the substring or lines from the specified file which contain this substring, 
 * preceded by the base-1 index of its first occurance in each line. 
 * Either outputs are made to standard out.
 *
 * @author Oleksandr Kashpir ID:1637705
 */
public class KMPSearch {
	/**
	 * Program entry point. Verifies that a valid numer of arguments has been passed 
	 * and creates a new <code>KMPTable</code> for the given substring.
	 * If a filename is also passed, calls function to search the lines of the corresponding file for the substring,
	 * otherwise, outputs the string representation of the <code>KMPTable</code>.
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("0 arguments passed. pattern is a required argument");
			System.exit(1);
		}
		if (args.length > 2) {
			String errorMess = String.format("Invalid number of arguments: %d. No more than 2 arguments allowed", args.length);
			System.err.println(errorMess);
			System.exit(1);
		}

		String pattern = args[0];
		KMPTable kmpTable = new KMPTable(pattern);

		if (args.length > 1) {
			String filename = args[1];
			searchFile(filename, kmpTable);
		}
		else {
			System.out.print(kmpTable);
		}
	}

	/**
	 * Searches the lines of a file for occurances of the pattern represented by the <code>kmpTable</code>.
	 *
	 * @param filename  name of the file to search through
	 * @param kmpTable  the KMP skip table to use for this search
	 */
	public static void searchFile(String filename, KMPTable kmpTable) { 
		try  {
			// Try open the file specified by the filename
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = reader.readLine();

			// Search every line
			while (line != null) {
				searchLine(line, kmpTable);
				line = reader.readLine();
			}

			reader.close();
		} 
		catch (IOException error) {
			String errorMess = String.format("Error reading from file: %s", error.getMessage());
			System.err.println(errorMess);
			System.exit(1);
		}
	}

	/**
	 * Searches the given line for an occurance of the pattern represented by the <code>kmpTable</code>
	 * If an occurance is found, outputs this line to standard output, preceded by:
	 * a 1-based index into that line where the pattern first occures,
	 * followed by a space.
	 *
	 * @param text  line to search
	 * @param the KMP skip table to use for this search
	 */
	public static void searchLine(String text, KMPTable kmpTable) {
		char TChar;
		int TIndex, PIndex, T, P, skip;

		TIndex = PIndex = skip = 0;

		P = kmpTable.getPatternLength();
		T = text.length();
		
		// While there is still space for the pattern to occure in the text
		while (TIndex + P <= T) {
			// Get the charecter at the current text index offset by the pattern index
			TChar = text.charAt(TIndex + PIndex);
			// Calcuate skip for the observed charecter
			skip = kmpTable.getSkip(TChar, PIndex);
			// Apply the skip to the text index
			// Move text index skip many charecters forward
			TIndex += skip;
			// Charecters after the good prefix are new and need to be considered
			// This way if skip is 0, the next charecter in the text is considered
			PIndex += -1*skip + 1;

			// If PIndex reaches the length of the pattern, the pattern charecters appear consecutively in the text
			// i.e. there is a substring of the line that matches the target pattern substring
			if (PIndex == P) {
				// Increment 1 for 1-based index into text
				TIndex++;
				// Output the line
				System.out.println(TIndex + " " + text);
				return;
			}
		}
	}


	/**
	 * A Knuth-Morris-Pratt skip table implementation that computes and stores the skip table for a given string <code>pattern</code>.
	 * Can be queried for how far to move the search in a string, given a charecter at some position in the pattern.
	 * This class is static so that it can be initialized from the parent class's static <code>main</code> method.
	 *
	 * @author Oleksandr Kashpir ID:1637705
	 */
	static class KMPTable {
		private int[][] KMPTable;
		// patternAlphabet stores the sorted array of unique charecters in the pattern
		private String pattern, patternAlphabet;

		/**
		 * Initialies a <code>KMPTable</code> by building the skip table for
		 * the passed <code>pattern</code>.
		 *
		 * @param pattern  substring to build skip table for
		 */
		public KMPTable(String pattern) {
			this.pattern = pattern;
			// Create the pattern's alphabet before building its skip array
			this.patternAlphabet = orderPattern();
			this.KMPTable = buildKMPTable();
		}

		/**
		 * Gets the charecter length of the underlying pattern.
		 *
		 * @return  the charecter length of the underlying pattern
		 */
		public int getPatternLength() {
			return pattern.length();
		}

		/**
		 * Builds the skip table.
		 *
		 * @return  the skip tabl for this <code>KMPTable</code>'s <code>pattern</code>
		 */
		private int[][] buildKMPTable() {
			String observedString;
			char currPatChar, currRowChar;
			int skip;
			// The skip table just stores the skip values, without the first row or column,
			// for more intuitive indexing
			int[][] KMPTable = new int[patternAlphabet.length()+1][pattern.length()];

			for(int row = 0; row <= patternAlphabet.length(); row++) {
				for (int col = 0; col < pattern.length(); col++) {
					// The last row's vaules are 1-based indices of the pattern charecters
					if (row == patternAlphabet.length()) {
						KMPTable[row][col] = col+1;
						continue;
					}
					
					currPatChar = pattern.charAt(col);
					currRowChar = patternAlphabet.charAt(row);
					// If the current column contains the charecter of this row...
					if (currPatChar == currRowChar) {
						// No need to skip
						KMPTable[row][col] = 0;
					}
					else {
						// Find the skip for this 
						// Concatinate the charecter of this row to the preciding charecters of the pattern
						observedString = pattern.substring(0, col) + currRowChar;
						// Find and store the skip
						skip = findSkip(observedString);
						KMPTable[row][col] = skip;
					}
				}
			}

			return KMPTable;
		}

		/**
		 * Finds the longest good prefix to the observed string
		 *
		 * @return  the minimum skip for a suffix of <code>observedString</code> to line up with a prefix of this pattern, if possible;
		 * 					the length of <code>observedString</code> otherwise.
		 */
		public static int findSkip(String observedString) {
			String patternPrefix, stringSuffix;
			// At most the length of the observed string can be skipped
			int maxSkip = observedString.length();

			// Compare increasingly shorter prefixes of the pattern with increasingly shorted suffixes of the observed string,
			// starting with all but the last, invalid, charecter of the observed string as the prefix, such that i is the minimum number 
			// of charecters to skip so that the suffix of the observed string lines up with a prefix of the pattern
			for (int i = 1; i < maxSkip; i++) {
				// Get all but the last i charecter of the observed string
				patternPrefix = observedString.substring(0, maxSkip-i);
				// Get the last i charecter of the observed string
				stringSuffix = observedString.substring(i, maxSkip);

				// If the resulting strings are equal, it is sufficient to skip the first i charecters in the observed string
				if (patternPrefix.equals(stringSuffix)) {
					return i;
				}
			}
			// If there is no good prefix, return the maximum possible skip
			return maxSkip;
		}

		/**
		 * Creates an ordered alphabet of the pattern.
		 */
		private String orderPattern() {
			int next;
			char currChar;

			char[] patternChars = this.pattern.toCharArray();
			int unsortedLength = patternChars.length - 1;
			int offset = 0;

			// Sort the pattern charecters with bubble sort and simultaneously remove duplicates
			// For all but the last position in the pattern
			for (int i = 0; i < unsortedLength; i++) {
				// For the remaining unsorted charecters, bubble up the greatest
				for (int curr = 0; curr < unsortedLength - i; curr++) {
					// 0 should never bubble up
					if (patternChars[curr] == 0) {
						continue;
					}

					next = curr + 1;
					// Remove duplicates
					// Duplicate charecters will be adjescent at some point
					// because they should bubble up to the same level.
					// Set current charecter to 0, to be bubbled past by the other charecters
					if (patternChars[curr] == patternChars[next]) {
						patternChars[curr] = 0;
						// Offset keeps track of how many first chareecters are 0s
						offset++;
					}
					// Bubble up charecters if its greater than the next
					else if (patternChars[curr] > patternChars[next]) {
						currChar = patternChars[curr];
						patternChars[curr] = patternChars[next];
						patternChars[next] = currChar;
					}
				}
			}

			// Return a string of all the unique charecters, all previous charecters are 0s in place of duplicates
			return new String(patternChars, offset, patternChars.length-offset);
		}

		/**
		 * Returns the correct skip given a charecter and the index of its occurance in this pattern.
		 *
		 * @param TChar   The observed charecter
		 * @param PIndex  The index into the pattern that is occures at
		 * @return        The correct skip value for KMP search
		 */
		public int getSkip(char TChar, int PIndex) {
			// Get index of observed charecter in the pattern alphabet, 
			// which the skip table rows are indexed by
			int row = patternAlphabet.indexOf(TChar);

			// If it does not occure in the alphabet, it does not occure in the pattern
			// so the last wildcard row of the table should be used
			if (row < 0) {
				row = KMPTable.length - 1;
			}
			int col = PIndex;

			return KMPTable[row][col];
		}

		/**
		 * Returns a <code>String</code> representation of this <code>KMPTable</code>, including this <code>pattern</code>
		 * as the first row and this <code>patternAlphabet</code> as the first column
		 *
		 * @return  <code>String</code> representation of the underlying skip table
		 */
		@Override
		public String toString() {
			String stringRep = "";

			for (int row = -1; row < KMPTable.length; row++) {
				// Append patternAlphabet charecter to start of every row,
				// forming first column
				if (row == -1 || row == KMPTable.length - 1) {
					stringRep += "*";
				}
				else {
					stringRep += patternAlphabet.charAt(row);
				}

				for (int col = 0; col < pattern.length(); col++) {
					stringRep += ",";

					// First row should contain the pattern charecters
					if (row == -1) {
						stringRep += pattern.charAt(col);
						continue;
					}

					stringRep += KMPTable[row][col];
				}
				stringRep += "\n";
			}

			return stringRep;
		}
	}
}
