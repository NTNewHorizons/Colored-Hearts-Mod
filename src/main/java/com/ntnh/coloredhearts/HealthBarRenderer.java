package com.ntnh.coloredhearts;

import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.HEALTH;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import com.hbm.items.armor.ArmorHEV;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class HealthBarRenderer extends Gui {

    // -------------------------------------------------------------------------
    // Mod compatibility flags
    // -------------------------------------------------------------------------
    private static final boolean isRpghudLoaded = Loader.isModLoaded("rpghud");
    private static final boolean isTukmc_vzLoaded = Loader.isModLoaded("tukmc_Vz");
    private static final boolean isBorderlandsLoaded = Loader.isModLoaded("borderlands");
    private static final boolean isHbmLoaded = Loader.isModLoaded("hbm");

    // -------------------------------------------------------------------------
    // Resources
    // -------------------------------------------------------------------------
    private static final ResourceLocation COLORED_HEARTS = new ResourceLocation(
        "coloredhearts",
        "textures/gui/newhearts.png");

    private static final Minecraft mc = Minecraft.getMinecraft();

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------
    private final Random rand = new Random();
    private int updateCounter = 0;

    // =========================================================================
    // Compatibility helpers
    // =========================================================================

    /** Returns true when the player is wearing any HEV chestplate (HBM). */
    private boolean isWearingHEVArmor(EntityPlayer player) {
        try {
            ItemStack chest = player.inventory.armorInventory[2];
            return chest != null && chest.getItem() instanceof ArmorHEV;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================================
    // Main overlay renderer
    // =========================================================================
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void renderHealthbar(RenderGameOverlayEvent.Pre event) {
        if (event.type != HEALTH) return;
        updateCounter++;

        // --- Mod compatibility bailouts ---
        if (isRpghudLoaded) return;
        if (isTukmc_vzLoaded && !isBorderlandsLoaded) return;

        // Let HBM's HEV suit render its own overlay — do NOT cancel the event
        if (isHbmLoaded && isWearingHEVArmor(mc.thePlayer)) return;

        // --- Setup ---
        mc.mcProfiler.startSection("health");
        GL11.glEnable(GL11.GL_BLEND);

        final int width = event.resolution.getScaledWidth();
        final int height = event.resolution.getScaledHeight();
        final int xBase = width / 2 - 91;
        int yBase = height - 39;

        // Damage-highlight blink
        boolean highlight = mc.thePlayer.hurtResistantTime / 3 % 2 == 1;
        if (mc.thePlayer.hurtResistantTime < 10) highlight = false;

        // Health values
        final IAttributeInstance attrMax = mc.thePlayer.getEntityAttribute(SharedMonsterAttributes.maxHealth);
        final int health = MathHelper.ceiling_float_int(mc.thePlayer.getHealth());
        final int healthLast = MathHelper.ceiling_float_int(mc.thePlayer.prevHealth);
        final float healthMax = Math.min(20F, (float) attrMax.getAttributeValue());
        float absorb = mc.thePlayer.getAbsorptionAmount();

        // Row layout
        final int healthRows = MathHelper.ceiling_float_int((healthMax + absorb) / 2.0F / 10.0F);
        final int rowHeight = Math.max(10 - (healthRows - 2), 3);

        rand.setSeed(updateCounter * 312871L);

        int left = xBase;
        int top = height - GuiIngameForge.left_height;
        if (!GuiIngameForge.renderExperiance) {
            top += 7;
            yBase += 7;
        }

        // -----------------------------------------------------------------------
        // Regeneration bounce — vanilla / TConstruct style:
        // one heart index bounces per tick, cycling through 0-9.
        // -----------------------------------------------------------------------
        int regenBounceIndex = -1;
        if (mc.thePlayer.isPotionActive(Potion.regeneration)) {
            regenBounceIndex = updateCounter % 10;
        }

        // Texture offsets
        final boolean hardcore = mc.theWorld.getWorldInfo()
            .isHardcoreModeEnabled();
        final int TOP = hardcore ? 9 * 5 : 0;
        final int tinkerBase = hardcore ? 27 : 0;
        final int BACKGROUND = highlight ? 25 : 16;

        int MARGIN = 16;
        int coloredHeartsY = tinkerBase;
        if (mc.thePlayer.isPotionActive(Potion.poison)) {
            MARGIN += 36;
            coloredHeartsY = 9 + tinkerBase;
        } else if (mc.thePlayer.isPotionActive(Potion.wither)) {
            MARGIN += 72;
            coloredHeartsY = 18 + tinkerBase;
        }

        // -----------------------------------------------------------------------
        // Pass 1 — vanilla heart icons (background + fill)
        // -----------------------------------------------------------------------
        for (int i = MathHelper.ceiling_float_int((healthMax + absorb) / 2.0F) - 1; i >= 0; --i) {
            final int row = MathHelper.ceiling_float_int((float) (i + 1) / 10.0F) - 1;
            int x = left + i % 10 * 8;
            int y = top - row * rowHeight;

            if (health <= 4) y += rand.nextInt(3) - 1; // low-health shake
            if (i == regenBounceIndex) y -= 2; // regen bounce

            // Empty heart outline
            drawTexturedModalRect(x, y, BACKGROUND, TOP, 9, 9);

            // Damage-highlight ghost hearts
            if (highlight) {
                if (i * 2 + 1 < healthLast) drawTexturedModalRect(x, y, MARGIN + 54, TOP, 9, 9);
                else if (i * 2 + 1 == healthLast) drawTexturedModalRect(x, y, MARGIN + 63, TOP, 9, 9);
            }

            // Absorption or actual health fill
            if (absorb > 0.0F) {
                if (absorb % 2.0F == 1.0F) drawTexturedModalRect(x, y, MARGIN + 153, TOP, 9, 9); // half absorption
                else drawTexturedModalRect(x, y, MARGIN + 144, TOP, 9, 9); // full absorption
                absorb -= 2.0F;
            } else if (i * 2 + 1 + 20 >= health) {
                if (i * 2 + 1 < health) drawTexturedModalRect(x, y, MARGIN + 36, TOP, 9, 9); // full heart
                else if (i * 2 + 1 == health) drawTexturedModalRect(x, y, MARGIN + 45, TOP, 9, 9); // half heart
            }
        }

        // -----------------------------------------------------------------------
        // Pass 2 — colored extra hearts (health > 20)
        // -----------------------------------------------------------------------
        if (health > 20) {
            mc.getTextureManager()
                .bindTexture(COLORED_HEARTS);

            // Render up to 2 layers at a time to avoid visual clutter
            for (int layer = Math.max(0, health / 20 - 2); layer < health / 20; layer++) {
                final int heartsInLayer = Math.min(10, (health - 20 * (layer + 1)) / 2);
                // Wrap texture column into 0-10 range
                final int textureCol = layer % 11;
                // Glow overlay logic for HP between 241-260
                final int overlayCount = (health > 240 && health <= 260) ? health - 240 : 0;

                for (int j = 0; j < heartsInLayer; j++) {
                    int y = 0;
                    if (health <= 4) y += rand.nextInt(3) - 1;
                    if (j == regenBounceIndex) y -= 2;

                    if ((layer + 1) * 20 + j * 2 + 21 >= health) {
                        drawColoredHeart(
                            xBase + 8 * j,
                            yBase + y,
                            textureCol,
                            coloredHeartsY,
                            health,
                            overlayCount,
                            j,
                            false);
                    }
                }

                // Half-heart remainder
                if (health % 2 == 1 && heartsInLayer < 10) {
                    int y = 0;
                    if (health <= 4) y += rand.nextInt(3) - 1;
                    if (heartsInLayer == regenBounceIndex) y -= 2;

                    drawColoredHeart(
                        xBase + 8 * heartsInLayer,
                        yBase + y,
                        textureCol,
                        coloredHeartsY,
                        health,
                        overlayCount,
                        heartsInLayer,
                        true);
                }
            }

            mc.getTextureManager()
                .bindTexture(Gui.icons);
        }

        // -----------------------------------------------------------------------
        // Finish up
        // -----------------------------------------------------------------------
        GuiIngameForge.left_height += 10;
        if (mc.thePlayer.getAbsorptionAmount() > 0) GuiIngameForge.left_height += 10;

        GL11.glDisable(GL11.GL_BLEND);
        mc.mcProfiler.endSection();

        event.setCanceled(true);
        MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Post(event, HEALTH));
    }

    // =========================================================================
    // Helper — draws one full or half colored heart, handling the >240 HP glow
    // =========================================================================
    private void drawColoredHeart(int x, int y, int textureCol, int textureRow, int health, int overlayCount,
        int heartIndex, boolean half) {
        final int u = half ? 9 + 18 * textureCol : 18 * textureCol;

        if (health <= 240) {
            // Normal colored heart
            drawTexturedModalRect(x, y, u, textureRow, 9, 9);
        } else {
            // Overflow base — use last texture column
            final int overflowU = half ? 9 + 18 * 10 : 18 * 10;
            drawTexturedModalRect(x, y, overflowU, textureRow, 9, 9);

            if (health <= 260) {
                // Partial glow overlay
                final int fullGlows = overlayCount / 2;
                final boolean halfGlow = (overlayCount % 2) == 1;
                if (!half && heartIndex < fullGlows) {
                    drawTexturedModalRect(x, y, 0, 54, 9, 9);
                } else if (half && heartIndex == fullGlows && halfGlow) {
                    drawTexturedModalRect(x, y, 9, 54, 9, 9);
                }
            } else {
                // Full glow overlay
                final int glowU = half ? 9 + 18 * textureCol : 18 * textureCol;
                drawTexturedModalRect(x, y, glowU, 54, 9, 9);
            }
        }
    }
}
