package org.squiddev.cctweaks.core.blocks;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.core.registry.IModule;

/**
 * Base class for all blocks
 */
public abstract class BaseBlock<T extends BaseTile> extends BlockContainer implements IModule {
	protected final String name;
	protected final Class<T> klass;

	public BaseBlock(String blockName, Material material, Class<T> klass) {
		super(material);

		this.klass = klass;
		name = blockName;

		setHardness(2);
		setBlockName(CCTweaks.RESOURCE_DOMAIN + "." + blockName);
		setBlockTextureName(CCTweaks.RESOURCE_DOMAIN + ":" + blockName);
		setCreativeTab(CCTweaks.getCreativeTab());
	}

	public BaseBlock(String name, Class<T> klass) {
		this(name, Material.rock, klass);
	}

	@SuppressWarnings("unchecked")
	public T getTile(IBlockAccess world, int x, int y, int z) {
		TileEntity tile = world.getTileEntity(x, y, z);
		if (tile != null && klass.isInstance(tile)) {
			return (T) tile;
		}

		return null;
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, Block block, int damage) {
		T tile = getTile(world, x, y, z);
		if (tile != null) tile.onRemove();

		super.breakBlock(world, x, y, z, block, damage);
	}

	@Override
	public boolean canLoad() {
		return true;
	}

	@Override
	public void preInit() {
		GameRegistry.registerBlock(this, name);
		GameRegistry.registerTileEntity(klass, name);
	}

	@Override
	public void init() {
	}
}
