package ut.distcomp.threepc.process;

import ut.distcomp.threepc.util.Timer;

public class LeaderFields {
    Timer voteTimer;
    boolean waitingForVotes;
    
    Timer ackTimer;
    boolean waitingForAck;
    
    int[] voteVector;
    
    public boolean allVotes () {
        boolean allVotes = true;
        for (int i : voteVector)
            if (i == Process.Vote.PENDING) {
                allVotes = false;
                break;
            }
        return allVotes;
    }
    
    public boolean anyVoteNo () {
        boolean anyVoteNo = false;
        for (int i : voteVector)
            if (i == Process.Vote.NO) {
                anyVoteNo = true;
                break;
            }
        return anyVoteNo;
    }
    
    int[] ackVector;
    
    public boolean allAcks () {
        boolean allAcks = true;
        for (int i : ackVector)
            if (i == Process.Vote.PENDING) {
                allAcks = false;
                break;
            }
        return allAcks;
    }
    
    Timer stateTimer;
    boolean waitingForStates;
    
    int[] stateVector;
    
    public boolean allStates () {
        boolean allStates = true;
        for (int i : stateVector)
            if (i == Process.State.UNKNOWN) {
                allStates = false;
                break;
            }
        return allStates;
    }
    
    public boolean anyState (int state) {
        boolean anyState = false;
        for (int i : stateVector)
            if (i == state) {
                anyState = true;
                break;
            }
        return anyState;
    }
    
    public boolean allStatesAre (int state) {
        boolean allStatesAre = true;
        for (int i : stateVector)
            if (i != state && i != Process.State.NA) {
                allStatesAre = false;
                break;
            }
        return allStatesAre;
    }
    
    

}
