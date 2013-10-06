package ut.distcomp.threepc.process;

import ut.distcomp.threepc.util.Timer;

public class Leader implements Process {
    
    private static Leader leader;
    private static final int TIMEOUT = 2000;
    Site site;
    
    private Leader () {
    }
    
    public static Leader getLeader (Site site) {
        leader = new Leader();
        leader.site = site;
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
            
        } else if (site.leaderFields.waitingForAck && parts[0].equals("ACK")) {
            if (Long.parseLong(parts[2]) == site.currentTransaction) {
                site.leaderFields.ackVector[Integer.parseInt(parts[1])] = Process.Ack.ACK;
                
            }
            if (site.leaderFields.allAcks()) {
                commit();
            } else if (site.leaderFields.ackTimer.timeout()) {
                commit();
            }
            
        }
        
        return false;
    }

    @Override
    public void checkTimeouts() {
        if (site.leaderFields.waitingForAck && site.leaderFields.ackTimer.timeout()) {
            commit();
        } else if (site.leaderFields.waitingForVotes && site.leaderFields.voteTimer.timeout()) {
            abort();
        }
    }
    
    private void die () {
        for (int i=0; i<site.numProcs; ++i)
            if (i != site.leader)
                site.sendMsg (i, "DEAD\t" + site.procNum);
    }
    
    private void commit () {
        site.leaderFields.waitingForAck = false;
        
        String upHosts = site.pingAll();
        
        for (int i=0; i<site.numProcs; ++i) {
            if (i != site.leader && site.leaderFields.ackVector[i] == Process.Ack.ACK) {
                site.sendMsg (i, "UPLIST\t" + site.procNum + "\t" + upHosts);
                site.sendMsg (i, "COMMIT\t" + site.procNum + "\t" + site.currentTransaction);
            }
        }
        
        actualCommit ();
        
    }
    
    private void actualCommit () {
        site.playlist = site.tempPlaylist;
        StateHelper.committed(site);
        System.out.println ("Commited!");
        site.endTransaction();
        
    }

    private void abort () {
        site.leaderFields.waitingForVotes = false;
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
    
    private void precommit () {
        site.leaderFields.waitingForVotes = false;
        StateHelper.precommit(site);
        
        String upHosts = site.pingAll();
        site.leaderFields.ackVector = new int[site.numProcs];
        
        for (int i=0; i<site.numProcs; ++i) {
            if (i != site.leader && site.leaderFields.voteVector[i] == Process.Vote.YES) {
                site.sendMsg (i, "UPLIST\t" + site.procNum + "\t" + upHosts);
                site.sendMsg (i, "PRECOMMIT\t" + site.procNum + "\t" + site.currentTransaction);
                site.leaderFields.ackVector[i] = Process.Ack.PENDING;
            } else {
                site.leaderFields.ackVector[i] = Process.Ack.NA;
            }
        }
        site.leaderFields.waitingForAck = true;
        site.leaderFields.ackTimer = new Timer (TIMEOUT);
        System.out.println ("Waiting for acks..");
        //site.waitForMessages();
    }
    
    private void voteReq (String input) {
        long transactionID = System.currentTimeMillis();
        site.startTransaction(transactionID);
        
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
        //site.waitForMessages();
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
            site.leaderFields.voteVector = new int[site.numProcs];
            boolean status = PlaylistHelper.addSong(site, parts, 0);
            if (loneWolf())
                actualCommit();
            else {
                site.leaderFields.voteVector[site.procNum] = (status)? Process.Vote.YES : Process.Vote.NO;
                voteReq(input);
            }
        } else if (parts[0].equals("REMOVE")) {
            site.leaderFields.voteVector = new int[site.numProcs];
            boolean status = PlaylistHelper.removeSong(site, parts, 0);
            if (loneWolf())
                actualCommit();
            else {
                site.leaderFields.voteVector[site.procNum] = (status)? Process.Vote.YES : Process.Vote.NO;
                voteReq(input);
            }
        } else if (parts[0].equals("EDIT")) {
            site.leaderFields.voteVector = new int[site.numProcs];
            boolean status = PlaylistHelper.editSong(site, parts, 0);
            if (loneWolf())
                actualCommit();
            else {
                site.leaderFields.voteVector[site.procNum] = (status)? Process.Vote.YES : Process.Vote.NO;
                voteReq(input);
            }
        } else if (parts[0].equals("DIE")) {
            die();
            System.exit (0);
        }
        
        return true;
    }

}
