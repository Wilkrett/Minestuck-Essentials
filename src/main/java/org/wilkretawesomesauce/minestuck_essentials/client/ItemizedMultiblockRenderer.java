package org.wilkretawesomesauce.minestuck_essentials.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.wilkretawesomesauce.minestuck_essentials.deploy.MultiblockPreview;

import java.util.Optional;

public class ItemizedMultiblockRenderer extends BlockEntityWithoutLevelRenderer
{
	public ItemizedMultiblockRenderer()
	{
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
	{
		Optional<MultiblockIcons.Icon> icon = MultiblockIcons.get(stack);
		if(icon.isPresent())
			renderIcon(icon.get(), poseStack, buffer, packedLight, packedOverlay);
		else
			MultiblockPreviews.get(stack).ifPresent(preview -> renderPreview(preview, poseStack, buffer, packedLight, packedOverlay));
	}

	private static void renderIcon(MultiblockIcons.Icon icon, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
	{
		int longest = Math.max(icon.width(), icon.height());
		float width = (float) icon.width() / longest;
		float height = (float) icon.height() / longest;
		float minX = (1F - width) / 2F;
		float minY = (1F - height) / 2F;

		VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(icon.texture()));
		PoseStack.Pose pose = poseStack.last();
		vertex(consumer, pose, minX, minY, 0F, 1F, packedLight, packedOverlay);
		vertex(consumer, pose, minX + width, minY, 1F, 1F, packedLight, packedOverlay);
		vertex(consumer, pose, minX + width, minY + height, 1F, 0F, packedLight, packedOverlay);
		vertex(consumer, pose, minX, minY + height, 0F, 0F, packedLight, packedOverlay);
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v, int light, int overlay)
	{
		consumer.addVertex(pose, x, y, 0.5F).setColor(-1).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, 0F, 0F, 1F);
	}

	private static void renderPreview(MultiblockPreview preview, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay)
	{
		Vec3i size = preview.size();
		float scale = 1F / Math.max(size.getX(), Math.max(size.getY(), size.getZ()));
		BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

		poseStack.pushPose();
		poseStack.translate(0.5F, 0.5F, 0.5F);
		poseStack.scale(scale, scale, scale);
		poseStack.translate(-size.getX() / 2F, -size.getY() / 2F, -size.getZ() / 2F);
		for(MultiblockPreview.PreviewBlock block : preview.blocks())
		{
			poseStack.pushPose();
			poseStack.translate(block.pos().getX(), block.pos().getY(), block.pos().getZ());
			blockRenderer.renderSingleBlock(block.state(), poseStack, buffer, packedLight, packedOverlay, ModelData.EMPTY, null);
			poseStack.popPose();
		}
		poseStack.popPose();
	}
}
