import java.io.*;
import java.net.*;
import java.util.*;

public class Controller {
    public static void main(String[] args) throws IOException {
        //Port to listen to
        int cport = Integer.parseInt(args[0]);
        //Replication factor
        String rFactor = args[1];
        //Timeout in milliseconds
        String timeout = args[2];
        //How long to wait (in seconds) to start the next rebalance operation
        String rebalPeriod = args[3];

        System.out.println(cport + " " + rFactor + " " + timeout + " " + rebalPeriod);

        Map<String, String> index = new HashMap<>();

        index.put("oogabooa.txt", "store complete");
        index.put("weewoo.txt", "store complete");
        index.put("fangfong.txt", "store complete");

        try {
            ServerSocket socket = new ServerSocket(cport);
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
//                        int firstSpace = firstBuffer.indexOf(" ");
//                        String command = firstBuffer.substring(0,firstSpace);

                        if (firstBuffer.startsWith("LIST")) {
                            String file_list = "LIST " + listToString(index.keySet());
                            System.out.println(file_list);
                            PrintStream ps = new PrintStream(client.getOutputStream());
                            ps.println(file_list);
                            ps.close();
                        }

//                        if (command.equals("STORE")) {
//                            int secondSpace = firstBuffer.indexOf(" ",firstSpace + 1);
//                            String fileName = firstBuffer.substring(firstSpace+1, secondSpace);
//                            int thirdSpace = firstBuffer.indexOf(" ", secondSpace + 1);
//                            String fileSize = firstBuffer.substring(secondSpace+1, thirdSpace);
//                            System.out.println("STORE " + fileName + " " + fileSize);
//                            index.put(fileName, "store in progress");
//                            in.close(); client.close();
//                        }



                        in.close(); client.close();

                    } catch (Exception e) {}
                } catch (Exception e) { System.out.println("error "+e); }
            }
        } catch (Exception e) { System.out.println("error "+e); }
        System.out.println();
    }

    static String listToString(Set<String> l) {
        StringBuilder sb = new StringBuilder("");
        for (String s : l) sb.append(" ").append(s);
        sb.deleteCharAt(0);
        return sb.toString();
    }
}
