package org.squiddev.cctweaks.core.turtle;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.permissions.ITurtlePermissionProvider;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import org.squiddev.cctweaks.core.Config;

import cpw.mods.fml.relauncher.ReflectionHelper;

import java.util.List;

/**
 * Various hooks for turtle patches
 */
public class TurtleHooks {
	private static List<ITurtlePermissionProvider> permissionProviders;

	private static List<ITurtlePermissionProvider> getPermissionProviders() {
		if (permissionProviders == null) {
			permissionProviders = ReflectionHelper.getPrivateValue(ComputerCraft.class, null, "permissionProviders");
		}

		return permissionProviders;
	}

	public static boolean isBlockBreakable(World world, int x, int y, int z, EntityPlayer player) {
		if (Config.Turtle.useServerProtected) {
			MinecraftServer server = MinecraftServer.getServer();
			if (server != null && !world.isRemote && server.isBlockProtected(world, x,y,z, player)) {
				return false;
			}
		}

		if (Config.Turtle.useBlockEvent) {
			Block block = world.getBlock(x,y,z);
			int meta = world.getBlockMetadata(x,y,z);
			if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(x,y,z,world, block,meta, player))) {
				return false;
			}
		}

		for (ITurtlePermissionProvider provider : getPermissionProviders()) {
			if (!provider.isBlockEditable(world, x,y,z)) {
				return false;
			}
		}

		return true;
	}
}