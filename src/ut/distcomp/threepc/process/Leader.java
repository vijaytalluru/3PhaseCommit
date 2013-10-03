package ut.distcomp.threepc.process;

public class Leader implements Process {
    
    private static final Leader leader = new Leader();
    private Leader () {
    }
    
    public static Leader getLeader () {
        return leader;
    }

    @Override
    public boolean processMsg(String msg) {
        
        return false;
    }

}
