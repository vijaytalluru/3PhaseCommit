package ut.distcomp.threepc.process;


public class PlaylistHelper {
    public static boolean addSong (Site site, String[] parts, int base) {
        System.out.println ("Adding song..");
        site.tempPlaylist = site.playlist.clone();
        return site.tempPlaylist.addSong (parts[base+1], parts[base+2]);
    }

    public static boolean removeSong (Site site, String[] parts, int base) {
        System.out.println ("Removing song..");
        site.tempPlaylist = site.playlist.clone();
        return site.tempPlaylist.removeSong (parts[base+1]);
    }

    public static boolean editSong (Site site, String[] parts, int base) {
        System.out.println ("Editing song..");
        site.tempPlaylist = site.playlist.clone();
        return site.tempPlaylist.editSong (parts[base+1], parts[base+2], parts[base+3]);
    }

}
