package server;

import java.io.IOException;
import java.net.ServerSocket;

public class ReplicaServer {
	
	ServerSocket listener;
	
	public ReplicaServer(int port) throws IOException {
		listener = new ServerSocket(port);
	}
	
    private void run() throws IOException {
    	
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
    	
    	ReplicaServer rs = new ReplicaServer(Integer.parseInt(args[0]));
    	rs.run();
    	
    }
	
}




