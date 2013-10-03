package ut.distcomp.threepc.process;

import ut.distcomp.framework.NetController;
import ut.distcomp.framework.Config;

public class Site {
    
    private NetController net;
    private int procNum, numProcs;
    
    public Site (int no, int total) {
        net = new NetController (new Config (no, total));
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
