import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class KMPSearch {
	public static void main(String[] args) {
		if (args.length > 2) {
			System.exit(1);
		}
		if (args.length < 1) {
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

	public static void searchFile(String filename, KMPTable kmpTable) { 
		try  {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = reader.readLine();

			while (line != null) {
				searchLine(line, kmpTable);
				line = reader.readLine();
			}

			reader.close();
		} catch (IOException e) {
			System.err.println("Error reading file");
		}
	}

	public static void searchLine(String text, KMPTable kmpTable) {
		char TChar;
		int TIndex, PIndex, T, P, skip;

		TIndex = PIndex = skip = 0;

		P = kmpTable.getPatternLength();
		T = text.length();
		
		while (TIndex + P <= T) {
			TChar = text.charAt(TIndex + PIndex);
			skip = kmpTable.getSkip(TChar, PIndex);
			TIndex += skip;
			if (skip > 0) {
				PIndex = 0;
				continue;
			}

			PIndex++;
			if (PIndex == P) {
				// Increment 1 because first charecter should be at index 1
				TIndex++;
				System.out.println(TIndex + " " + text);
				return;
			}
		}
	}

	static class KMPTable {
		private int[][] KMPTable;
		private String pattern, patternAlphabet;

		public KMPTable(String pattern) {
			this.pattern = pattern;
			this.patternAlphabet = orderPattern();
			this.KMPTable = buildKMPTable();
		}

		public int getPatternLength() {
			return pattern.length();
		}

		private int[][] buildKMPTable() {
			int[][] KMPTable = new int[patternAlphabet.length()+1][pattern.length()];

			char currPatChar, currLetter;
			for(int row = 0; row <= patternAlphabet.length(); row++) {
				for (int col = 0; col < pattern.length(); col++) {
					if (row == patternAlphabet.length()) {
						KMPTable[row][col] = col+1;
						continue;
					}

					currPatChar = pattern.charAt(col);
					currLetter = patternAlphabet.charAt(row);
					if (currPatChar == currLetter) {
						KMPTable[row][col] = 0;
					}
					else {
						int a = findSkip(pattern.substring(0, col) + currLetter);
						KMPTable[row][col] = a;
					}
				}
			}
			return KMPTable;
		}

		public static int findSkip(String real) {
			String subString;
			String o;
			for (int i = 1; i < real.length(); i++) {
				o = real.substring(0, real.length()-i);
				subString = real.substring(i, real.length());
				if (o.equals(subString)) {
					return i;
				}
			}
			return real.length();
		}

		private String orderPattern() {
			char[] patternChars = this.pattern.toCharArray();
			int unsortedLength = patternChars.length - 1;
			int offset = 0;
			int next;

			for (int i = 0; i < unsortedLength; i++) {
				for (int curr = 0; curr < unsortedLength - i; curr++) {
					// 0 should never bubble up
					if (patternChars[curr] == 0) {
						continue;
					}

					next = curr + 1;
					// Duplicate charecters will be adjescent at some point
					// because they should bubble up to the same level.
					// Set current charecter to 0, to be bubbled past by the other charecters
					if (patternChars[curr] == patternChars[next]) {
						patternChars[curr] = 0;
						offset++;
					}
					// Bubble up greater charecter
					else if (patternChars[curr] > patternChars[next]) {
						char currChar = patternChars[curr];
						patternChars[curr] = patternChars[next];
						patternChars[next] = currChar;
					}
				}
			}

			// Return a string of all the unique charecters, all previous charecters are 0s in place of duplicates
			return new String(patternChars, offset, patternChars.length-offset);
		}

		public int getSkip(char TChar, int PIndex) {
			int row = patternAlphabet.indexOf(TChar);
			if (row < 0) {
				row = KMPTable.length - 1;
			}
			int col = PIndex;

			return KMPTable[row][col];
		}

		@Override
		public String toString() {
			String stringRep = "";
			for (int row = -1; row < KMPTable.length; row++) {
				if (row == -1 || row == KMPTable.length - 1) {
					stringRep += "*";
				}
				else {
					stringRep += patternAlphabet.charAt(row);
				}
				for (int col = 0; col < pattern.length(); col++) {
					stringRep += ",";

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
