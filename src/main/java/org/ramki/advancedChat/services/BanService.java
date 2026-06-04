package org.ramki.advancedChat.service;

import org.ramki.advancedChat.ban.BanRecord;
import org.ramki.advancedChat.storage.BanRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class BanService {

    private final AtomicReference<ConcurrentHashMap<UUID, BanRecord>> cache =
            new AtomicReference<>(new ConcurrentHashMap<>());
    private final BanRepository repository;

    public BanService(BanRepository repository) {
        this.repository = repository;
    }

    public Optional<BanRecord> getActive(UUID uuid) {
        BanRecord record = this.cache.get().get(uuid);
        if (record == null) return Optional.empty();
        if (record.isExpired(System.currentTimeMillis())) {
            this.cache.get().remove(uuid);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    public boolean isBanned(UUID uuid) {
        return getActive(uuid).isPresent();
    }

    public void ban(BanRecord record) {
        this.cache.get().put(record.uuid(), record);
        if (this.repository != null) {
            this.repository.upsertAsync(record);
        }
    }

    public void unban(UUID uuid) {
        this.cache.get().remove(uuid);
        if (this.repository != null) {
            this.repository.deleteAsync(uuid);
        }
    }

    public void replaceCache(ConcurrentHashMap<UUID, BanRecord> next) {
        this.cache.set(next);
    }

    public void clear() {
        this.cache.set(new ConcurrentHashMap<>());
    }
}