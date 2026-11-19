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

    public UiPalette(float light) {
        panel = mix(0xf008080b, 0xf5f7f8fa, light);
        hudPanel = mix(0xcc09090d, 0xebf5f7fa, light);
        raised = mix(0xff131318, 0xffffffff, light);
        hover = mix(0xff23232b, 0xffe8eaef, light);
        border = mix(0x5cffffff, 0x40000000, light);
        subtleBorder = mix(0x26ffffff, 0x1f000000, light);
        text = mix(0xffffffff, 0xff09090c, light);
        muted = mix(0xffb8bcc8, 0xff4a4e59, light);
        faint = mix(0xff7e8390, 0xff767b87, light);
        accent = mix(0xfff4f7ff, 0xff0d0d11, light);
        accentText = mix(0xffffffff, 0xff000000, light);
        track = mix(0xff2f3039, 0xffc6cad4, light);
        field = mix(0xff050507, 0xffeff1f5, light);
        inset = mix(0xff0e0e13, 0xffe9ecf2, light);
        button = mix(0xff1a1a21, 0xffe2e5ec, light);
        selected = mix(0x38ffffff, 0x26000000, light);
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
