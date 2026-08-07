package me.foesio.foAuction.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerData {
    private final UUID uuid;
    private final List<AuctionListing> auctions;
    private final List<ClaimEntry> claims;
    private final List<SoldAuctionHistoryEntry> soldHistory;
    private final List<String> pendingNotifications;
    private SortMode sortMode;
    private FilterMode filterMode;

    public PlayerData(UUID uuid) {
        this(
                uuid,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                SortMode.NEWEST,
                FilterMode.ALL
        );
    }

    public PlayerData(
            UUID uuid,
            List<AuctionListing> auctions,
            List<ClaimEntry> claims,
            List<SoldAuctionHistoryEntry> soldHistory,
            List<String> pendingNotifications,
            SortMode sortMode,
            FilterMode filterMode
    ) {
        this.uuid = uuid;
        this.auctions = auctions;
        this.claims = claims;
        this.soldHistory = soldHistory;
        this.pendingNotifications = pendingNotifications;
        this.sortMode = sortMode;
        this.filterMode = filterMode;
    }

    public UUID getUuid() {
        return uuid;
    }

    public List<AuctionListing> getAuctions() {
        return auctions;
    }

    public List<ClaimEntry> getClaims() {
        return claims;
    }

    public List<SoldAuctionHistoryEntry> getSoldHistory() {
        return soldHistory;
    }

    public List<String> getPendingNotifications() {
        return pendingNotifications;
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;
    }

    public FilterMode getFilterMode() {
        return filterMode;
    }

    public void setFilterMode(FilterMode filterMode) {
        this.filterMode = filterMode;
    }

    public PlayerData snapshot() {
        List<AuctionListing> copiedAuctions = auctions.stream()
                .map(AuctionListing::copy)
                .toList();

        List<ClaimEntry> copiedClaims = claims.stream()
                .map(ClaimEntry::copy)
                .toList();

        List<SoldAuctionHistoryEntry> copiedHistory = soldHistory.stream()
                .map(SoldAuctionHistoryEntry::copy)
                .toList();

        List<String> copiedNotifications = new ArrayList<>(pendingNotifications);

        return new PlayerData(
                uuid,
                new ArrayList<>(copiedAuctions),
                new ArrayList<>(copiedClaims),
                new ArrayList<>(copiedHistory),
                copiedNotifications,
                sortMode,
                filterMode
        );
    }
}
