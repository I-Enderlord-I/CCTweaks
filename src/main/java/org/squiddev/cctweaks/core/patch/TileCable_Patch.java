package org.squiddev.cctweaks.core.patch;

import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.BlockCable;
import dan200.computercraft.shared.peripheral.modem.IReceiver;
import dan200.computercraft.shared.peripheral.modem.TileCable;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Facing;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import org.squiddev.cctweaks.api.network.INetworkNode;
import org.squiddev.cctweaks.api.network.NetworkHelpers;
import org.squiddev.cctweaks.api.network.NetworkVisitor;
import org.squiddev.cctweaks.api.network.Packet;
import org.squiddev.cctweaks.core.asm.patch.MergeVisitor;

import java.util.*;

import static org.squiddev.cctweaks.api.network.NetworkHelpers.canConnect;

@SuppressWarnings("all")
@MergeVisitor.Rename(from = "dan200/computercraft/shared/peripheral/modem/TileCable$Packet", to = "org/squiddev/cctweaks/api/network/Packet")
public class TileCable_Patch extends TileCable implements INetworkNode {
	public static final double MIN = 0.375;
	public static final double MAX = 1 - MIN;

	@MergeVisitor.Stub
	private static IIcon[] s_cableIcons;
	@MergeVisitor.Stub
	private Map<Integer, Set<IReceiver>> m_receivers;
	@MergeVisitor.Stub
	private Map<String, IPeripheral> m_peripheralsByName;
	@MergeVisitor.Stub
	private Map<String, RemotePeripheralWrapper> m_peripheralWrappersByName;
	@MergeVisitor.Stub
	private boolean m_peripheralsKnown;
	@MergeVisitor.Stub
	private boolean m_destroyed;
	@MergeVisitor.Stub
	private Queue<Packet> m_transmitQueue;

	@Override
	public void addReceiver(IReceiver receiver) {
		synchronized (m_receivers) {
			int channel = receiver.getChannel();
			Set<IReceiver> receivers = m_receivers.get(channel);
			if (receivers == null) {
				receivers = new HashSet<IReceiver>();
				m_receivers.put(channel, receivers);
			}
			receivers.add(receiver);
		}
	}

	@Override
	public void removeReceiver(IReceiver receiver) {
		synchronized (m_receivers) {
			int channel = receiver.getChannel();
			Set<IReceiver> receivers = m_receivers.get(channel);
			if (receivers != null) {
				receivers.remove(receiver);
			}
		}
	}

	@Override
	public void transmit(int channel, int replyChannel, Object payload, double range, double xPos, double yPos, double zPos, Object senderObject) {
		synchronized (m_transmitQueue) {
			m_transmitQueue.offer(new Packet(channel, replyChannel, payload, senderObject));
		}
	}

	private void attachPeripheral(String name, IPeripheral peripheral) {
		if (!m_peripheralWrappersByName.containsKey(name)) {
			RemotePeripheralWrapper wrapper = new RemotePeripheralWrapper(peripheral, m_modem.getComputer(), name);
			m_peripheralWrappersByName.put(name, wrapper);
			wrapper.attach();
		}
	}

	private void detachPeripheral(String name) {
		if (m_peripheralWrappersByName.containsKey(name)) {
			RemotePeripheralWrapper wrapper = m_peripheralWrappersByName.get(name);
			m_peripheralWrappersByName.remove(name);
			wrapper.detach();
		}
	}

	@Override
	public void networkChanged() {
		if (!worldObj.isRemote) {
			if (m_destroyed) {
				NetworkHelpers.fireNetworkInvalidateAdjacent(worldObj, xCoord, yCoord, zCoord);
			} else {
				NetworkHelpers.fireNetworkInvalidate(worldObj, xCoord, yCoord, zCoord);
			}
		}
	}

	@Override
	public Iterable<NetworkVisitor.SearchLoc> getExtraNodes() {
		return null;
	}

	private void dispatchPacket(Packet packet) {
		NetworkHelpers.sendPacket(worldObj, xCoord, yCoord, zCoord, packet);
	}

	@Override
	public boolean canBeVisited(ForgeDirection from) {
		// Can't be visited by other nodes if it is destroyed or has no cable
		return !m_destroyed && getPeripheralType() != PeripheralType.WiredModem;
	}

	@Override
	public boolean canVisitTo(ForgeDirection to) {
		return !m_destroyed && getPeripheralType() != PeripheralType.WiredModem;
	}

	@Override
	public Map<String, IPeripheral> getConnectedPeripherals() {
		String name = getConnectedPeripheralName();
		IPeripheral peripheral = getConnectedPeripheral();
		if (name != null && peripheral != null) {
			return Collections.singletonMap(name, peripheral);
		}
		return null;
	}

