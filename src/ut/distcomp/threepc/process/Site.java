package ut.distcomp.threepc.process;

import ut.distcomp.framework.NetController;
import ut.distcomp.framework.Config;

public class Site {
    
    private NetController net;
    private int procNum, numProcs;
    
    public Site (int no, int total) {
        int SLEEP = (int)((15-1.5*no)*1000);
        net = new NetController (new Config (no, total), SLEEP);
        procNum = no;
        numProcs = total;
    }
    
    public void testMe () {
        for (int i=0; i<numProcs; ++i)
            net.sendMsg (i, "Hello " + i + ", I am " + procNum);
        while (true)
            for (String msg : net.getReceivedMsgs()) {
                System.out.println (msg);
            }
    }
    
    public void finalize () {
        net.shutdown();
    }
    

}
