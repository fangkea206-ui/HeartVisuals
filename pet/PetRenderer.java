package ru.he3hauka.toolvisuals.client.cosmetic.pet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import ru.he3hauka.toolvisuals.client.ToolVisualsClient;
import ru.he3hauka.toolvisuals.client.animation.EffectTime;
import ru.he3hauka.toolvisuals.client.cosmetic.accessory.render.AccessoryPipeline;
import ru.he3hauka.toolvisuals.client.effects.render.EffectPipeline;

import static ru.he3hauka.toolvisuals.client.module.VisualModule.PETS;
import static ru.he3hauka.toolvisuals.client.module.VisualSetting.*;

public final class PetRenderer {
 @FunctionalInterface
 private interface VertexSink {
  void vertex(float x, float y, float z, int color);
 }

 private record Frame(
  Vec3 pos, Vec3 camera, float yaw, float pitch, float walk, float move,
  float scale, float speed, double seconds, int kind, int mode
 ) {}

 private static volatile Frame frame;
 private static ClientLevel lastLevel;
 private static Vec3 prevPos, currPos;
 private static float prevYaw, currYaw, prevPitch, currPitch, prevWalk, currWalk, prevMove, currMove;
 private static boolean initialized;

 private PetRenderer() {}

 public static void clear() {
  frame = null;
  initialized = false;
  lastLevel = null;
 }

 public static void tick(Minecraft client) {
  var config = ToolVisualsClient.CONFIG;
  if (!config.active(PETS) || client.player == null || client.level == null) {
   clear();
   return;
  }
  if (client.level != lastLevel) {
   clear();
   lastLevel = client.level;
  }
  if (client.isPaused()) return;

  int kind = (int) config.value(PET_KIND);
  int mode = (int) config.value(PET_MODE);
  float speedMult = config.number(PET_SPEED);

  var player = client.player;
  Vec3 p = player.position();
  double yawRad = Math.toRadians(player.getYRot());
  double fx = -Math.sin(yawRad), fz = Math.cos(yawRad);
  double rx = -fz, rz = fx;

  double tx, ty, tz;
  if (mode == 0) {
   // Над правым плечом
   tx = p.x + rx * 0.62 - fx * 0.28;
   tz = p.z + rz * 0.62 - fz * 0.28;
   ty = p.y + player.getBbHeight() * 0.84;
  } else {
   // Бежит / летит рядом
   tx = p.x + rx * 0.95 - fx * 1.15;
   tz = p.z + rz * 0.95 - fz * 1.15;
   boolean flyingPet = kind == 0 || kind == 3 || kind == 4;
   if (flyingPet) {
    ty = p.y + 0.55;
   } else {
    ty = findGroundY(client.level, tx, p.y, tz);
   }
  }

  Vec3 target = new Vec3(tx, ty, tz);
  float playerFaceYaw = (float) Math.atan2(fx, fz);

  if (!initialized || currPos.distanceToSqr(target) > 64.0) {
   prevPos = currPos = target;
   prevYaw = currYaw = playerFaceYaw;
   prevPitch = currPitch = 0f;
   prevWalk = currWalk = 0f;
   prevMove = currMove = 0f;
   initialized = true;
   return;
  }

  prevPos = currPos;
  prevYaw = currYaw;
  prevPitch = currPitch;
  prevWalk = currWalk;
  prevMove = currMove;

  double followRate = mode == 0 ? 0.30 : 0.16;
  currPos = currPos.lerp(target, followRate);

  double dx = currPos.x - prevPos.x;
  double dz = currPos.z - prevPos.z;
  double horizSpeed = Math.hypot(dx, dz);

  float targetMove = (float) Math.clamp(horizSpeed * 4.8, 0.0, 1.0);
  currMove = prevMove + (targetMove - prevMove) * 0.26f;
  currWalk = prevWalk + (float) (horizSpeed * 4.2 * speedMult);

  float targetYaw = horizSpeed > 0.015 ? (float) Math.atan2(dx, dz) : playerFaceYaw;
  currYaw = prevYaw + wrapAngle(targetYaw - prevYaw) * 0.22f;

  float targetPitch = (float) Math.clamp(horizSpeed * 1.15, 0.0, 0.36);
  currPitch = prevPitch + (targetPitch - prevPitch) * 0.25f;
 }

