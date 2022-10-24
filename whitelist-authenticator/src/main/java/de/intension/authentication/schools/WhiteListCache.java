package de.intension.authentication.schools;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.intension.authentication.dto.SchoolWhitelistEntry;

/**
 * In memory cache, storing whitelist entries for school and client IDs.
 */
public class WhiteListCache
{

    private final Object                          lock    = new Object();
    private final ArrayList<SchoolWhitelistEntry> entries = new ArrayList<>();
    private Date                                  lastUpdated;

    public static WhiteListCache getInstance()
    {
        return LazyHolder.INSTANCE;
    }

    /**
     * Clear and update cache entries.
     */
    public void updateCache(List<SchoolWhitelistEntry> entries)
    {
        synchronized(lock) {
            this.entries.clear();
            this.entries.addAll(entries);
        }
        this.lastUpdated = new Date();
    }

    /**
     * Clear cache.
     */
    public void clear(){
        synchronized(lock){
            this.entries.clear();
            this.lastUpdated = null;
        }
    }

    /**
     * Get all whitelist entries from cache.
     */
    public List<SchoolWhitelistEntry> getAll()
    {
        synchronized(lock) {
            return new ArrayList<>(this.entries);
        }
    }

    /**
     * Get current cache status.
     */
    public synchronized CacheStatus getState(int intervalInMinutes)
    {
        CacheStatus state = CacheStatus.VALID;
        if (lastUpdated == null) {
            state = CacheStatus.NOT_INITIALIZED;
        }
        else {
            Calendar nextUpdateTime = Calendar.getInstance();
            nextUpdateTime.setTime(lastUpdated);
            nextUpdateTime.add(Calendar.MINUTE, intervalInMinutes);
            if (nextUpdateTime.before(Calendar.getInstance())) {
                state = CacheStatus.OUTDATED;
                //set last updated state to prevent multiple update calls by different threads
                lastUpdated = new Date();
            }
        }
        return state;
    }

    /**
     * Initialization-on-demand holder idiom
     */
    private static class LazyHolder
    {
        static final WhiteListCache INSTANCE = new WhiteListCache();
    }

}
