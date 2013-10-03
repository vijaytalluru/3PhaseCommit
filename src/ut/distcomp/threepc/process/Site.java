package ut.distcomp.threepc.process;

import ut.distcomp.framework.NetController;
import ut.distcomp.framework.Config;
import java.util.List;

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
            for (String msg : listen())
                System.out.println (msg);
    }
    
    private List<String> listen () {
        List<String> msgs;
        for (msgs = net.getReceivedMsgs(); msgs == null; msgs = net.getReceivedMsgs());
        return msgs;
    }
    
    public void finalize () {
        net.shutdown();
    }
    

}
