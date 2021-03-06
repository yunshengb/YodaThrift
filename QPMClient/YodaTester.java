// Java packages
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.StringTokenizer;

/** 
* A Yoda tester that reads questions from an input file and outputs the results to a file.
*/
public class YodaTester {
	// list of queries
	private static ArrayList<Query> queries;

	// port number
	private static int port;

	public static void main(String [] args) {
		// check whether the question file is specified
		if (args.length == 0) {
			System.out.println("Please specify the question file.");
			System.out.println("Format of the question file must be:");
			System.out.println("Where is ...?");
			System.out.println("What is ...?");
			System.out.println("...");
			return;
		}

		// read queries from the question file and add them to the list
		queries = new ArrayList<Query>();
		try {
			readQuestionsFromFile(args[0]);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("*****************************************");
		System.out.println("Finish reading in " + queries.size()
			+ " questions. Going to send them to Yoda.");

		// collect the port number
		port = 8080; // the default port number is 8080
		if (args.length > 1) {
			port = Integer.parseInt(args[1]);
		}

		// send queries to QPMClient and update the queries list when receiving answers
		try {
			askYoda();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("*****************************************");
		System.out.println("Finish asking " + queries.size()
			+ " questions. Going to write the results to a file.");

		// write the results to a file in the current directory
		try {
			writeResultsToFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("*****************************************");
		System.out.println("Done.");

		return;
	}

	// read queries from the question file and add them to the list
	private static void readQuestionsFromFile(String question_file) throws IOException {
		String curr_line = null;
		BufferedReader br = new BufferedReader(new FileReader(question_file));

		// keep reading from the file until the end
		while ((curr_line = br.readLine()) != null) {
			// create a new queries and add it to queries
			queries.add(new Query(curr_line));
		}

		br.close();
		return;
	}

	// send queries to QPMClient and update the queries list when receiving answers
	private static void askYoda() throws IOException {
		for (Query q : queries) {
			// prepare for "./start-Yoda-client.sh (port) (question)"
			String question = q.question;
			System.out.println("*****************************************");
			System.out.println("Asking question " + q.id + ": " + question);
			Integer port_Integer = port;
			String[] command = {"/bin/bash", "start-Yoda-client.sh",
				port_Integer.toString(), question};

			// create and start a new process
			ProcessBuilder pb = new ProcessBuilder(command);
			Date start_date = new Date();
			System.out.println("Start time: " + start_date); // start recording time
			Process process = pb.start();

			// get the answer from the output of QPMClient and update the query object
			BufferedReader reader = 
				new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			int line_id = 0;
			while ((line = reader.readLine()) != null) {
				if (line_id > 1) {
					// skip the first two lines of output which is generated by QPMClient
					// and is not part of the answer
					builder.append(line);
					builder.append(System.getProperty("line.separator"));
				}
				++line_id;
			}
			reader.close();
			String result = builder.toString();
			q.answer = result; // update the query's answer
			Date end_date = new Date();
			System.out.println("End time: " + end_date); // finish recording time
			System.out.println("Answer: ");
			System.out.println(result);
			long duration  = end_date.getTime() - start_date.getTime();
			long diff_in_seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
			long minutes = diff_in_seconds / 60;
			long seconds = diff_in_seconds % 60;
			String duration_str = minutes + " min " + seconds + " sec";
			System.out.println("The process for question " + q.id
				+ " takes " + duration_str + ".\n");
			q.duration = duration_str;  // update the query's duration
		}

		return;
	}

	// write the results to a file in the current directory
	private static void writeResultsToFile() throws IOException {
		System.out.println("*****************************************");
		File file = new File("Yoda results " + new Date());
		System.out.println("Writing the results of queries to " + file);

		// if file doesn't exists, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		// write all results stored in queries
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		for (Query q : queries) {
			bw.write("*****************************************");
			bw.newLine();
			bw.write("Query " + q.id);
			bw.newLine();
			bw.write("Question: " + q.question);
			bw.newLine();
			bw.write("Answer: ");
			bw.newLine();
			bw.write(q.answer);
			bw.newLine();
			bw.write("Duration: " + q.duration);
			bw.newLine();
			bw.newLine();
			bw.newLine();
			bw.newLine();
		}

		bw.close();
		return;
	}

	// class representing a query
	private static class Query {
		private static int id_ = 1;
		private int id;
		private String question;
		private String answer;
		private String duration;

		// constructor of Query
		public Query(String question) {
			this.id = id_;
			++id_;
			this.question = question;
			this.answer = "default answer";
			this.duration = "default duration";
		}
	}
}

