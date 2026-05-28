package org.ramki.advancedChat.task;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.ramki.advancedChat.AdvancedChat;
import org.ramki.advancedChat.mute.MuteRecord;
import org.ramki.advancedChat.service.MuteService;
import org.ramki.advancedChat.storage.MuteRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MuteSyncTask {

    private final AdvancedChat plugin;
    private final MuteRepository repository;
    private final MuteService muteService;

    private BukkitTask task;

    public MuteSyncTask(AdvancedChat plugin, MuteRepository repository, MuteService muteService) {
        this.plugin = plugin;
        this.repository = repository;
        this.muteService = muteService;
    }

    public void start(long intervalTicks) {
        long interval = Math.max(1L, intervalTicks);
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, () -> {
            long now = System.currentTimeMillis();
            List<MuteRecord> active = this.repository.loadAllActive(now);
            ConcurrentHashMap<UUID, MuteRecord> next = new ConcurrentHashMap<>(Math.max(16, active.size() * 2));
            for (MuteRecord record : active) {
                next.put(record.uuid(), record);
            }
            this.muteService.replaceCache(next);
        }, interval, interval);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }
}
