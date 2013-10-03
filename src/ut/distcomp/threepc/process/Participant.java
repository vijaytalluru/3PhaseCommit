package ut.distcomp.threepc.process;

import ut.distcomp.threepc.util.Timer;

public class Participant implements Process {
    
    private static final Participant participant = new Participant();
    private static final int TIMEOUT = 2000;
    Site site;
    
    private Participant () {
    }
    
    public static Participant getParticipant (Site site) {
        participant.site = site;
        return participant;
    }
    

    @Override
    public boolean processMsg(String msg) {
        String[] parts = msg.split("\t");
        
        if (parts[0].equals("UPLIST")) {
            String[] hosts = parts[2].split(" ");
            for (String host : hosts)
                site.upList[Integer.parseInt(host)] = true;
            
        } else if (parts[0].equals("VOTEREQ")) {
            if (Integer.parseInt(parts[1]) == site.leader) {
                site.startTransaction(Long.parseLong(parts[2]));
                if (parts[3].equals("ADD")) {
                    boolean status = PlaylistHelper.addSong(site, parts, 3);
                    sendVoteResp (status);
                } else if (parts[3].equals("REMOVE")) {
                    boolean status = PlaylistHelper.removeSong(site, parts, 3);
                    sendVoteResp (status);
                } else if (parts[3].equals("EDIT")) {
                    boolean status = PlaylistHelper.editSong(site, parts, 3);
                    sendVoteResp (status);
                }
            }
            
        } else if (parts[0].equals("PRECOMMIT")) {
            if (Integer.parseInt(parts[1]) == site.leader) {
                StateHelper.precommit(site);
                sendAck();
            }
            
        } else if (parts[0].equals("COMMIT")) {
            if (Integer.parseInt(parts[1]) == site.leader) {
                site.playlist = site.tempPlaylist;
                StateHelper.committed(site);
                System.out.println ("Committed!");
                site.endTransaction();
            }
            
        } else if (parts[0].equals("ABORT")) {
            if (Integer.parseInt(parts[1]) == site.leader) {
                StateHelper.aborted(site);
                System.out.println ("Aborted!");
                site.endTransaction();
            }
            
        }

        return false;
    }
    
    private void sendVoteResp (boolean status) {
        if (status)
            StateHelper.uncertain(site);
        else {
            StateHelper.aborted(site);
            System.out.println ("Aborted!");
            site.endTransaction();
        }
        site.sendMsg (site.leader, "VOTERESP\t" + site.procNum + "\t" +
                        site.currentTransaction + "\t" + ((status)? "YES" : "NO"));
        if (status) {
            site.participantFields.waitingForPrecommit = true;
            site.participantFields.precommitTimer = new Timer (TIMEOUT);
            System.out.println ("Waiting for precommit..");
            site.waitForMessages();
        }
    }
    
    private void sendAck () {
        site.participantFields.waitingForPrecommit = false;
        site.sendMsg (site.leader, "ACK\t" + site.procNum + "\t" + site.currentTransaction);
        site.participantFields.waitingForCommit = true;
        site.participantFields.commitTimer = new Timer (TIMEOUT);
        System.out.println ("Waiting for commit..");
        site.waitForMessages();
    }
    
    @Override
    public boolean processInput(String input) {
        
        return false;
    }

}
