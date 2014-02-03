package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class TeleportSpell extends TargetedSpell implements TargetedEntitySpell {

	public TeleportSpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity target = getTargetedEntity(player, power);
			if (target == null) {
				return noTarget(player);
			}
			boolean ok = castAtEntity(player, target, power);
			if (!ok) {
				return noTarget(player);
			}
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		Location casterLoc = caster.getLocation();
		boolean ok = caster.teleport(target);
		if (ok) {
			playSpellEffects(EffectPosition.CASTER, casterLoc);
			playSpellEffects(EffectPosition.TARGET, target.getLocation());
		}
		return ok;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}