package org.zacsn.signal_dectect.domain.alert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AlertConfig {
    private final Set<String> watchlistKeywords;
    private final Set<String> watchlistMacs;
    private final Set<String> watchlistBrands;
    private final Set<String> whitelistMacs;
    private final Set<String> blacklistMacs;

    public AlertConfig(
            Set<String> watchlistKeywords,
            Set<String> watchlistMacs,
            Set<String> watchlistBrands,
            Set<String> whitelistMacs,
            Set<String> blacklistMacs
    ) {
        this.watchlistKeywords = copySet(watchlistKeywords);
        this.watchlistMacs = copySet(watchlistMacs);
        this.watchlistBrands = copySet(watchlistBrands);
        this.whitelistMacs = copySet(whitelistMacs);
        this.blacklistMacs = copySet(blacklistMacs);
    }

    public Set<String> getWatchlistKeywords() {
        return watchlistKeywords;
    }

    public Set<String> getWatchlistMacs() {
        return watchlistMacs;
    }

    public Set<String> getWatchlistBrands() {
        return watchlistBrands;
    }

    public Set<String> getWhitelistMacs() {
        return whitelistMacs;
    }

    public Set<String> getBlacklistMacs() {
        return blacklistMacs;
    }

    private static Set<String> copySet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(values));
    }
}
