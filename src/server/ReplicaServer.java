package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.FileReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReplicaServer {
	
    public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			System.out.println("Proper Usage is: java program serverPort");
        	System.exit(0);
		}	

    	ServerSocket listener = new ServerSocket(Integer.parseInt(args[0]));

    	try {
            while (true) {
                new ClientHandler(listener.accept()).start();
            }
        } finally {
            listener.close(); 
        }

    }	
	
}




