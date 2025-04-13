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
			diog = "###Issues with output for source line###" + "\n";
			diog += sourceLine + "\n";
			// +1 because our KMPSearch should output 1-based indexing
			int actualLineIndex = sourceLine.indexOf(pattern) + 1;

			String[] outputArr = outputLine.split(": ");
			String reportedLine = outputArr[1];
			int reportedLineIndex = Integer.valueOf(outputArr[0]);
			// +1 because our KMPSearch should output 1-based indexing
			int indexInReportedLine = reportedLine.indexOf(pattern) + 1;

			if (actualLineIndex < 0) {
				if (reportedLine.equals(sourceLine)) {
					isIssue = true;
					diog += "--False hit--\n";
					diog += outputLine + "\n";
					// Skip past the false hit
					outputLine = outputReader.readLine();
				}
				sourceLine = sourceReader.readLine();
				continue;
			}
			// There is an instance of a pattern in this source line
			if (!reportedLine.equals(sourceLine)) {
				isIssue = true;
				diog += "--Line miss-match--" + "\n";
				diog += "Source line:" + sourceLine + "\n";
				diog += "Output line:" + reportedLine + "\n";
			}
				
			if (actualLineIndex != reportedLineIndex) {
				isIssue = true;
				diog += "--Index miss-match--" + "\n";
				diog += "Actual index:" + actualLineIndex + "\n";
				diog += "Output index:" + reportedLineIndex + "\n";
			}

			if (reportedLineIndex != indexInReportedLine) {
				isIssue = true;
				diog += "--Internal index miss-match--" + "\n";
				diog += "Actual index:"  + actualLineIndex + "\n";
				diog += "Output index:"  + reportedLineIndex + "\n";
				diog += "Index in output line:" + indexInReportedLine + "\n";
			}
			if (isIssue) {
				isIssue = false;
				System.out.print(diog);
			}
			outputLine = outputReader.readLine();
			sourceLine = sourceReader.readLine();
		}
		sourceReader.close();
		if (outputLine != null) {
			System.out.println("!!Extra lines in output!!");
			while (outputLine != null) {
				System.out.println(outputLine);
				outputLine = outputReader.readLine();
			}
		}
	}
}
