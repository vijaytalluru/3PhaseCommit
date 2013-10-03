package ut.distcomp.threepc.process;

import ut.distcomp.threepc.util.Timer;

public class ParticipantFields {
    boolean waitingForPrecommit = false;
    Timer precommitTimer;
    
    boolean waitingForCommit = false;
    Timer commitTimer;

}