 public static void extract(LevelRenderState state) {
  var client = Minecraft.getInstance();
  var config = ToolVisualsClient.CONFIG;
  if (!initialized || !config.active(PETS) || client.player == null || client.level == null) {
   frame = null;
   return;
  }
  if (!config.flag(PET_FIRST_PERSON) && client.options.getCameraType().isFirstPerson()) {
   frame = null;
   return;
  }

  float pt = state.worldPartialTicks;
  Vec3 pos = prevPos.lerp(currPos, pt);
  float yaw = prevYaw + (currYaw - prevYaw) * pt;
  float pitch = prevPitch + (currPitch - prevPitch) * pt;
  float walk = prevWalk + (currWalk - prevWalk) * pt;
  float move = prevMove + (currMove - prevMove) * pt;
  double seconds = EffectTime.at(state.gameTime, pt) / 20.0;

  frame = new Frame(
   pos, state.cameraRenderState.pos, yaw, pitch, walk, move,
   config.number(PET_SCALE), config.number(PET_SPEED), seconds,
   (int) config.value(PET_KIND), (int) config.value(PET_MODE)
  );
 }

 public static void submit(SubmitNodeCollector collector) {
  var f = frame;
  if (f == null) return;

  var pose = new PoseStack();
  pose.pushPose();
  try {
   pose.translate(f.pos.x - f.camera.x, f.pos.y - f.camera.y, f.pos.z - f.camera.z);
   pose.rotate(Axis.YP, f.yaw);
   float s = f.scale * (f.mode == 0 ? 0.65f : 0.85f);
   pose.scale(s, s, s);

   switch (f.kind) {
    case 0 -> renderShadowFiend(pose, collector, f);
    case 1 -> renderCat(pose, collector, f);
    case 2 -> renderDog(pose, collector, f);
    case 3 -> renderAngel(pose, collector, f);
    case 4 -> renderDemon(pose, collector, f);
    default -> renderShadowFiend(pose, collector, f);
   }
  } finally {
   pose.popPose();
  }
 }

