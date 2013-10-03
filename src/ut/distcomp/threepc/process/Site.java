package ut.distcomp.threepc.process;

import ut.distcomp.framework.NetController;
import ut.distcomp.framework.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import ut.distcomp.threepc.playlist.Playlist;

public class Site {
    
    private NetController net;
    private int procNum, numProcs;
    private boolean[] upList;
    private int leader;
    private Playlist playlist;
    
    private Process type;
    
    public Site (int no, int total) {
        int SLEEP = (int)((15-1.5*no)*1000);
        net = new NetController (new Config (no, total), SLEEP);
        procNum = no;
        numProcs = total;
        
        upList = new boolean[total];
        leader = 0;
        type = Participant.getParticipant();
        
        playlist = new Playlist();
    }
    
    public void initializeMe () {
        for (int i=0; i<numProcs; ++i) {
            boolean success = net.sendMsg (i, "HELLO\t" + procNum);
            if (success)
                upList[i] = true;
        }
    }
    
    public void mainLoop () {
        electLeader();
        BufferedReader br = new BufferedReader (new InputStreamReader (System.in));
        
        while (true) {
            if (leader == procNum) {
                try {
                    String input = br.readLine();
                    if (input != null)
                        type.processInput (input);
                } catch (IOException e) {
                    
                }
            }
            List<String> msgs = listen();
            for (String msg : msgs) {
                System.out.println ("INCOMING:\t" + msg);
                type.processMsg (msg);
            }
        }
    }
    
    private void pingRandom () {
        sendMsg ((int)(Math.random()*numProcs), "PING\t" + procNum);
        try {
            //Thread.sleep(1000);
        } catch (Exception e) {
            
        }
    }
    
    private int electLeader () {
        int i;
        for (i=leader; !upList[i]; i=(i+1)%numProcs);
        leader = i;
        sendMsg (leader, "LEADER\t" + procNum);
        if (leader == procNum)
            iLeader();
        return leader;
    }
    
    private void iLeader () {
        type = Leader.getLeader();
        System.out.println ("I AM THE LEADERZZZ!!!!!");
    }
    
    private boolean totalFailure () {
        boolean failure = true;
        for (int i=0; i<numProcs; ++i)
            if (i != procNum && upList[i]) {
                failure = false;
                break;
            }
        return failure;
    }
    
    private List<String> listen () {
        List<String> msgs;
        long start = System.currentTimeMillis();
        for (msgs = net.getReceivedMsgs(); System.currentTimeMillis()-start < 1000 && (msgs == null || msgs.size() == 0); msgs = net.getReceivedMsgs());
        return msgs;
    }
    
    private boolean sendMsg (int to, String msg) {
        if (!upList[to])
            return false;
        boolean success = net.sendMsg (to, msg);
        if (!success) {
            upList[to] = false;
            if (to == leader)
                electLeader();
        }
        return success;
    }
    
    public void finalize () {
        net.shutdown();
    }
    

}
