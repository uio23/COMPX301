import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class XSort {
	public static void main(String[] args) {
		int initialRuns = Integer.parseInt(args[0]);

		if (initialRuns > 1024) {
			System.out.println("Initial runs length passed too long. Must be between 64 and 1024");
			return;
		} else if (initialRuns < 64) {
			System.out.println("Initial runs length passed too short. Must be between 64 and 1024");
			return;
		}

		createInitialRuns();	
	}

	public static void createInitialRuns() {
		String line;
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		try {
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
		}
		catch (IOException error) {
			System.out.println("Error reading from standard input");
			System.out.println(error.getMessage());
		}
	}
}
