package com.nisovin.magicspells.spells.instant;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.InstantSpell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;

public class GateSpell extends InstantSpell {
	
	private String world;
	private String coords;
	private boolean useSpellEffect;
	private int castTime;
	private String strGateFailed;
	private String strCastDone;
	private String strCastInterrupted;
	
	private HashSet<String> casting;

	public GateSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		world = config.getString("spells." + spellName + ".world", "CURRENT");
		coords = config.getString("spells." + spellName + ".coordinates", "SPAWN");
		useSpellEffect = config.getBoolean("spells." + spellName + ".use-spell-effect", true);
		castTime = getConfigInt("cast-time", 0);
		strGateFailed = config.getString("spells." + spellName + ".str-gate-failed", "Unable to teleport.");
		strCastDone = getConfigString("str-cast-done", "");
		strCastInterrupted = getConfigString("str-cast-interrupted", "");
		
		if (castTime > 0) {
			casting = new HashSet<String>();
			addListener(Event.Type.ENTITY_DAMAGE);
		}
	}

	@Override
	protected PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get world
			World world;
			if (this.world.equals("CURRENT")) {
				world = player.getWorld();
			} else if (this.world.equals("DEFAULT")) {
				world = Bukkit.getServer().getWorlds().get(0);
			} else {
				world = Bukkit.getServer().getWorld(this.world);
			}
			if (world == null) {
				// fail -- no world
				Bukkit.getServer().getLogger().warning("MagicSpells: " + name + ": world " + this.world + " does not exist");
				sendMessage(player, strGateFailed);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// get location
			Location location;
			if (coords.matches("^-?[0-9]+,[0-9]+,-?[0-9]+$")) {
				String[] c = coords.split(",");
				int x = Integer.parseInt(c[0]);
				int y = Integer.parseInt(c[1]);
				int z = Integer.parseInt(c[2]);
				location = new Location(world, x, y, z);
			} else if (coords.equals("SPAWN")) {
				location = world.getSpawnLocation();
			} else {
				// fail -- no location
				Bukkit.getServer().getLogger().warning("MagicSpells: " + name + ": " + this.coords + " is not a valid location");
				sendMessage(player, strGateFailed);
				return PostCastAction.ALREADY_HANDLED;
			}
			location.setX(location.getX()+.5);
			location.setZ(location.getZ()+.5);
			
			// check for landing point
			Block b = location.getBlock();
			if (b.getType() != Material.AIR || b.getRelative(0,1,0).getType() != Material.AIR) {
				// fail -- blocked
				Bukkit.getServer().getLogger().warning("MagicSpells: " + name + ": landing spot blocked");
				sendMessage(player, strGateFailed);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// spell effect
			if (useSpellEffect) {
				final ArrayList<Block> portals = new ArrayList<Block>();
				portals.add(b);
				portals.add(b.getRelative(0,1,0));
				b = player.getLocation().getBlock();
				if (b.getType() == Material.AIR) {
					portals.add(b);
				}
				if (b.getRelative(0,1,0).getType() == Material.AIR) {
					portals.add(b.getRelative(0,1,0));
				}
				for (Block block : portals) {
					block.setType(Material.PORTAL);
				}
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						for (Block block : portals) {
							if (block.getType() == Material.PORTAL) { 
								block.setType(Material.AIR);
							}
						}
					}
				}, 10);
			}
			
			// teleport caster
			if (castTime > 0) {
				// wait a bit
				casting.add(player.getName());
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Teleporter(player, location), castTime);
			} else {
				// go instantly
				player.teleport(location);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled()) return;
		Entity e = event.getEntity();
		if (e instanceof Player) {
			String name = ((Player)e).getName();
			if (casting.contains(name)) {
				casting.remove(name);
				sendMessage((Player)e, strCastInterrupted);
			}
		}
	}
	
	private class Teleporter implements Runnable {
		private Player player;
		private Location location;
		private Location target;
		
		public Teleporter(Player player, Location target) {
			this.player = player;
			this.location = player.getLocation().clone();
			this.target = target;
		}
		
		public void run() {
			if (casting.contains(player.getName())) {
				casting.remove(player.getName());
				Location loc = player.getLocation();
				if (Math.abs(location.getX()-loc.getX()) < .1 && Math.abs(location.getY()-loc.getY()) < .1 && Math.abs(location.getZ()-loc.getZ()) < .1) {
					player.teleport(target);
					sendMessage(player, strCastDone);
				} else {
					sendMessage(player, strCastInterrupted);
				}
			}
		}
	}

}
