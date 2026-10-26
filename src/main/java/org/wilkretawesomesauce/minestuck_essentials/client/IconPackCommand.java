package org.wilkretawesomesauce.minestuck_essentials.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import org.wilkretawesomesauce.minestuck_essentials.Minestuck_essentials;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public final class IconPackCommand
{
	public static final String PACK_FOLDER = Minestuck_essentials.MODID + "_icons";
	private static final String PACK_ID = "file/" + PACK_FOLDER;
	private static final String LANG = "commands." + Minestuck_essentials.MODID + ".resourcepack";

	private IconPackCommand()
	{
	}

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal("generate").then(Commands.literal("resourcepack").executes(context -> generate(context.getSource())));
	}

	private static int generate(CommandSourceStack source)
	{
		Minecraft mc = Minecraft.getInstance();
		Path pack = mc.getResourcePackDirectory().resolve(PACK_FOLDER);
		Map<String, TreeSet<String>> iconsByNamespace = new TreeMap<>();
		iconsByNamespace.put(Minestuck_essentials.MODID, new TreeSet<>());
		for(ResourceLocation structure : MultiblockPreviews.structures())
			iconsByNamespace.computeIfAbsent(structure.getNamespace(), namespace -> new TreeSet<>()).add(structure.getPath() + ".png");

		try
		{
			Path meta = pack.resolve("pack.mcmeta");
			if(Files.notExists(meta))
			{
				JsonObject packInfo = new JsonObject();
				packInfo.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
				packInfo.addProperty("description", "Icons for itemized multiblocks");
				JsonObject root = new JsonObject();
				root.add("pack", packInfo);
				Files.createDirectories(pack);
				Files.writeString(meta, new GsonBuilder().setPrettyPrinting().create().toJson(root));
			}
			for(String namespace : iconsByNamespace.keySet())
				Files.createDirectories(iconFolder(pack, namespace));
		} catch(IOException e)
		{
			Minestuck_essentials.LOGGER.error("Couldn't make the multiblock icon pack", e);
			source.sendFailure(Component.translatable(LANG + ".failed", e.getMessage()));
			return 0;
		}

		PackRepository packs = mc.getResourcePackRepository();
		packs.reload();
		boolean alreadyEnabled = packs.getSelectedIds().contains(PACK_ID);
		if(!alreadyEnabled)
		{
			List<String> selected = new ArrayList<>(packs.getSelectedIds());
			selected.add(PACK_ID);
			packs.setSelected(selected);
			mc.options.updateResourcePacks(packs);
		}

		source.sendSuccess(() -> Component.translatable(alreadyEnabled ? LANG + ".ready" : LANG + ".enabled"), false);
		iconsByNamespace.forEach((namespace, icons) -> {
			Path folder = iconFolder(pack, namespace);
			Component link = Component.literal(pack.getFileName() + "/" + pack.relativize(folder).toString().replace('\\', '/'))
					.withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, folder.toAbsolutePath().toString())));
			source.sendSuccess(() -> Component.translatable(LANG + ".folder", link), false);
			for(String icon : icons)
				source.sendSuccess(() -> Component.literal("  " + icon).withStyle(ChatFormatting.GRAY), false);
		});
		return 1;
	}

	private static Path iconFolder(Path pack, String namespace)
	{
		return pack.resolve("assets").resolve(namespace).resolve("textures").resolve("multiblock");
	}
}
