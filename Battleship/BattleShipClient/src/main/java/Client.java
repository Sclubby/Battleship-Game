import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client extends Thread {

    private Socket socketClient;
    private static ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Serializable> callback;

    public Client(Consumer<Serializable> call) {
        callback = call;
    }

    public void run() {
        boolean connected = false;
        // Keep trying to connect until successful
        while (!connected) {
            try {
                socketClient = new Socket("127.0.0.1", 5555); // Attempt to connect to the server
                out = new ObjectOutputStream(socketClient.getOutputStream());
                in = new ObjectInputStream(socketClient.getInputStream());
                socketClient.setTcpNoDelay(true);
                connected = true; // Update the connected flag upon successful connection
            } catch (Exception e) {
                System.out.println("Failed to connect to the server, retrying in 3 seconds...");
                try {
                    Thread.sleep(3000); // Wait for 3 seconds before retrying
                } catch (InterruptedException ie) {
                    System.out.println("Thread interrupted while waiting to reconnect.");
                    return; // Exit the method if the thread is interrupted
                }
            }
        }

        // Once connected, handle messages from the server
        while (true) {
            try {
                ServerMessage response = (ServerMessage) in.readObject();
                callback.accept(response);
            } catch (Exception e) {
                System.out.println("Error while reading from server.");
                break; // Exit the loop on error
            }
        }
    }

    public void sendRequest(Request request) {
        try {
            out.writeObject(request);
        } catch (IOException e) {
            e.printStackTrace(); // Add proper error handling
        }
    }
}