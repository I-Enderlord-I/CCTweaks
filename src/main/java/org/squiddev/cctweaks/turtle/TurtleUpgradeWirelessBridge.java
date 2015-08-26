package org.squiddev.cctweaks.turtle;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.shared.util.PeripheralUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.api.IDataCard;
import org.squiddev.cctweaks.api.network.INetworkCompatiblePeripheral;
import org.squiddev.cctweaks.api.network.IWorldNetworkNode;
import org.squiddev.cctweaks.api.network.IWorldNetworkNodeHost;
import org.squiddev.cctweaks.blocks.network.BlockNetworked;
import org.squiddev.cctweaks.blocks.network.TileNetworkedWirelessBridge;
import org.squiddev.cctweaks.core.Config;
import org.squiddev.cctweaks.core.network.bridge.NetworkBindingWithModem;
import org.squiddev.cctweaks.core.network.modem.BasicModemPeripheral;
import org.squiddev.cctweaks.core.network.modem.PeripheralCollection;
import org.squiddev.cctweaks.core.peripheral.PeripheralProxy;
import org.squiddev.cctweaks.core.registry.Module;
import org.squiddev.cctweaks.core.registry.Registry;

import java.util.Map;

/**
 * Turtle upgrade for the {@link TileNetworkedWirelessBridge} tile
 */
public class TurtleUpgradeWirelessBridge extends Module implements ITurtleUpgrade {
	@Override
	public int getUpgradeID() {
		return Config.Network.WirelessBridge.turtleId;
	}

	@Override
	public String getUnlocalisedAdjective() {
		return "turtle." + CCTweaks.RESOURCE_DOMAIN + ".wirelessBridge.adjective";
	}

	@Override
	public TurtleUpgradeType getType() {
		return TurtleUpgradeType.Peripheral;
	}

	@Override
	public ItemStack getCraftingItem() {
		return Config.Network.WirelessBridge.turtleEnabled ? new ItemStack(Registry.blockNetworked, 0) : null;
	}

	@Override
	public IPeripheral createPeripheral(ITurtleAccess turtle, TurtleSide side) {
		return Config.Network.WirelessBridge.turtleEnabled ? new TurtleBinding(turtle, side).getModem().modem : null;
	}

	@Override
	public TurtleCommandResult useTool(ITurtleAccess turtle, TurtleSide side, TurtleVerb verb, int direction) {
		return null;
	}

	@Override
	public IIcon getIcon(ITurtleAccess turtle, TurtleSide side) {
		return BlockNetworked.bridgeSmallIcon;
	}

	/**
	 * Called on turtle update. Used to check if messages should be sent
	 *
	 * @param turtle Current turtle
	 * @param side   Peripheral side
	 */
	@Override
	public void update(ITurtleAccess turtle, TurtleSide side) {
	}

	@Override
	public void init() {
		ComputerCraft.registerTurtleUpgrade(this);
	}

	public static class TurtleBinding extends NetworkBindingWithModem {
		public final ITurtleAccess turtle;
		public final TurtleSide side;

		public TurtleBinding(ITurtleAccess turtle, TurtleSide side) {
			super(new TurtlePosition(turtle));
			this.turtle = turtle;
			this.side = side;
		}

		@Override
		public BindingModem createModem() {
			return new TurtleModem();
		}

		@Override
		public TurtleModem getModem() {
			return (TurtleModem) modem;
		}

		@Override
		public void connect() {
			load(turtle.getUpgradeNBTData(side));
			getModem().load();

			super.connect();
		}

		public void save() {
			save(turtle.getUpgradeNBTData(side));
			getModem().save();
			turtle.updateUpgradeNBTData(side);
		}

