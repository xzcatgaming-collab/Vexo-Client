package dev.vexo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Whole mod in one file: HUD, Right Shift menu, logo. */
public class VexoClient implements ClientModInitializer {
    public static final Text LOGO = Text.literal("\u2726 ").formatted(Formatting.AQUA, Formatting.BOLD);

    // Settings (toggle them in the menu)
    public static boolean showFps = true;
    public static boolean showPing = true;
    public static boolean showServer = true;
    public static boolean showCoords = true;
    public static boolean showLogo = true;

    private static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vexoclient.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.create(Identifier.of("vexoclient", "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new Menu());
            }
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("vexoclient", "hud"), (ctx, tick) -> drawHud(ctx));
    }

    private static void drawHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        List<String> lines = new ArrayList<>();
        if (showFps) lines.add("FPS: " + mc.getCurrentFps());
        if (showPing) {
            PlayerListEntry e = mc.getNetworkHandler() == null ? null
                    : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            lines.add("Ping: " + (mc.isInSingleplayer() || e == null ? "-" : e.getLatency() + " ms"));
        }
        if (showServer) {
            ServerInfo s = mc.getCurrentServerEntry();
            lines.add("Server: " + (mc.isInSingleplayer() ? "Singleplayer" : s != null ? s.address : "Unknown"));
        }
        if (showCoords) {
            lines.add(String.format(Locale.ROOT, "XYZ: %.0f %.0f %.0f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        }
        if (lines.isEmpty()) return;

        int width = mc.textRenderer.getWidth("Vexo Client");
        for (String l : lines) width = Math.max(width, mc.textRenderer.getWidth(l));
        int h = (lines.size() + 1) * 10;
        ctx.fill(2, 2, 4 + width + 4, 4 + h, 0x90000000);
        ctx.drawTextWithShadow(mc.textRenderer, "Vexo Client", 5, 4, 0xFF55FFFF);
        int y = 14;
        for (String l : lines) {
            ctx.drawTextWithShadow(mc.textRenderer, l, 5, y, 0xFFFFFFFF);
            y += 10;
        }
    }

    /** Dropdown menu. Click the header to collapse, click a row to toggle. Esc closes. */
    public static class Menu extends Screen {
        private static boolean open = true;
        private static final int X = 12, Y = 12, W = 150, HEAD = 18, ROW = 15;
        private static final String[] NAMES = {"FPS", "Ping", "Server Address", "Coordinates", "Vexo Logo"};

        public Menu() { super(Text.literal("Vexo Client")); }

        private static boolean get(int i) {
            return switch (i) { case 0 -> showFps; case 1 -> showPing; case 2 -> showServer; case 3 -> showCoords; default -> showLogo; };
        }

        private static void flip(int i) {
            switch (i) { case 0 -> showFps = !showFps; case 1 -> showPing = !showPing; case 2 -> showServer = !showServer;
                         case 3 -> showCoords = !showCoords; default -> showLogo = !showLogo; }
        }

        @Override public boolean shouldPause() { return false; }

        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            super.render(ctx, mx, my, delta);
            ctx.fill(X, Y, X + W, Y + HEAD, 0xFF161A24);
            ctx.fill(X, Y + HEAD - 2, X + W, Y + HEAD, 0xFF55FFFF);
            ctx.drawTextWithShadow(textRenderer, "VEXO CLIENT", X + 6, Y + 5, 0xFF55FFFF);
            ctx.drawTextWithShadow(textRenderer, open ? "-" : "+", X + W - 12, Y + 5, 0xFFFFFFFF);
            if (!open) return;
            for (int i = 0; i < NAMES.length; i++) {
                int y = Y + HEAD + i * ROW;
                boolean hover = mx >= X && mx < X + W && my >= y && my < y + ROW;
                ctx.fill(X, y, X + W, y + ROW, hover ? 0xE0262C3A : 0xE01B2030);
                ctx.drawTextWithShadow(textRenderer, NAMES[i], X + 6, y + 4, 0xFFFFFFFF);
                String s = get(i) ? "ON" : "OFF";
                ctx.drawTextWithShadow(textRenderer, s, X + W - 6 - textRenderer.getWidth(s), y + 4,
                        get(i) ? 0xFF55FF55 : 0xFFFF5555);
            }
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            double mx = click.x(), my = click.y();
            if (click.button() == 0 && mx >= X && mx < X + W) {
                if (my >= Y && my < Y + HEAD) { open = !open; return true; }
                if (open) {
                    for (int i = 0; i < NAMES.length; i++) {
                        int y = Y + HEAD + i * ROW;
                        if (my >= y && my < y + ROW) { flip(i); return true; }
                    }
                }
            }
            return super.mouseClicked(click, doubled);
        }
    }
}
