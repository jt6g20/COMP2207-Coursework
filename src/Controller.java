import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        Map<String, String[]> index = new HashMap<>();
        Set<Integer> dstores = new HashSet<>();
        Map<String, Integer> fileAcks = new HashMap<>();

//        index.put("oogabooa.txt", "store complete");
//        index.put("weewoo.txt", "store complete");
//        index.put("fangfong.txt", "store complete");

        try {
            ServerSocket serverSocket = new ServerSocket(cport);
            for (;;) {
                try {
                    System.out.println("waiting for connection");
                    Socket socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();
                    OutputStream out = socket.getOutputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    PrintWriter pw = new PrintWriter(out, true);

                    new Thread(new Runnable(){
                        public void run() {
                            int port = 0;
                            int dstoreIndex = 0;
                            try {
                                System.out.println("connected");
                                for (;;) {
                                    String line = br.readLine();
                                    System.out.println("INPUT - " + line);
                                    String[] clientArgs = line.split(" ");
                                    String command = clientArgs[0];

                                    if (command.equals("DSTORE")) {
                                        port = Integer.parseInt(clientArgs[2]);
                                        System.out.println("Dstore port added: " + port);
                                        dstores.add(port);
                                        System.out.println("Dstores connected - " + dstores);
                                    }

                                    else if (command.equals("STORE")) {
                                        String fileName = clientArgs[1];
                                        String fileSize = clientArgs[2];

                                        synchronized (index) {
                                            if (!index.containsKey(fileName) || index.get(fileName)[0].equals("remove complete")) {

                                            }
                                            else {
                                                pw.println("ERROR_FILE_ALREADY_EXISTS");
                                                continue;
                                            }
                                            System.out.println("STORE " + fileName + " " + fileSize);
                                            index.put(fileName, new String[]{"store in progress", fileSize});
                                        }

                                        System.out.println("STORE_TO " + listToString(dstores));
                                        pw.println("STORE_TO " + listToString(dstores));

                                        synchronized (fileAcks) {
                                            fileAcks.wait();
                                            while (fileAcks.get(fileName) < dstores.size()) {
                                                fileAcks.notify();
                                                fileAcks.wait();
                                            }
                                            index.put(fileName, new String[]{"store complete", fileSize});
                                            System.out.println(fileName + " store complete");
                                            pw.println("STORE_COMPLETE");
                                            fileAcks.remove(fileName);
                                            System.out.println(hashmapToString(index));
                                        }
                                        //TODO failure handling
                                    } else if (command.startsWith("STORE_ACK")) {
                                        String fileName = clientArgs[1];
                                        synchronized (fileAcks) {
                                            if (fileAcks.containsKey(fileName)) fileAcks.put(fileName, fileAcks.get(fileName) + 1);
                                            else fileAcks.put(fileName, 1);

                                            System.out.println(fileAcks);

                                            if (fileAcks.get(fileName) == dstores.size()) {
                                                fileAcks.notify();
                                                System.out.println(fileName + " all store ACKS received");
                                            }
                                        }


                                    } else if (command.equals("LOAD")) {
                                        String fileName = clientArgs[1];
                                        String fileSize;
                                        System.out.println("LOAD " + fileName);

                                        synchronized (index) {
                                            if (!index.containsKey(fileName)
                                                    || index.get(fileName)[0].equals("store in progress")
                                                    || index.get(fileName)[0].equals("remove in progress")) {
                                                pw.println("ERROR_FILE_DOES_NOT_EXIST");
                                                continue;
                                            }
                                            fileSize = index.get(fileName)[1];
                                        }


                                        dstoreIndex = 0;
                                        for (int dstore : dstores) {
                                            pw.println("LOAD_FROM " + dstore + " " + fileSize);
                                            break;
                                        }
                                        //TODO failure handling
                                    } else if (command.equals("RELOAD")) {
                                        String fileName = clientArgs[1];
                                        dstoreIndex++;
                                        if (dstoreIndex > dstores.size() - 1) {
                                            pw.println("ERROR_LOAD");
                                        } else {
                                            String fileSize = index.get(fileName)[1];
                                            int elemIndex = 0;
                                            for (int dstore : dstores) {
                                                if (elemIndex == dstoreIndex) {
                                                    pw.println("LOAD_FROM " + dstore + " " + fileSize);
                                                    break;
                                                }
                                                elemIndex++;
                                            }
                                        }
                                    }

                                    else if (command.equals("REMOVE")) {
                                        String fileName = clientArgs[1];
                                        String fileSize = index.get(fileName)[1];

                                        synchronized (index) {
                                            if (!index.containsKey(fileName)
                                                    || index.get(fileName)[0].equals("remove complete")
                                                    || index.get(fileName)[0].equals("remove in progress")
                                                    || index.get(fileName)[0].equals("store in progress")) {
                                                pw.println("ERROR_FILE_DOES_NOT_EXIST");
                                                continue;
                                            }
                                            index.put(fileName, new String[]{"remove in progress", fileSize});
                                        }

                                        String host = InetAddress.getLocalHost().getHostAddress();
                                        for (int dstore : dstores) {
                                            try {
                                                Socket dstoreSocket = new Socket(host, dstore);
                                                new Thread(new Runnable() {
                                                    public void run() {
                                                        try {
                                                            PrintWriter dstorePw = new PrintWriter(dstoreSocket.getOutputStream(), true);
                                                            dstorePw.println("REMOVE " + fileName);
                                                            dstoreSocket.close();
                                                        } catch (Exception e) { System.out.println("error "+e); }
                                                    }
                                                }).start();
                                            } catch (Exception e) { System.out.println("error "+e); }
                                        }

                                        synchronized (fileAcks) {
                                            fileAcks.wait();
                                            while (fileAcks.get(fileName) < dstores.size()) {
                                                fileAcks.notify();
                                                fileAcks.wait();
                                            }
                                            index.put(fileName, new String[]{"remove complete", fileSize});
                                            System.out.println(fileName + " remove complete");
                                            pw.println("REMOVE_COMPLETE");
                                            fileAcks.remove(fileName);
                                            System.out.println(hashmapToString(index));
                                        }
                                        //TODO failure handling

                                    } else if (command.equals("REMOVE_ACK")) {
                                        String fileName = clientArgs[1];
                                        synchronized (fileAcks) {
                                            if (fileAcks.containsKey(fileName)) fileAcks.put(fileName, fileAcks.get(fileName) + 1);
                                            else fileAcks.put(fileName, 1);

                                            System.out.println(fileAcks);

                                            if (fileAcks.get(fileName) == dstores.size()) {
                                                    fileAcks.notify();
                                                    System.out.println(fileName + " all deletion ACKS received");
                                            }
                                        }
                                    }

                                    else if (command.startsWith("LIST")) {
                                        //Have to use starts with? command is 6 characters long with LIST??
                                        StringBuilder sb = new StringBuilder("");
                                        synchronized (index) {
                                            for (var s : index.keySet()) {
                                                if (index.get(s)[0].equals("store complete")) {
                                                    sb.append(" ").append(s);
                                                }
                                            }
                                            sb.deleteCharAt(0);
                                        }

                                        String file_list = "LIST " + sb;
                                        System.out.println(file_list);
                                        pw.println(file_list);
                                        //TODO failure handling
                                    } else {
                                        System.out.println("malformed message received: " + line);
                                    }
                                }
                            } catch (Exception e) {
                                //System.out.println("error " + e);
                                System.out.println("Something disconnected on port " + port);
                                dstores.remove(port);
                                System.out.println("If that was a dstore it has been removed");
                                System.out.println(dstores);
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

    static String hashmapToString(Map<String, String[]> m) {
        StringBuilder sb = new StringBuilder("");
        for (String s : m.keySet()) {
            sb.append(s).append(" ").append(m.get(s)[0]).append(" ");
        }
        return sb.toString();
    }
}
