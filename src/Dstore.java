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
            ServerSocket socket = new ServerSocket(port);
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
                        System.out.println("INPUT - " + firstBuffer);

                        in.close(); client.close();

                    } catch (Exception e) {}
                } catch (Exception e) { System.out.println("error "+e); }
            }
        } catch (Exception e) { System.out.println("error "+e); }
        System.out.println();
    }
}