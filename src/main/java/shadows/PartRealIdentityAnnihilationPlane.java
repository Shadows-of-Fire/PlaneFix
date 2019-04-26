package shadows;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.collect.ImmutableMap;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.core.AppEng;
import appeng.core.sync.packets.PacketTransitionEffect;
import appeng.hooks.TickHandler;
import appeng.me.GridAccessException;
import appeng.parts.automation.PartAnnihilationPlane;
import appeng.parts.automation.PartIdentityAnnihilationPlane;
import appeng.util.Platform;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;

public class PartRealIdentityAnnihilationPlane extends PartIdentityAnnihilationPlane {

	protected boolean breaking = false;

	protected static Field accepting;
	protected static MethodHandle isAccepting;
	protected static MethodHandle setAccepting;
	protected static MethodHandle canStoreItemStacks;
	protected static MethodHandle storeEntityItem;
	protected static ItemStack silkyBoi = new ItemStack(Items.DIAMOND_PICKAXE);

	static {
		try {
			accepting = PartAnnihilationPlane.class.getDeclaredField("isAccepting");
			accepting.setAccessible(true);
			isAccepting = MethodHandles.lookup().unreflectGetter(accepting);
			setAccepting = MethodHandles.lookup().unreflectSetter(accepting);
			Method m = PartAnnihilationPlane.class.getDeclaredMethod("canStoreItemStacks", List.class);
			m.setAccessible(true);
			canStoreItemStacks = MethodHandles.lookup().unreflect(m);
			m = PartAnnihilationPlane.class.getDeclaredMethod("storeEntityItem", EntityItem.class);
			m.setAccessible(true);
			storeEntityItem = MethodHandles.lookup().unreflect(m);
		} catch (Exception e) {
			e.printStackTrace();
		}
		EnchantmentHelper.setEnchantments(ImmutableMap.of(Enchantments.SILK_TOUCH, 1), silkyBoi);
	}

	public PartRealIdentityAnnihilationPlane(ItemStack is) {
		super(is);
	}

	@Override
	public TickRateModulation call(World world) throws Exception {
		this.breaking = false;
		try {
			return this.breakBlock(true);
		} catch (Throwable e) {
			throw new RuntimeException();
		}
	}

	@Override
	public TickRateModulation tickingRequest(final IGridNode node, final int ticksSinceLastCall) {
		if (this.breaking) return TickRateModulation.URGENT;
		try {
			setAccepting.invoke(this, true);
			return this.breakBlock(false);
		} catch (Throwable e) {
			throw new RuntimeException();
		}
	}

	protected TickRateModulation breakBlock(boolean modulate) throws Throwable {
		if ((boolean) isAccepting.invoke(this) && this.getProxy().isActive()) {
			try {
				final TileEntity te = this.getTile();
				final WorldServer w = (WorldServer) te.getWorld();

				final BlockPos pos = te.getPos().offset(this.getSide().getFacing());
				final IEnergyGrid energy = this.getProxy().getEnergy();

				if (this.canHandleBlock(w, pos)) {
					final List<ItemStack> items = this.obtainBlockDrops(w, pos);
					final float requiredPower = this.calculateEnergyUsage(w, pos, items);

					final boolean hasPower = energy.extractAEPower(requiredPower, Actionable.SIMULATE, PowerMultiplier.CONFIG) > requiredPower - 0.1;
					final boolean canStore = (boolean) canStoreItemStacks.invoke(this, items);

					if (hasPower && canStore) {
						if (modulate) {
							energy.extractAEPower(requiredPower, Actionable.MODULATE, PowerMultiplier.CONFIG);
							this.breakBlockAndStoreItems(w, pos);
							AppEng.proxy.sendToAllNearExcept(null, pos.getX(), pos.getY(), pos.getZ(), 64, w, new PacketTransitionEffect(pos.getX(), pos.getY(), pos.getZ(), this.getSide(), true));
						} else {
							this.breaking = true;
							TickHandler.INSTANCE.addCallable(this.getTile().getWorld(), this);
						}
						return TickRateModulation.URGENT;
					}
				}
			} catch (final GridAccessException e1) {
				// :P
			}
		}

		// nothing to do here :)
		return TickRateModulation.IDLE;
	}

	protected boolean canHandleBlock(final WorldServer w, final BlockPos pos) {
		final IBlockState state = w.getBlockState(pos);
		final Material material = state.getMaterial();
		final float hardness = state.getBlockHardness(w, pos);
		final boolean ignoreMaterials = material == Material.AIR || material == Material.LAVA || material == Material.WATER || material.isLiquid();
		final boolean ignoreBlocks = state.getBlock() == Blocks.BEDROCK || state.getBlock() == Blocks.END_PORTAL || state.getBlock() == Blocks.END_PORTAL_FRAME || state.getBlock() == Blocks.COMMAND_BLOCK;

		return !ignoreMaterials && !ignoreBlocks && hardness >= 0f && !w.isAirBlock(pos) && w.isBlockLoaded(pos) && w.canMineBlockBody(Platform.getPlayer(w), pos);
	}

	protected void breakBlockAndStoreItems(final WorldServer w, final BlockPos pos) throws Throwable {
		IBlockState state = w.getBlockState(pos);
		state.getBlock().harvestBlock(w, FakePlayerFactory.getMinecraft(w), pos, state, w.getTileEntity(pos), silkyBoi);
		w.setBlockToAir(pos);
		final AxisAlignedBB box = new AxisAlignedBB(pos).grow(0.2);
		for (EntityItem ei : w.getEntitiesWithinAABB(EntityItem.class, box)) {
			storeEntityItem.invoke(this, ei);
		}

	}

}
