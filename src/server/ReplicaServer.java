package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.io.FileReader;
import java.util.*;

public class ReplicaServer {
	
	private ServerSocket listener;
	private String config;
	
	private int minDelay;
	private int maxDelay;
    HashMap<Integer, String> ipMap;
    HashMap<Integer, Integer> portMap; 
	private int processPort;
	private String processIp;
	private int processId;
	
	private static int[] data = new int[26]; // store data from a-z
	
	public static int getData(int index){
		return data[index];
	}
	
	public static void setData(int index, int value){
		data[index] = value;
	}
	
	public ReplicaServer(int port, String config) throws IOException {
		listener = new ServerSocket(port);
		this.config = config;
		processPort = port;
		ipMap = new HashMap<Integer, String>();
		portMap = new HashMap<Integer, Integer>(); 
		//connect with all the other servers
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

					ipMap.put(id, ip);
					portMap.put(id, port);
					
					if (port == processPort){
						processId = id;
					}			
			}
    	} catch (IOException e) {
			e.printStackTrace();
		}

    	//print out results from the configuration file.
    	System.out.println("Finish reading from the configuration file.");
		for (int id : ipMap.keySet()) {
			System.out.println("Added: " + id + " " + ipMap.get(id) + " " + portMap.get(id));
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
    
    
//	/**
//	 * unicast Send msg to process id
//	 * @param id
//	 * @param msg
//	 * @throws IOException
//	 */
//    private void unicast(int id, String realMsg, String type) throws IOException {
//
//    	Socket socket;
//    	if(id == seqId){
//    		socket = new Socket("localhost", seqPort);
//    	}else{
//    		socket = new Socket(ipMap.get(id), portMap.get(id));
//    	}
//
//        try(PrintWriter out = new PrintWriter(socket.getOutputStream(), true)){
//        	if(type == "cosend"){
//        		String timeStampStr = timeStamp.toString();
//        		out.println(type + " " + processId + " " + timeStampStr + " " + realMsg); 
//			}
//        	else{
//        		out.println(type + " " + processId + " " + sendCounter + " " + realMsg);      //processId + timestamp + msg
//        	}
//        	System.out.println("Sent "+ realMsg +" to process "+ id +", system time is ­­­­­­­­­­­­­"+ System.currentTimeMillis());
//        }
//        finally{
//        	 socket.close();
//        }
//       
//    }
    
    
//    private void multicast(String realMsg, String type) {
//    	for(int id : ipMap.keySet()) {
//    		try {
//				unicast(id, realMsg, type);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//    	}
//    }
    
    
    
    private void run() throws IOException {
    	
    	readConfig(config);
    	
    	try {
            while (true) {
                new ClientHandler(listener.accept()).start();
            }
        } finally {
            listener.close(); 
        }

    }	
    
    public static void main(String[] args) throws Exception {
    	
    	if (args.length != 1) {
			System.out.println("Usage: java program serverPort");
        	System.exit(0);
		}	
    	
    	ReplicaServer rs = new ReplicaServer(Integer.parseInt(args[0]), "config");
    	rs.run();
    	
    }
  
}




