package ut.distcomp.threepc.process;

public class Participant implements Process {
    
    private static final Participant participant = new Participant();
    private Participant () {
    }
    
    public static Participant getParticipant () {
        return participant;
    }
    

    @Override
    public boolean processMsg(String msg) {
        
        return false;
    }

}
