package steve.crh.mixin;

import dev.abruptsteve.centuryrafflehelper.highlight.CenturyCakeHighlighter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class CenturyCakeMinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void crh$showCenturyCakeTeamGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (CenturyCakeHighlighter.shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }
}
