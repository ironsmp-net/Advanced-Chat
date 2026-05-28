package org.ramki.advancedChat.command;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.ramki.advancedChat.AdvancedChat;
import org.ramki.advancedChat.mute.MuteRecord;
import org.ramki.advancedChat.service.MuteService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class MuteCommand implements TabExecutor {

    private static final String STAFF_PERMISSION = "advancedchat.staff";

    private final AdvancedChat plugin;
    private final MuteService muteService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MuteCommand(AdvancedChat plugin, MuteService muteService) {
        this.plugin = plugin;
        this.muteService = muteService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission(STAFF_PERMISSION)) {
            sender.sendRichMessage("<red>No permission.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("unmute")) {
            return handleUnmute(sender, args);
        }
        return handleMute(sender, args);
    }

    private boolean handleMute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendRichMessage("<red>Usage: /mute <player> <number> <minute|hour|day> <reason>");
            return true;
        }

        String targetName = args[0];

        long number;
        try {
            number = Long.parseLong(args[1]);
            if (number <= 0L) {
                sender.sendRichMessage("<red>Number must be positive.");
                return true;
            }
        } catch (NumberFormatException ex) {
            sender.sendRichMessage("<red>Invalid number: <yellow>" + args[1]);
            return true;
        }

        long durationMillis = parseUnit(args[2], number);
        if (durationMillis < 0L) {
            sender.sendRichMessage("<red>Unknown time unit '<yellow>" + args[2] + "<red>'. Use minute, hour, or day.");
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            if (i > 3) reasonBuilder.append(' ');
            reasonBuilder.append(args[i]);
        }
        String reason = reasonBuilder.toString();

        UUID targetUuid;
        String resolvedName = targetName;
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        if (onlineTarget != null) {
            targetUuid = onlineTarget.getUniqueId();
            resolvedName = onlineTarget.getName();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(targetName);
            if (offline == null) {
                sender.sendRichMessage("<red>Player <yellow>" + targetName + "<red> not found. They must have joined this server at least once.");
                return true;
            }
            targetUuid = offline.getUniqueId();
            if (offline.getName() != null) resolvedName = offline.getName();
        }

        long now = System.currentTimeMillis();
        long expiresAt = now + durationMillis;
        MuteRecord record = new MuteRecord(targetUuid, resolvedName, expiresAt, reason, sender.getName(), now);
        this.muteService.mute(record);

        long days = TimeUnit.MILLISECONDS.toDays(durationMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis) % 24L;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60L;
        if (days == 0L && hours == 0L && minutes == 0L) minutes = 1L;

        sender.sendRichMessage("<green>Muted <yellow>" + resolvedName + "<green> for <yellow>"
                + days + "d " + hours + "h " + minutes + "m<green>. Reason: <yellow>" + reason);

        if (onlineTarget != null && onlineTarget.isOnline()) {
            String raw = this.plugin.getSettings().mute().chatBlockedMessage()
                    .replace("%days%", String.valueOf(days))
                    .replace("%hours%", String.valueOf(hours))
                    .replace("%minutes%", String.valueOf(minutes))
                    .replace("%reason%", reason)
                    .replace("%muter%", sender.getName())
                    .replace("%player%", resolvedName);
            if (this.plugin.isPapiEnabled()) {
                raw = PlaceholderAPI.setPlaceholders(onlineTarget, raw);
            }
            onlineTarget.sendMessage(this.miniMessage.deserialize(raw));
        }

        return true;
    }

    private boolean handleUnmute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendRichMessage("<red>Usage: /unmute <player>");
            return true;
        }

        String targetName = args[0];
        UUID targetUuid;
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        if (onlineTarget != null) {
            targetUuid = onlineTarget.getUniqueId();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(targetName);
            if (offline == null) {
                sender.sendRichMessage("<red>Player <yellow>" + targetName + "<red> not found.");
                return true;
            }
            targetUuid = offline.getUniqueId();
        }

        if (!this.muteService.isMuted(targetUuid)) {
            sender.sendRichMessage("<red>That player is not muted.");
            return true;
        }

        this.muteService.unmute(targetUuid);
        sender.sendRichMessage("<green>Unmuted <yellow>" + targetName + "<green>.");

        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendRichMessage("<green>You have been unmuted.");
        }
        return true;
    }

    private static long parseUnit(String unit, long number) {
        String u = unit.toLowerCase(Locale.ROOT);
        return switch (u) {
            case "m", "min", "mins", "minute", "minutes" -> TimeUnit.MINUTES.toMillis(number);
            case "h", "hr", "hrs", "hour", "hours" -> TimeUnit.HOURS.toMillis(number);
            case "d", "day", "days" -> TimeUnit.DAYS.toMillis(number);
            default -> -1L;
        };
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!sender.hasPermission(STAFF_PERMISSION)) return Collections.emptyList();

        if (command.getName().equalsIgnoreCase("unmute")) {
            if (args.length == 1) return onlinePlayerSuggestions(args[0]);
            return Collections.emptyList();
        }

        if (args.length == 1) return onlinePlayerSuggestions(args[0]);
        if (args.length == 2) return filterPrefix(args[1], List.of("1", "5", "10", "30", "60"));
        if (args.length == 3) return filterPrefix(args[2], List.of("minute", "hour", "day"));
        return Collections.emptyList();
    }

    private List<String> onlinePlayerSuggestions(String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(Locale.ROOT).startsWith(lower)) result.add(p.getName());
        }
        return result;
    }

    private List<String> filterPrefix(String prefix, List<String> options) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String opt : options) {
            if (opt.startsWith(lower)) result.add(opt);
        }
        return result;
    }
}