 // ================= 1. SHADOW FIEND (DOTA 2) =================
 private static void renderShadowFiend(PoseStack pose, SubmitNodeCollector collector, Frame f) {
  double t = f.seconds * f.speed;
  float bob = (float) Math.sin(t * 3.2) * 0.065f;
  float sway = (float) Math.sin(t * 2.1) * 0.08f;

  pose.pushPose();
  pose.translate(0, 0.14 + bob, 0);
  pose.rotate(Axis.XP, f.pitch * 0.85f);

  // Теневой вихрь снизу + мощный торс с пылающей грудью
  submit(pose, collector, false, s -> {
   box(s, -0.09f, 0.02f, -0.09f, 0.09f, 0.16f, 0.09f, 0x0d090e, false, 255);
   box(s, -0.14f, 0.16f, -0.12f, 0.14f, 0.34f, 0.12f, 0x140d15, false, 255);
   box(s, -0.22f, 0.34f, -0.15f, 0.22f, 0.68f, 0.15f, 0x191018, false, 255);
   // Шипастые наплечники
   box(s, -0.34f, 0.56f, -0.15f, -0.19f, 0.72f, 0.15f, 0x22121b, false, 255);
   box(s, 0.19f, 0.56f, -0.15f, 0.34f, 0.72f, 0.15f, 0x22121b, false, 255);
   box(s, -0.38f, 0.70f, -0.06f, -0.26f, 0.82f, 0.06f, 0x3b1218, false, 255);
   box(s, 0.26f, 0.70f, -0.06f, 0.38f, 0.82f, 0.06f, 0x3b1218, false, 255);
   // Огненное ядро и рёбра на груди
   box(s, -0.12f, 0.41f, 0.145f, 0.12f, 0.61f, 0.17f, 0xff3b14, true, 255);
   box(s, -0.06f, 0.46f, 0.16f, 0.06f, 0.56f, 0.18f, 0xffba26, true, 255);
   box(s, -0.17f, 0.56f, 0.148f, -0.09f, 0.59f, 0.168f, 0xff4c18, true, 255);
   box(s, 0.09f, 0.56f, 0.148f, 0.17f, 0.59f, 0.168f, 0xff4c18, true, 255);
   box(s, -0.15f, 0.47f, 0.148f, -0.09f, 0.50f, 0.168f, 0xff4c18, true, 255);
   box(s, 0.09f, 0.47f, 0.148f, 0.15f, 0.50f, 0.168f, 0xff4c18, true, 255);
  });

  // Голова с рогами и горящими глазами
  pose.pushPose();
  pose.translate(0, 0.70, 0.02);
  pose.rotate(Axis.YP, sway * 0.6f);
  submit(pose, collector, false, s -> {
   box(s, -0.14f, 0.0f, -0.13f, 0.14f, 0.25f, 0.14f, 0x160e16, false, 255);
   // Глаза и огненная пасть
   box(s, -0.10f, 0.13f, 0.135f, -0.03f, 0.18f, 0.155f, 0xff2a14, true, 255);
   box(s, 0.03f, 0.13f, 0.135f, 0.10f, 0.18f, 0.155f, 0xff2a14, true, 255);
   box(s, -0.07f, 0.04f, 0.135f, 0.07f, 0.08f, 0.15f, 0xff6a1a, true, 255);
   // Изогнутые демонические рога (левый и правый)
   box(s, -0.17f, 0.22f, -0.04f, -0.09f, 0.35f, 0.04f, 0x241016, false, 255);
   box(s, -0.22f, 0.33f, -0.08f, -0.14f, 0.46f, 0.01f, 0x45141a, false, 255);
   box(s, 0.09f, 0.22f, -0.04f, 0.17f, 0.35f, 0.04f, 0x241016, false, 255);
   box(s, 0.14f, 0.33f, -0.08f, 0.22f, 0.46f, 0.01f, 0x45141a, false, 255);
  });
  pose.popPose();

  // Когтистые руки (дышат и отводятся назад при движении)
  for (int side = -1; side <= 1; side += 2) {
   pose.pushPose();
   pose.translate(side * 0.28, 0.60, 0.0);
   pose.rotate(Axis.XP, (float) Math.sin(t * 2.8 + side) * 0.18f - 0.25f - f.move * 0.45f);
   pose.rotate(Axis.ZP, side * (0.22f + (float) Math.sin(t * 2.5) * 0.08f));
   int sd = side;
   submit(pose, collector, false, s -> {
    box(s, -0.055f, -0.30f, -0.055f, 0.055f, 0.0f, 0.055f, 0x170e16, false, 255);
    box(s, -0.045f, -0.39f, -0.03f, 0.045f, -0.29f, 0.06f, 0x5c141c, false, 255);
    box(s, sd * 0.01f - 0.02f, -0.43f, 0.01f, sd * 0.01f + 0.02f, -0.37f, 0.07f, 0xff3a18, true, 255);
   });
   pose.popPose();
  }

  // 3 вращающиеся души вокруг Shadow Fiend
  for (int i = 0; i < 3; i++) {
   double angle = t * 2.6 + i * (Math.TAU / 3.0);
   double ox = Math.cos(angle) * 0.50;
   double oz = Math.sin(angle) * 0.50;
   double oy = 0.48 + Math.sin(t * 3.8 + i * 2.1) * 0.14;
   pose.pushPose();
   pose.translate(ox, oy, oz);
   pose.rotate(Axis.YP, (float) (t * 4.0 + i));
   pose.rotate(Axis.XP, (float) (t * 3.0));
   submit(pose, collector, false, s ->
    box(s, -0.045f, -0.045f, -0.045f, 0.045f, 0.045f, 0.045f, 0xff5a1f, true, 255)
   );
   submit(pose, collector, true, s ->
    box(s, -0.085f, -0.085f, -0.085f, 0.085f, 0.085f, 0.085f, 0xff2210, true, 120)
   );
   pose.popPose();
  }

  pose.popPose();
 }

