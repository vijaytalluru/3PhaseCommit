package ut.distcomp.threepc;

import java.util.HashMap;
import java.util.Map;

import ut.distcomp.threepc.process.Site;

public class Driver {

    public static void main(String[] args) {
        int partialCommit = -1, delay = 0;
        Map<Integer, Integer> deathAfter = new HashMap<Integer, Integer> ();
        if (args.length > 2)
            for (int i=2; i<args.length; )
                if (args[i].equals("-partialCommit")) {
                    partialCommit = Integer.parseInt(args[i+1]);
                    i+=2;
                } else if (args[i].equals("-deathAfter")) {
                    deathAfter.put (Integer.parseInt(args[i+2]), Integer.parseInt(args[i+1]));
                    i+=3;
                } else if (args[i].equals("-delay")) {
                    delay = Integer.parseInt(args[i+1]);
                    i+=2;
                } else {
                    System.out.println("Incorrect args!");
                    System.exit(0);
                }
        Site site = new Site (Integer.parseInt(args[0]), Integer.parseInt(args[1]), delay, partialCommit, deathAfter);
        site.initializeMe();
        site.mainLoop();

    }

}
