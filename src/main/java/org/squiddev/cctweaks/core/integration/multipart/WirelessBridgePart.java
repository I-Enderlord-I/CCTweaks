package org.squiddev.cctweaks.core.integration.multipart;

import codechicken.lib.render.TextureUtils;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dan200.computercraft.client.render.FixedRenderBlocks;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.api.IDataCard;
import org.squiddev.cctweaks.api.IWorldPosition;
import org.squiddev.cctweaks.core.network.NetworkBinding;
import org.squiddev.cctweaks.core.registry.Registry;

/**
 * A multipart equivalent of {@link org.squiddev.cctweaks.core.blocks.WirelessBridge}
 */
public class WirelessBridgePart extends SidedNetworkPart {
	public static final String NAME = CCTweaks.NAME + ":wirelessBridge";

	@SideOnly(Side.CLIENT)
	public static BridgeRenderer renderBlocks;

	protected final NetworkBinding binding = new NetworkBinding(this);

	public WirelessBridgePart(int direction) {
		this.direction = (byte)direction;
	}

	public String getType() {
		return NAME;
	}

	@Override
	public IIcon getBrokenIcon(int i) {
		return Registry.blockWirelessBridge.getIcon(0, 0);
	}

	@Override
	public void onRemoved() {
		super.onRemoved();
		binding.remove();
	}

	@Override
	public void onAdded() {
		super.onAdded();
		binding.add();
	}

	@Override
	public void save(NBTTagCompound tag) {
		super.save(tag);
		binding.save(tag);
	}

	@Override
	public void load(NBTTagCompound tag) {
		super.load(tag);
		binding.load(tag);
	}

	@Override
	public boolean activate(EntityPlayer player, MovingObjectPosition hit, ItemStack item) {
		ItemStack stack = player.getHeldItem();
		if (stack != null && stack.getItem() instanceof IDataCard) {
			IDataCard card = (IDataCard) stack.getItem();

			if (player.isSneaking()) {
				binding.save(stack, card);
				card.notifyPlayer(player, IDataCard.Messages.Stored);
				return true;
			} else if (binding.load(stack, card)) {
				card.notifyPlayer(player, IDataCard.Messages.Loaded);
				return true;
			}
		}

		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean renderStatic(Vector3 pos, int pass) {
		if (pass == 0 || pass == -1) {
			TextureUtils.bindAtlas(0);

			BridgeRenderer render = renderBlocks;
			if (render == null) render = renderBlocks = new BridgeRenderer();

			Cuboid6 bounds = getBounds();
			render.setRenderBounds(bounds.min.x, bounds.min.y, bounds.min.z, bounds.max.x, bounds.max.y, bounds.max.z);

			if (pass == 0) {
				render.setWorld(world());
				render.renderStandardBlock(Registry.blockWirelessBridge, x(), y(), z());
			} else {
				render.renderBlockAsItem(Registry.blockWirelessBridge, 0, 1);
			}
			return true;
		}

		return false;
	}

	@Override
	public Iterable<IWorldPosition> getExtraNodes() {
		return binding.getPositions();
	}

	@SideOnly(Side.CLIENT)
	public static class BridgeRenderer extends FixedRenderBlocks {
		@Override
		public IIcon getBlockIcon(Block block, IBlockAccess world, int x, int y, int z, int side) {
			return Registry.blockWirelessBridge.getIcon(0, 0);
		}

		@Override
		public void setRenderBoundsFromBlock(Block block) {
		}
	}
}