		/**
		 * Custom modem that allows modifying bindings
		 */
		public class TurtleModem extends BindingModem {
			protected PeripheralCollection peripherals = new PeripheralCollection(2) {
				private IPeripheral peripheral = new PeripheralProxy("turtle") {
					@Override
					protected IPeripheral createPeripheral() {
						ChunkCoordinates pos = turtle.getPosition();
						return PeripheralUtil.getPeripheral(turtle.getWorld(), pos.posX, pos.posY, pos.posZ, 0);
					}
				};

				@Override
				protected IPeripheral[] getPeripherals() {
					IPeripheral[] peripherals = new IPeripheral[2];
					peripherals[0] = peripheral;

					IPeripheral opposite = turtle.getPeripheral(side == TurtleSide.Left ? TurtleSide.Right : TurtleSide.Left);
					if (opposite instanceof INetworkCompatiblePeripheral) peripherals[1] = opposite;

					return peripherals;
				}

				@Override
				protected World getWorld() {
					return turtle.getWorld();
				}

				@Override
				protected void changed() {
					super.changed();
					TurtleBinding.this.save();
				}
			};

			public void load() {
				NBTTagCompound data = turtle.getUpgradeNBTData(side);

				// Backwards compatibility
				if (data.hasKey("turtle_id")) {
					peripherals.ids[0] = data.getInteger("turtle_id");
					data.removeTag("turtle_id");
				}

				int[] ids = data.getIntArray("peripheral_ids");
				if (ids != null && ids.length == 6) System.arraycopy(ids, 0, peripherals.ids, 0, 6);
			}

			public void save() {
				NBTTagCompound tag = turtle.getUpgradeNBTData(side);
				tag.setIntArray("peripheral_ids", peripherals.ids);
			}

			@Override
			public Map<String, IPeripheral> getConnectedPeripherals() {
				return peripherals.getConnectedPeripherals();
			}

			@Override
			protected BasicModemPeripheral createPeripheral() {
				return new TurtleModemPeripheral(this);
			}

			@Override
			public boolean canConnect(ForgeDirection side) {
				return side == ForgeDirection.UNKNOWN;
			}
		}

		/**
		 * Extension of modem with bindToCard and bindFromCard methods
		 *
		 * Also calls {@link TurtleBinding#connect()} and {@link TurtleBinding#destroy()} on attach and detach.
		 */
		public class TurtleModemPeripheral extends BindingModemPeripheral implements IWorldNetworkNodeHost {
			public TurtleModemPeripheral(BindingModem modem) {
				super(modem);
			}

			@Override
			public String[] getMethodNames() {
				String[] methods = super.getMethodNames();
				String[] newMethods = new String[methods.length + 2];
				System.arraycopy(methods, 0, newMethods, 0, methods.length);


				int l = methods.length;
				newMethods[l] = "bindFromCard";
				newMethods[l + 1] = "bindToCard";

				return newMethods;
			}

			@Override
			public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws LuaException, InterruptedException {
				String[] methods = super.getMethodNames();
				switch (method - methods.length) {
					case 0: { // bindFromCard
						ItemStack stack = turtle.getInventory().getStackInSlot(turtle.getSelectedSlot());
						if (stack != null && stack.getItem() instanceof IDataCard) {
							IDataCard card = (IDataCard) stack.getItem();
							if (TurtleBinding.this.load(stack, card)) {
								TurtleBinding.this.save();
								return new Object[]{true};
							}
						}
						return new Object[]{false};
					}
					case 1: { // bindToCard
						ItemStack stack = turtle.getInventory().getStackInSlot(turtle.getSelectedSlot());
						if (stack != null && stack.getItem() instanceof IDataCard) {
							IDataCard card = (IDataCard) stack.getItem();
							TurtleBinding.this.save(stack, card);
							return new Object[]{true};
						}
						return new Object[]{false};
					}
				}

				return super.callMethod(computer, context, method, arguments);
			}

			@Override
			public synchronized void attach(IComputerAccess computer) {
				TurtleBinding.this.connect();
				super.attach(computer);
			}

			@Override
			public synchronized void detach(IComputerAccess computer) {
				super.detach(computer);
				TurtleBinding.this.destroy();
			}

			@Override
			public IWorldNetworkNode getNode() {
				return TurtleBinding.this;
			}
		}
	}
}
