package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
	
	private BufferedReader socketInput;
	private PrintWriter socketOutput;
	private Socket socket;
	private int consistency;  // indicate the consistency model
	private int writeNum;
	private int readNum;
    
    /*
     *  client constructor
     */
    public Client(String serverAddr, int serverPort) throws IOException {
		socket = new Socket(serverAddr, serverPort);
		socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		socketOutput = new PrintWriter(socket.getOutputStream(), true);
		consistency = 0;
		writeNum = 0;
		readNum = 0;
	}


    private void run() throws IOException {    
    	try (
    			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
    		) 
    	{
    		String userInput = stdIn.readLine();
    		
        	while ((userInput != null) && (consistency != 0)){
        		System.out.println("Choose consistency model. \n"
						+  "Type 1 for eventual consistency model. Type 2 for linearizability.\n");
        		int proposedCons = Integer.parseInt(userInput);
        		if ((proposedCons == 0) || (proposedCons == 1)){
        			consistency = proposedCons;
        			socketOutput.println("consistency" + consistency);
        		}
        	}
        	
        	while ((userInput != null) && (consistency != 0)){
        		System.out.println("Input W and R with space in between.\n");
        		int proposeWrite = Integer.parseInt(userInput.substring(0,1));
        		int proposeRead = Integer.parseInt(userInput.substring(2,3));
        				
        		if ((proposeWrite > 0) && (proposeWrite < 9) && (proposeRead > 0) && (proposeRead < 9)){
        			writeNum = proposeWrite;
        			readNum = proposeRead;
        			socketOutput.println("writeNum" + writeNum);
        			socketOutput.println("readNum" + readNum);
        		}
        	}
        	
        	while ((userInput = stdIn.readLine()) != null) {
        		
        		//out.println(userInput);
        	}
    	}
    	socket.close();
    }
    

	public static void main(String[] args) throws NumberFormatException, IOException {
    	if (args.length != 2) {
			System.out.println("Usage: java program serverAddr serverPort");
        	System.exit(0);
		}
    	
    	Client c = new Client(args[0], Integer.parseInt(args[1]));
    	c.run();
    	
    }
}
