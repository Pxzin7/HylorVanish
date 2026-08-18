package br.com.hylor.vanish;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class HylorVanish extends JavaPlugin implements Listener {

    private static final String STAFF_PERMISSION = "hylorvanish.staff";
    private final Set<UUID> vanished = new HashSet<UUID>();
    private File vanishedFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        vanishedFile = new File(getDataFolder(), "vanished.yml");
        loadVanished();

        Bukkit.getPluginManager().registerEvents(this, this);

        // Reaplica o vanish caso o plugin seja recarregado com jogadores online.
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                refreshVisibility();
            }
        }, 1L);

        // Mantém a ActionBar visível enquanto o staff estiver em vanish.
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                String bar = msg("actionbar", "&d&lMODO VANISH");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isVanished(player)) {
                        sendActionBar(player, bar);
                    }
                }
            }
        }, 10L, 20L);

        getLogger().info("HylorVanish habilitado. Vanish persistente carregado: " + vanished.size());
    }

    @Override
    public void onDisable() {
        saveVanished();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color(msg("only-player", "&cApenas jogadores podem usar este comando.")));
            return true;
        }

        Player player = (Player) sender;
        String name = command.getName().toLowerCase(Locale.ROOT);

        if (name.equals("v")) {
            if (!player.hasPermission(STAFF_PERMISSION)) {
                player.sendMessage(color(msg("no-permission", "&cVocê não possui permissão.")));
                return true;
            }

            if (isVanished(player)) {
                player.sendMessage(color(msg("already-vanished", "&eVocê já está no modo vanish. Use &f/vv &epara sair.")));
                return true;
            }

            vanished.add(player.getUniqueId());
            saveVanished();
            applyVanish(player);
            player.sendMessage(color(msg("vanish-enabled", "&aVocê entrou no modo vanish.")));
            sendActionBar(player, msg("actionbar", "&d&lMODO VANISH"));
            return true;
        }

        if (name.equals("vv")) {
            // Quem já está em vanish sempre pode sair, mesmo se a permissão for removida depois.
            if (!isVanished(player)) {
                if (!player.hasPermission(STAFF_PERMISSION)) {
                    player.sendMessage(color(msg("no-permission", "&cVocê não possui permissão.")));
                } else {
                    player.sendMessage(color(msg("not-vanished", "&eVocê não está no modo vanish.")));
                }
                return true;
            }

            vanished.remove(player.getUniqueId());
            saveVanished();
            removeVanish(player);
            player.sendMessage(color(msg("vanish-disabled", "&cVocê saiu do modo vanish.")));
            sendActionBar(player, "");
            return true;
        }

        return false;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player joining = event.getPlayer();

        // Se ele já estava salvo em vanish, nem mensagem de entrada é exibida.
        if (isVanished(joining) && getConfig().getBoolean("hide-join-quit-messages", true)) {
            event.setJoinMessage(null);
        }

        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                // O jogador que entrou recebe o estado de todos os vanished.
                for (Player hidden : Bukkit.getOnlinePlayers()) {
                    if (isVanished(hidden)) {
                        setVisibility(joining, hidden);
                    }
                }

                // Se quem entrou está em vanish, aplica para todos os viewers.
                if (isVanished(joining)) {
                    applyVanish(joining);
                    sendActionBar(joining, msg("actionbar", "&d&lMODO VANISH"));
                }
            }
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (isVanished(event.getPlayer()) && getConfig().getBoolean("hide-join-quit-messages", true)) {
            event.setQuitMessage(null);
        }
        // Não remove do Set: vanish é persistente e só /vv desativa.
    }

    private boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    private void applyVanish(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            setVisibility(viewer, target);
        }
    }

    private void setVisibility(Player viewer, Player target) {
        if (viewer.getUniqueId().equals(target.getUniqueId())) {
            return;
        }

        if (viewer.hasPermission(STAFF_PERMISSION)) {
            viewer.showPlayer(target);
        } else {
            viewer.hidePlayer(target);
        }
    }

    private void removeVanish(Player target) {
        // Ao sair do vanish, torna o jogador visível para todos.
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.getUniqueId().equals(target.getUniqueId())) {
                viewer.showPlayer(target);
            }
        }
    }

    private void refreshVisibility() {
        for (Player hidden : Bukkit.getOnlinePlayers()) {
            if (isVanished(hidden)) {
                applyVanish(hidden);
            }
        }
    }

    private String msg(String path, String def) {
        String value = getConfig().getString("messages." + path);
        return value == null ? def : value;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private void loadVanished() {
        vanished.clear();
        if (!vanishedFile.exists()) {
            saveVanished();
            return;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(vanishedFile), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("-")) continue;
                String raw = line.substring(1).trim();
                if (raw.startsWith("'") && raw.endsWith("'") && raw.length() > 1) {
                    raw = raw.substring(1, raw.length() - 1);
                }
                try {
                    vanished.add(UUID.fromString(raw));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (IOException ex) {
            getLogger().warning("Não foi possível ler vanished.yml: " + ex.getMessage());
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) {}
            }
        }
    }

    private synchronized void saveVanished() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Não foi possível criar a pasta do plugin.");
            return;
        }

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(vanishedFile), StandardCharsets.UTF_8));
            writer.println("vanished:");
            List<String> ids = new ArrayList<String>();
            for (UUID uuid : vanished) ids.add(uuid.toString());
            Collections.sort(ids);
            for (String uuid : ids) {
                writer.println("  - '" + uuid + "'");
            }
        } catch (IOException ex) {
            getLogger().warning("Não foi possível salvar vanished.yml: " + ex.getMessage());
        } finally {
            if (writer != null) writer.close();
        }
    }

    /**
     * ActionBar via reflection para manter compatibilidade com Spigot/Paper 1.8.8
     * sem depender diretamente das classes NMS na compilação.
     */
    private void sendActionBar(Player player, String message) {
        try {
            String craftPackage = player.getClass().getPackage().getName();
            String version = craftPackage.substring(craftPackage.lastIndexOf('.') + 1);
            String nms = "net.minecraft.server." + version + ".";

            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Field connectionField = handle.getClass().getField("playerConnection");
            Object connection = connectionField.get(handle);

            Class<?> iChatBaseComponent = Class.forName(nms + "IChatBaseComponent");
            Class<?> serializer = Class.forName(nms + "IChatBaseComponent$ChatSerializer");
            Method a = serializer.getMethod("a", String.class);

            String colored = color(message);
            String json = "{\"text\":\"" + escapeJson(colored) + "\"}";
            Object component = a.invoke(null, json);

            Class<?> packetClass = Class.forName(nms + "PacketPlayOutChat");
            Constructor<?> constructor = packetClass.getConstructor(iChatBaseComponent, byte.class);
            Object packet = constructor.newInstance(component, (byte) 2);

            Class<?> packetBase = Class.forName(nms + "Packet");
            Method sendPacket = connection.getClass().getMethod("sendPacket", packetBase);
            sendPacket.invoke(connection, packet);
        } catch (Throwable ignored) {
            // Se a implementação do servidor não for compatível com o pacote NMS esperado,
            // o vanish continua funcionando; apenas a ActionBar não é enviada.
        }
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
