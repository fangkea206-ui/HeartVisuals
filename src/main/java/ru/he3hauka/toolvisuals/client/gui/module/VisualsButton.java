package ru.he3hauka.toolvisuals.client.gui.module;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import ru.he3hauka.toolvisuals.client.ToolVisualsClient;
import ru.he3hauka.toolvisuals.client.gui.animation.Motion;
import ru.he3hauka.toolvisuals.client.gui.render.UiPaint;
import ru.he3hauka.toolvisuals.client.gui.surface.UiPalette;
import ru.he3hauka.toolvisuals.client.localization.UiI18n;
import ru.he3hauka.toolvisuals.client.module.VisualModule;
import ru.he3hauka.toolvisuals.client.module.VisualSetting;

public class VisualsButton extends Button {
 public enum Kind { TAB, CHIP, GHOST, ICON, SWITCH, OPTION, MODULE, MENU }
 final Kind kind;
 final UiPaint.Icon icon;
 public final Component detail;
 boolean moduleSound, muteClick, stackedTab, sidebarTab;
 private final BooleanSupplier selected;
 private final Motion hover = new Motion(0);
 private final Motion selection;
 private UiPalette palette = new UiPalette(0);
 private float opacity = 1, elapsed, hoveredFor;

 public VisualsButton(int x, int y, int width, int height, Component label, Kind kind, UiPaint.Icon icon,
 Component detail, Runnable action, BooleanSupplier selected) {
  super(x, y, width, height, label, button -> action.run(), DEFAULT_NARRATION);
  this.kind = kind;
  moduleSound = kind == Kind.MODULE;
  this.icon = icon;
  this.detail = detail;
  this.selected = selected;
  selection = new Motion(selected != null && selected.getAsBoolean() ? 1 : 0);
 }

 public void frame(UiPalette palette, float opacity, float elapsed) {
  this.palette = palette;
  this.opacity = opacity;
  this.elapsed = elapsed;
 }

 @Override
 public void playDownSound(SoundManager manager) {
  var config = ToolVisualsClient.CONFIG;
  if (muteClick || moduleSound && config.active(VisualModule.SOUND_CUSTOMIZER)
   && config.flag(VisualSetting.SOUND_MODULES)) return;
  super.playDownSound(manager);
 }

 public boolean showsHint() {
  return visible && isHovered() && hoveredFor > 0.65f && !detail.getString().isEmpty();
 }

 public static void drawMirrorToggle(GuiGraphicsExtractor g, float x, float y, float on, UiPalette palette, float a) {
  float t = Math.clamp(on, 0f, 1f);
  if (t > 0.01f) {
   UiPaint.rounded(g, x - 1.5f, y - 1.5f, 33, 19, 9.5f, UiPaint.alpha(palette.mirrorEdge, a * t * 0.28f));
  }
  int border = UiPalette.mix(palette.border, palette.accentText, t * 0.85f);
  int track = UiPalette.mix(palette.field, palette.accentText, t * 0.92f);
  UiPaint.rounded(g, x, y, 30, 16, 8, UiPaint.alpha(border, a));
  UiPaint.rounded(g, x + 1, y + 1, 28, 14, 7, UiPaint.alpha(track, a));
  UiPaint.rounded(g, x + 3, y + 1.5f, 24, 3.5f, 1.75f, UiPaint.alpha(palette.glassTop, a * (1f - t * 0.45f)));
  float kx = x + 3 + t * 14f;
  int knob = UiPalette.mix(palette.muted, palette.field, t);
  UiPaint.rounded(g, kx, y + 3, 10, 10, 5, UiPaint.alpha(knob, a));
 }

 @Override
 protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
  isHovered = UiPaint.hovered(graphics, this, mouseX, mouseY);
  boolean chosen = selected != null && selected.getAsBoolean();
  UiPaint.Icon image = icon == UiPaint.Icon.STAR && chosen ? UiPaint.Icon.STAR_FILLED : icon;
  float h = hover.step(isHoveredOrFocused() ? 1 : 0, elapsed, 18);
  float on = selection.step(chosen ? 1 : 0, elapsed, 20);
  hoveredFor = isHovered() ? hoveredFor + elapsed : 0;
  float a = opacity * (active ? 1 : 0.4f);
  int x = getX(), y = getY(), w = getWidth(), height = getHeight();

  if (kind == Kind.SWITCH) {
   drawMirrorToggle(graphics, x + (w - 30) / 2f, y + (height - 16) / 2f, on, palette, a);
   return;
  }

  if (kind != Kind.MODULE) {
   int base = kind == Kind.OPTION ? palette.field : kind == Kind.TAB ? palette.inset : palette.button;
   int fill = UiPalette.mix(base, palette.hover, h * 0.75f);
   fill = UiPalette.mix(fill, palette.selected, on);
   float strength = kind == Kind.GHOST ? Math.max(h * 0.9f, on * 0.85f)
    : kind == Kind.ICON ? Math.max(h * 0.9f, on * 0.7f)
    : kind == Kind.TAB ? Math.max(on, h * 0.5f) : 1f;
   if (strength > 0.01f) {
    float radius = kind == Kind.ICON ? 8f : 7f;
    int edge = UiPalette.mix(palette.subtleBorder, palette.mirrorEdge, Math.max(h * 0.55f, on * 0.9f));
    UiPaint.rounded(graphics, x, y, w, height, radius, UiPaint.alpha(edge, a * strength));
    UiPaint.rounded(graphics, x + 1, y + 1, w - 2, height - 2, Math.max(1f, radius - 1f), UiPaint.alpha(fill, a * strength));
    if (height >= 20 && w >= 24) {
     UiPaint.rounded(graphics, x + 3, y + 1.5f, w - 6, Math.min(7f, height * 0.3f), 3f,
      UiPaint.alpha(palette.glassTop, a * strength * (0.45f + h * 0.35f + on * 0.2f)));
    }
   }
  }

