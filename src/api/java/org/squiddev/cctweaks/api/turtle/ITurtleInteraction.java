package org.squiddev.cctweaks.api.turtle;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Add custom interactions to the turtle tool manipulator
 *
 * @see ITurtleRegistry#registerInteraction(ITurtleInteraction)
 * @see ITurtleRegistry#registerInteraction(Item, ITurtleInteraction)
 */
public interface ITurtleInteraction {
	/**
	 * Swing (left click) a tool
	 *
	 * The code is already being run in the main thread, and so you cannot yield.
	 *
	 * @param turtle    The turtle swinging the tool
	 * @param computer  The current computer
	 * @param player    The turtle player,
	 * @param stack     The item to be swung
	 * @param direction The direction to swing in
	 * @param hit       Place the item was used at. May be {@code null}.
	 * @return Result of interaction, or {@code null} if no action could be taken
	 */
	TurtleCommandResult swing(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player, ItemStack stack, ForgeDirection direction, MovingObjectPosition hit) throws LuaException;

	/**
	 * Use (right click) a tool
	 *
	 * The code is already being run in the main thread, and so you cannot yield.
	 *
	 * @param turtle    The turtle swinging the tool
	 * @param computer  The current computer
	 * @param player    The turtle player
	 * @param stack     The item to be swung
	 * @param direction The direction to swing in
	 * @param hit       Place the item was used at. May be {@code null}.
	 * @return Result of interaction, or {@code null} if no action could be taken
	 */
	TurtleCommandResult use(ITurtleAccess turtle, IComputerAccess computer, FakePlayer player, ItemStack stack, ForgeDirection direction, MovingObjectPosition hit) throws LuaException;
}