import java.io.*;
import java.net.*;
import java.util.*;

public class Dstore {
    public static void main(String[] args) throws IOException {
        //Port to listen to
        int port = Integer.parseInt(args[0]);
        //Controller's port to talk to
        int cport = Integer.parseInt(args[1]);
        //Timeout in milliseconds
        String timeout = args[2];
        //Where to store the data locally
        String fileFolder = args[3];

        try {
            //For listening for a client
            ServerSocket socket = new ServerSocket(port);
            //For communicating with the controller
            Socket controller = new Socket("Desktop", cport);
            //TODO gotta change this from "Destkop" to something general
            for(;;) {
                try {
                    PrintWriter pw = new PrintWriter(controller.getOutputStream(), true);
                    pw.println("DSTORE connected " + port);

                    System.out.println("waiting for connection");
                    Socket client = socket.accept();
                    System.out.println("client connected");
                    InputStream in = client.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    PrintWriter clientPw = new PrintWriter(client.getOutputStream(), true);

                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                for (;;) {
                                    String firstBuffer = br.readLine();
                                    if (firstBuffer == null) continue;
                                    System.out.println("INPUT - " + firstBuffer);
                                    String[] clientArgs = firstBuffer.split(" ");
                                    String command = clientArgs[0];

                                    if (command.startsWith("STORE")) {
                                        String fileName = clientArgs[1];
                                        int fileSize = Integer.parseInt(clientArgs[2]);
                                        //TODO error handling of parseInt
                                        System.out.println("STORE " + fileName + " " + fileSize);

                                        clientPw.println("ACK");

                                        File outputFile = new File(fileName);
                                        FileOutputStream out = new FileOutputStream(outputFile);
                                        out.write(in.readNBytes(fileSize));
                                        out.close();

                                        pw.println("STORE_ACK " + fileName);
                                    }
                                }
                            } catch (Exception e) {}
                        }
                    }).start();

                } catch (Exception e) { System.out.println("error "+e); }
            }
        } catch (Exception e) { System.out.println("error "+e); }
        System.out.println();
    }
}