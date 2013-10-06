package ut.distcomp.threepc.process;

import ut.distcomp.threepc.util.Timer;

public class RecoveryFields {
    Timer stateTimer;
    boolean waitingForStates;
    long recoveryTransaction;
    String[] recoveryTransactionParts;
    
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