	@MergeVisitor.Stub
	public IPeripheral getConnectedPeripheral() {
		return null;
	}

	@Override
	public void receivePacket(Packet packet, int distanceTravelled) {
		synchronized (m_receivers) {
			Set<IReceiver> receivers = m_receivers.get(packet.channel);
			if (receivers != null) {
				for (IReceiver receiver : receivers) {
					receiver.receive(packet.replyChannel, packet.payload, distanceTravelled, packet.senderObject);
				}
			}
		}
	}

	@Override
	public void networkInvalidated() {
		m_peripheralsKnown = false;
	}

	@Override
	public Object lock() {
		return m_peripheralsByName;
	}

	private void findPeripherals() {
		// TEs are not replaced on Multipart crashes
		if (getBlock() == null) {
			worldObj.removeTileEntity(xCoord, yCoord, zCoord);
			return;
		}

		final TileCable_Patch origin = this;
		synchronized (m_peripheralsByName) {
			final Map<String, IPeripheral> newPeripheralsByName = new HashMap<String, IPeripheral>();
			if (getPeripheralType() == PeripheralType.WiredModemWithCable) {
				new NetworkVisitor() {
					@MergeVisitor.Rewrite
					boolean ANNOTATION;

					public void visitNode(INetworkNode node, int distance) {
						if (node != origin) {
							Map<String, IPeripheral> peripherals = node.getConnectedPeripherals();
							if (peripherals != null) newPeripheralsByName.putAll(peripherals);
						}
					}
				}.visitNetwork(this);
			}

			Iterator it = m_peripheralsByName.keySet().iterator();
			while (it.hasNext()) {
				String periphName = (String) it.next();
				if (!newPeripheralsByName.containsKey(periphName)) {
					it.remove();
					detachPeripheral(periphName);
				}

			}

			for (String periphName : newPeripheralsByName.keySet()) {
				if (!m_peripheralsByName.containsKey(periphName)) {
					IPeripheral peripheral = newPeripheralsByName.get(periphName);
					if (peripheral != null) {
						m_peripheralsByName.put(periphName, peripheral);
						if (isAttached()) attachPeripheral(periphName, peripheral);
					}
				}
			}
		}
	}

	@Override
	public AxisAlignedBB getCableBounds() {
		int x = xCoord, y = yCoord, z = zCoord;
		IBlockAccess world = worldObj;

		return AxisAlignedBB.getBoundingBox(
			canConnect(world, x, y, z, ForgeDirection.WEST) ? 0 : MIN,
			canConnect(world, x, y, z, ForgeDirection.DOWN) ? 0 : MIN,
			canConnect(world, x, y, z, ForgeDirection.NORTH) ? 0 : MIN,
			canConnect(world, x, y, z, ForgeDirection.EAST) ? 1 : MAX,
			canConnect(world, x, y, z, ForgeDirection.UP) ? 1 : MAX,
			canConnect(world, x, y, z, ForgeDirection.SOUTH) ? 1 : MAX
		);
	}

	@Override
	public IIcon getTexture(int side) {
		PeripheralType type = getPeripheralType();
		if (BlockCable.renderAsModem) type = PeripheralType.WiredModem;

		switch (type) {
			case Cable:
			case WiredModemWithCable:
				int dir = -1;
				if (type == PeripheralType.WiredModemWithCable) {
					dir = getDirection();
					dir -= dir % 2;
				}

				int x = xCoord, y = yCoord, z = zCoord;
				IBlockAccess world = worldObj;

				if (canConnect(world, x, y, z, ForgeDirection.EAST) || canConnect(world, x, y, z, ForgeDirection.WEST)) {
					dir = dir == -1 || dir == 4 ? 4 : -2;
				}
				if (canConnect(world, x, y, z, ForgeDirection.UP) || canConnect(world, x, y, z, ForgeDirection.DOWN)) {
					dir = dir == -1 || dir == 0 ? dir = 0 : -2;
				}
				if (canConnect(world, x, y, z, ForgeDirection.NORTH) || canConnect(world, x, y, z, ForgeDirection.SOUTH)) {
					dir = dir == -1 || dir == 2 ? 2 : -2;
				}

				if (dir == -1) dir = 2;

				if (dir >= 0 && (side == dir || side == Facing.oppositeSide[dir])) return s_cableIcons[1];
				return s_cableIcons[0];
		}

		return super.getTexture(side);
	}

	@MergeVisitor.Stub
	private static class RemotePeripheralWrapper {
		public RemotePeripheralWrapper(IPeripheral peripheral, IComputerAccess computer, String name) {
		}

		public void attach() {
		}

		public void detach() {
		}
	}
}