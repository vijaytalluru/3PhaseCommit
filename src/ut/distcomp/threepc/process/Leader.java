package ut.distcomp.threepc.process;

import ut.distcomp.threepc.util.Timer;

public class Leader implements Process {
    
    private static Leader leader;
    private static final int BASETIMEOUT = 2000;
    private static final int TIMEOUT_MULTIPLIER = 2;
    private static int TIMEOUT;
    Site site;
    
    private Leader () {
    }
    
    public static Leader getLeader (Site site) {
        leader = new Leader();
        leader.site = site;
        TIMEOUT = BASETIMEOUT + site.DELAY*TIMEOUT_MULTIPLIER;
        return leader;
    }

    @Override
    public boolean processMsg(String msg) {
        String[] parts = msg.split("\t");
        
        if (site.leaderFields.waitingForVotes && parts[0].equals("VOTERESP")) {
            if (Long.parseLong(parts[2]) == site.currentTransaction) {
                if (parts[3].equals("YES"))
                    site.leaderFields.voteVector[Integer.parseInt(parts[1])] = Process.Vote.YES;
                else if (parts[3].equals("NO"))
                    site.leaderFields.voteVector[Integer.parseInt(parts[1])] = Process.Vote.NO;
            }
            if (site.leaderFields.allVotes()) {
                if (site.leaderFields.anyVoteNo()) {
                    abort();
                } else {
                    precommit();
                }
            } else if (site.leaderFields.voteTimer.timeout()) {
                abort();
            }
            site.countMsg (Integer.parseInt(parts[1]));
            return true;
            
        } else if (site.leaderFields.waitingForAck && parts[0].equals("ACK")) {
            if (Long.parseLong(parts[2]) == site.currentTransaction) {
                site.leaderFields.ackVector[Integer.parseInt(parts[1])] = Process.Ack.ACK;
                
            }
            if (site.leaderFields.allAcks()) {
                commit();
            } else if (site.leaderFields.ackTimer.timeout()) {
                commit();
            }
            site.countMsg (Integer.parseInt(parts[1]));
            return true;
            
        } else if (site.leaderFields.waitingForStates && parts[0].equals("STATERESP")) {
            if (Long.parseLong(parts[2]) == site.currentTransaction) {
                if (parts[3].equals(Process.State.stateStr.get(Process.State.ABORTED)))
                    site.leaderFields.stateVector[Integer.parseInt(parts[1])] = Process.State.ABORTED;
                if (parts[3].equals(Process.State.stateStr.get(Process.State.COMMITTED)))
                    site.leaderFields.stateVector[Integer.parseInt(parts[1])] = Process.State.COMMITTED;
                if (parts[3].equals(Process.State.stateStr.get(Process.State.UNCERTAIN)))
                    site.leaderFields.stateVector[Integer.parseInt(parts[1])] = Process.State.UNCERTAIN;
                if (parts[3].equals(Process.State.stateStr.get(Process.State.PRECOMMIT)))
                    site.leaderFields.stateVector[Integer.parseInt(parts[1])] = Process.State.PRECOMMIT;
                if (parts[3].equals(Process.State.stateStr.get(Process.State.UNKNOWN)))
                    site.leaderFields.stateVector[Integer.parseInt(parts[1])] = Process.State.UNKNOWN;
            }
            if (site.leaderFields.allStates() || site.leaderFields.stateTimer.timeout()) {
                processRecovery();
            }
            site.countMsg (Integer.parseInt(parts[1]));
            return true;
            
        } else if (parts[0].equals("STATEREQ-RECOVERY")) {
            long transactionID = Long.parseLong(parts[2]);
            String myState = site.getState (Long.parseLong(parts[2]));
            site.sendMsg(Integer.parseInt(parts[1]), "STATERESP-RECOVERY\t" + site.procNum + "\t" + transactionID + "\t" + myState);
            site.countMsg (Integer.parseInt(parts[1]));
            return true;
        }
        
        return false;
    }
    
    private void processRecovery () {
        site.leaderFields.waitingForStates = false;
        if (site.leaderFields.anyState(Process.State.ABORTED)) {
            recoveryAbort();
        } else if (site.leaderFields.anyState(Process.State.COMMITTED)) {
            recoveryCommit();
        } else if (site.leaderFields.allStatesAre(Process.State.UNCERTAIN)) {
            recoveryAbort();
        } else if (site.leaderFields.anyState(Process.State.PRECOMMIT)) {
            recoveryPrecommit();
        }
        
    }

    @Override
    public void checkTimeouts() {
        if (site.leaderFields.waitingForAck && site.leaderFields.ackTimer.timeout()) {
            commit();
        } else if (site.leaderFields.waitingForVotes && site.leaderFields.voteTimer.timeout()) {
            abort();
        } else if (site.leaderFields.waitingForStates && site.leaderFields.stateTimer.timeout()) {
            boolean atLeastOneWaiting = false;
            for (int i=0; i<site.numProcs; ++i) {
                if (site.leaderFields.stateVector[i] == Process.State.UNKNOWN && site.upList[i]) {
                    atLeastOneWaiting = true;
                    break;
                }
            }
            if (!atLeastOneWaiting)
                processRecovery();
        }
    }
    
