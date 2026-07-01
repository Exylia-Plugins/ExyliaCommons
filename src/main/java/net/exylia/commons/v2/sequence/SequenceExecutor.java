package net.exylia.commons.v2.sequence;

import net.exylia.commons.v2.compat.SoundCompat;
import net.exylia.commons.v2.debug.api.DebugAPI;
import net.exylia.commons.v2.tasks.api.TaskAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

/**
 * Executes a list of effect strings defined in YAML.
 *
 * Supported types:
 *   [PARTICLE]    TYPE;count:N;offset:X,Y,Z;speed:F;y:F;color:R,G,B;size:F
 *   [SOUND]       SOUND_NAME;volume;pitch
 *   [LIGHTNING]
 *   [EXPLOSION]
 *   [FIREWORK]    color:R,G,B;fade:R,G,B;type:TYPE;trail:true;power:N
 *   [COMMAND]     command {player} {world} {x} {y} {z}
 *   [DELAY]       seconds
 *   [POTION]      effect_type;duration;amplifier
 *   [BLOCK_BREAK] MATERIAL;count:N;offset:X,Y,Z;y:F
 *   [TITLE]       title;subtitle;fadeIn;stay;fadeOut
 *   [ACTION_BAR]  text
 *   [CIRCLE]      PARTICLE;radius:F;points:N;y:F;color:R,G,B;size:F;count:N
 *   [SPHERE]      PARTICLE;radius:F;points:N;color:R,G,B;size:F;count:N
 *   [BEAM]        PARTICLE;height:F;points:N;y:F;color:R,G,B;size:F;count:N
 *   [SPIRAL]      PARTICLE;height:F;radius:F;turns:N;points:N;y:F;color:R,G,B;size:F;count:N
 */
public class SequenceExecutor {

    private static final Particle PARTICLE_EXPLOSION = resolveParticle("EXPLOSION", "EXPLOSION_LARGE", "EXPLOSION_EMITTER");
    private static final Particle PARTICLE_BLOCK      = resolveParticle("BLOCK", "BLOCK_CRACK");

