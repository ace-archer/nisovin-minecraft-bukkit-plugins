package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.MagicConfig;

public class AreaEffectSpell extends TargetedSpell implements TargetedLocationSpell {

	private int radius;
	private int verticalRadius;
	private boolean pointBlank;
	private boolean failIfNoTargets;
	private int maxTargets;
	private List<String> spellNames;
	private boolean spellSourceInCenter;
	private List<TargetedSpell> spells;
	
	public AreaEffectSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radius = getConfigInt("horizontal-radius", 10);
		//radiusSquared *= radiusSquared;
		verticalRadius = getConfigInt("vertical-radius", 5);
		pointBlank = getConfigBoolean("point-blank", true);
		failIfNoTargets = getConfigBoolean("fail-if-no-targets", true);
		maxTargets = getConfigInt("max-targets", 0);
		spellSourceInCenter = getConfigBoolean("spell-source-in-center", false);
		spellNames = getConfigStringList("spells", null);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		spells = new ArrayList<TargetedSpell>();
		
		if (spellNames != null && spellNames.size() > 0) {
			for (String spellName : spellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (spell != null) {
					if (spell instanceof TargetedLocationSpell || spell instanceof TargetedEntitySpell) {
						spells.add((TargetedSpell)spell);
					} else {
						MagicSpells.error("AreaEffect spell '" + name + "' attempted to use non-targeted spell '" + spellName + "'");
					}
				} else {
					MagicSpells.error("AreaEffect spell '" + name + "' attempted to use non-existant spell '" + spellName + "'");
				}
			}
			spellNames.clear();
			spellNames = null;
		}
		
		if (spells.size() == 0) {
			MagicSpells.error("AreaEffect spell '" + name + "' has no spells!");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			
			// get location for aoe
			Location loc = null;
			if (pointBlank) {
				loc = player.getLocation();
			} else {
				try {
					Block block = getTargetedBlock(player, power);
					if (block != null && block.getType() != Material.AIR) {
						loc = block.getLocation();
					}
				} catch (IllegalStateException e) {
					loc = null;
				}
			}
			if (loc != null) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, loc);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					loc = null;
				} else {
					loc = event.getTargetLocation();
				}
			}
			if (loc == null) {
				return noTarget(player);
			}
			
			// cast spells on nearby entities
			boolean done = doAoe(player, loc, power);
			
			// check if no targets
			if (!done && failIfNoTargets) {
				return noTarget(player);
			}
			
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean doAoe(Player player, Location location, float power) {
		int count = 0;
		
		BoundingBox box = new BoundingBox(location, radius, verticalRadius);
		List<Entity> entities = new ArrayList<Entity>(location.getWorld().getEntitiesByClasses(LivingEntity.class));
		Collections.shuffle(entities);
		for (Entity e : entities) {
			if (e instanceof LivingEntity && box.contains(e)) {
				LivingEntity target = (LivingEntity)e;
				if (!target.isDead() && ((player == null && validTargetList.canTarget(target)) || validTargetList.canTarget(player, target))) {
					if (player != null) {
						SpellTargetEvent event = new SpellTargetEvent(this, player, target);
						Bukkit.getPluginManager().callEvent(event);
						if (event.isCancelled()) {
							continue;
						} else {
							target = event.getTarget();
						}
					}
					for (TargetedSpell spell : spells) {
						if (player != null) {
							if (spellSourceInCenter && spell instanceof TargetedEntityFromLocationSpell) {
								((TargetedEntityFromLocationSpell)spell).castAtEntityFromLocation(player, location, target, power);
							} else if (spell instanceof TargetedEntitySpell) {
								((TargetedEntitySpell)spell).castAtEntity(player, target, power);
							} else if (spell instanceof TargetedLocationSpell) {
								((TargetedLocationSpell)spell).castAtLocation(player, target.getLocation(), power);
							}
						} else {
							if (spell instanceof TargetedEntityFromLocationSpell) {
								((TargetedEntityFromLocationSpell)spell).castAtEntityFromLocation(location, target, power);
							} else if (spell instanceof TargetedEntitySpell) {
								((TargetedEntitySpell)spell).castAtEntity(target, power);
							} else if (spell instanceof TargetedLocationSpell) {
								((TargetedLocationSpell)spell).castAtLocation(target.getLocation(), power);
							}
						}
					}
					playSpellEffects(EffectPosition.TARGET, target);
					if (spellSourceInCenter) {
						playSpellEffectsTrail(location, target.getLocation());
					} else if (player != null) {
						playSpellEffectsTrail(player.getLocation(), target.getLocation());
					}
					count++;
					if (maxTargets > 0 && count >= maxTargets) {
						break;
					}
				}
			}
		}

		if (player != null) {
			playSpellEffects(EffectPosition.CASTER, player);
		}
		playSpellEffects(EffectPosition.SPECIAL, location);
		
		return count > 0;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		return doAoe(caster, target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return doAoe(null, target, power);
	}
	
}
