package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.FileReader;
import java.util.*;

public class ReplicaServer2 {
	
	private ServerSocket listener;
	private String config;
	
	private int minDelay;
	private int maxDelay;
	private HashMap<Integer, String> serverIPMap;
	private HashMap<Integer, Integer> serverPortMap; 
	private HashMap<Integer, BufferedReader> serverBRMap;
	private HashMap<Integer, PrintWriter> serverPWMap;
	private HashMap<Integer, Socket> serverSocket;
//	  private HashMap<Integer, Socket> clientSocket;
//    private HashMap<Integer, String> clientIPMap;
//    private HashMap<Integer, Integer> clientPortMap; 
	private int processPort;
	private String processIp;
	private int processId;
	private int numClient;
	
	
	private static int[] data = new int[26]; // store data from a-z
	
	public static int getData(int index){
		return data[index];
	}
	
	public static void setData(int index, int value){
		data[index] = value;
	}
	
	// Ctor
	public ReplicaServer2(int port, String config) throws IOException {
		numClient = 0;
		listener = new ServerSocket(port);
		this.config = config;
		processPort = port;
		serverIPMap = new HashMap<Integer, String>();
		serverPortMap = new HashMap<Integer, Integer>();
		serverSocket = new HashMap<Integer, Socket>();
		serverBRMap = new HashMap<Integer, BufferedReader>();
		serverPWMap = new HashMap<Integer, PrintWriter>();
		//connect with all the other servers
		readConfig(config);
		
		for (int i=0; i<serverIPMap.size(); i++) {
			if (i + 1 != processId){
				try {
					Socket socket = new Socket(serverIPMap.get(i + 1), serverPortMap.get(i + 1));
//					PrintWriter pw = new PrintWriter(
//							socket.getOutputStream(), true);
//					BufferedReader br = new BufferedReader(
//							new InputStreamReader(socket.getInputStream()));
					
//					pw.println("id " + processId);// Servers hands shaking
					
					new ClientHandler(socket).start(); // start a client handler
					
//					serverSocket.put(i + 1, socket);
//					serverPWMap.put(i + 1, pw);
//					serverBRMap.put(i + 1, br);
					
				} catch (ConnectException ce) {
					System.out.println("Fail to connect to host: " + (i+1));
					continue;
				}
			}	
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
    		String[] delayLimits = currLine.split(" ", 2);
			minDelay = Integer.parseInt(delayLimits[0]);
			maxDelay = Integer.parseInt(delayLimits[1]);

    		while ((currLine = configReader.readLine()) != null) {
    			
					String[] processInfo = currLine.split(" ", 3);
					int id = Integer.parseInt(processInfo[0]);
					String ip = processInfo[1];
					int port = Integer.parseInt(processInfo[2]);

					serverIPMap.put(id, ip);
					serverPortMap.put(id, port);
					
					if (port == processPort){
						processId = id;
					}			
			}
    	} catch (IOException e) {
			e.printStackTrace();
		}

    	//print out results from the configuration file.
    	System.out.println("Finish reading from the configuration file.");
		for (int id : serverIPMap.keySet()) {
			System.out.println("Added: " + id + " " + serverIPMap.get(id) + " " + serverPortMap.get(id));
		}
		System.out.println("Current process Id is :" + processId);
		System.out.println("Bounds of delay in milliSec: " + minDelay + " " + maxDelay);
    }
	
    /*
     * Write client request to the log file of the replicaserver
     */
    private void writeLog(String logMessage) throws IOException{
    	BufferedWriter logWriter = null;
    	try  
    	{
    	    FileWriter fstream = new FileWriter("out" + processId + ".txt", true); //true tells to append data.
    	    logWriter = new BufferedWriter(fstream);
    	    logWriter.write(logMessage);
    	}
    	catch (IOException e)
    	{
    	    System.err.println("Error: " + e.getMessage());
    	}
    	finally
    	{
    	    if(logWriter != null) {
    	    	logWriter.close();
    	    }
    	}
    }
    
    
    
    
    private void run() throws IOException {
    	
    	
    	
    	try {
            while (true) {
                new ClientHandler(listener.accept()).start();
            }
        } finally {
            listener.close(); 
            
        	for(Socket s : serverSocket.values()) {
        		s.close();
        	}
        }


    }	
    
    public static void main(String[] args) throws Exception {
    	
    	if (args.length != 1) {
			System.out.println("Usage: java program serverPort");
        	System.exit(0);
		}	
    	
    	ReplicaServer2 rs = new ReplicaServer2(Integer.parseInt(args[0]), "config");
    	rs.run();
    }
    
    public class ClientHandler extends Thread {
    	int serverId;
    	int clientId;
    	BufferedReader socketInput;
    	private PrintWriter socketOutput;
    	private Socket socket;
    	boolean isClient;

    	// Constructor
    	public ClientHandler(Socket socket) {
    		this.socket = socket;
    	}

    	public void run() {
    		try {
    			socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    			socketOutput = new PrintWriter(socket.getOutputStream(), true);
    			
    			socketOutput.println("serverid " + processId);
    			
            	while(true) {
            		String rawInput = socketInput.readLine();
            		
                    if (rawInput == null) {
                        return;
                    }
                    if (rawInput.indexOf("serverid") == 0) {
                    	serverId = Integer.parseInt(rawInput.split(" ")[1]);
                    	if (!serverSocket.containsKey(serverId)){
                    		serverSocket.put(serverId, socket);
                        	serverPWMap.put(serverId, socketOutput);
                        	serverBRMap.put(serverId, socketInput);
                    	}
                    	isClient = false;
                    	/////testing//////
                    	System.out.println("server: " + serverId);
                    	//////////////////
                    	break;
                    }
                    if (rawInput.indexOf("client") == 0) {
                    	clientId = numClient++;
                    	/////testing//////
                    	System.out.println("client: " + clientId);
                    	//////////////////
                    }
            	}
            	
            	
            	
            	while (true) {
            		String rawInput = socketInput.readLine();
            		if (rawInput == null) {
                        return;
                    }
            		// something need to be done
            	}
                 
    		} catch (IOException e) {
                System.out.println(e);
            } finally {
            	try {
                    socket.close();
                } catch (IOException e) {
                }
            } 		
    	}
    }
  
}




