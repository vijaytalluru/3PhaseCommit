package ut.distcomp.threepc.process;

import ut.distcomp.framework.NetController;
import ut.distcomp.framework.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ut.distcomp.threepc.playlist.Playlist;
import ut.distcomp.threepc.util.Logger;
import ut.distcomp.threepc.util.Timer;

public class Site {
    private static final int BASETIMEOUT = 10000;
    private static final int TIMEOUT_MULTIPLIER = 5;
    private static int TIMEOUT;
    
    NetController net;
    int procNum, numProcs;
    boolean[] upList;
    int leader;
    Playlist playlist, tempPlaylist;
    
    Process type;
    int state;
    Logger logger, transactionLogger;
    boolean inTransaction;
    long currentTransaction;
    
    LeaderFields leaderFields;
    ParticipantFields participantFields;
    RecoveryFields recoveryFields;
    
    int DELAY;
    int PARTIALCOMMIT;
    Map<Integer, Integer> DEATHAFTER;
    Map<Integer, Integer> messageCount;
    
    public Site (int no, int total, int delay, int partialCommit, Map<Integer, Integer> deathAfter) {
        int SLEEP = (int)((1.5*(total-no))*1000);
        net = new NetController (new Config (no, total), SLEEP);
        
        procNum = no;
        numProcs = total;
        
        upList = new boolean[total];
        leader = 0;
        type = Participant.getParticipant(this);
        participantFields = new ParticipantFields();
        
        transactionLogger = new Logger(procNum, 0);
        playlist = new Playlist();
        
        DELAY = delay;
        TIMEOUT = BASETIMEOUT + DELAY*TIMEOUT_MULTIPLIER;
        PARTIALCOMMIT = partialCommit;
        DEATHAFTER = deathAfter;
        messageCount = new HashMap<Integer, Integer>();
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
        if (lastLogInTransaction())
            recover();
        else
            electLeader(false);
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
                System.out.println ("Done with input!");
            }
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
            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                
            }
        }
    }
    
    public void die () {
        for (int i=0; i<numProcs; ++i)
            if (i != leader)
                sendMsg (i, "DEAD\t" + procNum);
        System.exit(0);
    }
    
    public void countMsg (int from) {
        if (messageCount.containsKey(from)) {
            messageCount.put (from, messageCount.get(from)+1);
            if (DEATHAFTER.containsKey(from) && messageCount.get(from) >= DEATHAFTER.get(from))
                die();
        } else {
            messageCount.put (from, 1);
            if (DEATHAFTER.containsKey(from) && messageCount.get(from) >= DEATHAFTER.get(from))
                die();
        }
    }
    
    public boolean lastLogInTransaction() {
        List<String> transactions = transactionLogger.readLines();
        if (transactions.size() > 1) {
            long lastTransaction = Long.parseLong(transactions.get(transactions.size()-1));
            int lastState = getStateFromLog (lastTransaction);
            if (lastState == Process.State.COMMITTED || lastState == Process.State.ABORTED)
                return false;
            else
                return true;
        }
        return false;
    }
    
    public void recover () {
        System.out.println("Attempting recovery..");
        List<String> transactions = transactionLogger.readLines();
        for (String transaction : transactions) {
            long transactionID = Long.parseLong(transaction);
            int lastState = getStateFromLog (transactionID);
            if (lastState == Process.State.COMMITTED) {
                String[] parts = getTransaction(transactionID);
                if (parts[0].equals("ADD")) {
                    tempPlaylist = playlist.clone();
                    PlaylistHelper.addSong(this, parts, 0, false);
                    playlist = tempPlaylist;
                } else if (parts[0].equals("REMOVE")) {
                    tempPlaylist = playlist.clone();
                    PlaylistHelper.removeSong(this, parts, 0, false);
                    playlist = tempPlaylist;
                } else if (parts[0].equals("EDIT")) {
                    tempPlaylist = playlist.clone();
                    PlaylistHelper.editSong(this, parts, 0, false);
                    playlist = tempPlaylist;
                }
            }
            else if (lastState == Process.State.ABORTED)
                continue;
            else {
                recoveryFields = new RecoveryFields();
                while (recoveryFields != null) {
                    recoveryFields.recoveryTransaction = transactionID;
                    recoveryFields.recoveryTransactionParts = getTransaction(transactionID);
                    List<Integer> participants = new ArrayList<Integer>();
                    for (String participant : getUplist (transactionID).trim().split(" "))
                        participants.add(Integer.parseInt(participant));
                    recoveryFields.stateVector = new int [numProcs];
                    for (int p : participants)
                        sendMsg (p, "STATEREQ-RECOVERY\t" + procNum + "\t" + transactionID);
                    for (int i=0; i<numProcs; ++i) {
                        if (i != procNum && upList[i])
                            recoveryFields.stateVector[i] = Process.State.UNKNOWN;
                        else
                            recoveryFields.stateVector[i] = Process.State.NA;
                    }
                    recoveryFields.waitingForStates = true;
                    recoveryFields.stateTimer = new Timer(TIMEOUT);
                    try {
                        Thread.sleep(TIMEOUT/2);
                    } catch (InterruptedException e) {
                        System.out.println("Recovery interrupted!");
                        System.exit(0);
                    }
                }
            }
        }
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
        String up = new String (upHosts);
        logger.write("UPLIST\t" + up);
        return up;
    }
    
    public void startTransaction (long transactionID) {
        inTransaction = true;
        currentTransaction = transactionID;
        logger = new Logger (procNum, transactionID);
    }
    
    public void endTransaction () {
        inTransaction = false;
        StateHelper.virgin(this);
        //logger = null;
    }
    
    public void pingRandom () {
        sendMsg ((int)(Math.random()*numProcs), "PING\t" + procNum);
        try {
            //Thread.sleep(1000);
        } catch (Exception e) {
            
        }
    }
    
    public int electLeader (boolean newLeader) {
        System.out.println ("Looking for leader..");
        int i;
        for (i=leader; !upList[i]; i=(i+1)%numProcs);
        leader = i;
        if (newLeader)
            sendMsg (leader, "LEADER_NEW\t" + procNum);
        else
            sendMsg (leader, "LEADER\t" + procNum);
        if (leader == procNum)
            iLeader(newLeader);
        return leader;
    }
    
    public void iLeader (boolean newLeader) {
        type = Leader.getLeader(this);
        leaderFields = new LeaderFields();
        System.out.println ("I AM THE LEADERZZZ!!!!!");
        if (newLeader) {
            startTransaction (currentTransaction);
            type.handleNewLeader();
        }
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
    
    public int getStateFromLog (long transactionID) {
        Logger tempLogger = new Logger(procNum, transactionID);
        List<String> logLines = tempLogger.readLines();
        for (int i=logLines.size()-1; i>=0; --i) {
            String logLine = logLines.get(i);
            if (logLine.equals("COMMIT"))
                return Process.State.COMMITTED;
            if (logLine.equals("ABORT"))
                return Process.State.ABORTED;
        }
        return Process.State.UNKNOWN;
    }
    
    public String[] getTransaction (long transactionID) {
        Logger tempLogger = new Logger(procNum, transactionID);
        List<String> logLines = tempLogger.readLines();
        return logLines.get(0).trim().split("\t");
    }
    
    public String getState (long transactionID) {
        if (state == Process.State.VIRGIN || currentTransaction != transactionID)
            return Process.State.stateStr.get(getStateFromLog (transactionID));
        else
            return Process.State.stateStr.get(state);
    }
    
    public String getUplist (long transactionID) {
        Logger tempLogger = new Logger(procNum, transactionID);
        List<String> logLines = tempLogger.readLines();
        for (int i=1; i<logLines.size(); ++i) {
            String[] parts = logLines.get(0).trim().split("\t");
            if (parts[0].equals("UPLIST"))
                return parts[1];
        }
        return "";
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
                electLeader(true);
        }
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            
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