 // ================= 2. КОШЕЧКА (MINECRAFT) =================
 private static void renderCat(PoseStack pose, SubmitNodeCollector collector, Frame f) {
  double t = f.seconds * f.speed;
  float legSwing = (float) Math.sin(f.walk) * 0.65f * f.move;
  float hoverBob = f.mode == 0 ? (float) Math.sin(t * 3.0) * 0.045f : (float) Math.abs(Math.sin(f.walk)) * 0.03f * f.move;

  pose.pushPose();
  pose.translate(0, hoverBob, 0);

  // Тело (стильная чёрно-белая кошечка с белой грудкой)
  submit(pose, collector, false, s -> {
   box(s, -0.13f, 0.22f, -0.28f, 0.13f, 0.44f, 0.24f, 0x1c1c22, false, 255);
   box(s, -0.09f, 0.21f, 0.04f, 0.09f, 0.36f, 0.25f, 0xf2f2f6, false, 255);
  });

  // Голова с ушками, зелёными глазками и розовым носиком
  pose.pushPose();
  pose.translate(0, 0.40, 0.26);
  pose.rotate(Axis.YP, (float) Math.sin(t * 1.4) * 0.14f);
  pose.rotate(Axis.ZP, (float) Math.sin(t * 0.9) * 0.05f);
  submit(pose, collector, false, s -> {
   box(s, -0.14f, -0.08f, -0.08f, 0.14f, 0.17f, 0.17f, 0x1e1e25, false, 255);
   // Мордочка + розовый нос
   box(s, -0.07f, -0.07f, 0.16f, 0.07f, 0.02f, 0.22f, 0xf5f5f8, false, 255);
   box(s, -0.025f, -0.01f, 0.215f, 0.025f, 0.02f, 0.23f, 0xff8fab, false, 255);
   // Изумрудные глаза
   box(s, -0.11f, 0.03f, 0.168f, -0.04f, 0.08f, 0.178f, 0x4ef088, true, 255);
   box(s, 0.04f, 0.03f, 0.168f, 0.11f, 0.08f, 0.178f, 0x4ef088, true, 255);
   // Ушки с розовой серединкой
   box(s, -0.13f, 0.17f, -0.01f, -0.05f, 0.26f, 0.06f, 0x1c1c22, false, 255);
   box(s, 0.05f, 0.17f, -0.01f, 0.13f, 0.26f, 0.06f, 0x1c1c22, false, 255);
   box(s, -0.11f, 0.18f, 0.055f, -0.07f, 0.23f, 0.066f, 0xff9bb5, false, 255);
   box(s, 0.07f, 0.18f, 0.055f, 0.11f, 0.23f, 0.066f, 0xff9bb5, false, 255);
  });
  pose.popPose();

  // 4 анимированные лапки с белыми носочками
  float[][] legs = {
   {-0.08f, 0.16f, 1f}, {0.08f, 0.16f, -1f},
   {-0.08f, -0.20f, -1f}, {0.08f, -0.20f, 1f}
  };
  for (var leg : legs) {
   pose.pushPose();
   pose.translate(leg[0], 0.23, leg[1]);
   pose.rotate(Axis.XP, legSwing * leg[2]);
   submit(pose, collector, false, s -> {
    box(s, -0.042f, -0.18f, -0.042f, 0.042f, 0.0f, 0.042f, 0x1c1c22, false, 255);
    box(s, -0.044f, -0.23f, -0.044f, 0.044f, -0.17f, 0.044f, 0xf2f2f6, false, 255);
   });
   pose.popPose();
  }

  // Виляющий двухсегментный хвост с белым кончиком
  pose.pushPose();
  pose.translate(0, 0.38, -0.28);
  pose.rotate(Axis.YP, (float) Math.sin(t * 4.0) * 0.35f);
  pose.rotate(Axis.XP, 0.55f + (float) Math.sin(t * 2.5) * 0.12f);
  submit(pose, collector, false, s -> {
   box(s, -0.032f, -0.032f, -0.22f, 0.032f, 0.032f, 0.0f, 0x1c1c22, false, 255);
   box(s, -0.032f, -0.01f, -0.34f, 0.032f, 0.06f, -0.21f, 0xf2f2f6, false, 255);
  });
  pose.popPose();

  pose.popPose();
 }

