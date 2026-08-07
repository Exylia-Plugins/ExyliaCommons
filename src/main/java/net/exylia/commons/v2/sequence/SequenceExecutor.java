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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

/**
 * Executes a list of effect strings defined in YAML.
 *
 * Supported types:
 *   [PARTICLE]      TYPE;count:N;offset:X,Y,Z;speed:F;y:F;color:R,G,B;size:F
 *   [SOUND]         SOUND_NAME;volume;pitch
 *   [LIGHTNING]
 *   [EXPLOSION]
 *   [FIREWORK]      color:R,G,B;fade:R,G,B;type:TYPE;trail:true;power:N
 *   [COMMAND]       command {player} {world} {x} {y} {z}
 *   [DELAY]         seconds
 *   [POTION]        effect_type;duration;amplifier
 *   [BLOCK_BREAK]   MATERIAL;count:N;offset:X,Y,Z;y:F
 *   [TITLE]         title;subtitle;fadeIn;stay;fadeOut
 *   [ACTION_BAR]    text
 *
 * Geometric shapes — all accept ticks:N and interval:F for progressive animation:
 *   [CIRCLE]        PARTICLE;radius:F;points:N;y:F;color:R,G,B;size:F;count:N;ticks:N;interval:F
 *   [SPHERE]        PARTICLE;radius:F;points:N;color:R,G,B;size:F;count:N;ticks:N;interval:F
 *   [BEAM]          PARTICLE;height:F;points:N;y:F;color:R,G,B;size:F;count:N;ticks:N;interval:F
 *   [SPIRAL]        PARTICLE;height:F;radius:F;turns:N;points:N;y:F;color:R,G,B;size:F;count:N;ticks:N;interval:F
 *   [DOUBLE_HELIX]  PARTICLE;height:F;radius:F;turns:N;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [TORNADO]       PARTICLE;height:F;radius:F;top_radius:F;turns:N;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [STAR]          PARTICLE;radius:F;spikes:N;inner:F;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [CAGE]          PARTICLE;radius:F;height:F;columns:N;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [DISC]          PARTICLE;radius:F;rings:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [VORTEX]        PARTICLE;radius:F;turns:N;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [WAVE]          PARTICLE;length:F;amplitude:F;frequency:N;arms:N;angle:F;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [CROSS]         PARTICLE;radius:F;arms:N;angle:F;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [GALAXY]        PARTICLE;radius:F;turns:N;arms:N;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [TORUS]         PARTICLE;radius:F;tube:F;segments:N;tube_segments:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [BURST]         PARTICLE;radius:F;beams:N;angle:F;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [PYRAMID]       PARTICLE;base:F;height:F;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [RING_PULSE]    PARTICLE;radius:F;rings:N;spacing:F;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [WINGS]         PARTICLE;span:F;arch:F;depth:F;dir:F;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [ARCH]          PARTICLE;radius:F;arc:F;dir:F;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 *   [CLAW]          PARTICLE;radius:F;claws:N;spread:F;curve:F;dir:F;drop:F;points:N;y:F;color:R,G,B;size:F;ticks:N;interval:F
 */
public class SequenceExecutor {

    /**
     * Kill/hit effects are location-anchored spectacles, not gameplay-critical broadcasts.
     * Capping visibility to 32 blocks keeps distant players from seeing/hearing effects
     * that have nothing to do with them, regardless of Minecraft's chunk tracking range.
     */
    private static final double MAX_EFFECT_DISTANCE = 32.0;
    private static final double MAX_EFFECT_DISTANCE_SQUARED = MAX_EFFECT_DISTANCE * MAX_EFFECT_DISTANCE;

