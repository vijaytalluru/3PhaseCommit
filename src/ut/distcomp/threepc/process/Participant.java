package ut.distcomp.threepc.process;

import ut.distcomp.threepc.util.Timer;

public class Participant implements Process {
    
    private static Participant participant;
    private static final int BASETIMEOUT = 2000;
    private static final int TIMEOUT_MULTIPLIER = 10;
    private static int TIMEOUT;
    Site site;
    
    private Participant () {
    }
    
    public static Participant getParticipant (Site site) {
        participant = new Participant();
        participant.site = site;
        TIMEOUT = BASETIMEOUT + site.DELAY*TIMEOUT_MULTIPLIER;
        return participant;
    }
    

    @Override
    public boolean processMsg(String msg) {
        String[] parts = msg.split("\t");
        
        if (parts[0].equals("UPLIST")) {
            if (Integer.parseInt(parts[1]) == site.leader) {
                String[] hosts = parts[2].split(" ");
                for (int i=0; i<site.numProcs; ++i)
                    site.upList[i] = false;
                for (String host : hosts)
                    site.upList[Integer.parseInt(host)] = true;
                if (site.logger != null)
                    site.logger.write(parts[0] + "\t" + parts[2]);
            }
            return true;
            
        } else if (parts[0].equals("VOTEREQ")) {
            site.leader = Integer.parseInt(parts[1]);
            site.transactionLogger.write(parts[2]);
            site.startTransaction(Long.parseLong(parts[2]));
            if (site.FAILURE == 2)
                site.dirtyDie();
            if (parts[3].equals("ADD")) {
                boolean status = PlaylistHelper.addSong(site, parts, 3, true);
                sendVoteResp (status);
            } else if (parts[3].equals("REMOVE")) {
                boolean status = PlaylistHelper.removeSong(site, parts, 3, true);
                sendVoteResp (status);
            } else if (parts[3].equals("EDIT")) {
                boolean status = PlaylistHelper.editSong(site, parts, 3, true);
                sendVoteResp (status);
            }
            site.countMsg (Integer.parseInt(parts[1]));
            return true;
            
        } else if (parts[0].equals("PRECOMMIT")) {
            if (Integer.parseInt(parts[1]) == site.leader && Long.parseLong(parts[2]) == site.currentTransaction) {
                StateHelper.precommit(site);
                if (site.FAILURE == 3)
                    site.dirtyDie();
                sendAck();
            }
            site.countMsg (Integer.parseInt(parts[1]));
            return true;
            
        } else if (parts[0].equals("COMMIT")) {
            if (Integer.parseInt(parts[1]) == site.leader && Long.parseLong(parts[2]) == site.currentTransaction) {
                if (site.FAILURE == 10)
                    site.dirtyDie();
                commit();
            }
            site.countMsg (Integer.parseInt(parts[1]));
            return true;
            
        } else if (parts[0].equals("ABORT")) {
            if (Integer.parseInt(parts[1]) == site.leader && Long.parseLong(parts[2]) == site.currentTransaction) {
                abort();
            }
            site.countMsg (Integer.parseInt(parts[1]));
            return true;
            
        } else if (parts[0].equals("LEADER") && !site.leader()) {
            site.leader = site.procNum;
            site.iLeader(false);
            return true;
            
        } else if (parts[0].equals("LEADER_NEW") && !site.leader()) {
            site.leader = site.procNum;
            site.iLeader(true);
            return true;
            
        } else if (parts[0].equals("DEAD") && Integer.parseInt(parts[1]) == site.leader) {
            handleDeadLeader();
            return true;
            
        } else if (parts[0].equals("STATEREQ")) {
            site.upList[site.leader]= false; 
            site.leader = Integer.parseInt(parts[1]);
            site.upList[site.leader]= true;
            long transactionID = Long.parseLong(parts[2]);
            String myState = site.getState (transactionID);
            site.sendMsg(site.leader, "STATERESP\t" + site.procNum + "\t" + transactionID + "\t" + myState);
            resetTimers();
            site.countMsg (Integer.parseInt(parts[1]));
            return true;
            
        } else if (parts[0].equals("STATEREQ-RECOVERY")) {
            long transactionID = Long.parseLong(parts[2]);
            String myState = site.getState (Long.parseLong(parts[2]));
            site.sendMsg(Integer.parseInt(parts[1]), "STATERESP-RECOVERY\t" + site.procNum + "\t" + transactionID + "\t" + myState);
            site.countMsg (Integer.parseInt(parts[1]));
            return true;
            
        } else if (parts[0].equals("STATERESP-RECOVERY") && site.recoveryFields != null) {
            if (Long.parseLong(parts[2]) == site.recoveryFields.recoveryTransaction) {
                if (parts[3].equals(Process.State.stateStr.get(Process.State.ABORTED)))
                    site.recoveryFields.stateVector[Integer.parseInt(parts[1])] = Process.State.ABORTED;
                if (parts[3].equals(Process.State.stateStr.get(Process.State.COMMITTED)))
                    site.recoveryFields.stateVector[Integer.parseInt(parts[1])] = Process.State.COMMITTED;
                if (parts[3].equals(Process.State.stateStr.get(Process.State.UNCERTAIN)))
                    site.recoveryFields.stateVector[Integer.parseInt(parts[1])] = Process.State.UNCERTAIN;
                if (parts[3].equals(Process.State.stateStr.get(Process.State.PRECOMMIT)))
                    site.recoveryFields.stateVector[Integer.parseInt(parts[1])] = Process.State.PRECOMMIT;
                if (parts[3].equals(Process.State.stateStr.get(Process.State.LOST)))
                    site.recoveryFields.stateVector[Integer.parseInt(parts[1])] = Process.State.LOST;
            }
            if (site.recoveryFields.anyState(Process.State.ABORTED)) {
                abort();
                site.recoveryFields = null;
            } else if (site.recoveryFields.anyState(Process.State.COMMITTED)) {
                String[] tparts = site.recoveryFields.recoveryTransactionParts;
                if (tparts[0].equals("ADD")) {
                    PlaylistHelper.addSong(site, tparts, 0, false);
                    commit();
                } else if (tparts[0].equals("REMOVE")) {
                    PlaylistHelper.removeSong(site, tparts, 0, false);
                    commit();
                } else if (tparts[0].equals("EDIT")) {
                    PlaylistHelper.editSong(site, tparts, 0, false);
                    commit();
                }
                site.recoveryFields = null;
            } else if (site.recoveryFields.allStatesAre(Process.State.LOST)) {
                System.out.println("All states lost. Aborting..");
                abort();
                site.recoveryFields = null;
            }
            site.countMsg (Integer.parseInt(parts[1]));
            return true;
        }

        return false;
    }
    
