package org.ramki.advancedChat.task;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.ramki.advancedChat.AdvancedChat;
import org.ramki.advancedChat.ban.BanRecord;
import org.ramki.advancedChat.service.BanService;
import org.ramki.advancedChat.storage.BanRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BanSyncTask {

    private final AdvancedChat plugin;
    private final BanRepository repository;
    private final BanService banService;

    private BukkitTask task;

    public BanSyncTask(AdvancedChat plugin, BanRepository repository, BanService banService) {
        this.plugin = plugin;
        this.repository = repository;
        this.banService = banService;
    }

    public void start(long intervalTicks) {
        long interval = Math.max(1L, intervalTicks);
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, () -> {
            long now = System.currentTimeMillis();
            List<BanRecord> active = this.repository.loadAllActive(now);
            ConcurrentHashMap<UUID, BanRecord> next = new ConcurrentHashMap<>(Math.max(16, active.size() * 2));
            for (BanRecord record : active) {
                next.put(record.uuid(), record);
            }
            this.banService.replaceCache(next);
        }, interval, interval);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }
}