    static final NamespacedKey EFFECT_FIREWORK_KEY = new NamespacedKey("exylia_commons", "effect_firework");

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
            case "CIRCLE"       -> executeCircle(args, ctx);
            case "SPHERE"       -> executeSphere(args, ctx);
            case "BEAM"         -> executeBeam(args, ctx);
            case "SPIRAL"       -> executeSpiral(args, ctx);
            case "DOUBLE_HELIX" -> executeDoubleHelix(args, ctx);
            case "TORNADO"      -> executeTornado(args, ctx);
            case "STAR"         -> executeStar(args, ctx);
            case "CAGE"         -> executeCage(args, ctx);
            case "DISC"         -> executeDisc(args, ctx);
            case "VORTEX"       -> executeVortex(args, ctx);
            case "WAVE"         -> executeWave(args, ctx);
            case "CROSS"        -> executeCross(args, ctx);
            case "GALAXY"       -> executeGalaxy(args, ctx);
            case "TORUS"        -> executeTorus(args, ctx);
            case "BURST"        -> executeBurst(args, ctx);
            case "PYRAMID"      -> executePyramid(args, ctx);
            case "RING_PULSE"   -> executeRingPulse(args, ctx);
            case "WINGS"        -> executeWings(args, ctx);
            case "ARCH"         -> executeArch(args, ctx);
            case "CLAW"         -> executeClaw(args, ctx);
            default             -> DebugAPI.logPluginWarn("Unknown sequence effect type: " + type);
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

