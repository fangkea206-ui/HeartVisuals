package ru.he3hauka.toolvisuals.client.gui.surface;

public final class UiPalette {
 public final int panel;
 public final int hudPanel;
 public final int raised;
 public final int hover;
 public final int border;
 public final int text;
 public final int muted;
 public final int faint;
 public final int accent;
 public final int accentText;
 public final int track;
 public final int field;
 public final int inset;
 public final int button;
 public final int selected;
 public final int subtleBorder;
 public final int mirrorEdge;
 public final int mirrorSheen;
 public final int glassTop;

 public UiPalette(float light) {
  panel = mix(0xe6070709, 0xedf2f3f6, light);
  hudPanel = mix(0xd209090c, 0xe4f7f8fa, light);
  raised = mix(0xeb121216, 0xf7ffffff, light);
  hover = mix(0xf5222229, 0xf7dedee6, light);
  border = mix(0xff42424c, 0xffb6b6c2, light);
  subtleBorder = mix(0xff23232a, 0xffd5d5df, light);
  text = mix(0xfffafaff, 0xff0c0c10, light);
  muted = mix(0xffa4a4b0, 0xff4e4e5a, light);
  faint = mix(0xff6e6e7a, 0xff787886, light);
  accent = mix(0xffc8c8d4, 0xff1c1c24, light);
  accentText = mix(0xffffffff, 0xff09090d, light);
  track = mix(0xff34343e, 0xffbebec9, light);
  field = mix(0xe8050507, 0xf2fafafd, light);
  inset = mix(0xd90c0c10, 0xe8e7e8ee, light);
  button = mix(0xeb18181f, 0xf0e4e4ec, light);
  selected = mix(0xf5282831, 0xf7d2d2dc, light);
  mirrorEdge = mix(0x66ffffff, 0xccffffff, light);
  mirrorSheen = mix(0x1effffff, 0x4dffffff, light);
  glassTop = mix(0x14ffffff, 0x33ffffff, light);
 }

 public static int mix(int from, int to, float amount) {
  float t = Math.max(0, Math.min(1, amount));
  int result = 0;
  for (int shift = 0; shift <= 24; shift += 8) {
   int a = (from >>> shift) & 255;
   int b = (to >>> shift) & 255;
   result |= Math.round(a + (b - a) * t) << shift;
  }
  return result;
 }
}
