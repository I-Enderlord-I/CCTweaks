package org.squiddev.cctweaks.core.patch;

import dan200.computercraft.shared.turtle.blocks.ITurtleTile;
import dan200.computercraft.shared.turtle.items.TurtleItemFactory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.squiddev.patcher.visitors.MergeVisitor;

/**
 * Copy across rom_id from the computer tile to the item
 */
@MergeVisitor.Rename(
	from = "org/squiddev/cctweaks/core/patch/TileComputerBase_Patch",
	to = "dan200/computercraft/shared/computer/blocks/TileComputerBase"
)
public class TurtleItemFactory_Patch extends TurtleItemFactory {
	public static ItemStack create(ITurtleTile turtle) {
		ItemStack stack = native_create(turtle);
		if (stack != null && turtle instanceof TileComputerBase_Patch) {
			TileComputerBase_Patch compTile = (TileComputerBase_Patch) turtle;
			if (compTile.hasDisk) {
				NBTTagCompound tag = stack.getTagCompound();
				if (tag == null) stack.setTagCompound(tag = new NBTTagCompound());

				tag.setInteger("rom_id", compTile.diskId);
			}
			if (compTile.hasColor) {
				NBTTagCompound tag = stack.getTagCompound();
				if (tag == null) stack.setTagCompound(tag = new NBTTagCompound());

				tag.setInteger("original_color", compTile.originalColor);
			}
		}

		return stack;
	}

	@MergeVisitor.Rename(from = "create")
	@MergeVisitor.Stub
	public static ItemStack native_create(ITurtleTile turtle) {
		return TurtleItemFactory.create(turtle);
	}
}