    private void commit () {
        site.leaderFields.waitingForAck = false;

        if (site.FAILURE == 8)
            site.dirtyDie();
        if (site.FAILURE == 11) {
            actualCommit();
            site.dirtyDie();
        }

        String upHosts = site.pingAll();
        if (site.PARTIALCOMMIT != -1) {
            site.sendMsg (site.PARTIALCOMMIT, "UPLIST\t" + site.procNum + "\t" + upHosts);
            site.sendMsg (site.PARTIALCOMMIT, "COMMIT\t" + site.procNum + "\t" + site.currentTransaction);
            site.die();
        } else {
            for (int i=0; i<site.numProcs; ++i)
                if (i != site.leader) {
                    site.sendMsg (i, "UPLIST\t" + site.procNum + "\t" + upHosts);
                    site.sendMsg (i, "COMMIT\t" + site.procNum + "\t" + site.currentTransaction);
                }
        }
        actualCommit ();
        if (site.FAILURE == 10)
            site.dirtyDie();
    }
    
    private void recoveryCommit () {
        String upHosts = site.pingAll();

        if (site.FAILURE == 11) {
            actualCommit();
            site.dirtyDie();
        }

        if (site.PARTIALCOMMIT != -1) {
            site.sendMsg (site.PARTIALCOMMIT, "UPLIST\t" + site.procNum + "\t" + upHosts);
            site.sendMsg (site.PARTIALCOMMIT, "COMMIT\t" + site.procNum + "\t" + site.currentTransaction);
            site.die();
        } else {
            for (int i=0; i<site.numProcs; ++i)
                if (i != site.leader) {
                    site.sendMsg (i, "UPLIST\t" + site.procNum + "\t" + upHosts);
                    site.sendMsg (i, "COMMIT\t" + site.procNum + "\t" + site.currentTransaction);
                }
        }
        actualCommit ();
    }
    
    private void actualCommit () {
        site.logger.write("COMMIT");
        site.playlist = site.tempPlaylist;
        StateHelper.committed(site);
        System.out.println ("Commited!");
        site.endTransaction();
        
    }

    private void abort () {
        site.logger.write("ABORT");
        site.leaderFields.waitingForVotes = false;
        site.leaderFields.waitingForAck = false;
        StateHelper.aborted(site);
        String upHosts = site.pingAll();
        for (int i=0; i<site.numProcs; ++i) {
            if (i != site.leader && site.leaderFields.voteVector[i] == Process.Vote.YES) {
                site.sendMsg (i, "UPLIST\t" + site.procNum + "\t" + upHosts);
                site.sendMsg (i, "ABORT\t" + site.procNum + "\t" + site.currentTransaction);
            }
        }
        System.out.println ("Aborted!");
        site.endTransaction();
    }

    private void recoveryAbort () {
        site.logger.write("ABORT");
        StateHelper.aborted(site);
        String upHosts = site.pingAll();
        for (int i=0; i<site.numProcs; ++i) {
            if (i != site.leader) {
                site.sendMsg (i, "UPLIST\t" + site.procNum + "\t" + upHosts);
                site.sendMsg (i, "ABORT\t" + site.procNum + "\t" + site.currentTransaction);
            }
        }
        System.out.println ("Aborted!");
        site.endTransaction();
    }

    private void loneAbort () {
        site.logger.write("ABORT");
        site.leaderFields.waitingForVotes = false;
        site.leaderFields.waitingForAck = false;
        StateHelper.aborted(site);
        System.out.println ("Aborted!");
        site.endTransaction();
    }
    
    private void precommit () {
        site.leaderFields.waitingForVotes = false;
        StateHelper.precommit(site);
        
        String upHosts = site.pingAll();
        site.leaderFields.ackVector = new int[site.numProcs];

        int dieCounter = 0;

        for (int i=0; i<site.numProcs; ++i) {
            if (i != site.leader) {
                site.sendMsg (i, "UPLIST\t" + site.procNum + "\t" + upHosts);
                site.sendMsg (i, "PRECOMMIT\t" + site.procNum + "\t" + site.currentTransaction);
                dieCounter++;
                if (site.FAILURE == 4 && dieCounter > 0)
                    site.dirtyDie();
                site.leaderFields.ackVector[i] = Process.Ack.PENDING;
            } else {
                site.leaderFields.ackVector[i] = Process.Ack.NA;
            }
        }

        if (site.FAILURE == 9)
            site.dirtyDie();

        site.leaderFields.waitingForAck = true;
        site.leaderFields.ackTimer = new Timer (TIMEOUT);
        System.out.println ("Waiting for acks..");
    }
    
