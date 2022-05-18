import java.io.*;
import java.net.*;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
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


        File folder = new File(fileFolder);
        for (File file : folder.listFiles()) {
            if (!file.getName().endsWith(".class")) file.delete();
        }

        try {
            //For listening for a client
            ServerSocket socket = new ServerSocket(port);
            //For communicating with the controller
            String host = InetAddress.getLocalHost().getHostAddress();
            Socket controller = new Socket(host, cport);
            for(;;) {
                try {
                    PrintWriter pw = new PrintWriter(controller.getOutputStream(), true);
                    pw.println("DSTORE connected " + port);

                    System.out.println("waiting for connection");
                    Socket client = socket.accept();
                    System.out.println("client connected");

                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                OutputStream out = client.getOutputStream();
                                InputStream in = client.getInputStream();
                                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                                PrintWriter clientPw = new PrintWriter(client.getOutputStream(), true);

                                String firstBuffer = br.readLine();
                                System.out.println("INPUT - " + firstBuffer);
                                String[] clientArgs = firstBuffer.split(" ");
                                String command = clientArgs[0];

                                if (command.startsWith("STORE")) {
                                    String fileName = clientArgs[1];
                                    int fileSize = Integer.parseInt(clientArgs[2]);
                                    System.out.println("STORE " + fileName + " " + fileSize);

                                    clientPw.println("ACK");

                                    File outputFile = new File(fileFolder + "/" + fileName);
                                    FileOutputStream outFile = new FileOutputStream(outputFile);
                                    outFile.write(in.readNBytes(fileSize));
                                    outFile.close();

                                    System.out.println("STORE_ACK " + fileName);
                                    pw.println("STORE_ACK " + fileName);
                                } else if (command.startsWith("LOAD_DATA")) {
                                    String fileName = clientArgs[1];
                                    File inputFile = new File(fileFolder + "/" + fileName);
                                    if (inputFile.exists()) {
                                        FileInputStream inf = new FileInputStream(inputFile);
                                        out.write(inf.readAllBytes());
                                        inf.close();
                                    }
                                } else if (command.startsWith("REMOVE")) {
                                    String fileName = clientArgs[1];
                                    File file = new File(fileFolder + "/" + fileName);
                                    System.out.println(Arrays.toString(new File(fileFolder).listFiles()));
                                    if (file.exists()) {
                                        file.delete();
                                        System.out.println(fileName + " was deleted");
                                        pw.println("REMOVE_ACK " + fileName);
                                    } else {
                                        pw.println("ERROR_FILE_DOES_NOT_EXIST " + fileName);
                                    }
                                }

                                client.close();

                            } catch (Exception e) { System.out.println("error "+e); }
                        }
                    }).start();

                } catch (Exception e) { System.out.println("error "+e); }
            }
        } catch (Exception e) { System.out.println("error "+e); }
        System.out.println();
    }
}