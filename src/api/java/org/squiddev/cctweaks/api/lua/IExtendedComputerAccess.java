package org.squiddev.cctweaks.api.lua;

import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.peripheral.IComputerAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * An extended version of {@link IComputerAccess}, providing some additional functionality.
 *
 * You should always check before casting to this, as peripheral methods may not provide this.
 */
public interface IExtendedComputerAccess extends IComputerAccess {
	/**
	 * Get the path the computer saves its files to.
	 *
	 * If this value is not {@code null}, it will either be a directory or a file which does not exist. You should run
	 * {@link File#mkdirs()} should you need to access a directory.
	 *
	 * @return The computer's path, or {@code null} if it cannot be determined.
	 */
	@Nullable
	File getRootMountPath();

	/**
	 * Get the mount for this computer.
	 *
	 * @return The computer's mount.
	 */
	@Nonnull
	IWritableMount getRootMount();
}