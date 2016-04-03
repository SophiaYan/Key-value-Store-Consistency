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
	
	public BufferedReader socketInput;
	private PrintWriter socketOutput;
	private Socket socket;
	private int consistency;  // indicate the consistency model, 1 for eventual, 2 for linearizability
	private int writeNum;
	private int readNum;
	private int serverId;
	private String latestReq;
	private listenerThread listener;
	
    private HashMap<Integer, String> ipMap;
    private HashMap<Integer, Integer> portMap; 
    
    /*
     *  client constructor
     */
    public Client(int serverId) throws IOException {
    	
    	ipMap = new HashMap<Integer, String>();
    	portMap = new HashMap<Integer, Integer>();
    	readConfig("config");
    	
		boolean isSucceed; 
		do {
			isSucceed = makeConnection(serverId);
			if (isSucceed){
				this.serverId = serverId;
			}
			serverId = (serverId + 1) % 9;
		} while (!isSucceed);
    		
		consistency = 0;
		writeNum = 0;
		readNum = 0;
		
	}
    
    /**
     * This is a helper function to read the config file and store corresponding info properly
     * @param config
     */
    private void readConfig(String config){
    	// Read and store process info from config file
    	try (BufferedReader configReader = new BufferedReader(new FileReader(config))) {

    		String currLine = configReader.readLine();
    		currLine = null;
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
    
    private boolean makeConnection(int serverId) {
    	try {
    		
    		socket = new Socket(ipMap.get(serverId), portMap.get(serverId));
    		socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    		socketOutput = new PrintWriter(socket.getOutputStream(), true);
    		
    		socketOutput.println("client"); // hands shaking with server
    		
    		try {
    			listener = new listenerThread();
    			listener.start();
    		} catch (Exception e){
    			return false;
    		}
    		return true;
    		
    	} catch (Exception e){
    		return false;
    	}
    }
    


    private void run() throws IOException, InterruptedException {    
    	try (
    			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
    		) 
    	{
    		
    		String userInput = null;
    		System.out.println("Choose consistency model. \n"
					+  "Type 1 for eventual consistency model. Type 2 for linearizability.\n");
        	
    		while (((consistency == 0)  && (userInput = stdIn.readLine()) != null) ){
        		int proposedCons = Integer.parseInt(userInput.substring(0,1));
        		if ((proposedCons == 1) || (proposedCons == 2)){
        			consistency = proposedCons;
        			socketOutput.println("consistency" + consistency);
        		}
        	}
        	
    		System.out.println(" Input W number and R number with space in between.\n");
;        	while ((userInput = stdIn.readLine()) != null){	
        		String [] tokens = userInput.split(" ");
        		int proposeWrite = Integer.parseInt(tokens[0]);
        		int proposeRead = Integer.parseInt(tokens[1]);
        				
        		if ((proposeWrite > 0) && (proposeWrite < 10) && (proposeRead > 0) && (proposeRead < 10)){
        			writeNum = proposeWrite;
        			readNum = proposeRead;
        			socketOutput.println("writeNum" + writeNum);
        			socketOutput.println("readNum" + readNum);
        			break;
        		}
        	}
        	
			System.out.println("Please input request. \n");
        	while ((userInput = stdIn.readLine()) != null) {
        		if (userInput.indexOf("get") == 0){
        			sendRequest(userInput);
        		} else if ((userInput.indexOf("put") == 0)){
        			sendRequest(userInput);
        		} else if (userInput.equals("dump")){
        			sendRequest(userInput);
        		} else if (userInput.indexOf("delay") == 0){
        			delay(userInput);
        		} else {
        			System.out.println("Wrong instruction.\n");
        		}
        	}
    	}
    	socket.close();
    }
    
    private void sendRequest(String req){
    	
    	latestReq = req;
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
    		synchronized(listener){
                try{
                    listener.wait();
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
    		
    	} catch (Exception e) {
    
    		// this is to handle connection lost		
    		int newServerId = (serverId + 1) % 9;
    			
    		boolean isSucceed; 
    		do {
    			isSucceed = makeConnection(newServerId);
   				if (isSucceed){
   					this.serverId = newServerId;
   				}
   				newServerId = (newServerId + 1) % 9;    			
   			} while (!isSucceed);
    		
    		// update info
    		serverId = newServerId;
   			socketOutput.println("writeNum" + writeNum);
   			socketOutput.println("readNum" + readNum);
   		
   			sendRequest(req);
    		
    	}   	
    }
    

    private void delay(String req) throws InterruptedException {
    	String [] tokens = req.split(" ");
    	int delayTime = Integer.parseInt(tokens[1]);
    	TimeUnit.MILLISECONDS.sleep((long) delayTime);
    }
    

	public static void main(String[] args) throws NumberFormatException, IOException, InterruptedException {
    	if (args.length != 1) {
			System.out.println("Please put the id of one replicaserver");
        	System.exit(0);
		}
    	
    	Client c = new Client(Integer.parseInt(args[0]));   // take the id of the server as argument
    	c.run();
    	
    }
	
	private class listenerThread extends Thread {
		
		public listenerThread(){}
		
		public void run(){
			
		     try{
		    	 String receiveMessage;
		         while ((receiveMessage = socketInput.readLine()) != null) { 
		        	 
		        	 if (receiveMessage.indexOf("serverid") == 0) {
		        		 System.out.println("Connecting to server: " + receiveMessage.split(" ")[1]); // greeting from server
		        		 continue;
		        	 }
		        	 
		            synchronized(this){ 
		                notify();
		            }
		            System.out.println(receiveMessage); 
		         }
		         
		      // handle connection lost
		         synchronized(this){ 
		        	 notify();
		         }
		         
		         boolean isSucceed; 
		    	do {
		    		isSucceed = makeConnection(serverId);
		   			serverId = (serverId + 1) % 9;
		   		} while (!isSucceed);
		   		
		   		sendRequest(latestReq);
		         
		      }catch(Exception e){
		    	// handle connection lost
		    	synchronized(this){ 
		            notify();
		        }
		         
		        boolean isSucceed; 
		    	do {
		    		isSucceed = makeConnection(serverId);
		   			serverId = (serverId + 1) % 9;
		   		} while (!isSucceed);
		   		
		   		sendRequest(latestReq);
		      }	
		
		}
		
	}

}


