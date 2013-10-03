package ut.distcomp.threepc.process;

public interface Process {
    public static final class State {
        static int ABORTED = 0;
        static int UNCERTAIN = 1;
        static int PRECOMMIT = 2;
        static int COMMITTED = 3;
        static int VIRGIN = 4;
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

}
