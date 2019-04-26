package shadows;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import appeng.items.parts.PartType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = PlaneFix.MODID, name = PlaneFix.MODNAME, version = PlaneFix.VERSION, dependencies = "required-after:appliedenergistics2", acceptableRemoteVersions = "*")
public class PlaneFix {

	public static final String MODID = "planefix";
	public static final String MODNAME = "PlaneFix";
	public static final String VERSION = "1.0.0";

	private static GameProfile MINECRAFT = new GameProfile(UUID.fromString("41C82C87-7AfB-4024-BA57-13D2C99CAE77"), "[Minecraft]");

	@EventHandler
	public void postInit(FMLPostInitializationEvent e) throws Exception {
		EnumHelper.setFailsafeFieldValue(PartType.class.getDeclaredField("myPart"), PartType.IDENTITY_ANNIHILATION_PLANE, PartRealIdentityAnnihilationPlane.class);
		EnumHelper.setFailsafeFieldValue(PartType.class.getDeclaredField("constructor"), PartType.IDENTITY_ANNIHILATION_PLANE, PartRealIdentityAnnihilationPlane.class.getConstructor(ItemStack.class));
	}

	@EventHandler
	public void started(FMLServerStartedEvent e) {
		if (e.getSide() == Side.SERVER) {
			MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
			server.getPlayerList().addOp(MINECRAFT);
		}
	}

}
