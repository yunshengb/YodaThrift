//Java packages
import java.util.List;
import java.util.ArrayList;

//Thrift java libraries 
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

//Generated code
import QPMstubs.QPM;

/** 
* A Yoda Client that sends a signle query to Yoda Server and prints the results.
*/
public class QPMClient {
	
	public static void main(String [] args) {
		// collect the port number and query
		int port = 8080;

		if (args.length >= 1) {
			port = Integer.parseInt(args[0]);
		}

		String query = "What is the speed of light?";

		if (args.length >= 2) {
			query = args[1];
		}
	 
		// initialize thrift objects
		TTransport transport = new TSocket("clarity08.eecs.umich.edu", port);
		//TTransport transport = new TSocket("localhost", port);
		TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
		QPM.Client client = new QPM.Client(protocol);
		try {
			// talk to the parser
			transport.open();
			System.out.println("///// Connecting to Yoda... It might take a while for Yoda to find an answer. /////");
			String answer = client.handleQuery(query);
			
			System.out.println("///// Answer: /////");
			System.out.println(answer);
			transport.close();
		} catch (TException x) {
			x.printStackTrace();
		}
		
		return;
	}
}
