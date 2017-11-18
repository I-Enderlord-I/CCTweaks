package org.squiddev.cctweaks.core.rom;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.items.ComputerItemFactory;
import dan200.computercraft.shared.media.items.ItemDiskExpanded;
import dan200.computercraft.shared.media.items.ItemDiskLegacy;
import dan200.computercraft.shared.util.Colour;
import dan200.computercraft.shared.util.ImpostorShapelessRecipe;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.oredict.RecipeSorter;

import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.api.computer.ICustomRomItem;
import org.squiddev.cctweaks.core.Config;
import org.squiddev.cctweaks.core.registry.Module;
import org.squiddev.cctweaks.items.ItemBase;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Create a crafting recipe which can set the ROM of any computer item.
 */
public class CraftingSetRom extends Module implements IRecipe {
	private static final ComputerFamily[] FAMILIES = new ComputerFamily[]{
		ComputerFamily.Normal,
		ComputerFamily.Advanced,
	};
	public static CraftingSetRom INSTANCE;

	@Override
	public void init() {
		RecipeSorter.register(CCTweaks.RESOURCE_DOMAIN + ":custom_rom", CraftingSetRom.class, RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless");
		GameRegistry.addRecipe(this);
		INSTANCE = this;

		ItemStack disk = new ItemStack(ComputerCraft.Items.diskExpanded);

		// Create some custom recipes. We don't bother with turtles, pocket computers or anything else: we just
		// want to show it is possible, not spam the recipe list.
		for (ComputerFamily family : FAMILIES) {
			ItemStack withoutRom = ComputerItemFactory.create(-1, null, family);

			ItemStack withRom = ComputerItemFactory.create(-1, null, family);
			ItemBase.getTag(withRom).setInteger("rom_id", -1);

			GameRegistry.addRecipe(new ImpostorShapelessRecipe(withRom, new ItemStack[]{withoutRom, disk}));
			GameRegistry.addRecipe(new ImpostorShapelessRecipe(disk, new ItemStack[]{withRom}));
			GameRegistry.addRecipe(new ImpostorShapelessRecipe(disk, new ItemStack[]{withRom, disk}));
		}
	}

	@Override
	public boolean matches(InventoryCrafting inv, World world) {
		return Config.Computer.CustomRom.enabled && Config.Computer.CustomRom.crafting && getCraftingResult(inv) != null;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {
		ICustomRomItem customRom = null;
		ItemStack romStack = null;

		ItemDiskLegacy disk = null;
		ItemStack diskStack = null;

		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack stack = inv.getStackInSlot(i);
			if (stack == null) continue;

			// We don't want to craft multiple items at once.
			if (stack.stackSize > 1) return null;

			Item item = stack.getItem();
			if (item instanceof ItemDiskLegacy) {
				if (disk != null) return null;
				disk = (ItemDiskLegacy) item;
				diskStack = stack;

				// Ensure this disk exists
				if (disk.getDiskID(stack) < 0) return null;
			} else if (item instanceof ICustomRomItem) {
				if (customRom != null) return null;
				customRom = (ICustomRomItem) item;
				romStack = stack;
			} else {
				return null;
			}
		}

		if (customRom == null) return null;

		if (customRom.hasCustomRom(romStack)) {
			// Crafting with a disk will result in a disk with the old ROM
			return newDisk(romStack);
		} else {
			if (diskStack == null) return null;

			// Crafting without a disk will remove the ROM
			ItemStack result = romStack.copy();
			customRom.setCustomRom(result, disk.getDiskID(diskStack));
			customRom.setOriginalColor(result, disk.getColor(diskStack));
			return result;
		}
	}

	@Override
	public int getRecipeSize() {
		return 2;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return null;
	}

	private static ItemStack newDisk(ItemStack romStack) {
		return ItemDiskExpanded.createFromIDAndColour(((ICustomRomItem)romStack.getItem()).getCustomRom(romStack), null, ((ICustomRomItem)romStack.getItem()).getOriginalColor(romStack));
	}

	public static ItemStack copyRom(ItemStack to, ItemStack from) {
		NBTTagCompound fromTag = from.getTagCompound();
		if (fromTag != null) {
			NBTTagCompound toTag = to.getTagCompound();
			if (toTag == null) to.setTagCompound(toTag = new NBTTagCompound());

			if (fromTag.hasKey("rom_id")) toTag.setTag("rom_id", fromTag.getTag("rom_id"));
			if (fromTag.hasKey("original_color")) toTag.setTag("original_color", fromTag.getTag("original_color"));
		}

		return to;
	}
	
	public static ItemStack createComputerContainer(ItemStack stack, ItemStack newROM){
		if(stack == null || !(stack.getItem() instanceof ICustomRomItem))
			return null;
		if(newROM != null && !(newROM.getItem() instanceof ItemDiskLegacy))
			return null;
		
		if(newROM == null) {
			((ICustomRomItem)stack.getItem()).clearCustomRom(stack);
			((ICustomRomItem)stack.getItem()).setOriginalColor(stack,Colour.Black.getHex());
		}
		else {
			((ICustomRomItem)stack.getItem()).setCustomRom(stack, ((ItemDiskLegacy)newROM.getItem()).getDiskID(newROM));
			((ICustomRomItem)stack.getItem()).setOriginalColor(stack,((ItemDiskLegacy)newROM.getItem()).getColor(newROM));
		}
		
		return new ItemStack(new ItemComputerContainer(stack));
	}
	
	private static class ItemComputerContainer extends ItemBlock{
		private ItemStack stack;
		public ItemComputerContainer(ItemStack stack) {
			super(net.minecraft.init.Blocks.stone);
			this.stack = stack;
		}
		
		@Override
		public boolean hasContainerItem(ItemStack itemStack) {
			return true;
		}

		@Override
		public ItemStack getContainerItem(ItemStack original) {
			ItemStack result = stack.copy();
			return result;
		}
		
		@Override
		public boolean doesContainerItemLeaveCraftingGrid(ItemStack itemStack)
		{
			return false;
		}
	}
}