    private static Particle resolveParticle(String... candidates) {
        for (String name : candidates) {
            try { return Particle.valueOf(name); } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    public void execute(SequenceContext ctx, List<String> effects) {
        TaskAPI.at(ctx.getLocation(), () -> executeFrom(ctx, effects, 0));
    }

    public void executeOnCurrentThread(SequenceContext ctx, List<String> effects) {
        executeFrom(ctx, effects, 0);
    }

    private void executeFrom(SequenceContext ctx, List<String> effects, int startIndex) {
        for (int i = startIndex; i < effects.size(); i++) {
            String raw = effects.get(i);
            if (raw == null || raw.isBlank()) continue;

            int closeBracket = raw.indexOf(']');
            if (closeBracket < 2 || raw.charAt(0) != '[') {
                DebugAPI.logPluginWarn("Invalid effect format: " + raw);
                continue;
            }

            String type = raw.substring(1, closeBracket).trim().toUpperCase();
            String args = closeBracket + 1 < raw.length() ? raw.substring(closeBracket + 1).trim() : "";

            if (type.equals("DELAY")) {
                double seconds = parseDouble(args, 0.0);
                if (seconds > 0) {
                    final int nextIndex = i + 1;
                    TaskAPI.atLater(
                        ctx.getLocation(),
                        () -> executeFrom(ctx, effects, nextIndex),
                        (long) (seconds * 1000), TimeUnit.MILLISECONDS
                    );
                    return;
                }
            } else {
                dispatch(type, args, ctx);
            }
        }
    }

    private void dispatch(String type, String args, SequenceContext ctx) {
        switch (type) {
            case "PARTICLE"    -> executeParticle(args, ctx);
            case "SOUND"       -> executeSound(args, ctx);
            case "LIGHTNING"   -> executeLightning(ctx);
            case "EXPLOSION"   -> executeExplosion(ctx);
            case "FIREWORK"    -> executeFirework(args, ctx);
            case "COMMAND"     -> executeCommand(args, ctx);
            case "POTION"      -> executePotion(args, ctx);
            case "BLOCK_BREAK" -> executeBlockBreak(args, ctx);
            case "TITLE"       -> executeTitle(args, ctx);
            case "ACTION_BAR"  -> executeActionBar(args, ctx);
            case "CIRCLE"      -> executeCircle(args, ctx);
            case "SPHERE"      -> executeSphere(args, ctx);
            case "BEAM"        -> executeBeam(args, ctx);
            case "SPIRAL"      -> executeSpiral(args, ctx);
            default            -> DebugAPI.logPluginWarn("Unknown sequence effect type: " + type);
        }
    }

    // ── [PARTICLE] ────────────────────────────────────────────────────────────

    private void executeParticle(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;

        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;

        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        int count = 1;
        double oX = 0, oY = 0, oZ = 0, speed = 0, yShift = 0;
        Color dustColor = null;
        float dustSize = 1.0f;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.startsWith("count:"))       count     = parseInt(part.substring(6), 1);
            else if (part.startsWith("offset:")) { String[] xyz = part.substring(7).split(","); if (xyz.length >= 3) { oX = parseDouble(xyz[0], 0); oY = parseDouble(xyz[1], 0); oZ = parseDouble(xyz[2], 0); } }
            else if (part.startsWith("speed:"))  speed     = parseDouble(part.substring(6), 0);
            else if (part.startsWith("y:"))      yShift    = parseDouble(part.substring(2), 0);
            else if (part.startsWith("color:"))  dustColor = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))   dustSize  = (float) parseDouble(part.substring(5), 1.0);
        }

        Location loc = yShift != 0 ? ctx.getLocation().clone().add(0, yShift, 0) : ctx.getLocation();
        spawnParticleAt(world, particle, loc, count, oX, oY, oZ, speed, dustColor, dustSize, ctx);
    }

    // ── [SOUND] ───────────────────────────────────────────────────────────────

    private void executeSound(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;

        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;

        Sound sound = SoundCompat.fromName(parts[0].trim());
        if (sound == null) {
            DebugAPI.logPluginWarn("Unknown sound: " + parts[0].trim());
            return;
        }

        float volume = parts.length > 1 ? (float) parseDouble(parts[1].trim(), 1.0) : 1.0f;
        float pitch  = parts.length > 2 ? (float) parseDouble(parts[2].trim(), 1.0) : 1.0f;
        world.playSound(ctx.getLocation(), sound, volume, pitch);
    }

    // ── [LIGHTNING] ───────────────────────────────────────────────────────────

    private void executeLightning(SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        world.strikeLightningEffect(ctx.getLocation());
    }

    // ── [EXPLOSION] ───────────────────────────────────────────────────────────

    private void executeExplosion(SequenceContext ctx) {
        Location loc = ctx.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        world.createExplosion(loc, 0F, false, false);
        if (PARTICLE_EXPLOSION != null) world.spawnParticle(PARTICLE_EXPLOSION, loc, 1);
    }

    // ── [FIREWORK] ────────────────────────────────────────────────────────────

    private void executeFirework(String args, SequenceContext ctx) {
        Location loc = ctx.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        String[] parts = args.split(";");
        Color color = Color.RED;
        Color fade  = Color.ORANGE;
        FireworkEffect.Type type = FireworkEffect.Type.BALL_LARGE;
        boolean trail = true;
        int power = 0;

        for (String part : parts) {
            part = part.trim();
            if      (part.startsWith("color:")) color = parseColor(part.substring(6), Color.RED);
            else if (part.startsWith("fade:"))  fade  = parseColor(part.substring(5), Color.ORANGE);
            else if (part.startsWith("type:"))  { try { type = FireworkEffect.Type.valueOf(part.substring(5).trim().toUpperCase()); } catch (Exception ignored) {} }
            else if (part.startsWith("trail:")) trail = Boolean.parseBoolean(part.substring(6).trim());
            else if (part.startsWith("power:")) power = parseInt(part.substring(6), 0);
        }

        final Color fc = color, ff = fade;
        final FireworkEffect.Type ft = type;
        final boolean ftr = trail;
        final int fp = power;

        Firework fw = world.spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withColor(fc).withFade(ff).with(ft).trail(ftr).build());
        meta.setPower(fp);
        fw.setFireworkMeta(meta);
        TaskAPI.atLater(loc, fw::detonate, 50L, TimeUnit.MILLISECONDS);
    }

    // ── [COMMAND] ─────────────────────────────────────────────────────────────

    private void executeCommand(String args, SequenceContext ctx) {
        Location loc = ctx.getLocation();
        String cmd = args
            .replace("{player}", ctx.getSourcePlayer() != null ? ctx.getSourcePlayer().getName() : "")
            .replace("{world}",  loc.getWorld() != null ? loc.getWorld().getName() : "")
            .replace("{x}",      String.valueOf(loc.getBlockX()))
            .replace("{y}",      String.valueOf(loc.getBlockY()))
            .replace("{z}",      String.valueOf(loc.getBlockZ()));
        TaskAPI.runSync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
    }

    // ── [POTION] ──────────────────────────────────────────────────────────────

    private void executePotion(String args, SequenceContext ctx) {
        if (!(ctx.getTargetEntity() instanceof LivingEntity target)) return;

        String[] parts = args.split(";");
        if (parts.length < 1 || parts[0].isBlank()) return;

        PotionEffectType potionType = Registry.EFFECT.get(NamespacedKey.minecraft(parts[0].trim().toLowerCase()));
        if (potionType == null) {
            DebugAPI.logPluginWarn("Unknown potion effect: " + parts[0].trim());
            return;
        }

        int duration  = parts.length > 1 ? parseInt(parts[1].trim(), 100) : 100;
        int amplifier = parts.length > 2 ? parseInt(parts[2].trim(), 0)   : 0;
        target.addPotionEffect(new PotionEffect(potionType, duration, amplifier));
    }

    // ── [BLOCK_BREAK] ─────────────────────────────────────────────────────────

    private void executeBlockBreak(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;

        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;

        Material material;
        try {
            material = Material.valueOf(parts[0].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            DebugAPI.logPluginWarn("Unknown material for BLOCK_BREAK: " + parts[0].trim());
            return;
        }

        int count = 20;
        double oX = 0.3, oY = 0.3, oZ = 0.3, yShift = 0;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("count:"))  count  = parseInt(part.substring(6), 20);
            else if (part.startsWith("offset:")) { String[] xyz = part.substring(7).split(","); if (xyz.length >= 3) { oX = parseDouble(xyz[0], 0.3); oY = parseDouble(xyz[1], 0.3); oZ = parseDouble(xyz[2], 0.3); } }
            else if (part.startsWith("y:"))      yShift = parseDouble(part.substring(2), 0);
        }

        Location loc = yShift != 0 ? ctx.getLocation().clone().add(0, yShift, 0) : ctx.getLocation();
        if (PARTICLE_BLOCK != null) world.spawnParticle(PARTICLE_BLOCK, loc, count, oX, oY, oZ, 0.1, material.createBlockData());
    }

    // ── [TITLE] ───────────────────────────────────────────────────────────────

    private void executeTitle(String args, SequenceContext ctx) {
        if (ctx.getSourcePlayer() == null) return;

        String[] parts = args.split(";");
        String titleText    = parts.length > 0 ? parts[0].trim() : "";
        String subtitleText = parts.length > 1 ? parts[1].trim() : "";
        int fadeIn  = parts.length > 2 ? parseInt(parts[2].trim(), 10) : 10;
        int stay    = parts.length > 3 ? parseInt(parts[3].trim(), 70) : 70;
        int fadeOut = parts.length > 4 ? parseInt(parts[4].trim(), 20) : 20;

        Component title    = LegacyComponentSerializer.legacyAmpersand().deserialize(titleText);
        Component subtitle = LegacyComponentSerializer.legacyAmpersand().deserialize(subtitleText);

        ctx.getSourcePlayer().showTitle(Title.title(title, subtitle,
            Title.Times.times(
                Duration.ofMillis(fadeIn  * 50L),
                Duration.ofMillis(stay    * 50L),
                Duration.ofMillis(fadeOut * 50L)
            )
        ));
    }

    // ── [ACTION_BAR] ──────────────────────────────────────────────────────────

    private void executeActionBar(String args, SequenceContext ctx) {
        if (ctx.getSourcePlayer() == null) return;
        Component text = LegacyComponentSerializer.legacyAmpersand().deserialize(args.trim());
        ctx.getSourcePlayer().sendActionBar(text);
    }

    // ── [CIRCLE] ──────────────────────────────────────────────────────────────

    private void executeCircle(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;

        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;

        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 1.0, yShift = 0, dustSize = 1.0;
        int points = 16, count = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("radius:")) radius    = parseDouble(part.substring(7), 1.0);
            else if (part.startsWith("points:")) points    = parseInt(part.substring(7), 16);
            else if (part.startsWith("y:"))      yShift    = parseDouble(part.substring(2), 0);
            else if (part.startsWith("count:"))  count     = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))  dustColor = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))   dustSize  = parseDouble(part.substring(5), 1.0);
        }

        double step = 2 * Math.PI / points;
        for (int i = 0; i < points; i++) {
            double angle = i * step;
            Location loc = ctx.getLocation().clone().add(radius * Math.cos(angle), yShift, radius * Math.sin(angle));
            spawnParticleAt(world, particle, loc, count, 0, 0, 0, 0, dustColor, (float) dustSize, ctx);
        }
    }

    // ── [SPHERE] ──────────────────────────────────────────────────────────────

    private void executeSphere(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;

        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;

        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 1.0, dustSize = 1.0;
        int points = 32, count = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("radius:")) radius    = parseDouble(part.substring(7), 1.0);
            else if (part.startsWith("points:")) points    = parseInt(part.substring(7), 32);
            else if (part.startsWith("count:"))  count     = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))  dustColor = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))   dustSize  = parseDouble(part.substring(5), 1.0);
        }

        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < points; i++) {
            double y     = 1.0 - (i / (double) (points - 1)) * 2.0;
            double r     = Math.sqrt(1.0 - y * y);
            double theta = goldenAngle * i;
            Location loc = ctx.getLocation().clone().add(
                radius * r * Math.cos(theta),
                radius * y + 1.0,
                radius * r * Math.sin(theta)
            );
            spawnParticleAt(world, particle, loc, count, 0, 0, 0, 0, dustColor, (float) dustSize, ctx);
        }
    }

    // ── [BEAM] ────────────────────────────────────────────────────────────────

    private void executeBeam(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;

        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;

        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double height = 3.0, yShift = 0, dustSize = 1.0;
        int points = 20, count = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("height:")) height    = parseDouble(part.substring(7), 3.0);
            else if (part.startsWith("points:")) points    = parseInt(part.substring(7), 20);
            else if (part.startsWith("y:"))      yShift    = parseDouble(part.substring(2), 0);
            else if (part.startsWith("count:"))  count     = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))  dustColor = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))   dustSize  = parseDouble(part.substring(5), 1.0);
        }

        double step = height / Math.max(points, 1);
        for (int i = 0; i <= points; i++) {
            Location loc = ctx.getLocation().clone().add(0, yShift + i * step, 0);
            spawnParticleAt(world, particle, loc, count, 0, 0, 0, 0, dustColor, (float) dustSize, ctx);
        }
    }

    // ── [SPIRAL] ─────────────────────────────────────────────────────────────

    private void executeSpiral(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;

        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;

        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double height = 3.0, radius = 1.0, yShift = 0, dustSize = 1.0;
        int turns = 2, points = 40, count = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("height:")) height    = parseDouble(part.substring(7), 3.0);
            else if (part.startsWith("radius:")) radius    = parseDouble(part.substring(7), 1.0);
            else if (part.startsWith("turns:"))  turns     = parseInt(part.substring(6), 2);
            else if (part.startsWith("points:")) points    = parseInt(part.substring(7), 40);
            else if (part.startsWith("y:"))      yShift    = parseDouble(part.substring(2), 0);
            else if (part.startsWith("count:"))  count     = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))  dustColor = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))   dustSize  = parseDouble(part.substring(5), 1.0);
        }

        double totalAngle = turns * 2.0 * Math.PI;
        double angleStep  = totalAngle / Math.max(points, 1);
        double heightStep = height / Math.max(points, 1);

        for (int i = 0; i < points; i++) {
            double angle = i * angleStep;
            Location loc = ctx.getLocation().clone().add(
                radius * Math.cos(angle),
                yShift + i * heightStep,
                radius * Math.sin(angle)
            );
            spawnParticleAt(world, particle, loc, count, 0, 0, 0, 0, dustColor, (float) dustSize, ctx);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void spawnParticleAt(World world, Particle particle, Location loc, int count,
                                  double oX, double oY, double oZ, double speed,
                                  Color dustColor, float dustSize, SequenceContext ctx) {
        Object data = dustColor != null ? new Particle.DustOptions(dustColor, dustSize) : null;
        BiPredicate<Player, UUID> filter = ctx.getParticleFilter();

        if (filter != null && ctx.getSourcePlayer() != null) {
            UUID sourceId = ctx.getSourcePlayer().getUniqueId();
            for (Player observer : world.getPlayers()) {
                if (filter.test(observer, sourceId)) {
                    observer.spawnParticle(particle, loc, count, oX, oY, oZ, speed, data);
                }
            }
        } else {
            world.spawnParticle(particle, loc, count, oX, oY, oZ, speed, data);
        }
    }

    private Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            DebugAPI.logPluginWarn("Unknown particle type: " + name.trim());
            return null;
        }
    }

    private Color parseColor(String s, Color def) {
        if (s == null || s.isBlank()) return def;
        String[] rgb = s.split(",");
        if (rgb.length < 3) return def;
        try {
            return Color.fromRGB(parseInt(rgb[0], 255), parseInt(rgb[1], 255), parseInt(rgb[2], 255));
        } catch (Exception e) {
            return def;
        }
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