    private void recoveryPrecommit () {
        String upHosts = site.pingAll();
        site.leaderFields.ackVector = new int[site.numProcs];
        
        if (site.FAILURE == 6)
            site.dirtyDie();

        int dieCounter = 0;
        
        for (int i=0; i<site.numProcs; ++i) {
            if (i != site.leader) {
                site.sendMsg (i, "UPLIST\t" + site.procNum + "\t" + upHosts);
                site.sendMsg (i, "PRECOMMIT\t" + site.procNum + "\t" + site.currentTransaction);
                dieCounter++;
                if (site.FAILURE == 4 && dieCounter > 1)
                    site.dirtyDie();
                site.leaderFields.ackVector[i] = Process.Ack.PENDING;
            } else {
                site.leaderFields.ackVector[i] = Process.Ack.NA;
            }
        }
        site.leaderFields.waitingForAck = true;
        site.leaderFields.ackTimer = new Timer (TIMEOUT);
        System.out.println ("Waiting for acks..");
    }
    
    private void voteReq (String input, long transactionID) {
        String upHosts = site.pingAll();
        
        for (int i=0; i<site.numProcs; ++i) {
            if (i != site.leader && site.upList[i]) {
                site.sendMsg (i, "UPLIST\t" + site.procNum + "\t" + upHosts);
                site.sendMsg (i, "VOTEREQ\t" + site.procNum + "\t" + transactionID + "\t" + input);
                site.leaderFields.voteVector[i] = Process.Vote.PENDING;
            } else
                site.leaderFields.voteVector[i] = Process.Vote.NA;
        }
        
        site.leaderFields.waitingForVotes = true;
        site.leaderFields.voteTimer = new Timer (TIMEOUT);
        
        System.out.println ("Waiting for votes..");
    }
    
    @Override
    public void handleNewLeader() {
        site.logger.write("START-LEADER");
        String upHosts = site.pingAll();
//        if (loneWolf()) {
//            site.endTransaction();
//            return;
//        }
        site.leaderFields.stateVector = new int[site.numProcs];
        
        for (int i=0; i<site.numProcs; ++i) {
            if (i != site.leader && site.upList[i]) {
                site.sendMsg (i, "STATEREQ\t" + site.procNum + "\t" + site.currentTransaction);
                site.sendMsg (i, "UPLIST\t" + site.procNum + "\t" + upHosts);
                site.leaderFields.stateVector[i] = Process.State.UNKNOWN;
            } else
                site.leaderFields.stateVector[i] = Process.State.NA;
        }
        
        site.leaderFields.waitingForStates = true;
        site.leaderFields.stateTimer = new Timer (TIMEOUT);
        site.leaderFields.stateVector[site.procNum] = (site.state == Process.State.VIRGIN)? site.getStateFromLog (site.currentTransaction) : site.state;
        
        System.out.println ("Waiting for states..");
    }
    
    private boolean loneWolf () {
        for (int i=0; i<site.numProcs; ++i)
            if (site.upList[i] && i != site.procNum)
                return false;
        return true;
    }
    
    @Override
    public boolean processInput(String input) {
        System.out.println ("Processing input..\t" + input);
        if (site.state != Process.State.VIRGIN) {
            System.out.println ("Not a virgin.");
            return false;
        }
        String[] parts = input.trim().split("\t");
        if (parts[0].equals("ADD")) {
            long tID = initInput();
            boolean status = PlaylistHelper.addSong(site, parts, 0, true);
            site.logger.write("START-3PC");
            if (loneWolf())
                if (status)
                    actualCommit();
                else
                    loneAbort();
            else {
                site.leaderFields.voteVector[site.procNum] = (status)? Process.Vote.YES : Process.Vote.NO;
                voteReq(input, tID);
            }
        } else if (parts[0].equals("REMOVE")) {
            long tID = initInput();
            boolean status = PlaylistHelper.removeSong(site, parts, 0, true);
            site.logger.write("START-3PC");
            if (loneWolf())
                if (status)
                    actualCommit();
                else
                    loneAbort();
            else {
                site.leaderFields.voteVector[site.procNum] = (status)? Process.Vote.YES : Process.Vote.NO;
                voteReq(input, tID);
            }
        } else if (parts[0].equals("EDIT")) {
            long tID = initInput();
            boolean status = PlaylistHelper.editSong(site, parts, 0, true);
            site.logger.write("START-3PC");
            if (loneWolf())
                if (status)
                    actualCommit();
                else
                    loneAbort();
            else {
                site.leaderFields.voteVector[site.procNum] = (status)? Process.Vote.YES : Process.Vote.NO;
                voteReq(input, tID);
            }
        } else if (parts[0].equals("DIE")) {
            site.die();
        }
        
        return true;
    }
    
    private long initInput () {
        long transactionID = System.currentTimeMillis();
        site.transactionLogger.write(String.valueOf(transactionID));
        site.startTransaction(transactionID);
        site.leaderFields.voteVector = new int[site.numProcs];
        return transactionID;
    }

}
