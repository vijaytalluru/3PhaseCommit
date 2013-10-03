package ut.distcomp.threepc;

import ut.distcomp.threepc.process.Site;

public class Driver {

    public static void main(String[] args) {
        Site site = new Site (Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        site.testMe();

    }

}
