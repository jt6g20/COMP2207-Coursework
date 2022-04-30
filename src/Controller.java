import java.io.*;
import java.net.*;
import java.util.*;

public class Controller {
    public static void main(String[] args) throws IOException {
        //Port to listen to
        String cport = args[0];
        //Replication factor
        String rFactor = args[1];
        //Timeout in milliseconds
        String timeout = args[2];
        //How long to wait (in seconds) to start the next rebalance operation
        String rebalPeriod = args[3];

        System.out.println(cport + " " + rFactor + " " + timeout + " " + rebalPeriod);

        List<String> index = new ArrayList<>();


    }
}
