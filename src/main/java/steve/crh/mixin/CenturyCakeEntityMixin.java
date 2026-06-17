package steve.crh.mixin;

import dev.abruptsteve.centuryrafflehelper.highlight.CenturyCakeHighlighter;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class CenturyCakeEntityMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void crh$getCenturyCakeTeamColor(CallbackInfoReturnable<Integer> cir) {
        int outlineRgb = CenturyCakeHighlighter.outlineRgbFor((Entity) (Object) this);
        if (outlineRgb != 0) {
            cir.setReturnValue(outlineRgb);
        }
    }
}