 // ================= 3. СОБАЧКА (MINECRAFT ВОЛК) =================
 private static void renderDog(PoseStack pose, SubmitNodeCollector collector, Frame f) {
  double t = f.seconds * f.speed;
  float legSwing = (float) Math.sin(f.walk) * 0.65f * f.move;
  float hoverBob = f.mode == 0 ? (float) Math.sin(t * 3.0) * 0.045f : (float) Math.abs(Math.sin(f.walk)) * 0.035f * f.move;

  pose.pushPose();
  pose.translate(0, hoverBob, 0);

  // Тело, пушистая грива и красный ошейник с золотой биркой
  submit(pose, collector, false, s -> {
   box(s, -0.15f, 0.26f, -0.28f, 0.15f, 0.52f, 0.06f, 0xd8d6d0, false, 255);
   box(s, -0.18f, 0.25f, 0.05f, 0.18f, 0.56f, 0.24f, 0xc8c5be, false, 255);
   // Ошейник
   box(s, -0.165f, 0.30f, 0.22f, 0.165f, 0.52f, 0.28f, 0xd9262c, false, 255);
   box(s, -0.035f, 0.26f, 0.27f, 0.035f, 0.33f, 0.30f, 0xffd046, true, 255);
  });

  // Волчья голова с наклоном
  pose.pushPose();
  pose.translate(0, 0.44, 0.29);
  pose.rotate(Axis.ZP, (float) Math.sin(t * 1.1) * 0.12f);
  pose.rotate(Axis.YP, (float) Math.sin(t * 1.5) * 0.10f);
  submit(pose, collector, false, s -> {
   box(s, -0.15f, -0.11f, -0.06f, 0.15f, 0.16f, 0.18f, 0xdcdad4, false, 255);
   // Вытянутая морда и чёрный нос
   box(s, -0.075f, -0.10f, 0.18f, 0.075f, 0.02f, 0.30f, 0xe8e5df, false, 255);
   box(s, -0.04f, -0.02f, 0.295f, 0.04f, 0.025f, 0.32f, 0x1b1b1f, false, 255);
   // Глаза
   box(s, -0.12f, 0.02f, 0.178f, -0.05f, 0.07f, 0.188f, 0xffffff, false, 255);
   box(s, -0.10f, 0.025f, 0.184f, -0.06f, 0.065f, 0.192f, 0x18181c, false, 255);
   box(s, 0.05f, 0.02f, 0.178f, 0.12f, 0.07f, 0.188f, 0xffffff, false, 255);
   box(s, 0.06f, 0.025f, 0.184f, 0.10f, 0.065f, 0.192f, 0x18181c, false, 255);
   // Стоячие ушки
   box(s, -0.14f, 0.16f, -0.01f, -0.06f, 0.26f, 0.05f, 0xc2bfb8, false, 255);
   box(s, 0.06f, 0.16f, -0.01f, 0.14f, 0.26f, 0.05f, 0xc2bfb8, false, 255);
  });
  pose.popPose();

  // 4 лапки
  float[][] legs = {
   {-0.09f, 0.16f, 1f}, {0.09f, 0.16f, -1f},
   {-0.09f, -0.20f, -1f}, {0.09f, -0.20f, 1f}
  };
  for (var leg : legs) {
   pose.pushPose();
   pose.translate(leg[0], 0.27, leg[1]);
   pose.rotate(Axis.XP, legSwing * leg[2]);
   submit(pose, collector, false, s ->
    box(s, -0.048f, -0.27f, -0.048f, 0.048f, 0.0f, 0.048f, 0xcbc8c0, false, 255)
   );
   pose.popPose();
  }

  // Радостно виляющий пушистый хвост
  pose.pushPose();
  pose.translate(0, 0.44, -0.28);
  pose.rotate(Axis.YP, (float) Math.sin(t * 7.5) * 0.45f);
  pose.rotate(Axis.XP, 0.35f);
  submit(pose, collector, false, s ->
   box(s, -0.048f, -0.048f, -0.27f, 0.048f, 0.048f, 0.0f, 0xc8c5be, false, 255)
  );
  pose.popPose();

  pose.popPose();
 }

