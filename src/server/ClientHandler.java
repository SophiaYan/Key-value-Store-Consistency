package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientHandler extends Thread {
	BufferedReader socketInput;
	private Socket socket;

	// Constructor
	public ClientHandler(Socket socket) {
		this.socket = socket;
	}

	public void run() {
		try {

			socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        	while(true) {
        		String rawInput = socketInput.readLine();
        		
        		//ReplicaServer.getData(0);
        		
                if (rawInput == null) {
                    return;
                }
                
                System.out.println(rawInput);
                                
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