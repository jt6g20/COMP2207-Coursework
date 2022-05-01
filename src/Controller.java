import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
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
            for (;;) {
                try {
                    System.out.println("waiting for connection");
                    Socket client = socket.accept();
                    InputStream in = client.getInputStream();
                    OutputStream out = client.getOutputStream();
                    PrintStream ps = new PrintStream(out);
                    try {
                        System.out.println("connected");
                        for (;;) {
                            byte[] buf = new byte[1000];
                            int buflen = in.read(buf);
                            String firstBuffer = new String(buf,0,buflen);
                            System.out.println("INPUT - " + firstBuffer);

                            if (firstBuffer.startsWith("LIST")) {
                                String file_list = "LIST " + listToString(index.keySet());
                                System.out.println(file_list);
                                //Not sure why this line doesn't work?
//                                ps.write(file_list.getBytes(StandardCharsets.UTF_8));
                                ps.println(file_list);
                            } else {
                                int firstSpace = firstBuffer.indexOf(" ");
                                String command = firstBuffer.substring(0,firstSpace);
                                System.out.println(command);

                                if (command.equals("STORE")) {
                                    int secondSpace = firstBuffer.indexOf(" ",firstSpace + 1);
                                    String fileName = firstBuffer.substring(firstSpace+1, secondSpace);
                                    String fileSize = firstBuffer.substring(secondSpace+1);
                                    System.out.println("STORE " + fileName + " " + fileSize);
                                    index.put(fileName, "store in progress");
                                }

                                if (command.equals("LOAD")) {
                                    String fileName = firstBuffer.substring(firstSpace+1);
                                    System.out.println("LOAD " + fileName);
//                                  Controller selects one the R Dstores that stores that file, let port be its endpoint
                                    ps.println("LOAD_FROM 1234 1234");
                                }
                                if (command.equals("REMOVE")) {
                                    String fileName = firstBuffer.substring(firstSpace+1);
                                    index.put(fileName, "remove in progerss");
                                    index.put(fileName, "remove complete");
                                    ps.println("REMOVE_COMPLETE");
                                }
                            }
                        }

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
