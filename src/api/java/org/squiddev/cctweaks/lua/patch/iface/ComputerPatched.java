package org.squiddev.cctweaks.lua.patch.iface;

import dan200.computercraft.api.filesystem.IMount;

/**
 * Methods which are patched onto {@link dan200.computercraft.core.computer.Computer}. You can safely cast to this.
 */
public interface ComputerPatched {
	/**
	 * Set a custom mount for this computer
	 *
	 * @param biosPath The custom bios path to use
	 * @param mount    The custom mount to use
	 */
	void setRomMount(String biosPath, IMount mount);
}