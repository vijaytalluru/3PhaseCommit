package ut.distcomp.threepc.process;

public interface Process {
    public static final int ABORTED = 0;
    public static final int UNCERTAIN = 1;
    public static final int PRECOMMIT = 2;
    public static final int COMMITTED = 3;
    
    public boolean processMsg (String msg);
    public boolean processInput (String input);

}
