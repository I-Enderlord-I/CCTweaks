package org.squiddev.cctweaks.core.network;

import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.Collections;
import java.util.Map;

/**
 * A modem that only has one peripheral
 */
public abstract class SinglePeripheralModem extends BasicModem {
	public int id = -1;

	@Override
	public Map<String, IPeripheral> getConnectedPeripherals() {
		if (!peripheralEnabled) return null;

		IPeripheral peripheral = getPeripheral();

		if (peripheral == null) return null;
		return Collections.singletonMap(peripheral.getType() + "_" + id, peripheral);
	}

	public String getPeripheralName() {
		if (!peripheralEnabled) return null;

		IPeripheral peripheral = getPeripheral();
		if (peripheral == null) return null;
		return peripheral.getType() + "_" + id;
	}

	public abstract IPeripheral getPeripheral();
}