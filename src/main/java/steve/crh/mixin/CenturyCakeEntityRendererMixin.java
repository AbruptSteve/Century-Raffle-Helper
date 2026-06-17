package steve.crh.mixin;

import dev.abruptsteve.centuryrafflehelper.highlight.CenturyCakeHighlighter;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class CenturyCakeEntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void crh$applyCenturyCakeOutline(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo ci) {
        int outlineColor = CenturyCakeHighlighter.outlineColorFor(entity, state);
        if (outlineColor != 0) {
            state.outlineColor = outlineColor;
        }
    }
}
