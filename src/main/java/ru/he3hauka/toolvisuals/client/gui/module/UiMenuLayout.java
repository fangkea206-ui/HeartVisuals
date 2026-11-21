package ru.he3hauka.toolvisuals.client.gui.module;

public final class UiMenuLayout {
 public final int x, y, width, height, moduleWidth, paneWidth, sidebarWidth;
 public final boolean compact;

 public UiMenuLayout(int viewportWidth, int viewportHeight, boolean settings) {
  int available = Math.max(260, viewportWidth - 32);
  int main = Math.min(760, available), pane = 0;
  compact = settings && available < 940;
  if (settings && !compact) {
   pane = 350;
   main = Math.min(740, available - pane - 28);
  }
  moduleWidth = main;
  sidebarWidth = main >= 520 ? 152 : 122;
  width = settings && !compact ? main + pane + 28 : main;
  height = Math.max(260, Math.min(settings ? 572 : 512, viewportHeight - 32));
  paneWidth = compact ? width - 36 : pane;
  x = (viewportWidth - width) / 2;
  y = (viewportHeight - height) / 2;
 }
}