 // ================= 4. АНГЕЛ =================
 private static void renderAngel(PoseStack pose, SubmitNodeCollector collector, Frame f) {
  double t = f.seconds * f.speed;
  float bob = (float) Math.sin(t * 3.0) * 0.07f;
  float wingFlap = (float) Math.sin(t * (5.2 + f.move * 4.0)) * (0.42f + f.move * 0.25f);

  pose.pushPose();
  pose.translate(0, 0.18 + bob, 0);
  pose.rotate(Axis.XP, f.pitch * 0.7f);

  // Белое одеяние с золотым поясом и светлая голова
  submit(pose, collector, false, s -> {
   box(s, -0.14f, 0.04f, -0.10f, 0.14f, 0.50f, 0.10f, 0xf2f5ff, false, 255);
   box(s, -0.148f, 0.24f, -0.108f, 0.148f, 0.30f, 0.108f, 0xffd76b, true, 255);
   // Ручки
   box(s, -0.21f, 0.24f, -0.05f, -0.14f, 0.47f, 0.05f, 0xe8ecfa, false, 255);
   box(s, 0.14f, 0.24f, -0.05f, 0.21f, 0.47f, 0.05f, 0xe8ecfa, false, 255);
   // Голова + сияющие голубые глаза
   box(s, -0.13f, 0.51f, -0.13f, 0.13f, 0.77f, 0.13f, 0xfff0e0, false, 255);
   box(s, -0.135f, 0.67f, -0.135f, 0.135f, 0.78f, 0.135f, 0xfff4c2, false, 255);
   box(s, -0.09f, 0.60f, 0.128f, -0.03f, 0.65f, 0.138f, 0x6ce0ff, true, 255);
   box(s, 0.03f, 0.60f, 0.128f, 0.09f, 0.65f, 0.138f, 0x6ce0ff, true, 255);
  });

  // Вращающийся золотой нимб над головой
  pose.pushPose();
  pose.translate(0, 0.90, 0);
  pose.rotate(Axis.YP, (float) (t * 2.2));
  submit(pose, collector, false, s -> {
   box(s, -0.15f, -0.02f, -0.15f, 0.15f, 0.02f, -0.10f, 0xffe478, true, 255);
   box(s, -0.15f, -0.02f, 0.10f, 0.15f, 0.02f, 0.15f, 0xffe478, true, 255);
   box(s, -0.15f, -0.02f, -0.10f, -0.10f, 0.02f, 0.10f, 0xffe478, true, 255);
   box(s, 0.10f, -0.02f, -0.10f, 0.15f, 0.02f, 0.10f, 0xffe478, true, 255);
  });
  submit(pose, collector, true, s ->
   box(s, -0.19f, -0.035f, -0.19f, 0.19f, 0.035f, 0.19f, 0xffe699, true, 75)
  );
  pose.popPose();

  // Машущие ангельские крылья
  for (int side = -1; side <= 1; side += 2) {
   pose.pushPose();
   pose.translate(side * 0.08, 0.44, -0.10);
   pose.rotate(Axis.YP, side * (0.45f + wingFlap));
   int sd = side;
   submit(pose, collector, false, s -> {
    float x0 = sd > 0 ? 0.0f : -0.34f, x1 = sd > 0 ? 0.34f : 0.0f;
    float tx0 = sd > 0 ? 0.28f : -0.52f, tx1 = sd > 0 ? 0.52f : -0.28f;
    box(s, x0, -0.14f, -0.025f, x1, 0.14f, 0.025f, 0xffffff, false, 255);
    box(s, tx0, -0.06f, -0.02f, tx1, 0.20f, 0.02f, 0xeef6ff, true, 255);
   });
   pose.popPose();
  }

  pose.popPose();
 }

