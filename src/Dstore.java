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
            //For communicating with whatever talks to this Dstore, client or controller
            ServerSocket socket = new ServerSocket(port);
            //For communicating with the controller
            Socket controller = new Socket("Desktop", cport);
            for(;;) {
                try {
                    System.out.println("waiting for connection");
                    Socket client = socket.accept();
                    try {
                        System.out.println("connected");
                        InputStream in = client.getInputStream();
                        byte[] buf = new byte[1000];
                        int buflen = in.read(buf);
                        String firstBuffer = new String(buf,0,buflen);

                        if (firstBuffer.startsWith("STORE")) {
                            int firstSpace = firstBuffer.indexOf(" ");
                            int secondSpace = firstBuffer.indexOf(" ",firstSpace+1);
                            int thirdSpace = firstBuffer.indexOf(" ", secondSpace + 1);
                            String fileName = firstBuffer.substring(firstSpace+1,secondSpace);
                            String fileSize = firstBuffer.substring(secondSpace+1, thirdSpace);

                            PrintStream ps = new PrintStream(client.getOutputStream());
                            ps.println("ACK");
                            ps.close();

                            File outputFile = new File(fileName);
                            FileOutputStream out = new FileOutputStream(outputFile);
                            while ((buflen=in.read(buf)) != -1) {
                                System.out.print("*");
                                out.write(buf,0,buflen);
                            }

                            PrintStream cps = new PrintStream(controller.getOutputStream());
                            cps.println("STORE_ACK " + fileName);
                            cps.close();
                        }

                        in.close(); client.close();

                    } catch (Exception e) {}
                } catch (Exception e) { System.out.println("error "+e); }
            }
        } catch (Exception e) { System.out.println("error "+e); }
        System.out.println();
    }
}