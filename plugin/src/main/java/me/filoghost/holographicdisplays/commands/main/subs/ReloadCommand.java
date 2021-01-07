/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package me.filoghost.holographicdisplays.commands.main.subs;

import me.filoghost.fcommons.logging.Log;
import me.filoghost.holographicdisplays.Colors;
import me.filoghost.holographicdisplays.HolographicDisplays;
import me.filoghost.holographicdisplays.Permissions;
import me.filoghost.holographicdisplays.bridge.bungeecord.BungeeServerTracker;
import me.filoghost.holographicdisplays.commands.Messages;
import me.filoghost.holographicdisplays.commands.main.HologramSubCommand;
import me.filoghost.holographicdisplays.common.Utils;
import me.filoghost.holographicdisplays.disk.Configuration;
import me.filoghost.holographicdisplays.disk.HologramDatabase;
import me.filoghost.holographicdisplays.disk.UnicodeSymbols;
import me.filoghost.holographicdisplays.event.HolographicDisplaysReloadEvent;
import me.filoghost.holographicdisplays.exception.HologramLineParseException;
import me.filoghost.holographicdisplays.exception.HologramNotFoundException;
import me.filoghost.holographicdisplays.exception.InvalidFormatException;
import me.filoghost.holographicdisplays.exception.WorldNotFoundException;
import me.filoghost.holographicdisplays.object.CraftHologram;
import me.filoghost.holographicdisplays.object.NamedHologram;
import me.filoghost.holographicdisplays.object.NamedHologramManager;
import me.filoghost.holographicdisplays.placeholder.AnimationsRegister;
import me.filoghost.holographicdisplays.placeholder.PlaceholdersManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ReloadCommand extends HologramSubCommand {

    public ReloadCommand() {
        super("reload");
        setPermission(Permissions.COMMAND_BASE + "reload");
    }

    @Override
    public String getPossibleArguments() {
        return "";
    }

    @Override
    public int getMinimumArguments() {
        return 0;
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {            
        long startMillis = System.currentTimeMillis();

        UnicodeSymbols.load(HolographicDisplays.getInstance());
        Configuration.load(HolographicDisplays.getInstance());
        
        BungeeServerTracker.resetTrackedServers();
        BungeeServerTracker.restartTask(Configuration.bungeeRefreshSeconds);
        
        HologramDatabase.loadYamlFile(HolographicDisplays.getInstance());
        try {
            AnimationsRegister.loadAnimations(HolographicDisplays.getInstance());
        } catch (Exception e) {
            Log.warning("Failed to load animation files!", e);
        }
        
        PlaceholdersManager.untrackAll();
        NamedHologramManager.clearAll();
        
        Set<String> savedHolograms = HologramDatabase.getHolograms();
        if (savedHolograms != null && savedHolograms.size() > 0) {
            for (String singleSavedHologram : savedHolograms) {
                try {
                    NamedHologram singleHologramEntity = HologramDatabase.loadHologram(singleSavedHologram);
                    NamedHologramManager.addHologram(singleHologramEntity);
                } catch (HologramNotFoundException e) {
                    Messages.sendWarning(sender, "Hologram '" + singleSavedHologram + "' not found, skipping it.");
                } catch (InvalidFormatException e) {
                    Messages.sendWarning(sender, "Hologram '" + singleSavedHologram + "' has an invalid location format.");
                } catch (HologramLineParseException e) {
                    Messages.sendWarning(sender, "Hologram '" + singleSavedHologram + "' has an invalid line: " + Utils.uncapitalize(e.getMessage()));
                } catch (WorldNotFoundException e) {
                    Messages.sendWarning(sender, "Hologram '" + singleSavedHologram + "' was in the world '" + e.getMessage() + "' but it wasn't loaded.");
                }
            }
        }
        
        for (CraftHologram hologram : NamedHologramManager.getHolograms()) {
            hologram.refreshAll();
        }
        
        long endMillis = System.currentTimeMillis();
        
        sender.sendMessage(Colors.PRIMARY + "Configuration reloaded successfully in " + (endMillis - startMillis) + "ms!");
        
        Bukkit.getPluginManager().callEvent(new HolographicDisplaysReloadEvent());
    }
    
    @Override
    public List<String> getTutorial() {
        return Arrays.asList("Reloads the holograms from the database.");
    }
    
    @Override
    public SubCommandType getType() {
        return SubCommandType.GENERIC;
    }

}
