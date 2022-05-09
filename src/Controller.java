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
        Set<Integer> dstores = new HashSet<>();

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
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    PrintWriter pw = new PrintWriter(client.getOutputStream(), true);
                    new Thread(new Runnable(){
                        public void run() {
                            try {
                                System.out.println("connected");
                                for (;;) {
                                    byte[] buf = new byte[1000];
//                                  int buflen = in.read(buf);
                                    String firstBuffer = br.readLine();
                                    if (firstBuffer == null) continue;
                                    System.out.println("INPUT - " + firstBuffer);
                                    String[] clientArgs = firstBuffer.split(" ");
                                    String command = clientArgs[0];

                                    if (command.equals("DSTORE")) {
                                        System.out.println("Dstore port added: " + client.getPort());
                                        dstores.add(client.getPort());
                                    }

                                    if (command.startsWith("LIST")) {
                                        //Have to use starts with? command is 6 characters long with LIST??
                                        String file_list = "LIST " + listToString(index.keySet());
                                        System.out.println(file_list);
                                        //Not sure why this line doesn't work?
//                                pw.write(file_list.getBytes(StandardCharsets.UTF_8));
                                        pw.println(file_list);
                                        //TODO failure handling
                                    } else {
                                        if (command.equals("STORE")) {
                                            String fileName = clientArgs[1];
                                            String fileSize = clientArgs[2];
                                            System.out.println("STORE " + fileName + " " + fileSize);
                                            index.put(fileName, "store in progress");
                                            System.out.println("STORE_TO " + listToString(dstores));
                                            pw.println("STORE_TO " + listToString(dstores));
                                            //TODO failure handling
                                        }

                                        if (command.equals("LOAD")) {
                                            String fileName = clientArgs[1];
                                            System.out.println("LOAD " + fileName);
//                                  Controller selects one the R Dstores that stores that file, let port be its endpoint
                                            pw.println("LOAD_FROM 1234 1234");
                                            //TODO failure handling
                                        }
                                        if (command.equals("REMOVE")) {
                                            String fileName = clientArgs[1];
                                            index.put(fileName, "remove in progress");
                                            index.put(fileName, "remove complete");
                                            pw.println("REMOVE_COMPLETE");
                                            //TODO failure handling
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                System.out.println("error " + e);
                            }
                        }
                    }).start();
                } catch (Exception e) { System.out.println("error "+e); }
            }
        } catch (Exception e) { System.out.println("error "+e); }
        System.out.println();
    }

    static String listToString(Set l) {
        StringBuilder sb = new StringBuilder("");
        for (var s : l) sb.append(" ").append(s);
        sb.deleteCharAt(0);
        return sb.toString();
    }
}
