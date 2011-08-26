package com.nisovin.MagicSpells.Spells;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.util.config.Configuration;

import com.nisovin.MagicSpells.InstantSpell;
import com.nisovin.MagicSpells.MagicSpells;

public class ExternalCommandSpell extends InstantSpell {
	
	@SuppressWarnings("unused")
	private static final String SPELL_NAME = "external";

	private boolean castWithItem;
	private boolean castByCommand;
	private String[] commandToExecute;
	private String[] commandToExecuteLater;
	private int commandDelay;
	private String[] commandToBlock;
	private boolean requirePlayerTarget;
	private boolean obeyLos;
	private String strCantUseCommand;
	private String strNoTarget;

	public ExternalCommandSpell(Configuration config, String spellName) {
		super(config, spellName);
		
		addListener(Event.Type.PLAYER_COMMAND_PREPROCESS);
		
		castWithItem = config.getBoolean("spells." + spellName + ".can-cast-with-item", true);
		castByCommand = config.getBoolean("spells." + spellName + ".can-cast-by-command", true);
		commandToExecute = config.getString("spells." + spellName + ".command-to-execute", "").split("\\|\\|");
		commandToExecuteLater = config.getString("spells." + spellName + ".command-to-execute-later", "").split("\\|\\|");
		commandDelay = getConfigInt("command-delay", 0);
		commandToBlock = config.getString("spells." + spellName + ".command-to-block", "").split("\\|\\|");
		requirePlayerTarget = getConfigBoolean("require-player-target", false);
		obeyLos = getConfigBoolean("obey-los", true);
		strCantUseCommand = config.getString("spells." + spellName + ".str-cant-use-command", "&4You don't have permission to do that.");
		strNoTarget = getConfigString("str-no-target", "No target found.");
	}

	@Override
	protected PostCastAction castSpell(Player player, SpellCastState state, String[] args) {
		if (commandToExecute.equals("")) {
			Bukkit.getServer().getLogger().severe("MagicSpells: External command spell '" + name + "' has no command to execute.");
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			Player target = null;
			if (requirePlayerTarget) {
				target = getTargetedPlayer(player, range, obeyLos);
				if (target == null) {
					sendMessage(player, strNoTarget);
					return PostCastAction.ALREADY_HANDLED;
				}
			}
			for (String comm : commandToExecute) {
				if (args != null && args.length > 0) {
					for (int i = 0; i < args.length; i++) {
						comm = comm.replace("%"+(i+1), args[i]);
					}
				}
				comm = comm.replace("%a", player.getName());
				if (target != null) {
					comm = comm.replace("%t", target.getName());
				}
				player.performCommand(comm);
			}
			if (commandToExecuteLater != null && commandToExecuteLater.length > 0 && !commandToExecuteLater[0].isEmpty()) {
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new DelayedCommand(player), commandDelay);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!event.getPlayer().isOp() && commandToBlock.length > 0) {
			String msg = event.getMessage();
			for (String comm : commandToBlock) {
				comm = comm.trim();
				if (!comm.equals("") && msg.startsWith("/" + commandToBlock)) {
					event.setCancelled(true);
					sendMessage(event.getPlayer(), strCantUseCommand);
					return;
				}
			}
		}
	}

	@Override
	public boolean canCastByCommand() {
		return castByCommand;
	}

	@Override
	public boolean canCastWithItem() {
		return castWithItem;
	}
	
	private class DelayedCommand implements Runnable {

		private Player player;
		
		public DelayedCommand(Player player) {
			this.player = player;
		}
		
		@Override
		public void run() {
			for (String comm : commandToExecuteLater) {
				if (comm != null && !comm.isEmpty()) {
					player.performCommand(comm);
				}
			}			
		}
		
	}

}
