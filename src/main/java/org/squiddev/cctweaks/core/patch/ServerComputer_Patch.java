package org.squiddev.cctweaks.core.patch;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.ComputerCraftPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.List;

import org.squiddev.cctweaks.api.IContainerComputer;
import org.squiddev.cctweaks.core.Config;
import org.squiddev.patcher.visitors.MergeVisitor;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;

/**
 * - Adds {@link IComputerEnvironmentExtended} and suspending events on timeout
 * - Adds isMostlyOn for detecting when a computer is on or starting up
 * - Various network changes
 */
@MergeVisitor.Rename(
	from = {"org/squiddev/cctweaks/lua/patch/Computer_Patch", "org/squiddev/cctweaks/lua/patch/ComputerThread_Rewrite"},
	to = {"dan200/computercraft/core/computer/Computer", "dan200/computercraft/core/computer/ComputerThread"}
)
public class ServerComputer_Patch extends ServerComputer {
	@MergeVisitor.Stub
	private int m_ticksSincePing = 0;

	@MergeVisitor.Stub
	private World m_world;

	@MergeVisitor.Stub
	private ChunkCoordinates m_position;

	@MergeVisitor.Stub
	private Computer m_computer;

	@MergeVisitor.Stub
	public ServerComputer_Patch() {
		super(null, -1, null, -1, null, -1, -1);
	}

	public void broadcastState() {
		ComputerCraftPacket packet = new ComputerCraftPacket();
		packet.m_packetType = 7;
		packet.m_dataInt = new int[]{this.getInstanceID()};
		packet.m_dataNBT = new NBTTagCompound();
		writeDescription(packet.m_dataNBT);

		if (Config.Packets.updateLimiting && m_world != null && m_position != null) {
			int distance = MathHelper.clamp_int(MinecraftServer.getServer().getConfigurationManager().getViewDistance(), 3, 32) * 16;

			// Send to players within the render distance
			ComputerCraft.networkEventChannel.sendToAllAround(
				encode(packet),
				new NetworkRegistry.TargetPoint(
					m_world.provider.dimensionId,
					m_position.posX + 0.5,
					m_position.posY + 0.5,
					m_position.posZ + 0.5,
					distance
				)
			);

			// Send to all players outside the range who are using the terminal
			for (EntityPlayerMP player : (List<EntityPlayerMP>)MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
				Container container = player.openContainer;
				if (container instanceof IContainerComputer && ((IContainerComputer) container).getComputer() == this) {
					if (player.worldObj != m_world || player.getDistanceSq(m_position.posX,m_position.posY,m_position.posZ) > distance * distance) {
						ComputerCraft.sendToPlayer(player, packet);
					}
				}
			}
		} else {
			ComputerCraft.sendToAllPlayers(packet);
		}
	}

	private static FMLProxyPacket encode(ComputerCraftPacket packet) {
		PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
		packet.toBytes(buffer);
		return new FMLProxyPacket(buffer, "CC");
	}
}