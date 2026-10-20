package dev.vexo.mixin;

import dev.vexo.VexoClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void vexo$logo(CallbackInfoReturnable<Text> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if ((Object) this instanceof PlayerEntity p && VexoClient.showLogo
                && mc.player != null && p.getUuid().equals(mc.player.getUuid())) {
            cir.setReturnValue(Text.empty().append(VexoClient.LOGO).append(cir.getReturnValue()));
        }
    }
}