    private void commit () {
        site.playlist = site.tempPlaylist;
        site.logger.write("COMMIT");
        StateHelper.committed(site);
        site.participantFields.waitingForCommit = false;
        System.out.println ("Committed!");
        site.endTransaction();
    }
    
    private void abort () {
        site.logger.write("ABORT");
        StateHelper.aborted(site);
        site.participantFields.waitingForCommit = false;
        site.participantFields.waitingForPrecommit = false;
        System.out.println ("Aborted!");
        site.endTransaction();
    }
    
    private void resetTimers () {
        if (site.participantFields.waitingForCommit)
            site.participantFields.commitTimer = new Timer(TIMEOUT);
        else if (site.participantFields.waitingForPrecommit)
            site.participantFields.precommitTimer = new Timer(TIMEOUT);
    }
    
    private void handleDeadLeader () {
        site.upList[site.leader] = false;
        site.electLeader(true);
    }

    @Override
    public void checkTimeouts() {
        if (site.participantFields.waitingForCommit && site.participantFields.commitTimer.timeout()) {
            if (site.FAILURE == 12)
                site.dirtyDie();
            resetTimers();
            handleDeadLeader();
        } else if (site.participantFields.waitingForPrecommit && site.participantFields.precommitTimer.timeout()) {
            resetTimers();
            handleDeadLeader();
        }
    }
    
    private void sendVoteResp (boolean status) {
        String decision = (status)? "YES" : "NO";
        site.logger.write(decision);
        if (status)
            StateHelper.uncertain(site);
        else {
            abort();
        }
        site.sendMsg (site.leader, "VOTERESP\t" + site.procNum + "\t" +
                        site.currentTransaction + "\t" + (decision));
        if (status) {
            site.participantFields.waitingForPrecommit = true;
            site.participantFields.precommitTimer = new Timer (TIMEOUT);
            System.out.println ("Waiting for precommit..");
        }
    }
    
    private void sendAck () {
        site.participantFields.waitingForPrecommit = false;
        site.sendMsg (site.leader, "ACK\t" + site.procNum + "\t" + site.currentTransaction);
        site.participantFields.waitingForCommit = true;
        site.participantFields.commitTimer = new Timer (TIMEOUT);
        System.out.println ("Waiting for commit..");
    }
    
    @Override
    public boolean processInput(String input) {
        System.out.println("Participant need no input.");
        return false;
    }
    
    @Override
    public void handleNewLeader() {
        System.out.println("Participant not new leader.");
        return;
    }

}
