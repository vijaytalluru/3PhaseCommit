package ut.distcomp.threepc.playlist;

import java.util.Map;
import java.util.HashMap;

public class Playlist {
    Map<String,String> songs = new HashMap<String,String>();

    public boolean addSong (String song, String url) {
        if (!songs.containsKey(song)) {
            songs.put(song, url);
            return true;
        } else
            return false;
    }
    
    public boolean removeSong (String song) {
        if (songs.containsKey(song)) {
            songs.remove(song);
            return true;
        } else
            return false;
    }
    
    public boolean editSong (String song, String newSong, String url) {
        if (songs.containsKey(song)) {
            if (song.equals(newSong)) {
                songs.put(song, url);
            } else {
                String oldUrl = songs.get(song);
                songs.remove(song);
                songs.put(newSong, oldUrl);
            }
            return true;
        } else
            return false;
    }
    
    public Playlist clone () {
        Playlist newPlaylist = new Playlist();
        for (String song : songs.keySet())
            newPlaylist.songs.put (song, songs.get(song));
        return newPlaylist;
    }
    
    public String toString () {
        StringBuffer sb = new StringBuffer();
        for (String song : songs.keySet())
            sb.append(song + "\t" + songs.get(song) + "\n");
        return new String(sb);
    }
}