  int foreground = kind == Kind.GHOST || kind == Kind.TAB || kind == Kind.ICON
   ? UiPalette.mix(palette.muted, palette.accentText, Math.max(on, h))
   : palette.text;
  if (kind == Kind.MODULE || kind == Kind.OPTION) {
   foreground = UiPalette.mix(palette.text, palette.accentText, on);
  }

  if (kind == Kind.ICON) {
   UiPaint.icon(graphics, image, x + (w - 15) / 2f, y + (height - 15) / 2f, 15,
    icon == UiPaint.Icon.STAR && chosen ? palette.accentText : foreground, a);
   return;
  }

  if (kind == Kind.TAB && sidebarTab) {
   if (on > 0.01f) {
    float barH = Math.max(6f, (height - 12f) * on);
    UiPaint.rounded(graphics, x + 2.5f, y + (height - barH) / 2f, 2.5f, barH, 1.25f,
     UiPaint.alpha(palette.accentText, a * on));
   }
   float slide = Math.max(h * 0.45f, on) * 3.5f;
   float tx = x + 12f + slide;
   if (image != null) {
    UiPaint.icon(graphics, image, tx, y + (height - 13f) / 2f, 13f, foreground, a);
    tx += 20f;
   }
   String label = UiPaint.fit(graphics, getMessage(), Math.max(10f, x + w - tx - 8f), 0.88f);
   UiPaint.baseline(graphics, label, tx, UiPaint.textBaseline(graphics, y, height, 0.88f), 0.88f, foreground, a);
   return;
  }

  if (kind == Kind.TAB && icon != null) {
   float scale = stackedTab ? 0.8f : 0.85f, size = stackedTab ? 11 : 13, gap = 5;
   String label = UiPaint.fit(graphics, getMessage(), Math.max(10f, w - (stackedTab ? 10 : size + gap + 12)), scale);
   if (stackedTab) {
    UiPaint.icon(graphics, image, x + (w - size) / 2f, y + 3, size, foreground, a);
    UiPaint.centered(graphics, label, x, y + 15, w, height - 16, scale, foreground, a);
   } else {
    float left = x + (w - size - gap - UiPaint.textWidth(graphics, label, scale)) / 2f;
    UiPaint.icon(graphics, image, left, y + (height - size) / 2f, size, foreground, a);
    UiPaint.baseline(graphics, label, left + size + gap,
     UiPaint.textBaseline(graphics, y, height, scale), scale, foreground, a);
   }
   return;
  }

  float tx = x + 10;
  float labelScale = kind == Kind.CHIP || kind == Kind.GHOST ? 0.85f : 0.9f;
  if (icon == null && (kind == Kind.TAB || kind == Kind.CHIP || kind == Kind.GHOST)) {
   float scale = kind == Kind.TAB ? 0.8f : labelScale;
   UiPaint.centered(graphics, UiPaint.fit(graphics, getMessage(), Math.max(10f, w - 8), scale), x, y, w, height, scale, foreground, a);
   return;
  }

  int reserve = kind == Kind.OPTION || kind == Kind.MENU ? 46 : 10;
  if (kind == Kind.MODULE) {
   if (on > 0.01f) {
    float barH = 38f * on;
    UiPaint.rounded(graphics, x + 3f, y + (height - barH) / 2f, 2.5f, barH, 1.25f,
     UiPaint.alpha(palette.accentText, a * on));
   }
   tx = x + 13f + h * 2.5f;
   labelScale = 0.95f;
   reserve = 48;
  } else if (icon != null) {
   UiPaint.icon(graphics, image, x + 10, y + (height - 13) / 2f, 13, foreground, a);
   tx += 20;
  }
  if (kind == Kind.OPTION) tx = x + 9;

  String label = UiPaint.fit(graphics, getMessage(), Math.max(10f, x + w - tx - reserve), labelScale);
  float baseline = UiPaint.textBaseline(graphics, y, kind == Kind.MODULE ? 35 : height, labelScale);
  UiPaint.baseline(graphics, label, tx, baseline, labelScale, foreground, a);
  if (kind == Kind.MODULE) {
   UiPaint.baseline(graphics, UiPaint.fit(graphics, detail, Math.max(10f, w - 24), 0.78f), tx,
    UiPaint.textBaseline(graphics, y + 34, 24, 0.78f), 0.78f, palette.muted, a);
  }
  if ((kind == Kind.MODULE && selected != null) || kind == Kind.OPTION) {
   drawMirrorToggle(graphics, x + w - 39, y + ((kind == Kind.MODULE ? 35 : height) - 16) / 2f, on, palette, a);
  }
  if (kind == Kind.MENU && on > 0.01f) {
   UiPaint.icon(graphics, UiPaint.Icon.CHECK, x + w - 25, y + (height - 14) / 2f, 14, palette.accentText, a * on);
  }
 }

 @Override
 protected MutableComponent createNarrationMessage() {
  if (selected != null) return UiI18n.tr("toolvisuals.toggle_narration", getMessage(),
   UiI18n.tr(selected.getAsBoolean() ? "toolvisuals.ui.on" : "toolvisuals.ui.off"));
  return super.createNarrationMessage();
 }
        }