        for (Player observer : nearbyObservers(world, ctx)) {
            observer.playSound(ctx.getLocation(), sound, volume, pitch);
        }
    }

    // ── [LIGHTNING] ───────────────────────────────────────────────────────────

    private void executeLightning(SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        for (Player observer : nearbyObservers(world, ctx)) {
            observer.spawnParticle(Particle.FLASH, ctx.getLocation(), 1);
            observer.spawnParticle(Particle.ELECTRIC_SPARK, ctx.getLocation().clone().add(0, 1.5, 0),
                    60, 0.3, 1.5, 0.3, 0.3);
            observer.playSound(ctx.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
        }
    }

    // ── [EXPLOSION] ───────────────────────────────────────────────────────────

    private void executeExplosion(SequenceContext ctx) {
        Location loc = ctx.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        for (Player observer : nearbyObservers(world, ctx)) {
            if (PARTICLE_EXPLOSION != null) observer.spawnParticle(PARTICLE_EXPLOSION, loc, 1);
        }
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
        fw.getPersistentDataContainer().set(EFFECT_FIREWORK_KEY, PersistentDataType.BYTE, (byte) 1);
        hideFromDistantPlayers(world, loc, fw, ctx);
        TaskAPI.atLater(loc, fw::detonate, 50L, TimeUnit.MILLISECONDS);
    }

    /**
     * Firework entities are normally visible through Minecraft's chunk tracking range,
     * which is far beyond MAX_EFFECT_DISTANCE. Explicitly hide the entity (and its
     * detonation burst/sound) from anyone outside the effect's visibility radius.
     */
    private void hideFromDistantPlayers(World world, Location loc, org.bukkit.entity.Entity entity, SequenceContext ctx) {
        org.bukkit.plugin.Plugin plugin = net.exylia.commons.v2.tasks.core.TaskManager.getInstance().getPlugin();
        for (Player online : world.getPlayers()) {
            if (online.getLocation().distanceSquared(loc) > MAX_EFFECT_DISTANCE_SQUARED) {
                online.hideEntity(plugin, entity);
            }
        }
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
        if (PARTICLE_BLOCK == null) return;
        org.bukkit.block.data.BlockData blockData = material.createBlockData();
        for (Player observer : nearbyObservers(world, ctx)) {
            observer.spawnParticle(PARTICLE_BLOCK, loc, count, oX, oY, oZ, 0.1, blockData);
        }
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

        double radius = 1.0, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int points = 16, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 1.0);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 16);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double step = 2 * Math.PI / points;
        List<Location> locs = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double angle = i * step;
            locs.add(ctx.getLocation().clone().add(radius * Math.cos(angle), yShift, radius * Math.sin(angle)));
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [SPHERE] ──────────────────────────────────────────────────────────────

    private void executeSphere(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 1.0, dustSize = 1.0, intervalSecs = 0.05;
        int points = 32, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 1.0);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 32);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
        }

        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
        List<Location> locs = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double y     = 1.0 - (i / (double) (points - 1)) * 2.0;
            double r     = Math.sqrt(1.0 - y * y);
            double theta = goldenAngle * i;
            locs.add(ctx.getLocation().clone().add(
                radius * r * Math.cos(theta),
                radius * y + 1.0,
                radius * r * Math.sin(theta)
            ));
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [BEAM] ────────────────────────────────────────────────────────────────

    private void executeBeam(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double height = 3.0, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int points = 20, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("height:"))   height       = parseDouble(part.substring(7), 3.0);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 20);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double step = height / Math.max(points, 1);
        List<Location> locs = new ArrayList<>(points + 1);
        for (int i = 0; i <= points; i++) {
            locs.add(ctx.getLocation().clone().add(0, yShift + i * step, 0));
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [SPIRAL] ─────────────────────────────────────────────────────────────

    private void executeSpiral(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double height = 3.0, radius = 1.0, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int turns = 2, points = 40, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("height:"))   height       = parseDouble(part.substring(7), 3.0);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 1.0);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 40);
            else if (part.startsWith("turns:"))    turns        = parseInt(part.substring(6), 2);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double totalAngle = turns * 2.0 * Math.PI;
        double angleStep  = totalAngle / Math.max(points, 1);
        double heightStep = height / Math.max(points, 1);

        List<Location> locs = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double angle = i * angleStep;
            locs.add(ctx.getLocation().clone().add(
                radius * Math.cos(angle),
                yShift + i * heightStep,
                radius * Math.sin(angle)
            ));
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [DOUBLE_HELIX] ────────────────────────────────────────────────────────

    private void executeDoubleHelix(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double height = 3.0, radius = 1.0, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int turns = 2, points = 40, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("height:"))   height       = parseDouble(part.substring(7), 3.0);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 1.0);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 40);
            else if (part.startsWith("turns:"))    turns        = parseInt(part.substring(6), 2);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double totalAngle = turns * 2.0 * Math.PI;
        double angleStep  = totalAngle / Math.max(points, 1);
        double heightStep = height / Math.max(points, 1);

        List<Location> locs = new ArrayList<>(points * 2);
        for (int i = 0; i < points; i++) {
            double angle = i * angleStep;
            double y     = yShift + i * heightStep;
            locs.add(ctx.getLocation().clone().add(radius * Math.cos(angle), y, radius * Math.sin(angle)));
            locs.add(ctx.getLocation().clone().add(radius * Math.cos(angle + Math.PI), y, radius * Math.sin(angle + Math.PI)));
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [TORNADO] ─────────────────────────────────────────────────────────────

    private void executeTornado(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double height = 4.0, baseRadius = 1.5, topRadius = 0.2, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int turns = 3, points = 60, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:"))   intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("top_radius:")) topRadius    = parseDouble(part.substring(11), 0.2);
            else if (part.startsWith("height:"))     height       = parseDouble(part.substring(7), 4.0);
            else if (part.startsWith("radius:"))     baseRadius   = parseDouble(part.substring(7), 1.5);
            else if (part.startsWith("points:"))     points       = parseInt(part.substring(7), 60);
            else if (part.startsWith("turns:"))      turns        = parseInt(part.substring(6), 3);
            else if (part.startsWith("ticks:"))      ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))      count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))      dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))       dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))          yShift       = parseDouble(part.substring(2), 0);
        }

        double totalAngle = turns * 2.0 * Math.PI;
        double angleStep  = totalAngle / Math.max(points, 1);
        double heightStep = height / Math.max(points, 1);

        List<Location> locs = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t     = (double) i / points;
            double r     = baseRadius + (topRadius - baseRadius) * t;
            double angle = i * angleStep;
            locs.add(ctx.getLocation().clone().add(r * Math.cos(angle), yShift + i * heightStep, r * Math.sin(angle)));
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [STAR] ────────────────────────────────────────────────────────────────

    private void executeStar(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 1.5, innerRatio = 0.5, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int spikes = 5, points = 8, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 1.5);
            else if (part.startsWith("spikes:"))   spikes       = parseInt(part.substring(7), 5);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 8);
            else if (part.startsWith("inner:"))    innerRatio   = parseDouble(part.substring(6), 0.5);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double innerRadius = radius * innerRatio;
        int totalVerts     = spikes * 2;
        double angleStep   = 2.0 * Math.PI / totalVerts;
        double startAngle  = -Math.PI / 2;

        List<Location> locs = new ArrayList<>(totalVerts * (points + 1));
        for (int i = 0; i < totalVerts; i++) {
            double rA = (i % 2 == 0) ? radius : innerRadius;
            double rB = (i % 2 == 0) ? innerRadius : radius;
            double aA = startAngle + i * angleStep;
            double aB = aA + angleStep;
            for (int j = 0; j <= points; j++) {
                double t = (double) j / points;
                double x = rA * Math.cos(aA) * (1 - t) + rB * Math.cos(aB) * t;
                double z = rA * Math.sin(aA) * (1 - t) + rB * Math.sin(aB) * t;
                locs.add(ctx.getLocation().clone().add(x, yShift, z));
            }
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [CAGE] ────────────────────────────────────────────────────────────────

    private void executeCage(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 1.5, height = 3.0, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int columns = 8, points = 16, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("columns:"))  columns      = parseInt(part.substring(8), 8);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 1.5);
            else if (part.startsWith("height:"))   height       = parseDouble(part.substring(7), 3.0);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 16);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double angleStep  = 2.0 * Math.PI / Math.max(columns, 1);
        double heightStep = height / Math.max(points, 1);
        double[] cosAngles = new double[columns];
        double[] sinAngles = new double[columns];
        for (int col = 0; col < columns; col++) {
            double angle = col * angleStep;
            cosAngles[col] = Math.cos(angle);
            sinAngles[col] = Math.sin(angle);
        }

        List<Location> locs = new ArrayList<>(columns * (points + 1));
        for (int pt = 0; pt <= points; pt++) {
            double y = yShift + pt * heightStep;
            for (int col = 0; col < columns; col++) {
                locs.add(ctx.getLocation().clone().add(radius * cosAngles[col], y, radius * sinAngles[col]));
            }
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [DISC] ────────────────────────────────────────────────────────────────

    private void executeDisc(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 2.0, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int rings = 5, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 2.0);
            else if (part.startsWith("rings:"))    rings        = parseInt(part.substring(6), 5);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        List<Location> locs = new ArrayList<>();
        locs.add(ctx.getLocation().clone().add(0, yShift, 0));
        for (int ring = 1; ring <= rings; ring++) {
            double r    = radius * ((double) ring / rings);
            int    pts  = Math.max(8, (int) (r * 16));
            double step = 2.0 * Math.PI / pts;
            for (int i = 0; i < pts; i++) {
                double angle = i * step;
                locs.add(ctx.getLocation().clone().add(r * Math.cos(angle), yShift, r * Math.sin(angle)));
            }
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [VORTEX] ──────────────────────────────────────────────────────────────

    private void executeVortex(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 2.0, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int turns = 3, points = 60, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 2.0);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 60);
            else if (part.startsWith("turns:"))    turns        = parseInt(part.substring(6), 3);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double totalAngle = turns * 2.0 * Math.PI;
        List<Location> locs = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t     = (double) i / points;
            double r     = radius * (1.0 - t);
            double angle = t * totalAngle;
            locs.add(ctx.getLocation().clone().add(r * Math.cos(angle), yShift, r * Math.sin(angle)));
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [WAVE] ────────────────────────────────────────────────────────────────

    private void executeWave(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double length = 5.0, amplitude = 1.0, yShift = 0, dustSize = 1.0, angleDeg = 0, intervalSecs = 0.05;
        int frequency = 2, arms = 1, points = 40, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:"))  intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("amplitude:")) amplitude    = parseDouble(part.substring(10), 1.0);
            else if (part.startsWith("frequency:")) frequency    = parseInt(part.substring(10), 2);
            else if (part.startsWith("length:"))    length       = parseDouble(part.substring(7), 5.0);
            else if (part.startsWith("points:"))    points       = parseInt(part.substring(7), 40);
            else if (part.startsWith("angle:"))     angleDeg     = parseDouble(part.substring(6), 0);
            else if (part.startsWith("ticks:"))     ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))     count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))     dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("arms:"))      arms         = parseInt(part.substring(5), 1);
            else if (part.startsWith("size:"))      dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))         yShift       = parseDouble(part.substring(2), 0);
        }

        double armOffset = 2.0 * Math.PI / Math.max(arms, 1);
        double[] dxArr = new double[arms];
        double[] dzArr = new double[arms];
        for (int a = 0; a < arms; a++) {
            double dir = Math.toRadians(angleDeg) + a * armOffset;
            dxArr[a] = Math.cos(dir);
            dzArr[a] = Math.sin(dir);
        }

        List<Location> locs = new ArrayList<>(arms * (points + 1));
        for (int i = 0; i <= points; i++) {
            double t    = (double) i / points;
            double dist = -length / 2.0 + t * length;
            double yOsc = amplitude * Math.sin(frequency * 2.0 * Math.PI * t);
            for (int a = 0; a < arms; a++) {
                locs.add(ctx.getLocation().clone().add(dist * dxArr[a], yShift + yOsc, dist * dzArr[a]));
            }
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [CROSS] ───────────────────────────────────────────────────────────────

    private void executeCross(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 2.0, yShift = 0, dustSize = 1.0, angleDeg = 0, intervalSecs = 0.05;
        int arms = 4, points = 16, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 2.0);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 16);
            else if (part.startsWith("angle:"))    angleDeg     = parseDouble(part.substring(6), 0);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("arms:"))     arms         = parseInt(part.substring(5), 4);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double step = 2.0 * Math.PI / Math.max(arms, 1);
        double[] dxArr = new double[arms];
        double[] dzArr = new double[arms];
        for (int a = 0; a < arms; a++) {
            double angle = Math.toRadians(angleDeg) + a * step;
            dxArr[a] = Math.cos(angle);
            dzArr[a] = Math.sin(angle);
        }

        List<Location> locs = new ArrayList<>(arms * (points + 1));
        for (int j = 0; j <= points; j++) {
            double r = (double) j / points * radius;
            for (int a = 0; a < arms; a++) {
                locs.add(ctx.getLocation().clone().add(r * dxArr[a], yShift, r * dzArr[a]));
            }
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [GALAXY] ──────────────────────────────────────────────────────────────

    private void executeGalaxy(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 2.5, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int turns = 2, arms = 2, points = 50, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 2.5);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 50);
            else if (part.startsWith("turns:"))    turns        = parseInt(part.substring(6), 2);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("arms:"))     arms         = parseInt(part.substring(5), 2);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double armOffset  = 2.0 * Math.PI / Math.max(arms, 1);
        double totalAngle = turns * 2.0 * Math.PI;

        List<Location> locs = new ArrayList<>(arms * points);
        for (int i = 0; i < points; i++) {
            double t = (double) i / points;
            double r = t * radius;
            for (int a = 0; a < arms; a++) {
                double angle = a * armOffset + t * totalAngle;
                locs.add(ctx.getLocation().clone().add(r * Math.cos(angle), yShift, r * Math.sin(angle)));
            }
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [TORUS] ───────────────────────────────────────────────────────────────

    private void executeTorus(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 1.5, tubeRadius = 0.5, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int segments = 20, tubeSegments = 10, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:"))     intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("tube_segments:")) tubeSegments = parseInt(part.substring(14), 10);
            else if (part.startsWith("segments:"))     segments     = parseInt(part.substring(9), 20);
            else if (part.startsWith("radius:"))       radius       = parseDouble(part.substring(7), 1.5);
            else if (part.startsWith("ticks:"))        ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))        count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))        dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("tube:"))         tubeRadius   = parseDouble(part.substring(5), 0.5);
            else if (part.startsWith("size:"))         dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))            yShift       = parseDouble(part.substring(2), 0);
        }

        List<Location> locs = new ArrayList<>(segments * tubeSegments);
        for (int i = 0; i < segments; i++) {
            double phi = 2.0 * Math.PI * i / segments;
            for (int j = 0; j < tubeSegments; j++) {
                double theta = 2.0 * Math.PI * j / tubeSegments;
                double x = (radius + tubeRadius * Math.cos(theta)) * Math.cos(phi);
                double y = tubeRadius * Math.sin(theta);
                double z = (radius + tubeRadius * Math.cos(theta)) * Math.sin(phi);
                locs.add(ctx.getLocation().clone().add(x, yShift + y + 1.0, z));
            }
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [BURST] ───────────────────────────────────────────────────────────────

    private void executeBurst(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 2.0, yShift = 0, dustSize = 1.0, angleDeg = 0, intervalSecs = 0.05;
        int beams = 8, points = 10, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 2.0);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 10);
            else if (part.startsWith("angle:"))    angleDeg     = parseDouble(part.substring(6), 0);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("beams:"))    beams        = parseInt(part.substring(6), 8);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double step = 2.0 * Math.PI / Math.max(beams, 1);
        double[] dxArr = new double[beams];
        double[] dzArr = new double[beams];
        for (int b = 0; b < beams; b++) {
            double angle = Math.toRadians(angleDeg) + b * step;
            dxArr[b] = Math.cos(angle);
            dzArr[b] = Math.sin(angle);
        }

        List<Location> locs = new ArrayList<>(beams * (points + 1));
        for (int j = 0; j <= points; j++) {
            double r = (double) j / points * radius;
            for (int b = 0; b < beams; b++) {
                locs.add(ctx.getLocation().clone().add(r * dxArr[b], yShift, r * dzArr[b]));
            }
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [PYRAMID] ─────────────────────────────────────────────────────────────

    private void executePyramid(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double base = 2.0, height = 3.0, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int points = 16, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("height:"))   height       = parseDouble(part.substring(7), 3.0);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 16);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("base:"))     base         = parseDouble(part.substring(5), 2.0);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double[] cx = {base, base, -base, -base};
        double[] cz = {base, -base, -base, base};

        List<Location> locs = new ArrayList<>(8 * (points + 1));
        for (int j = 0; j <= points; j++) {
            double t = (double) j / points;
            for (int i = 0; i < 4; i++) {
                int next = (i + 1) % 4;
                locs.add(ctx.getLocation().clone().add(
                    cx[i] * (1 - t) + cx[next] * t, yShift, cz[i] * (1 - t) + cz[next] * t));
            }
            for (int i = 0; i < 4; i++) {
                locs.add(ctx.getLocation().clone().add(cx[i] * (1 - t), yShift + height * t, cz[i] * (1 - t)));
            }
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [RING_PULSE] ──────────────────────────────────────────────────────────

    private void executeRingPulse(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 2.0, spacing = 0.4, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int rings = 6, points = 24, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("spacing:"))  spacing      = parseDouble(part.substring(8), 0.4);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 2.0);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 24);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("rings:"))    rings        = parseInt(part.substring(6), 6);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double step = 2.0 * Math.PI / Math.max(points, 1);
        List<Location> locs = new ArrayList<>(rings * points);
        for (int ring = 0; ring < rings; ring++) {
            double y = yShift + ring * spacing;
            for (int i = 0; i < points; i++) {
                double angle = i * step;
                locs.add(ctx.getLocation().clone().add(radius * Math.cos(angle), y, radius * Math.sin(angle)));
            }
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [WINGS] ───────────────────────────────────────────────────────────────
    // Two mirrored arcs forming organic wings. arch>0 curves up, arch<0 curves down (bat).
    // dir rotates the wing plane around Y axis (0 = wings spread along X axis).

    private void executeWings(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double span = 2.5, arch = 1.0, depth = 0.4, dirDeg = 0.0, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int points = 30, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 30);
            else if (part.startsWith("depth:"))    depth        = parseDouble(part.substring(6), 0.4);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("span:"))     span         = parseDouble(part.substring(5), 2.5);
            else if (part.startsWith("arch:"))     arch         = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("dir:"))      dirDeg       = parseDouble(part.substring(4), 0.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double dirRad = Math.toRadians(dirDeg);
        double cosDir = Math.cos(dirRad);
        double sinDir = Math.sin(dirRad);

        List<Location> locs = new ArrayList<>((points + 1) * 2);
        for (int i = 0; i <= points; i++) {
            double t    = (double) i / points;
            double sine = Math.sin(t * Math.PI);
            for (int w = 0; w < 2; w++) {
                double sign = (w == 0) ? 1.0 : -1.0;
                double lx = sign * t * span;
                double ly = yShift + arch * sine;
                double lz = depth * sine;
                double x  = lx * cosDir - lz * sinDir;
                double z  = lx * sinDir + lz * cosDir;
                locs.add(ctx.getLocation().clone().add(x, ly, z));
            }
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [ARCH] ────────────────────────────────────────────────────────────────
    // A symmetric upright arc (gate/arch shape). arc=180 is a full semicircle.
    // dir rotates the horizontal spread around Y axis.

    private void executeArch(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 1.5, arcDeg = 180.0, dirDeg = 0.0, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int points = 20, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 1.5);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 20);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("arc:"))      arcDeg       = parseDouble(part.substring(4), 180.0);
            else if (part.startsWith("dir:"))      dirDeg       = parseDouble(part.substring(4), 0.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double halfArc = Math.toRadians(arcDeg / 2.0);
        double dirRad  = Math.toRadians(dirDeg);
        double cosDir  = Math.cos(dirRad);
        double sinDir  = Math.sin(dirRad);

        List<Location> locs = new ArrayList<>(points + 1);
        for (int i = 0; i <= points; i++) {
            double t     = (double) i / points;
            double theta = Math.PI / 2.0 - halfArc + t * 2.0 * halfArc;
            double u     = radius * Math.cos(theta);
            double v     = radius * Math.sin(theta);
            double x     = u * cosDir;
            double z     = u * sinDir;
            locs.add(ctx.getLocation().clone().add(x, yShift + v, z));
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── [CLAW] ────────────────────────────────────────────────────────────────
    // N curved lines radiating outward from center, drooping downward at the tip.
    // spread = total angle covered by all claws; curve = angular drift per claw.

    private void executeClaw(String args, SequenceContext ctx) {
        World world = ctx.getLocation().getWorld();
        if (world == null) return;
        String[] parts = args.split(";");
        if (parts.length == 0 || parts[0].isBlank()) return;
        Particle particle = parseParticle(parts[0]);
        if (particle == null) return;

        double radius = 2.0, spreadDeg = 90.0, curveDeg = 20.0, dirDeg = 0.0, drop = 0.3, yShift = 0, dustSize = 1.0, intervalSecs = 0.05;
        int claws = 3, points = 14, count = 1, ticks = 1;
        Color dustColor = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            if      (part.startsWith("interval:")) intervalSecs = parseDouble(part.substring(9), 0.05);
            else if (part.startsWith("spread:"))   spreadDeg    = parseDouble(part.substring(7), 90.0);
            else if (part.startsWith("radius:"))   radius       = parseDouble(part.substring(7), 2.0);
            else if (part.startsWith("points:"))   points       = parseInt(part.substring(7), 14);
            else if (part.startsWith("claws:"))    claws        = parseInt(part.substring(6), 3);
            else if (part.startsWith("curve:"))    curveDeg     = parseDouble(part.substring(6), 20.0);
            else if (part.startsWith("ticks:"))    ticks        = parseInt(part.substring(6), 1);
            else if (part.startsWith("count:"))    count        = parseInt(part.substring(6), 1);
            else if (part.startsWith("color:"))    dustColor    = parseColor(part.substring(6), null);
            else if (part.startsWith("drop:"))     drop         = parseDouble(part.substring(5), 0.3);
            else if (part.startsWith("size:"))     dustSize     = parseDouble(part.substring(5), 1.0);
            else if (part.startsWith("dir:"))      dirDeg       = parseDouble(part.substring(4), 0.0);
            else if (part.startsWith("y:"))        yShift       = parseDouble(part.substring(2), 0);
        }

        double spreadRad  = Math.toRadians(spreadDeg);
        double startAngle = Math.toRadians(dirDeg) - spreadRad / 2.0;
        double angleStep  = claws > 1 ? spreadRad / (claws - 1) : 0.0;
        double curveRad   = Math.toRadians(curveDeg);
        double[] baseAngles = new double[claws];
        for (int c = 0; c < claws; c++) baseAngles[c] = startAngle + c * angleStep;

        List<Location> locs = new ArrayList<>(claws * (points + 1));
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            double r = t * radius;
            for (int c = 0; c < claws; c++) {
                double angle = baseAngles[c] + curveRad * t;
                double x = r * Math.cos(angle);
                double z = r * Math.sin(angle);
                double y = yShift - drop * t * t * radius;
                locs.add(ctx.getLocation().clone().add(x, y, z));
            }
        }
        spawnAnimated(world, particle, locs, count, dustColor, (float) dustSize, ticks, (long)(intervalSecs * 1000), ctx);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void spawnAnimated(World world, Particle particle, List<Location> locs,
                                int count, Color dustColor, float dustSize,
                                int ticks, long intervalMs, SequenceContext ctx) {
        int total = locs.size();
        if (total == 0) return;
        if (ticks <= 1 || total == 1) {
            for (Location loc : locs)
                spawnParticleAt(world, particle, loc, count, 0, 0, 0, 0, dustColor, dustSize, ctx);
            return;
        }
        for (int i = 0; i < total; i++) {
            long delay = (long) ((double) i / (total - 1) * (ticks - 1) * intervalMs);
            Location loc = locs.get(i);
            if (delay == 0) {
                spawnParticleAt(world, particle, loc, count, 0, 0, 0, 0, dustColor, dustSize, ctx);
            } else {
                TaskAPI.atLater(loc, () ->
                    spawnParticleAt(world, particle, loc, count, 0, 0, 0, 0, dustColor, dustSize, ctx),
                    delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void spawnParticleAt(World world, Particle particle, Location loc, int count,
                                  double oX, double oY, double oZ, double speed,
                                  Color dustColor, float dustSize, SequenceContext ctx) {
        Object data = resolveParticleData(particle, dustColor, dustSize);
        if (data == null && particle.getDataType() != Void.class) return;

        for (Player observer : nearbyObservers(world, ctx)) {
            observer.spawnParticle(particle, loc, count, oX, oY, oZ, speed, data);
        }
    }

    /**
     * Resolves which players should perceive this effect: always capped to
     * {@link #MAX_EFFECT_DISTANCE} blocks from the effect location, further narrowed by
     * the caller's optional particleFilter (e.g. visibility toggle) when present.
     */
    private List<Player> nearbyObservers(World world, SequenceContext ctx) {
        Location loc = ctx.getLocation();
        BiPredicate<Player, UUID> filter = ctx.getParticleFilter();
        UUID sourceId = ctx.getSourcePlayer() != null ? ctx.getSourcePlayer().getUniqueId() : null;

        List<Player> observers = new ArrayList<>();
        for (Player online : world.getPlayers()) {
            if (online.getLocation().distanceSquared(loc) > MAX_EFFECT_DISTANCE_SQUARED) continue;
            if (filter != null && sourceId != null && !filter.test(online, sourceId)) continue;
            observers.add(online);
        }
        return observers;
    }

    private Object resolveParticleData(Particle particle, Color dustColor, float dustSize) {
        Class<?> type = particle.getDataType();
        if (type == Particle.DustOptions.class)              return dustColor != null ? new Particle.DustOptions(dustColor, dustSize) : null;
        if (type == Particle.DustTransition.class)           return dustColor != null ? new Particle.DustTransition(dustColor, Color.WHITE, dustSize) : null;
        if (type == Float.class)                             return 0.0f;
        if (type == Integer.class)                           return 0;
        if (type == Color.class)                             return dustColor != null ? dustColor : Color.WHITE;
        if (type == org.bukkit.block.data.BlockData.class)   return Bukkit.createBlockData(Material.STONE);
        if (type == org.bukkit.inventory.ItemStack.class)    return new org.bukkit.inventory.ItemStack(Material.SNOWBALL);
        if (type == Void.class)                              return null;
        return null;
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
