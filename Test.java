import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Test {
	public static void main(String[] args) throws IOException {
		String pattern = args[0];
		String source = args[1];

		BufferedReader sourceReader = new BufferedReader(new FileReader(source));
		BufferedReader outputReader = new BufferedReader(new InputStreamReader(System.in));
		String sourceLine = sourceReader.readLine();
		String outputLine = outputReader.readLine();
		String diog = "";
		boolean isIssue = false;

		while (sourceLine != null) {
			if (isIssue) {
				isIssue = false;
				System.out.print(diog);
			}

			diog = "###Issues with output for source line###" + "\n";
			diog += sourceLine + "\n";

			// Process source line
			// +1 because our KMPSearch should output 1-based indexing
			int actualLineIndex = sourceLine.indexOf(pattern) + 1;

			// If there is no pattern in current source line, there shouldn't be an output line for it
			if (actualLineIndex <= 0) {
				sourceLine = sourceReader.readLine();
				continue;
			}

			// Check if program output missed the pattern in a line
			if (outputLine == null) {
				if (actualLineIndex > 0) {
					isIssue = true;
					diog += "--Line missing in output--" + "\n";
					diog += "Actual index at:" + actualLineIndex + "\n";
				}
				sourceLine = sourceReader.readLine();
				continue;
			}

			// Otherwise, proccess output line
			String[] outputArr = outputLine.split(" ", 2);
			String reportedLine = outputArr[1];
			int reportedLineIndex = Integer.valueOf(outputArr[0]);
			// +1 because our KMPSearch should output 1-based indexing
			int indexInReportedLine = reportedLine.indexOf(pattern) + 1;

			// Outputted wrong line
			if (!reportedLine.equals(sourceLine)) {
				isIssue = true;
				diog += "--Line miss-match--" + "\n";
				diog += "Source line:" + sourceLine + "\n";
				diog += "Output line:" + reportedLine + "\n";
			}
				
			// Outputted wrong index
			if (actualLineIndex != reportedLineIndex) {
				isIssue = true;
				diog += "--Index miss-match--" + "\n";
				diog += "Actual index:" + actualLineIndex + "\n";
				diog += "Output index:" + reportedLineIndex + "\n";
			}

			// Outputted wrong index for the line outputed
			if (reportedLineIndex != indexInReportedLine) {
				isIssue = true;
				diog += "--Internal index miss-match--" + "\n";
				diog += "Actual index:"  + actualLineIndex + "\n";
				diog += "Output index:"  + reportedLineIndex + "\n";
				diog += "Index in output line:" + indexInReportedLine + "\n";
			}
			outputLine = outputReader.readLine();
			sourceLine = sourceReader.readLine();
		}
		sourceReader.close();

		if (outputLine != null) {
			System.out.println("--Extra lines in output--");
			while (outputLine != null) {
				System.out.println(outputLine);
				outputLine = outputReader.readLine();
			}
		}
	}
}
