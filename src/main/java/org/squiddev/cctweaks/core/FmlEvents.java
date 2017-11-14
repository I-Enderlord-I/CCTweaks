package org.squiddev.cctweaks.core;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dan200.computercraft.shared.computer.core.IComputer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.media.items.ItemDiskLegacy;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.oredict.RecipeSorter;

import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.api.IContainerComputer;
import org.squiddev.cctweaks.api.computer.ICustomRomItem;
import org.squiddev.cctweaks.core.rom.CraftingSetRom;
import org.squiddev.cctweaks.lua.lib.DelayedTasks;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;

/**
 * This handles various events
 */
public final class FmlEvents {
	private static FmlEvents instance;

	public FmlEvents() {
		if (instance == null) {
			instance = this;
		} else {
			throw new IllegalStateException("Events already exists");
		}
	}

	private final Queue<Runnable> serverQueue = new LinkedList<Runnable>();
	private final Queue<Runnable> clientQueue = new LinkedList<Runnable>();
	private static final Map<EntityPlayerMP, Integer> oldContainer = new WeakHashMap<EntityPlayerMP, Integer>();

	public static void schedule(Runnable runnable) {
		synchronized (instance.serverQueue) {
			instance.serverQueue.add(runnable);
		}
	}

	public static void scheduleClient(Runnable runnable) {
		synchronized (instance.clientQueue) {
			instance.clientQueue.add(runnable);
		}
	}

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			DelayedTasks.update();
			synchronized (serverQueue) {
				Runnable scheduled;
				while ((scheduled = serverQueue.poll()) != null) {
					scheduled.run();
				}
			}
		} else if (event.phase == TickEvent.Phase.END) {
			// Track container changes and send it when a new container is opened.

			for (EntityPlayerMP player : (List<EntityPlayerMP>)MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
				Container container = player.openContainer;
				if (container == null) {
					oldContainer.remove(player);
				} else {
					Integer oldIndexObj = oldContainer.get(player);

					if (oldIndexObj == null || oldIndexObj != container.windowId) {
						oldContainer.put(player, container.windowId);
						if (container instanceof IContainerComputer) {
							IComputer computer = ((IContainerComputer) container).getComputer();
							if (computer instanceof ServerComputer) {
								((ServerComputer) computer).sendState(player);
							}
						}
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			synchronized (clientQueue) {
				Runnable scheduled;
				while ((scheduled = clientQueue.poll()) != null) {
					scheduled.run();
				}
			}
		}
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent eventArgs) {
		if (eventArgs.modID.equals(CCTweaks.ID)) {
			Config.sync();
		}
	}
	
	@SubscribeEvent
	public void onItemCrafted(ItemCraftedEvent event){
		if (!(event.craftMatrix instanceof InventoryCrafting) || !CraftingSetRom.INSTANCE.matches((InventoryCrafting)event.craftMatrix, event.player.worldObj))
			return;
		
		ItemStack romStack = null;
		ItemStack diskStack = null;
		int slot = 0;

		for (int i = 0; i < event.craftMatrix.getSizeInventory(); i++) {
			ItemStack stack = event.craftMatrix.getStackInSlot(i);
			if (stack == null) continue;
			Item item = stack.getItem();
			if (item instanceof ItemDiskLegacy) {
				diskStack = stack;
			} else if (item instanceof ICustomRomItem) {
				romStack = stack;
				slot = i;
			}
		}
		
		if(((ICustomRomItem) romStack.getItem()).hasCustomRom(romStack)){
			event.craftMatrix.setInventorySlotContents(slot, CraftingSetRom.createComputerContainer(romStack, diskStack));
		}
	}
}
