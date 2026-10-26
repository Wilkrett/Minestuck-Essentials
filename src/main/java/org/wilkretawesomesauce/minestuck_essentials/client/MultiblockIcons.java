package org.wilkretawesomesauce.minestuck_essentials.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import org.wilkretawesomesauce.minestuck_essentials.Minestuck_essentials;
import org.wilkretawesomesauce.minestuck_essentials.item.ItemizedMultiblock;
import org.wilkretawesomesauce.minestuck_essentials.item.MSEItemComponents;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class MultiblockIcons
{
	private static final int MAX_SIZE = 64;
	private static final Map<ResourceLocation, Optional<Icon>> ICONS = new HashMap<>();

	public record Icon(ResourceLocation texture, int width, int height)
	{
	}

	private MultiblockIcons()
	{
	}

	public static Optional<Icon> get(ItemStack stack)
	{
		ItemizedMultiblock itemized = stack.get(MSEItemComponents.ITEMIZED_MULTIBLOCK.get());
		if(itemized == null)
			return Optional.empty();
		if(itemized.icon().isPresent())
			return ICONS.computeIfAbsent(itemized.icon().get(), icon -> load(icon, true));
		return ICONS.computeIfAbsent(itemized.structure().withPrefix("multiblock/"), icon -> load(icon, false));
	}

	private static Optional<Icon> load(ResourceLocation icon, boolean warnIfMissing)
	{
		Minecraft mc = Minecraft.getInstance();
		Optional<Resource> resource = mc.getResourceManager().getResource(icon.withPath(path -> "textures/" + path + ".png"));
		if(resource.isEmpty())
		{
			if(warnIfMissing)
				Minestuck_essentials.LOGGER.warn("Itemized multiblock icon {} doesn't exist", icon);
			return Optional.empty();
		}

		try(InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream))
		{
			float scale = Math.min(1F, (float) MAX_SIZE / Math.max(image.getWidth(), image.getHeight()));
			int width = Math.max(1, Math.round(image.getWidth() * scale));
			int height = Math.max(1, Math.round(image.getHeight() * scale));
			NativeImage resized = new NativeImage(image.format(), width, height, false);
			image.resizeSubRectTo(0, 0, image.getWidth(), image.getHeight(), resized);

			DynamicTexture texture = new DynamicTexture(resized);
			texture.setFilter(true, false);
			ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath(Minestuck_essentials.MODID, "multiblock_icon/" + icon.getNamespace() + "/" + icon.getPath());
			mc.getTextureManager().register(textureId, texture);
			return Optional.of(new Icon(textureId, width, height));
		} catch(IOException e)
		{
			Minestuck_essentials.LOGGER.error("Couldn't load itemized multiblock icon {}", icon, e);
			return Optional.empty();
		}
	}

	public static void clear()
	{
		ICONS.values().forEach(icon -> icon.ifPresent(loaded -> Minecraft.getInstance().getTextureManager().release(loaded.texture())));
		ICONS.clear();
	}
}
