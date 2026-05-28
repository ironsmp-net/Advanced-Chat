package org.ramki.advancedChat.service;

import org.ramki.advancedChat.mute.MuteRecord;
import org.ramki.advancedChat.storage.MuteRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class MuteService {

    private final AtomicReference<ConcurrentHashMap<UUID, MuteRecord>> cache =
            new AtomicReference<>(new ConcurrentHashMap<>());
    private final MuteRepository repository;

    public MuteService(MuteRepository repository) {
        this.repository = repository;
    }

    public Optional<MuteRecord> getActive(UUID uuid) {
        MuteRecord record = this.cache.get().get(uuid);
        if (record == null) return Optional.empty();
        if (record.isExpired(System.currentTimeMillis())) {
            this.cache.get().remove(uuid);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    public boolean isMuted(UUID uuid) {
        return getActive(uuid).isPresent();
    }

    public void mute(MuteRecord record) {
        this.cache.get().put(record.uuid(), record);
        if (this.repository != null) {
            this.repository.upsertAsync(record);
        }
    }

    public void unmute(UUID uuid) {
        this.cache.get().remove(uuid);
        if (this.repository != null) {
            this.repository.deleteAsync(uuid);
        }
    }

    public void replaceCache(ConcurrentHashMap<UUID, MuteRecord> next) {
        this.cache.set(next);
    }

    public void clear() {
        this.cache.set(new ConcurrentHashMap<>());
    }
}
