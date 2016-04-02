package client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Client {
	
	private BufferedReader socketInput;
	private PrintWriter socketOutput;
	private Socket socket;
	private int consistency;  // indicate the consistency model
	private int writeNum;
	private int readNum;
	private int serverId;
	private boolean isFinished;  // indicate whether each operation has finished or not, true for has finished, false for not
	
    private HashMap<Integer, String> ipMap;
    private HashMap<Integer, Integer> portMap; 
    
    /*
     *  client constructor
     */
    public Client(int serverId) throws IOException {
    	ipMap = new HashMap<Integer, String>();
    	portMap = new HashMap<Integer, Integer>();
    	readConfig("/Applications/eclipse-epsilon-1.2-macosx-cocoa-x86_64/CS425MP2/Key-value-Store-Consistency");
    	
    	//this.serverId = serverId;
		boolean isSucceed; 
		do {
			isSucceed = makeConnection(serverId);
			serverId = (serverId + 1) % 9;
		} while (!isSucceed);
    	
		this.serverId = serverId - 1;
		consistency = 0;
		writeNum = 0;
		readNum = 0;
		isFinished = true;
		
	}
    
    private boolean makeConnection(int serverId) {
    	try {
    		socket = new Socket(ipMap.get(serverId), portMap.get(serverId));
    		socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    		socketOutput = new PrintWriter(socket.getOutputStream(), true);
    		return true;
    	} catch (Exception e){
    		return false;
    	}
    }
    
    /**
     * This is a helper function to read the config file and store corresponding info properly
     * @param config
     */
    private void readConfig(String config){
    	// Read and store process info from config file
    	try (BufferedReader configReader = new BufferedReader(new FileReader(config))) {

    		String currLine = configReader.readLine();		
    		
    		while ((currLine = configReader.readLine()) != null) {
    			
					String[] processInfo = currLine.split(" ", 3);
					int id = Integer.parseInt(processInfo[0]);
					String ip = processInfo[1];
					int port = Integer.parseInt(processInfo[2]);

					ipMap.put(id, ip);
					portMap.put(id, port);
					
			}
    	} catch (IOException e) {
			e.printStackTrace();
		}
    }
	

    private void run() throws IOException, InterruptedException {    
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
        		if (userInput.indexOf("get") == 0 && userInput.length() == 5){
        			sendRequest(userInput);
        		} else if ((userInput.indexOf("put") == 0) && (userInput.length() == 7)){
        			sendRequest(userInput);
        		} else if (userInput.equals("dump")){
        			sendRequest(userInput);
        		} else if (userInput.indexOf("delay") == 0){
        			delay(userInput);
        		} else {
        			System.out.println("Wrong instruction.\n");
        		}
        		//out.println(userInput);
        	}
    	}
    	socket.close();
    }
    
    private void sendRequest(String req){
    	
//    	while(!isFinished){
//    	};
    	
    	String [] tokens = req.split(" "); 	
    	String realRequest;
    	if (tokens[0].equals("get")){
    		realRequest = "g" + tokens[1];
    	} else if (tokens[0].equals("dump")) {
    		realRequest = "d";
    	} else {
    		realRequest = "p" + tokens[1] + tokens[2];
    	}
    	
    	try{
    		socketOutput.println(realRequest);
    	} catch (Exception e) {
    		// this is to handle connection lost
    		
    		int newServerId = (serverId + 1) % 9;
    		
    		try {
    			
    			socket = new Socket(ipMap.get(newServerId), portMap.get(newServerId));
    			socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    			socketOutput = new PrintWriter(socket.getOutputStream(), true);
    			serverId = newServerId;
    			isFinished = false;
    			
    		} catch (IOException IOe) {
    			
    			boolean isSucceed; 
    			do {
    				newServerId = (newServerId + 1) % 9;
    				isSucceed = makeConnection(newServerId);
    			} while (!isSucceed);
    			
    			// update info
    			serverId = newServerId;
    			socketOutput.println("writeNum" + writeNum);
    			socketOutput.println("readNum" + readNum);
    			isFinished = false;
    		}
    		
    	}
    	
    	//  reading (you'll get -1 as return value) or writing (an IOException (broken pipe) will be thrown) 
    	//try catch if connection is lost
    }
    

    private void delay(String req) throws InterruptedException {
    	String [] tokens = req.split(" ");
    	int delayTime = Integer.parseInt(tokens[1]);
    	TimeUnit.MILLISECONDS.sleep((long) delayTime);
    }
    

	public static void main(String[] args) throws NumberFormatException, IOException, InterruptedException {
    	if (args.length != 2) {
			System.out.println("Usage: java program serverAddr serverPort");
        	System.exit(0);
		}
    	
    	Client c = new Client(Integer.parseInt(args[1]));   // take the id of the server as argument
    	c.run();
    	
    }
}
