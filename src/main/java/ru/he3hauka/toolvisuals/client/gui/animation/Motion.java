package ru.he3hauka.toolvisuals.client.gui.animation;

public final class Motion {
 private float value;

 public Motion(float initialValue) {
  value = initialValue;
 }

 public float value() {
  return value;
 }

 public void snap(float next) {
  value = next;
 }

 public float step(float target, float elapsedSeconds, float response) {
  if (elapsedSeconds <= 0) return value;
  float blend = (float) -Math.expm1(-response * elapsedSeconds);
  value += (target - value) * blend;
  if (Math.abs(target - value) < 0.0001f) value = target;
  return value;
 }

 public static float easeOutCubic(float t) {
  float u = 1f - Math.clamp(t, 0f, 1f);
  return 1f - u * u * u;
 }

 public static float easeOutBack(float t) {
  float x = Math.clamp(t, 0f, 1f) - 1f;
  float c = 1.35f;
  return 1f + (c + 1f) * x * x * x + c * x * x;
 }

 public static float stagger(float progress, int index, float delay, float span) {
  return easeOutCubic((progress - index * delay) / Math.max(0.05f, span));
 }
}
