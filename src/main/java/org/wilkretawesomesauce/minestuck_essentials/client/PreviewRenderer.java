package org.wilkretawesomesauce.minestuck_essentials.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.wilkretawesomesauce.minestuck_essentials.Minestuck_essentials;
import org.wilkretawesomesauce.minestuck_essentials.item.ItemizedMultiblock;
import org.wilkretawesomesauce.minestuck_essentials.item.ItemizedMultiblockItem;
import org.wilkretawesomesauce.minestuck_essentials.item.ItemizerSelection;
import org.wilkretawesomesauce.minestuck_essentials.item.MSEItemComponents;
import org.wilkretawesomesauce.minestuck_essentials.item.MSEItems;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = Minestuck_essentials.MODID, value = Dist.CLIENT)
public final class PreviewRenderer
{
	private static final int ROOM_CHECK_INTERVAL = 10;
	@Nullable
	private static BoundingBox checkedBox;
	private static long checkedTime;
	private static boolean hasRoom;

	private PreviewRenderer()
	{
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event)
	{
		if(event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if(mc.level == null || player == null)
			return;

		InteractionHand itemizerHand = heldHand(player, MSEItems.ITEMIZER.get());
		if(itemizerHand != null)
		{
			renderSelection(event, mc, player.getItemInHand(itemizerHand));
			return;
		}
		InteractionHand multiblockHand = heldHand(player, MSEItems.ITEMIZED_MULTIBLOCK.get());
		if(multiblockHand != null)
			renderPlacement(event, mc, player, multiblockHand);
	}

	private static void renderSelection(RenderLevelStageEvent event, Minecraft mc, ItemStack itemizer)
	{
		ItemizerSelection selection = itemizer.get(MSEItemComponents.ITEMIZER_SELECTION.get());
		if(selection == null || selection.dimension() != mc.level.dimension())
			return;

		BlockPos end = selection.end().orElseGet(() -> lookedAtBlock(mc) instanceof BlockHitResult hit ? hit.getBlockPos() : selection.start());
		BoundingBox area = selection.area(end);
		int maxSize = StructureBlockEntity.MAX_SIZE_PER_AXIS;
		boolean valid = area.getXSpan() <= maxSize && area.getYSpan() <= maxSize && area.getZSpan() <= maxSize;
		renderBox(event, mc, area, valid, selection.isComplete() ? 1F : 0.6F);
	}

	private static void renderPlacement(RenderLevelStageEvent event, Minecraft mc, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		ItemizedMultiblock itemized = stack.get(MSEItemComponents.ITEMIZED_MULTIBLOCK.get());
		if(itemized == null || itemized.size().isEmpty() || !(lookedAtBlock(mc) instanceof BlockHitResult hit))
			return;

		BlockPlaceContext context = new BlockPlaceContext(player, hand, stack, hit);
		Direction facing = context.getHorizontalDirection();
		BoundingBox box = ItemizedMultiblockItem.placementBox(itemized.size().get(), itemized.rotationTowards(facing), context.getClickedPos(), facing);

		long time = mc.level.getGameTime();
		if(!box.equals(checkedBox) || time - checkedTime >= ROOM_CHECK_INTERVAL)
		{
			hasRoom = ItemizedMultiblockItem.canPlaceIn(context, box);
			checkedBox = box;
			checkedTime = time;
		}
		renderBox(event, mc, box, hasRoom, 1F);
	}

	@Nullable
	private static InteractionHand heldHand(Player player, Item item)
	{
		if(player.getMainHandItem().is(item))
			return InteractionHand.MAIN_HAND;
		return player.getOffhandItem().is(item) ? InteractionHand.OFF_HAND : null;
	}

	@Nullable
	private static HitResult lookedAtBlock(Minecraft mc)
	{
		return mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK ? mc.hitResult : null;
	}

	private static void renderBox(RenderLevelStageEvent event, Minecraft mc, BoundingBox area, boolean valid, float alpha)
	{
		Vec3 camPos = event.getCamera().getPosition();
		AABB box = AABB.of(area).move(-camPos.x, -camPos.y, -camPos.z);

		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
		LevelRenderer.renderLineBox(event.getPoseStack(), consumer, box, valid ? 0F : 1F, valid ? 1F : 0F, 0F, alpha);
		bufferSource.endBatch(RenderType.lines());
	}
}
