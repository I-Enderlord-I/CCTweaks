package org.squiddev.cctweaks.core.patch;

import java.util.List;

import org.squiddev.cctweaks.api.IContainerComputer;
import org.squiddev.cctweaks.core.Config;
import org.squiddev.patcher.visitors.MergeVisitor;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.ComputerCraftPacket;
import dan200.computercraft.shared.util.NBTUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * - Adds {@link IComputerEnvironmentExtended} and suspending events on timeout
 * - Adds isMostlyOn for detecting when a computer is on or starting up -
 * Various network changes
 */
@MergeVisitor.Rename(from = { "org/squiddev/cctweaks/lua/patch/Computer_Patch",
		"org/squiddev/cctweaks/lua/patch/ComputerThread_Rewrite",
		"org/squiddev/cctweaks/core/patch/Terminal_Patch", }, to = { "dan200/computercraft/core/computer/Computer",
				"dan200/computercraft/core/computer/ComputerThread", "dan200/computercraft/core/terminal/Terminal", })
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
	private NBTTagCompound m_userData;

	@MergeVisitor.Stub
	public ServerComputer_Patch() {
		super(null, -1, null, -1, null, -1, -1);
	}

	@Override
	public void handlePacket(ComputerCraftPacket packet, EntityPlayer sender) {
		switch (packet.m_packetType) {
		case ComputerCraftPacket.TurnOn:
			turnOn();
			break;
		case ComputerCraftPacket.Reboot:
			reboot();
			break;
		case ComputerCraftPacket.Shutdown:
			shutdown();
			break;
		case ComputerCraftPacket.QueueEvent: {
			String event = packet.m_dataString[0];
			Object[] arguments = null;
			if (packet.m_dataNBT != null)
				arguments = NBTUtil.decodeObjects(packet.m_dataNBT);
			queueEvent(event, arguments);
			break;
		}
		case ComputerCraftPacket.RequestComputerUpdate:
			sendState(sender, false);
			break;
		case ComputerCraftPacket.SetLabel: {
			String label = packet.m_dataString != null && packet.m_dataString.length >= 1 ? packet.m_dataString[0]
					: null;
			setLabel(label);
			break;
		}
		}
	}

	public void broadcastState() {
		ComputerCraftPacket packet = new ComputerCraftPacket();
		packet.m_packetType = ComputerCraftPacket.ComputerChanged;
		packet.m_dataInt = new int[] { this.getInstanceID() };
		packet.m_dataNBT = new NBTTagCompound();
		writeDescription(packet.m_dataNBT);

		if (Config.Packets.updateLimiting && m_world != null && m_position != null) {
			int distance = MathHelper.clamp_int(MinecraftServer.getServer().getConfigurationManager().getViewDistance(),
					3, 32) * 16;

			// Send to players within the render distance
			ComputerCraft.networkEventChannel.sendToAllAround(encode(packet),
					new NetworkRegistry.TargetPoint(m_world.provider.dimensionId, m_position.posX + 0.5,
							m_position.posY + 0.5, m_position.posZ + 0.5, distance));

			// Send to all players outside the range who are using the terminal
			for (EntityPlayerMP player : (List<EntityPlayerMP>) MinecraftServer.getServer()
					.getConfigurationManager().playerEntityList) {
				Container container = player.openContainer;
				if (container instanceof IContainerComputer && ((IContainerComputer) container).getComputer() == this) {
					ComputerCraft.sendToPlayer(player, packet);
				}
			}
		} else {
			ComputerCraft.sendToAllPlayers(packet);
		}
	}

	public void sendState(EntityPlayer player) {
		sendState(player, true);
	}

	private void sendState(EntityPlayer player, boolean withTerminal) {
		ComputerCraftPacket packet = createStatePacket();
		writeDescription(packet.m_dataNBT, withTerminal || !Config.Packets.terminalLimiting);
		ComputerCraft.sendToPlayer(player, packet);
	}

	private ComputerCraftPacket createStatePacket() {
		ComputerCraftPacket packet = new ComputerCraftPacket();
		packet.m_packetType = ComputerCraftPacket.ComputerChanged;
		packet.m_dataInt = new int[] { getInstanceID() };
		packet.m_dataNBT = new NBTTagCompound();
		return packet;
	}

	public void writeDescription(NBTTagCompound tag, boolean withTerminal) {
		tag.setBoolean("colour", isColour());
		Terminal terminal = getTerminal();
		if (terminal != null) {
			NBTTagCompound termTag = new NBTTagCompound();
			termTag.setInteger("term_width", terminal.getWidth());
			termTag.setInteger("term_height", terminal.getHeight());
			((Terminal_Patch) terminal).writeToNBT(termTag, withTerminal);
			tag.setTag("terminal", termTag);
		}

		tag.setInteger("id", m_computer.getID());
		String label = m_computer.getLabel();
		if (label != null)
			tag.setString("label", label);

		tag.setBoolean("on", m_computer.isOn());
		tag.setBoolean("blinking", m_computer.isBlinking());
		if (m_userData != null)
			tag.setTag("userData", m_userData.copy());
	}

	private static FMLProxyPacket encode(ComputerCraftPacket packet) {
		PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
		packet.toBytes(buffer);
		return new FMLProxyPacket(buffer, "CC");
	}
}