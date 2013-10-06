package ut.distcomp.threepc.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Logger {
    private File log;
    private BufferedWriter writer;
    private BufferedReader reader;
    
    public Logger (int procNum, long transactionID) {
        log = new File (transactionID + "_" + procNum + ".log");
    }
    
    public void write (String s) {
        try {
            writer = new BufferedWriter (new FileWriter (log, true));
            writer.write(s);
            writer.write("\n");
            writer.close();
        } catch (IOException e) {
            System.out.println("Can't write to file!");
        }
    }
    
    public List<String> readLines () {
        List<String> lines = new ArrayList<String> ();
        try {
            reader = new BufferedReader (new FileReader (log));
            for (String s = reader.readLine(); s != null && s!= ""; s = reader.readLine())
                lines.add (s);
            reader.close();
        } catch (IOException e) {
            System.out.println("Can't read from file!");
        }
        return lines;
        
    }

}
