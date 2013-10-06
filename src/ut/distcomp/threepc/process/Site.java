package ut.distcomp.threepc.process;

import ut.distcomp.framework.NetController;
import ut.distcomp.framework.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import ut.distcomp.threepc.playlist.Playlist;
import ut.distcomp.threepc.util.Timer;

public class Site {
    
    NetController net;
    int procNum, numProcs;
    boolean[] upList;
    int leader;
    Playlist playlist, tempPlaylist;
    
    Process type;
    int state;
    boolean inTransaction;
    long currentTransaction;
    
    LeaderFields leaderFields;
    ParticipantFields participantFields;
    
    public Site (int no, int total) {
        int SLEEP = (int)((1.5*(total-no))*1000);
        net = new NetController (new Config (no, total), SLEEP);
        procNum = no;
        numProcs = total;
        
        upList = new boolean[total];
        leader = 0;
        type = Participant.getParticipant(this);
        participantFields = new ParticipantFields();
        
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
        StateHelper.virgin(this);
        if (logInTransaction())
            recover();
        else
            electLeader();
        waitForMessages();
        BufferedReader br = new BufferedReader (new InputStreamReader (System.in));
        
        while (true) {
            if (!inTransaction && leader()) {
                System.out.println ("Waiting for input..");
                try {
                    String input = br.readLine();
                    if (input != null && !input.equals(""))
                        type.processInput (input);
                        
                } catch (IOException e) {
                    System.out.println("IOException!");
                }
            }
            System.out.println ("Done with input!");
            while (!leader() || inTransaction) {
                waitForMessages();
            }
        }
    }
    
    public void waitForMessages () {
        System.out.println("Listening..");
        List<String> msgs = listen();
        while ((!leader() || inTransaction) && (msgs == null || msgs.size() == 0)) {
            msgs = listen();
            if (inTransaction)
                type.checkTimeouts();
        }
        for (String msg : msgs) {
            System.out.println ("INCOMING:\t" + msg);
            type.processMsg (msg);
        }
    }
    
    public boolean logInTransaction() {
        return false;
    }
    
    public void recover () {
        
    }
    
    public boolean leader() {
        return procNum == leader;
    }
    
    public String pingAll () {
        StringBuffer upHosts = new StringBuffer();
        for (int i=0; i<numProcs; ++i)
            if (upList[i])
                sendMsg (i, "PING\t" + procNum);
        for (int i=0; i<numProcs; ++i)
            if (upList[i])
                upHosts.append(i + " ");
        return new String (upHosts);
    }
    
    public void startTransaction (long transactionID) {
        inTransaction = true;
        currentTransaction = transactionID;
    }
    
    public void endTransaction () {
        inTransaction = false;
        StateHelper.virgin(this);
    }
    
    public void pingRandom () {
        sendMsg ((int)(Math.random()*numProcs), "PING\t" + procNum);
        try {
            //Thread.sleep(1000);
        } catch (Exception e) {
            
        }
    }
    
    public int electLeader () {
        System.out.println ("Looking for leader..");
        int i;
        for (i=leader; !upList[i]; i=(i+1)%numProcs);
        leader = i;
        sendMsg (leader, "LEADER\t" + procNum);
        if (leader == procNum)
            iLeader();
        return leader;
    }
    
    public void iLeader () {
        type = Leader.getLeader(this);
        leaderFields = new LeaderFields();
        System.out.println ("I AM THE LEADERZZZ!!!!!");
    }
    
    public boolean totalFailure () {
        boolean failure = true;
        for (int i=0; i<numProcs; ++i)
            if (i != procNum && upList[i]) {
                failure = false;
                break;
            }
        return failure;
    }
    
    public List<String> listen () {
        List<String> msgs;
        Timer timer = new Timer(1000);
        for (msgs = net.getReceivedMsgs(); !timer.timeout() && (msgs == null || msgs.size() == 0); msgs = net.getReceivedMsgs());
        return msgs;
    }
    
    public boolean sendMsg (int to, String msg) {
        System.out.println ("SEND:\t" + to + "\t" + msg);
        if (!upList[to])
            return false;
        boolean success = net.sendMsg (to, msg);
        if (!success) {
            System.out.println("Send to " + to + " failed!");
            upList[to] = false;
            if (to == leader)
                electLeader();
        }
        return success;
    }
    
    public void finalize () {
        net.shutdown();
    }
    
    public void printVector (int[] vector) {
        for (int i : vector) {
            System.out.print (i + " ");
        }
        System.out.println();
    }
    
    public void printVector (boolean[] vector) {
        for (boolean i : vector) {
            System.out.print (i + " ");
        }
        System.out.println();
    }
    

}
