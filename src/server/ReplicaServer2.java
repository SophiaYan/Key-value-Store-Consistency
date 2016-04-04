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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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
	private HashMap<Integer, BlockingQueue<String>> clientBQMap;
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
		clientBQMap = new HashMap<Integer, BlockingQueue<String>>();
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
					
					new GeneralHandler(socket).start(); // start a client handler
					
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
                new GeneralHandler(listener.accept()).start();
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
    
    public class GeneralHandler extends Thread {
    	int serverId;
    	int clientId;
    	BufferedReader socketInput;
    	private PrintWriter socketOutput;
    	private Socket socket;

    	// Constructor
    	public GeneralHandler(Socket socket) {
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
                    if (rawInput.indexOf("serverid") == 0) { // Is a server
                    	serverId = Integer.parseInt(rawInput.split(" ")[1]);
                    	if (!serverSocket.containsKey(serverId)){
                    		serverSocket.put(serverId, socket);
                        	serverPWMap.put(serverId, socketOutput);
                        	serverBRMap.put(serverId, socketInput);
                    	}
                    	/////testing//////
                    	System.out.println("server: " + serverId);
                    	//////////////////
                    	new ServerHandler(serverId).start();
                    	break;
                    }
                    if (rawInput.indexOf("client") == 0) { // Is a client
                    	clientId = numClient++;
                    	/////testing//////
                    	System.out.println("client: " + clientId);
                    	//////////////////
                    	BlockingQueue<String> bq = new ArrayBlockingQueue<String>(256);
                    	clientBQMap.put(clientId, bq);
                    	new ClientHandler(clientId, socket, socketInput, socketOutput, bq).start();
                    	break;
                    }
            	}
            	
            	
            	
//            	while (true) {
//            		String rawInput = socketInput.readLine();
//            		if (rawInput == null) {
//                        return;
//                    }
//            		// something need to be done
//            	}
                 
    		} catch (IOException e) {
                System.out.println(e);
            } 		
    	}
    }
    
    public class ClientHandler extends Thread {
    	int W;
    	int R;
    	int clientId;
    	private BufferedReader socketInput;
    	private PrintWriter socketOutput;
    	private Socket socket;
    	private BlockingQueue<String> bq;
    	private Queue<String> dq;
    	
    	public ClientHandler(int clientId, Socket socket, BufferedReader socketInput, PrintWriter socketOutput, BlockingQueue<String> bq) {
    		this.clientId = clientId;
    		this.socket = socket;
    		this.socketInput = socketInput;
    		this.socketOutput = socketOutput;
    		this.bq = bq;
    		this.dq = new LinkedList<String>();
    	}
    	
    	public void run() {
    		
    		try {

    			
    			while (true) {
    				try {
    					String rawInput = socketInput.readLine();
    					if (rawInput == null) {
                            return;
                        }
    					else if (rawInput.indexOf("writeNum") == 0) {
    						W = Integer.parseInt(rawInput.split(" ")[1]);
    					}
    					else if (rawInput.indexOf("readNum") == 0) {
    						R = Integer.parseInt(rawInput.split(" ")[1]);
    					}
    					else if (rawInput.indexOf("p") == 0) {
    						char variable = rawInput.charAt(1);
    						int value = Integer.parseInt(rawInput.substring(2));
    						data[variable - 'a'] = value; // local write
    						bq.put("A");
    						multicast(clientId + " " + rawInput); // e.g. 1 px3 : client 1 writes 3 to x
    						while(dq.size() < W) {
    							dq.add(bq.take());
    						}
    						bq.clear();
    						socketOutput.println("A");
    						dq.clear();
    					}
    					else if (rawInput.indexOf("g") == 0) {
    						char variable = rawInput.charAt(1);
    						bq.put(new StringBuilder(data[variable - 'a']).toString()); // local read
    						multicast(clientId + " " + rawInput); // e.g. 1 gx : client 1 reads x
    						while(dq.size() < R) {
    							dq.add(bq.take());
    						}
    						bq.clear();
    						System.out.println(dq.peek());
    						socketOutput.println(dq.peek());
    						dq.clear();
    					}
    					else if (rawInput.indexOf("d") == 0) {
    					}
    					else continue; // ignore invalid input
    					
    				} catch (IOException e) {
    					e.printStackTrace();
    				} catch (InterruptedException e) {
						e.printStackTrace();
					}	
            	}
    			
    		} finally {
    			try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}

    	}

		private void multicast(String msg) {
			for (PrintWriter pw : serverPWMap.values()) {
				pw.println(msg);
			}
		}
    }
    
    public class ServerHandler extends Thread {
    	int serverId;
    	private BufferedReader socketInput;
    	private PrintWriter socketOutput;
    	private Socket socket;

    	public ServerHandler(int serverId) {
    		this.serverId = serverId;
    		this.socket = serverSocket.get(serverId);
    		this.socketInput = serverBRMap.get(serverId);
    		this.socketOutput = serverPWMap.get(serverId);
    	}
    	
    	public void run() {
    		try {
    			
            	while (true) {
	        		String rawInput = socketInput.readLine();
	        		if (rawInput == null) {
	                    return;
	                }
	        		
	        		String[] tokens = rawInput.split(" ");
	        		/////testing///////
	        		System.out.println(rawInput);
	        		///////////////////
	        		int clientId = Integer.parseInt(tokens[0]);
	        		if (tokens[1].indexOf("p") == 0) {
	        			String request = tokens[1];
	        			data[request.charAt(1) - 'a'] = Integer.parseInt(request.substring(2));
	        			System.out.println(data[request.charAt(1) - 'a']);
	        			socketOutput.println(clientId + " " + "A"); // e.g. 1 A : finished writing on request from client 1
	        		}
	        		else if (tokens[1].indexOf("g") == 0) {
	        			String request = tokens[1];
	        			int readVal = data[request.charAt(1) - 'a'];
	        			System.out.println(request.charAt(1));
	        			System.out.println(request.charAt(1) - 'a');
	        			System.out.println(data[request.charAt(1) - 'a']);
	        			System.out.println(clientId + " " + readVal);
	        			socketOutput.println(clientId + " " + readVal); // e.g. 1 3 : reply with value 3 on request from client 1
	        		}
	        		else if (tokens[1].indexOf("A") == 0) {
	        			String reply = tokens[1];
	        			clientBQMap.get(clientId).put("A"); // e.g. 1 A : a server just finishing writing on client 1's request, let's tell client 1
	        		} 
	        		else if (tokens[1].charAt(0) - '0' >= 0 && tokens[1].charAt(0) - '0' <= 9) {
	        			String reply = tokens[1];
	        			clientBQMap.get(clientId).put(tokens[1]); // e.g. 1 A : a server just finishing reading on client 1's request, let's tell client 1 what is read
	        		}
	        		else continue; // ignore invalid msg
	        		
	        		// msg processing needs to be done here
            	}
            	
    		} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
    			try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}
    }
    
  
}




