package org.squiddev.cctweaks.core.network.modem;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.network.IPacketNetwork;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.modem.ModemPeripheral;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.squiddev.cctweaks.api.network.INetworkController;
import org.squiddev.cctweaks.api.peripheral.IPeripheralTargeted;
import org.squiddev.cctweaks.lua.lib.BinaryConverter;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * A peripheral for modems that provide peripherals
 *
 * @see BasicModem
 */
public class BasicModemPeripheral<T extends BasicModem> extends ModemPeripheral implements IPeripheralTargeted {
	public final T modem;
	private final int methodLength;
	protected boolean changed;

	public BasicModemPeripheral(T modem) {
		this.modem = modem;
		methodLength = super.getMethodNames().length;
	}

	@Nonnull
	@Override
	public World getWorld() {
		return (World) modem.getPosition().getBlockAccess();
	}

	@Nonnull
	@Override
	public Vec3d getPosition() {
		BlockPos position = modem.getPosition().getPosition();
		return new Vec3d(position.getX(), position.getY(), position.getZ());
	}

	@Override
	public double getRange() {
		return 256;
	}

	@Override
	public boolean isInterdimensional() {
		return false;
	}

	@Override
	protected IPacketNetwork getNetwork() {
		return modem;
	}

	@Override
	public boolean equals(IPeripheral other) {
		return other instanceof BasicModemPeripheral && ((BasicModemPeripheral) other).modem.equals(this.modem);
	}

	@Nonnull
	@Override
	public String[] getMethodNames() {
		String[] methods = super.getMethodNames();
		String[] newMethods = new String[methods.length + 5];
		System.arraycopy(methods, 0, newMethods, 0, methods.length);

		int l = methods.length;
		newMethods[l] = "getNamesRemote";
		newMethods[l + 1] = "isPresentRemote";
		newMethods[l + 2] = "getTypeRemote";
		newMethods[l + 3] = "getMethodsRemote";
		newMethods[l + 4] = "callRemote";

		return newMethods;
	}

	@Override
	public Object[] callMethod(@Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments) throws LuaException, InterruptedException {
		switch (method - methodLength) {
			case 0: { // getNamesRemote
				int idx = 1;
				Map<Object, Object> table = new HashMap<Object, Object>();
				for (String name : modem.getAttachedNetwork().getPeripheralsOnNetwork().keySet()) {
					table.put(idx++, name);
				}
				return new Object[]{table};
			}
			case 1: { // isPresentRemote
				PeripheralAccess access = modem.getPeripheral(parseString(arguments, 0));
				return new Object[]{access != null && access.getType() != null};
			}
			case 2: { // getTypeRemote
				PeripheralAccess access = modem.getPeripheral(parseString(arguments, 0));
				String type = null;
				return access == null || (type = access.getType()) != null ? new Object[]{type} : null;
			}
			case 3: {// getMethodsRemote
				PeripheralAccess access = modem.getPeripheral(parseString(arguments, 0));
				String[] names;
				if (access != null && (names = access.getMethodNames()) != null) {
					Map<Object, Object> table = new HashMap<Object, Object>();
					for (int i = 0; i < names.length; i++) {
						table.put(i + 1, names[i]);
					}
					return new Object[]{table};
				}
				return null;
			}
			case 4: {
				String remoteName = parseString(arguments, 0);
				String methodName = parseString(arguments, 1);

				// Trim argument's first two values
				Object[] methodArgs = new Object[arguments.length - 2];
				System.arraycopy(arguments, 2, methodArgs, 0, arguments.length - 2);

				// Get the peripheral and call it
				PeripheralAccess access = modem.getPeripheral(remoteName);
				if (access == null) throw new LuaException("No peripheral: " + remoteName);
				return access.callMethod(context, methodName, methodArgs);
			}
		}

		return super.callMethod(computer, context, method, arguments);
	}

	@Override
	public void attach(@Nonnull IComputerAccess computer) {
		super.attach(computer);

		INetworkController controller = modem.getAttachedNetwork();
		if (controller != null) {
			for (Map.Entry<String, IPeripheral> peripheral : controller.getPeripheralsOnNetwork().entrySet()) {
				modem.attachPeripheral(peripheral.getKey(), peripheral.getValue());
			}
		}
	}

	@Override
	public void detach(@Nonnull IComputerAccess computer) {
		modem.detachPeripherals();
		super.detach(computer);
	}

	private static String parseString(Object[] arguments, int index) throws LuaException {
		if (arguments.length > index) {
			if (arguments[index] instanceof byte[]) {
				return BinaryConverter.decodeString((byte[]) arguments[index]);
			} else if (arguments[index] instanceof String) {
				return (String) arguments[index];
			}
		}

		throw new LuaException("Expected string");

	}

	@Override
	public synchronized boolean pollChanged() {
		boolean changed = super.pollChanged();
		if (this.changed) {
			this.changed = false;
			return true;
		} else {
			return changed;
		}
	}

	@Override
	public Object getTarget() {
		return modem;
	}
}
