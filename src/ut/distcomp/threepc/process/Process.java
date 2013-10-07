package ut.distcomp.threepc.process;

import java.util.HashMap;

public interface Process {
    public static final class State {
        static int ABORTED = 0;
        static int UNCERTAIN = 1;
        static int PRECOMMIT = 2;
        static int COMMITTED = 3;
        static int VIRGIN = 4;
        static int UNKNOWN = 5;
        static int NA = 6;
        static int LOST = 7;
        
        static HashMap<Integer, String> stateStr;
        static {
            stateStr = new HashMap<Integer, String> ();
            stateStr.put (ABORTED, "ABORTED");
            stateStr.put (UNCERTAIN, "UNCERTAIN");
            stateStr.put (PRECOMMIT, "PRECOMMIT");
            stateStr.put (COMMITTED, "COMMITTED");
            stateStr.put (VIRGIN, "VIRGIN");
            stateStr.put (UNKNOWN, "UNKNOWN");
            stateStr.put (NA, "NA");
            stateStr.put (LOST, "LOST");
        }
    }
    
    public static final class Vote {
        static int PENDING = 0;
        static int NO = 1;
        static int YES = 2;
        static int NA = 3;
    }
    
    public static final class Ack {
        static int PENDING = 0;
        static int ACK = 1;
        static int NA = 2;
    }
    
    public boolean processMsg (String msg);
    public boolean processInput (String input);
    public void checkTimeouts ();
    public void handleNewLeader ();

}
