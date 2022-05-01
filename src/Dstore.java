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
            for(;;) {
                try {
                    PrintWriter pw = new PrintWriter(controller.getOutputStream(), true);
                    pw.println("DSTORE connected");
                    System.out.println("waiting for connection");
                    Socket client = socket.accept();
                    try {
                        System.out.println("connected");
                        InputStream in = client.getInputStream();
                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        byte[] buf = new byte[1000];
                        int buflen = in.read(buf);
                        String firstBuffer = br.readLine();
                        String[] clientArgs = firstBuffer.split(" ");
                        String command = clientArgs[0];

                        if (command.startsWith("STORE")) {
                            String fileName = clientArgs[1];
                            String fileSize = clientArgs[2];

                            PrintStream ps = new PrintStream(client.getOutputStream());
                            pw.println("ACK");

                            File outputFile = new File(fileName);
                            FileOutputStream out = new FileOutputStream(outputFile);
                            while ((buflen=in.read(buf)) != -1) {
                                System.out.print("*");
                                out.write(buf,0,buflen);
                            }

                            pw.println("STORE_ACK " + fileName);
                        }
                    } catch (Exception e) {}
                } catch (Exception e) { System.out.println("error "+e); }
            }
        } catch (Exception e) { System.out.println("error "+e); }
        System.out.println();
    }
}