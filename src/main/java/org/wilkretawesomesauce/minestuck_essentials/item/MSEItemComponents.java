package org.wilkretawesomesauce.minestuck_essentials.item;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuck_essentials.Minestuck_essentials;

public final class MSEItemComponents
{
	public static final DeferredRegister.DataComponents REGISTRY =
			DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Minestuck_essentials.MODID);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemizedMultiblock>> ITEMIZED_MULTIBLOCK = REGISTRY.registerComponentType(
			"itemized_multiblock", builder -> builder.persistent(ItemizedMultiblock.CODEC).networkSynchronized(ItemizedMultiblock.STREAM_CODEC));
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemizerSelection>> ITEMIZER_SELECTION = REGISTRY.registerComponentType(
			"itemizer_selection", builder -> builder.persistent(ItemizerSelection.CODEC).networkSynchronized(ItemizerSelection.STREAM_CODEC));

	private MSEItemComponents()
	{
	}
}
