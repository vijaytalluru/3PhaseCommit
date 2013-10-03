package ut.distcomp.threepc.process;

public class StateHelper {
    
    public static void virgin (Site site) {
        site.state = Process.State.VIRGIN;
        System.out.println("STATE:\t" + site.state);
    }
    
    public static void aborted (Site site) {
        site.state = Process.State.ABORTED;
        System.out.println("STATE:\t" + site.state);
    }
    
    public static void uncertain (Site site) {
        site.state = Process.State.UNCERTAIN;
        System.out.println("STATE:\t" + site.state);
    }
    
    public static void precommit (Site site) {
        site.state = Process.State.PRECOMMIT;
        System.out.println("STATE:\t" + site.state);
    }
    
    public static void committed (Site site) {
        site.state = Process.State.COMMITTED;
        System.out.println("STATE:\t" + site.state);
        System.out.println("NEW PLAYLIST:\n" + site.playlist);
    }

}
