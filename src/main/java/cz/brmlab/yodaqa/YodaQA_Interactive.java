package cz.brmlab.yodaqa;

// Thrift java libraries 
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;

// Thrift client-side code
import org.apache.thrift.protocol.TBinaryProtocol;

// Generated code
import QPMThrift.QPM;

import cz.brmlab.yodaqa.flow.MultiCASPipeline;


/* FIXME: Massive code duplication of YodaQA_Interactive and YodaQA_GS.
 * Let's abstract out the processing pipeline later. */

public class YodaQA_Interactive {
	/** 
	* An object whose methods are implementations of the question-answer thrift
	* interface.
	*/
	public static MultiCASPipeline handler;

	/**
	* An object responsible for communication between the handler
	* and the server. It decodes serialized data using the input protocol,
	* delegates processing to the handler, and writes the response
	* using the output protocol.
	*/
	public static QPM.Processor<MultiCASPipeline> processor;


	public static void main(String[] args) throws Exception {
		try {
			// collect the port number
			int tmp_port = 8080;
			if (args.length == 1) {
				tmp_port = Integer.parseInt(args[0].trim());
			} else {
				System.out.println("Using default port for Yoda: "
						+ tmp_port);
			}

			// create a new thread to handle the requests
			final int port = tmp_port;
			handler = new MultiCASPipeline();
			processor = new QPM.Processor<MultiCASPipeline>(handler);
			Runnable simple = new Runnable() {
				public void run() {
					simple(processor, port);
				}
			};      
			new Thread(simple).start();
		} catch (Exception e) {
			e.printStackTrace();
		}   
	}


	/**
	 * Listens for requests and forwards request information
	 * to handler.
	 * @param processor the thrift processor that will handle serialization
	 * and communication with the handler once a request is received.
	 * @param port the port at which the parser service will listen.
	 */
	public static void simple(QPM.Processor<MultiCASPipeline> processor, final int port) {
		try {
			// create a multi-threaded server: TNonblockingServer
			TNonblockingServerTransport transport = new TNonblockingServerSocket(port);
			TNonblockingServer.Args args = new TNonblockingServer.Args(transport)
			.processor(processor)	
			.protocolFactory(new TBinaryProtocol.Factory())
			.transportFactory(new TFramedTransport.Factory());
			TNonblockingServer server = new TNonblockingServer(args);
			System.out.println("Starting Yoda at port " + port + "...");
			server.serve();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
