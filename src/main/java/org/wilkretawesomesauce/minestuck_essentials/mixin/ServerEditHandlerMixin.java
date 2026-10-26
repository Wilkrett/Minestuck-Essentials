package org.wilkretawesomesauce.minestuck_essentials.mixin;

import com.mraof.minestuck.computer.editmode.DeployList;
import com.mraof.minestuck.computer.editmode.ServerEditHandler;
import com.mraof.minestuck.skaianet.SburbPlayerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wilkretawesomesauce.minestuck_essentials.item.MSEItemComponents;

@Mixin(ServerEditHandler.class)
public abstract class ServerEditHandlerMixin
{
	@Inject(method = "cleanStackComponentsIfNotDeployable", at = @At("HEAD"), cancellable = true)
	private static void minestuck_essentials$keepItemizedMultiblock(ItemStack stack, SburbPlayerData playerData, Level level, CallbackInfo ci)
	{
		if(stack.has(MSEItemComponents.ITEMIZED_MULTIBLOCK.get()) && DeployList.containsItemStack(stack, playerData, level))
			ci.cancel();
	}
}
