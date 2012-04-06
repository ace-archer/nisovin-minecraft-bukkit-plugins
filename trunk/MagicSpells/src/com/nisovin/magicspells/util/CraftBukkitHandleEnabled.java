package com.nisovin.magicspells.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.DataWatcher;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EntityTNTPrimed;
import net.minecraft.server.MobEffect;
import net.minecraft.server.Packet103SetSlot;
import net.minecraft.server.Packet22Collect;
import net.minecraft.server.Packet40EntityMetadata;
import net.minecraft.server.Packet42RemoveMobEffect;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftTNTPrimed;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.MagicSpells;

public class CraftBukkitHandleEnabled implements CraftBukkitHandle {

	@Override
	public void playPotionEffect(final Player player, final Entity entity, int color, int duration) {
		final DataWatcher dw = new DataWatcher();
		dw.a(8, Integer.valueOf(0));
		dw.watch(8, Integer.valueOf(color));
		
		Packet40EntityMetadata packet = new Packet40EntityMetadata(entity.getEntityId(), dw);
		((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
			public void run() {
				DataWatcher dwReal = ((CraftLivingEntity)entity).getHandle().getDataWatcher();
				dw.watch(8, dwReal.getInt(8));
				Packet40EntityMetadata packet = new Packet40EntityMetadata(entity.getEntityId(), dw);
				((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
			}
		}, duration);
	}

	@Override
	public void addPotionGraphicalEffect(LivingEntity entity, int color, int duration) {
		final EntityLiving el = ((CraftLivingEntity)entity).getHandle();
		final DataWatcher dw = el.getDataWatcher();
		dw.watch(8, Integer.valueOf(color));
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
			public void run() {
				int c = 0;
				if (!el.effects.isEmpty()) {
					c = net.minecraft.server.PotionBrewer.a(el.effects.values());
				}
				dw.watch(8, Integer.valueOf(c));
			}
		}, duration);
	}

	@Override
	public void entityPathTo(LivingEntity creature, LivingEntity target) {
		//EntityCreature entity = ((CraftCreature)creature).getHandle();
		//entity.pathEntity = entity.world.findPath(entity, ((CraftLivingEntity)target).getHandle(), 16.0F);
	}

	@Override
	public void makeMinion(LivingEntity entity, Player master) {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void queueChunksForUpdate(Player player, Set<Chunk> chunks) {
		for (Chunk chunk : chunks) {
			ChunkCoordIntPair intPair = new ChunkCoordIntPair(chunk.getX(), chunk.getZ());
			((CraftPlayer)player).getHandle().chunkCoordIntPairQueue.add(intPair);
		}
	}

	@Override
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {
		net.minecraft.server.ItemStack nmsItem = null;
		if (item != null) {
			nmsItem = new net.minecraft.server.ItemStack(item.getTypeId(), slot+1, item.getDurability());
		} else {
			nmsItem = null;
		}
		Packet103SetSlot packet = new Packet103SetSlot(0, (short)slot+36, nmsItem);
		((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
	}

	@Override
	public void stackByData(int itemId, String var) {
		try {
			boolean ok = false;
			try {
				// attempt to make books with different data values stack separately
				Method method = net.minecraft.server.Item.class.getDeclaredMethod(var, boolean.class);
				if (method.getReturnType() == net.minecraft.server.Item.class) {
					method.setAccessible(true);
					method.invoke(net.minecraft.server.Item.byId[itemId], true);
					ok = true;
				}
			} catch (Exception e) {
			}
			if (!ok) {
				// otherwise limit stack size to 1
				Field field = net.minecraft.server.Item.class.getDeclaredField("maxStackSize");
				field.setAccessible(true);
				field.setInt(net.minecraft.server.Item.byId[itemId], 1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void toggleLeverOrButton(Block block) {
		net.minecraft.server.Block.byId[block.getType().getId()].interact(((CraftWorld)block.getWorld()).getHandle(), block.getX(), block.getY(), block.getZ(), null);
	}

	@Override
	public void pressPressurePlate(Block block) {
		block.setData((byte) (block.getData() ^ 0x1));
		net.minecraft.server.World w = ((CraftWorld)block.getWorld()).getHandle();
		w.applyPhysics(block.getX(), block.getY(), block.getZ(), block.getType().getId());
		w.applyPhysics(block.getX(), block.getY()-1, block.getZ(), block.getType().getId());
	}

	@Override
	public void removeMobEffect(LivingEntity entity, PotionEffectType type) {
        try {
            // remove effect
    		entity.removePotionEffect(type);
            // alert player that effect is gone
            if (entity instanceof Player) {
                    EntityPlayer player = ((CraftPlayer)entity).getHandle();
                    player.netServerHandler.sendPacket(new Packet42RemoveMobEffect(player.id, new MobEffect(type.getId(), 0, 0)));
            }
            // remove graphical effect
            ((CraftLivingEntity)entity).getHandle().getDataWatcher().watch(8, Integer.valueOf(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	@Override
	public void collectItem(Player player, Item item) {
		Packet22Collect packet = new Packet22Collect(item.getEntityId(), player.getEntityId());
		((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
	}

	@Override
	public boolean simulateTnt(Location target, float explosionSize) {
        EntityTNTPrimed e = new EntityTNTPrimed(((CraftWorld)target.getWorld()).getHandle(), target.getX(), target.getY(), target.getZ());
        CraftTNTPrimed c = new CraftTNTPrimed((CraftServer)Bukkit.getServer(), e);
        ExplosionPrimeEvent event = new ExplosionPrimeEvent(c, explosionSize, false);
        Bukkit.getServer().getPluginManager().callEvent(event);
        return event.isCancelled();
	}

	@Override
	public boolean createExplosionByPlayer(Player player, Location location, float size) {
		return !((CraftWorld)location.getWorld()).getHandle().createExplosion(((CraftPlayer)player).getHandle(), location.getX(), location.getY(), location.getZ(), size, false).wasCanceled;
	}
	
	/*private class PathfinderGoalUseOwnerTarget extends PathfinderGoalTarget {
		
		EntityHuman master;
		EntityLiving target;
		
		public PathfinderGoalUseOwnerTarget(EntityLiving creature, EntityHuman master) {
			super(creature, 32.0F, false);
			this.master = master;
		}

		@Override
		public boolean a() {
			this.target = master.ao();
			return this.a(target, false);
		}
		
		@Override
		public void c() {
			this.c.b(target);
			super.c();
		}
		
	}
	
	private class PathfinderGoalFollowMaster extends PathfinderGoal {
		
		LivingEntity creature;
		Player master;
		
		float speed = 0.3F;
		float distanceFollow = 10F;
		float distanceCaughtUpSquared = 2F * 2F;
		
		public PathfinderGoalFollowMaster(LivingEntity creature, Player master) {
			this.creature = creature;
			this.master = master;
			this.a(3);
		}
		
		@Override
		public boolean a() { // apply goal check ?
			if (creature.getLocation().distanceSquared(master.getLocation()) < distanceFollow*distanceFollow) {
				return false;
			} else {
				return true;
			}
		}
		
		@Override
		public boolean b() { // terminate goal processing ?
			if (creature.getLocation().distanceSquared(master.getLocation()) < distanceCaughtUpSquared
		}
		
	}*/

}