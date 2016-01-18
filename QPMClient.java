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

public class QPMClient {
	
  public static void main(String [] args) {
  	// collect the port number
  	int port = 8080;
  	
   
    // initialize thrift objects
    TTransport transport = new TSocket("localhost", port);
    TProtocol protocol = new TBinaryProtocol(new TFramedTransport(transport));
    QPM.Client client = new QPM.Client(protocol);
    try {
    	// talk to the parser
      transport.open();
      System.out.println("Connecting to Yoda...");
      String answer = client.handleQuery("What is the speed of light");
      
      System.out.println("Answer: " + answer);
      transport.close();
    } catch (TException x) {
      x.printStackTrace();
    }
    
    return;
  }
}
