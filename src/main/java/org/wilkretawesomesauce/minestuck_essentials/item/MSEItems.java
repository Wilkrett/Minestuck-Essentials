package org.wilkretawesomesauce.minestuck_essentials.item;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.wilkretawesomesauce.minestuck_essentials.Minestuck_essentials;

public final class MSEItems
{
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(Minestuck_essentials.MODID);

	public static final DeferredItem<ItemizedMultiblockItem> ITEMIZED_MULTIBLOCK = REGISTRY.register(
			"itemized_multiblock", () -> new ItemizedMultiblockItem(new Item.Properties()));
	public static final DeferredItem<ItemizerItem> ITEMIZER = REGISTRY.register(
			"itemizer", () -> new ItemizerItem(new Item.Properties().stacksTo(1)));

	private MSEItems()
	{
	}

	public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event)
	{
		if(event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
			event.accept(ITEMIZER);
	}
}
