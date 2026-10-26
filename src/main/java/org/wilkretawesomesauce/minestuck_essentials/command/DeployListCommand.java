package org.wilkretawesomesauce.minestuck_essentials.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mraof.minestuck.api.alchemy.GristSet;
import com.mraof.minestuck.api.alchemy.GristType;
import com.mraof.minestuck.api.alchemy.GristTypes;
import com.mraof.minestuck.api.alchemy.MutableGristSet;
import com.mraof.minestuck.api.alchemy.recipe.GristCostRecipe;
import com.mraof.minestuck.command.argument.GristSetArgument;
import com.mraof.minestuck.computer.editmode.DeployList;
import com.mraof.minestuck.computer.editmode.DeployListLoader;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.wilkretawesomesauce.minestuck_essentials.Minestuck_essentials;
import org.wilkretawesomesauce.minestuck_essentials.deploy.MultiblockDeployListLoader;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DeployListCommand
{
	private static final String PACK_NAME = Minestuck_essentials.MODID;
	private static final String PACK_ID = "file/" + PACK_NAME;
	private static final String LITERAL = "deploy_list";
	private static final String REGISTER = "register";
	private static final String LANG = "commands." + Minestuck_essentials.MODID + "." + LITERAL + "." + REGISTER;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private DeployListCommand()
	{
	}

	public static LiteralArgumentBuilder<CommandSourceStack> getArgumentBuilder()
	{
		return Commands.literal(LITERAL)
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal(REGISTER)
						.then(positionArguments(context -> ""))
						.then(Commands.argument("name", StringArgumentType.string())
								.then(positionArguments(context -> StringArgumentType.getString(context, "name")))));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, Coordinates> positionArguments(Function<CommandContext<CommandSourceStack>, String> name)
	{
		return Commands.argument("pos", BlockPosArgument.blockPos())
				.executes(context -> registerBlock(context, name.apply(context), null))
				.then(Commands.argument("to", BlockPosArgument.blockPos())
						.executes(context -> registerMultiblock(context, name.apply(context), null))
						.then(gristCostArgument(DeployListCommand::defaultMultiblockCost)
								.executes(context -> registerMultiblock(context, name.apply(context), getGristCost(context)))))
				.then(gristCostArgument(DeployListCommand::defaultBlockCost)
						.executes(context -> registerBlock(context, name.apply(context), getGristCost(context))));
	}

	public static String registerCommand(BlockPos start, BlockPos end)
	{
		String command = String.join(" ", MSECommands.ROOT, LITERAL, REGISTER, "\"\"", start.getX() + " " + start.getY() + " " + start.getZ());
		return start.equals(end) ? command : command + " " + end.getX() + " " + end.getY() + " " + end.getZ();
	}

	private static int registerBlock(CommandContext<CommandSourceStack> context, String name, @Nullable GristSet gristCost) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		ItemStack stack = itemFor(level, BlockPosArgument.getLoadedBlockPos(context, "pos"), source.getPlayer());
		if(stack.isEmpty())
		{
			source.sendFailure(Component.translatable(LANG + ".no_item"));
			return 0;
		}

		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
		String fileName = fileName(name).orElse(itemId.getNamespace() + "_" + itemId.getPath().replace('/', '_'));
		try
		{
			Path file = datapackData(source.getServer()).resolve("minestuck/deploy_list/" + fileName + ".json");
			if(Files.exists(file))
			{
				source.sendFailure(Component.translatable(LANG + ".already_registered", stack.getHoverName(), displayPath(source.getServer(), file)));
				return 0;
			}

			GristSet cost = gristCost != null ? gristCost : itemCost(level, stack.getItem());
			DeployListLoader.DeployDataList list = new DeployListLoader.DeployDataList(0, new DeployListLoader.DeployDataEntry(stack, 0,
					DeployList.EntryLists.DEPLOY, new DeployListLoader.GristCost(cost != null ? cost : GristSet.EMPTY), false));
			writeJson(source.getServer(), file, DeployListLoader.DeployDataList.CODEC, list);

			reloadDatapacks(source);
			Component path = displayPath(source.getServer(), file);
			if(cost != null)
				source.sendSuccess(() -> Component.translatable(LANG + ".item", stack.getHoverName(), costText(cost), path), true);
			else
				source.sendSuccess(() -> Component.translatable(LANG + ".item.free", stack.getHoverName(), path), true);
			return 1;
		} catch(IOException | IllegalStateException e)
		{
			Minestuck_essentials.LOGGER.error("Couldn't register {} to the deploy list", itemId, e);
			source.sendFailure(Component.translatable(LANG + ".failed", e.getMessage()));
			return 0;
		}
	}

	private static int registerMultiblock(CommandContext<CommandSourceStack> context, String name, @Nullable GristSet gristCost) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		BoundingBox area = area(context);
		int maxSize = StructureBlockEntity.MAX_SIZE_PER_AXIS;
		if(area.getXSpan() > maxSize || area.getYSpan() > maxSize || area.getZSpan() > maxSize)
		{
			source.sendFailure(Component.translatable(LANG + ".too_big", maxSize));
			return 0;
		}

		AreaCost areaCost = AreaCost.of(level, area, source.getPlayer());
		if(areaCost.blocks() == 0)
		{
			source.sendFailure(Component.translatable(LANG + ".empty"));
			return 0;
		}

		try
		{
			Path data = datapackData(source.getServer());
			Optional<String> chosenFileName = fileName(name);
			String fileName = chosenFileName.orElseGet(() -> nextMultiblockName(data));
			Path structureFile = data.resolve("structure/" + fileName + ".nbt");
			Path entryFile = data.resolve(MultiblockDeployListLoader.DIRECTORY + "/" + fileName + ".json");
			Optional<Component> displayName = name.isBlank() ? Optional.empty() : Optional.of(Component.literal(name.strip()));
			if(chosenFileName.isPresent() && (Files.exists(structureFile) || Files.exists(entryFile)))
			{
				source.sendFailure(Component.translatable(LANG + ".already_registered", displayName.orElseThrow(),
						displayPath(source.getServer(), Files.exists(entryFile) ? entryFile : structureFile)));
				return 0;
			}

			StructureTemplate template = new StructureTemplate();
			template.fillFromWorld(level, new BlockPos(area.minX(), area.minY(), area.minZ()),
					new Vec3i(area.getXSpan(), area.getYSpan(), area.getZSpan()), false, Blocks.AIR);
			template.setAuthor(source.getTextName());
			Files.createDirectories(structureFile.getParent());
			NbtIo.writeCompressed(template.save(new CompoundTag()), structureFile);

			GristSet cost = gristCost != null ? gristCost : areaCost.cost();
			ResourceLocation structureId = ResourceLocation.fromNamespaceAndPath(Minestuck_essentials.MODID, fileName);
			Direction facing = source.getEntity() != null ? source.getEntity().getDirection() : Direction.NORTH;
			MultiblockDeployListLoader.MultiblockDeployList list = new MultiblockDeployListLoader.MultiblockDeployList(0, List.of(
					new MultiblockDeployListLoader.Entry(structureId, displayName, facing, Optional.empty(), 0, DeployList.EntryLists.DEPLOY, List.of(new DeployListLoader.GristCost(cost)))));
			writeJson(source.getServer(), entryFile, MultiblockDeployListLoader.MultiblockDeployList.CODEC, list);

			reloadDatapacks(source);
			Component path = displayPath(source.getServer(), entryFile);
			Component multiblock = displayName.orElse(Component.literal(structureId.toString()));
			source.sendSuccess(() -> Component.translatable(LANG + ".multiblock", areaCost.blocks(), multiblock, costText(cost), path), true);
			if(gristCost == null && areaCost.freeBlocks() > 0)
				source.sendSuccess(() -> Component.translatable(LANG + ".multiblock.free_blocks", areaCost.freeBlocks()), false);
			return 1;
		} catch(IOException | IllegalStateException e)
		{
			Minestuck_essentials.LOGGER.error("Couldn't itemize {} to the deploy list", area, e);
			source.sendFailure(Component.translatable(LANG + ".failed", e.getMessage()));
			return 0;
		}
	}

	private static BoundingBox area(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		return BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(context, "pos"), BlockPosArgument.getLoadedBlockPos(context, "to"));
	}

	private static ItemStack itemFor(ServerLevel level, BlockPos pos, @Nullable Player player)
	{
		ItemStack clone = level.getBlockState(pos).getCloneItemStack(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false), level, pos, player);
		return clone.isEmpty() ? ItemStack.EMPTY : new ItemStack(clone.getItem());
	}

	@Nullable
	private static GristSet itemCost(Level level, Item item)
	{
		return item == Items.AIR ? null : GristCostRecipe.findCostForItem(new ItemStack(item), null, false, level);
	}

	private record AreaCost(GristSet cost, int blocks, int freeBlocks)
	{
		static AreaCost of(ServerLevel level, BoundingBox area, @Nullable Player player)
		{
			MutableGristSet cost = MutableGristSet.newDefault();
			Map<Item, Optional<GristSet>> itemCosts = new HashMap<>();
			int blocks = 0;
			int freeBlocks = 0;
			for(BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ()))
			{
				if(level.getBlockState(pos).isAir())
					continue;
				blocks++;
				Optional<GristSet> blockCost = itemCosts.computeIfAbsent(itemFor(level, pos, player).getItem(), item -> Optional.ofNullable(itemCost(level, item)));
				if(blockCost.isPresent())
					cost.add(blockCost.get());
				else
					freeBlocks++;
			}
			return new AreaCost(cost.asImmutable(), blocks, freeBlocks);
		}
	}

	private static RequiredArgumentBuilder<CommandSourceStack, ?> gristCostArgument(DefaultCost defaultCost)
	{
		return Commands.argument("gristcost", GristSetArgument.nonNegativeSet())
				.suggests((context, builder) -> suggestGristCost(context, builder, defaultCost));
	}

	private static GristSet getGristCost(CommandContext<CommandSourceStack> context)
	{
		return GristSetArgument.getNonNegativeGristArgument(context, "gristcost");
	}

	@FunctionalInterface
	private interface DefaultCost
	{
		@Nullable
		GristSet get(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
	}

	@Nullable
	private static GristSet defaultBlockCost(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		ServerLevel level = context.getSource().getLevel();
		return itemCost(level, itemFor(level, BlockPosArgument.getLoadedBlockPos(context, "pos"), context.getSource().getPlayer()).getItem());
	}

	@Nullable
	private static GristSet defaultMultiblockCost(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		BoundingBox area = area(context);
		int maxSize = StructureBlockEntity.MAX_SIZE_PER_AXIS;
		if(area.getXSpan() > maxSize || area.getYSpan() > maxSize || area.getZSpan() > maxSize)
			return null;
		return AreaCost.of(context.getSource().getLevel(), area, context.getSource().getPlayer()).cost();
	}

	private static CompletableFuture<Suggestions> suggestGristCost(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder, DefaultCost defaultCost)
	{
		String remaining = builder.getRemaining();
		if(remaining.isEmpty())
		{
			try
			{
				GristSet cost = defaultCost.get(context);
				if(cost != null && !cost.isEmpty())
					builder.suggest(cost.asAmounts().stream()
							.map(amount -> amount.amount() + " " + amount.type().getIdOrThrow())
							.collect(Collectors.joining(" ")));
			} catch(CommandSyntaxException ignored)
			{
			}
			return builder.buildFuture();
		}

		int wordStart = remaining.lastIndexOf(' ') + 1;
		List<String> previousWords = Arrays.stream(remaining.substring(0, wordStart).split(" ")).filter(word -> !word.isEmpty()).toList();
		if(previousWords.size() % 2 == 0)
			return builder.buildFuture();

		Set<ResourceLocation> usedTypes = new HashSet<>();
		for(int i = 1; i < previousWords.size(); i += 2)
			usedTypes.add(ResourceLocation.tryParse(previousWords.get(i)));
		return SharedSuggestionProvider.suggestResource(GristTypes.REGISTRY.keySet().stream()
				.filter(id -> !id.equals(GristType.DUMMY_ID) && !usedTypes.contains(id)), builder.createOffset(builder.getStart() + wordStart));
	}

	private static Component costText(GristSet cost)
	{
		return cost.isEmpty() ? Component.translatable("minestuck.free") : cost.asTextComponent();
	}

	private static Optional<String> fileName(String name)
	{
		String fileName = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_").replaceAll("^_+|_+$", "");
		return fileName.isEmpty() ? Optional.empty() : Optional.of(fileName);
	}

	private static String nextMultiblockName(Path data)
	{
		for(int i = 1; ; i++)
		{
			String name = "multiblock_" + i;
			if(Files.notExists(data.resolve("structure/" + name + ".nbt"))
					&& Files.notExists(data.resolve(MultiblockDeployListLoader.DIRECTORY + "/" + name + ".json")))
				return name;
		}
	}

	private static Path datapackData(MinecraftServer server) throws IOException
	{
		Path pack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_NAME);
		Path meta = pack.resolve("pack.mcmeta");
		if(Files.notExists(meta))
		{
			JsonObject packInfo = new JsonObject();
			packInfo.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(PackType.SERVER_DATA));
			packInfo.addProperty("description", "Deploy list entries registered with /minessentials");
			JsonObject root = new JsonObject();
			root.add("pack", packInfo);
			Files.createDirectories(pack);
			Files.writeString(meta, GSON.toJson(root));
		}
		return pack.resolve("data").resolve(Minestuck_essentials.MODID);
	}

	private static <T> void writeJson(MinecraftServer server, Path file, Codec<T> codec, T value) throws IOException
	{
		JsonElement json = codec.encodeStart(server.registryAccess().createSerializationContext(JsonOps.INSTANCE), value).getOrThrow();
		Files.createDirectories(file.getParent());
		Files.writeString(file, GSON.toJson(json));
	}

	private static Component displayPath(MinecraftServer server, Path file)
	{
		Path datapacks = server.getWorldPath(LevelResource.DATAPACK_DIR);
		return Component.literal(datapacks.getFileName() + "/" + datapacks.relativize(file).toString().replace('\\', '/'));
	}

	private static void reloadDatapacks(CommandSourceStack source)
	{
		MinecraftServer server = source.getServer();
		PackRepository packs = server.getPackRepository();
		packs.reload();
		List<String> selected = new ArrayList<>(packs.getSelectedIds());
		if(!selected.contains(PACK_ID))
			selected.add(PACK_ID);

		server.reloadResources(selected)
				.thenRun(() -> DeployList.onConditionsUpdated(server))
				.exceptionally(e -> {
					Minestuck_essentials.LOGGER.warn("Failed to reload datapacks", e);
					source.sendFailure(Component.translatable("commands.reload.failure"));
					return null;
				});
	}
}
