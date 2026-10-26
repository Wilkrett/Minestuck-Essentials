package org.wilkretawesomesauce.minestuck_essentials.deploy;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mraof.minestuck.api.alchemy.GristSet;
import com.mraof.minestuck.api.alchemy.MutableGristSet;
import com.mraof.minestuck.computer.editmode.DeployList;
import com.mraof.minestuck.computer.editmode.DeployListLoader;
import com.mraof.minestuck.skaianet.SburbPlayerData;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.wilkretawesomesauce.minestuck_essentials.Minestuck_essentials;
import org.wilkretawesomesauce.minestuck_essentials.item.ItemizedMultiblock;
import org.wilkretawesomesauce.minestuck_essentials.item.MSEItemComponents;
import org.wilkretawesomesauce.minestuck_essentials.item.MSEItems;
import org.wilkretawesomesauce.minestuck_essentials.network.MultiblockPreviewsPayload;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = Minestuck_essentials.MODID)
public class MultiblockDeployListLoader extends SimpleJsonResourceReloadListener
{
	public static final String DIRECTORY = Minestuck_essentials.MODID + "/deploy_list";
	private static final List<String> ADDED_ENTRIES = new ArrayList<>();
	private static Map<ResourceLocation, MultiblockPreview> previews = Map.of();

	@SubscribeEvent
	public static void onAddReloadListeners(AddReloadListenerEvent event)
	{
		event.addListener(new MultiblockDeployListLoader());
	}

	@SubscribeEvent
	public static void onDatapackSync(OnDatapackSyncEvent event)
	{
		MultiblockPreviewsPayload payload = new MultiblockPreviewsPayload(previews);
		event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
	}

	public MultiblockDeployListLoader()
	{
		super(new GsonBuilder().create(), DIRECTORY);
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> jsonEntries, ResourceManager resourceManager, ProfilerFiller profiler)
	{
		for(DeployList.EntryLists list : DeployList.EntryLists.values())
			list.getList().removeIf(entry -> ADDED_ENTRIES.contains(entry.getName()));
		ADDED_ENTRIES.clear();

		List<Map.Entry<ResourceLocation, MultiblockDeployList>> lists = new ArrayList<>();
		jsonEntries.forEach((id, json) -> MultiblockDeployList.CODEC.parse(JsonOps.INSTANCE, json)
				.resultOrPartial(message -> Minestuck_essentials.LOGGER.error("Couldn't entirely parse multiblock deploy list {}: {}", id, message))
				.ifPresent(list -> lists.add(Map.entry(id, list))));
		lists.sort((left, right) -> Float.compare(right.getValue().priority(), left.getValue().priority()));

		Map<ResourceLocation, Optional<CompoundTag>> templates = new HashMap<>();
		Map<ResourceLocation, MultiblockPreview> newPreviews = new HashMap<>();
		for(Map.Entry<ResourceLocation, MultiblockDeployList> list : lists)
		{
			List<Entry> entries = list.getValue().entries();
			for(int i = 0; i < entries.size(); i++)
			{
				Entry entry = entries.get(i);
				String name = Minestuck_essentials.MODID + ":multiblock/" + list.getKey() + "-" + i;
				Optional<CompoundTag> template = templates.computeIfAbsent(entry.structure(), structure -> readTemplate(resourceManager, structure));
				template.flatMap(MultiblockPreview::fromTemplate).ifPresent(preview -> newPreviews.put(entry.structure(), preview));
				ItemStack stack = entry.createStack(template.flatMap(MultiblockPreview::templateSize));
				ADDED_ENTRIES.add(name);
				DeployList.registerItem(name, entry.tier(), playerData -> isAvailable(entry, playerData),
						(playerData, level) -> stack, (isPrimary, playerData) -> currentCost(entry, playerData), entry.category());
			}
		}
		previews = Map.copyOf(newPreviews);
	}

	private static Optional<CompoundTag> readTemplate(ResourceManager resourceManager, ResourceLocation structure)
	{
		Optional<Resource> resource = resourceManager.getResource(structure.withPath(path -> "structure/" + path + ".nbt"));
		if(resource.isEmpty())
		{
			Minestuck_essentials.LOGGER.warn("Itemized multiblock {} points at a structure that doesn't exist", structure);
			return Optional.empty();
		}

		try(InputStream stream = resource.get().open())
		{
			CompoundTag tag = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
			return Optional.of(DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), tag, NbtUtils.getDataVersion(tag, 500)));
		} catch(IOException e)
		{
			Minestuck_essentials.LOGGER.error("Couldn't read structure {}", structure, e);
			return Optional.empty();
		}
	}

	private boolean isAvailable(Entry entry, SburbPlayerData playerData)
	{
		return entry.cost().isEmpty() || findCost(entry, playerData).isPresent();
	}

	private GristSet currentCost(Entry entry, SburbPlayerData playerData)
	{
		MutableGristSet set = MutableGristSet.newDefault();
		findCost(entry, playerData).ifPresent(cost -> {
			cost.grist().ifPresent(set::add);
			if(cost.primary() != 0)
				set.add(playerData.getBaseGrist(), cost.primary());
		});
		return set.asImmutable();
	}

	private Optional<DeployListLoader.GristCost> findCost(Entry entry, SburbPlayerData playerData)
	{
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		ServerPlayer player = server == null ? null : playerData.playerId().getPlayer(server);
		if(player == null)
			return Optional.empty();

		for(DeployListLoader.GristCost cost : entry.cost())
		{
			if(cost.test(getContext(), player))
				return Optional.of(cost);
		}
		return Optional.empty();
	}

	public record MultiblockDeployList(float priority, List<Entry> entries)
	{
		public static final Codec<MultiblockDeployList> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.FLOAT.optionalFieldOf("priority", 0F).forGetter(MultiblockDeployList::priority),
				Entry.CODEC.listOf().fieldOf("entries").forGetter(MultiblockDeployList::entries)
		).apply(instance, MultiblockDeployList::new));
	}

	public record Entry(ResourceLocation structure, Optional<Component> name, Direction facing, Optional<ResourceLocation> icon, int tier, DeployList.EntryLists category, List<DeployListLoader.GristCost> cost)
	{
		private static final Codec<DeployList.EntryLists> CATEGORY_CODEC = StringRepresentable.fromValues(DeployList.EntryLists::values)
				.validate(category -> category == DeployList.EntryLists.ALL
						? DataResult.error(() -> "Cannot add a deploylist entry to all") : DataResult.success(category));

		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("structure").forGetter(Entry::structure),
				ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(Entry::name),
				ItemizedMultiblock.HORIZONTAL_DIRECTION.optionalFieldOf("facing", Direction.NORTH).forGetter(Entry::facing),
				ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(Entry::icon),
				Codec.INT.optionalFieldOf("tier", 0).forGetter(Entry::tier),
				CATEGORY_CODEC.optionalFieldOf("category", DeployList.EntryLists.ATHENEUM).forGetter(Entry::category),
				DeployListLoader.GristCost.LIST_CODEC.optionalFieldOf("cost", List.of()).forGetter(Entry::cost)
		).apply(instance, Entry::new));

		public ItemStack createStack(Optional<Vec3i> size)
		{
			ItemStack stack = new ItemStack(MSEItems.ITEMIZED_MULTIBLOCK.get());
			stack.set(MSEItemComponents.ITEMIZED_MULTIBLOCK.get(), new ItemizedMultiblock(structure, name, facing, size, icon));
			return stack;
		}
	}
}
