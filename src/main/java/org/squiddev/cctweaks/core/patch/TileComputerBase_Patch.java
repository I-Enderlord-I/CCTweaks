package org.squiddev.cctweaks.core.patch;

import dan200.computercraft.shared.computer.blocks.TileComputerBase;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.nbt.NBTTagCompound;
import org.squiddev.cctweaks.core.patch.iface.IExtendedComputerTile;
import org.squiddev.patcher.visitors.MergeVisitor;

/**
 * Ensures NBT data sync
 */
public abstract class TileComputerBase_Patch extends TileComputerBase implements IExtendedComputerTile {
	public boolean hasDisk;
	public int diskId;
	public boolean hasColor;
	public int originalColor;

	@MergeVisitor.Stub
	public ServerComputer createServerComputer() {
		return null;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		native_readFromNBT(compound);
		if (compound.hasKey("rom_id", 99)) {
			hasDisk = true;
			diskId = compound.getInteger("rom_id");
		}
		if (compound.hasKey("original_color", 99)) {
			hasColor = true;
			originalColor = compound.getInteger("original_color");
		}
	}

	@MergeVisitor.Rename(from = {"readFromNBT", "func_145839_a"})
	@MergeVisitor.Stub
	public void native_readFromNBT(NBTTagCompound compound) {
	}

	@Override
	public void writeToNBT(NBTTagCompound compound) {
		native_writeToNBT(compound);
		if (hasDisk) {
			compound.setInteger("rom_id", diskId);
		} else {
			compound.removeTag("rom_id");
		}
		if (hasColor) {
			compound.setInteger("original_color", originalColor);
		} else {
			compound.removeTag("original_color");
		}
	}

	@MergeVisitor.Rename(from = {"writeToNBT", "func_145841_b"})
	@MergeVisitor.Stub
	public void native_writeToNBT(NBTTagCompound compound) {
	}

	public void writeDescription(NBTTagCompound tag) {
		super.writeDescription(tag);
		tag.setInteger("instanceID", createServerComputer().getInstanceID());
		if (hasDisk) tag.setInteger("rom_id", diskId);
		if (hasColor) tag.setInteger("original_color", originalColor);
	}

	public void readDescription(NBTTagCompound gag) {
		super.readDescription(gag);
		m_instanceID = gag.getInteger("instanceID");
		if (gag.hasKey("rom_id", 99)) {
			hasDisk = true;
			diskId = gag.getInteger("rom_id");
		}
		if (gag.hasKey("original_color", 99)) {
			hasColor = true;
			originalColor = gag.getInteger("original_color");
		}
	}
}