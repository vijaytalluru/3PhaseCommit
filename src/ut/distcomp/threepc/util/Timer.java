package ut.distcomp.threepc.util;

public class Timer {
    long start;
    long limit;
    
    public Timer (long millis) {
        start = System.currentTimeMillis();
        limit = millis;
    }
    
    public boolean timeout() {
        return System.currentTimeMillis() - start > limit;
    }
    
    public long remaining() {
        return start + limit - System.currentTimeMillis();
    }

}
