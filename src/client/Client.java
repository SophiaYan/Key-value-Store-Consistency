package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
	BufferedReader in;
    PrintWriter out;
    Socket socket;
    
    
    
    public Client(String serverAddr, int serverPort) throws IOException {
		socket = new Socket(serverAddr, serverPort);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    out = new PrintWriter(socket.getOutputStream(), true);
	}


    private void run() throws IOException {    
    	try (
    			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
    		) 
    	{
    		System.out.println("Now you can type below: ");
        	String userInput;
        	while ((userInput = stdIn.readLine()) != null) {
        		out.println(userInput);
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