 // ================= 5. ДЕМОН =================
 private static void renderDemon(PoseStack pose, SubmitNodeCollector collector, Frame f) {
  double t = f.seconds * f.speed;
  float bob = (float) Math.sin(t * 3.6) * 0.07f;
  float wingFlap = (float) Math.sin(t * (5.8 + f.move * 4.5)) * (0.45f + f.move * 0.25f);

  pose.pushPose();
  pose.translate(0, 0.18 + bob, 0);
  pose.rotate(Axis.XP, f.pitch * 0.9f);

  // Тёмно-алое тело с рунами и голова с рогами
  submit(pose, collector, false, s -> {
   box(s, -0.14f, 0.06f, -0.10f, 0.14f, 0.50f, 0.10f, 0x201018, false, 255);
   box(s, -0.06f, 0.24f, 0.095f, 0.06f, 0.40f, 0.112f, 0xff2448, true, 255);
   // Руки
   box(s, -0.21f, 0.24f, -0.05f, -0.14f, 0.47f, 0.05f, 0x2c121e, false, 255);
   box(s, 0.14f, 0.24f, -0.05f, 0.21f, 0.47f, 0.05f, 0x2c121e, false, 255);
   // Голова + алые глаза + рога
   box(s, -0.13f, 0.51f, -0.13f, 0.13f, 0.77f, 0.13f, 0x26111c, false, 255);
   box(s, -0.095f, 0.61f, 0.128f, -0.03f, 0.66f, 0.142f, 0xff1e40, true, 255);
   box(s, 0.03f, 0.61f, 0.128f, 0.095f, 0.66f, 0.142f, 0xff1e40, true, 255);
   box(s, -0.15f, 0.76f, -0.02f, -0.08f, 0.91f, 0.05f, 0x521222, false, 255);
   box(s, -0.19f, 0.89f, -0.06f, -0.12f, 0.99f, 0.01f, 0xff2848, true, 255);
   box(s, 0.08f, 0.76f, -0.02f, 0.15f, 0.91f, 0.05f, 0x521222, false, 255);
   box(s, 0.12f, 0.89f, -0.06f, 0.19f, 0.99f, 0.01f, 0xff2848, true, 255);
  });

  // Острые демонические крылья
  for (int side = -1; side <= 1; side += 2) {
   pose.pushPose();
   pose.translate(side * 0.08, 0.44, -0.10);
   pose.rotate(Axis.YP, side * (0.42f + wingFlap));
   int sd = side;
   submit(pose, collector, false, s -> {
    float x0 = sd > 0 ? 0.0f : -0.34f, x1 = sd > 0 ? 0.34f : 0.0f;
    float tx0 = sd > 0 ? 0.26f : -0.52f, tx1 = sd > 0 ? 0.52f : -0.26f;
    // Каркас и алая перепонка
    box(s, x0, 0.09f, -0.025f, x1, 0.15f, 0.025f, 0x160910, false, 255);
    box(s, x0, -0.12f, -0.015f, x1, 0.10f, 0.015f, 0x8c1430, false, 255);
    box(s, tx0, 0.04f, -0.02f, tx1, 0.22f, 0.02f, 0xb81a3c, false, 255);
   });
   pose.popPose();
  }

  // Хвост с пикой на конце
  pose.pushPose();
  pose.translate(0, 0.14, -0.10);
  pose.rotate(Axis.YP, (float) Math.sin(t * 4.8) * 0.42f);
  submit(pose, collector, false, s -> {
   box(s, -0.025f, -0.025f, -0.28f, 0.025f, 0.025f, 0.0f, 0x240f18, false, 255);
   box(s, -0.06f, -0.04f, -0.36f, 0.06f, 0.04f, -0.27f, 0xff2244, true, 255);
  });
  pose.popPose();

  pose.popPose();
 }

 // ================= ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =================
 private static double findGroundY(ClientLevel level, double x, double refY, double z) {
  int startY = (int) Math.floor(refY) + 1;
  for (int y = startY; y >= startY - 4; y--) {
   if (!level.getBlockState(BlockPos.containing(x, y, z)).isAir()
    && level.getBlockState(BlockPos.containing(x, y + 1, z)).isAir()) {
    return y + 1.0
    }
  }
  return refY;
 }

 private static float wrapAngle(float angle) {
  while (angle > Math.PI) angle -= (float) Math.TAU;
  while (angle < -Math.PI) angle += (float) Math.TAU;
  return angle;
 }

 private static void submit(PoseStack pose, SubmitNodeCollector collector, boolean glow, Consumer<VertexSink> emitter) {
  collector.submitCustomGeometry(pose, glow ? EffectPipeline.TYPE : AccessoryPipeline.TYPE,
   (matrix, vertices) -> emitter.accept((x, y, z, color) -> vertices.addVertex(matrix, x, y, z).setColor(color)));
 }

 private static void box(VertexSink s, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, boolean emissive, int alpha) {
  int top = argb(rgb, 1.0f, alpha);
  int bot = argb(rgb, emissive ? 1.0f : 0.58f, alpha);
  int front = argb(rgb, emissive ? 1.0f : 0.88f, alpha);
  int back = argb(rgb, emissive ? 1.0f : 0.80f, alpha);
  int left = argb(rgb, emissive ? 1.0f : 0.74f, alpha);
  int right = argb(rgb, emissive ? 1.0f : 0.70f, alpha);

  quad(s, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, top);
  quad(s, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, bot);
  quad(s, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, front);
  quad(s, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, back);
  quad(s, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, left);
  quad(s, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, right);
 }

 private static void quad(VertexSink s, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int color) {
  s.vertex(ax, ay, az, color);
  s.vertex(bx, by, bz, color);
  s.vertex(cx, cy, cz, color);
  s.vertex(dx, dy, dz, color);
 }

 private static int argb(int rgb, float shade, int alpha) {
  int r = Math.clamp(Math.round(((rgb >> 16) & 255) * shade), 0, 255);
  int g = Math.clamp(Math.round(((rgb >> 8) & 255) * shade), 0, 255);
  int b = Math.clamp(Math.round((rgb & 255) * shade), 0, 255);
  return (Math.clamp(alpha, 0, 255) << 24) | (r << 16) | (g << 8) | b;
 }
}
