package net.exylia.commons.v2.visual.builder;

import net.exylia.commons.v2.visual.config.ParticleConfig;
import net.exylia.commons.v2.visual.validation.ValidationResult;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.ArrayList;
import java.util.List;

public class ParticleBuilder extends VisualBuilder<ParticleConfig, ParticleBuilder> {
    private Particle particle;
    private int count = 1;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double offsetZ = 0.0;
    private double extra = 0.0;
    private Color color;
    private ParticleConfig.ParticleScope scope = ParticleConfig.ParticleScope.PLAYER;
    private Location location;

    private ParticleBuilder() {
    }

    public static ParticleBuilder create() {
        return new ParticleBuilder();
    }

    public ParticleBuilder particle(Particle particle) {
        this.particle = particle;
        return this;
    }

    public ParticleBuilder particle(String particleName) {
        try {
            this.particle = Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid particle name: " + particleName);
        }
        return this;
    }

    public ParticleBuilder count(int count) {
        this.count = count;
        return this;
    }

    public ParticleBuilder offset(double x, double y, double z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        return this;
    }

    public ParticleBuilder offsetX(double offsetX) {
        this.offsetX = offsetX;
        return this;
    }

    public ParticleBuilder offsetY(double offsetY) {
        this.offsetY = offsetY;
        return this;
    }

    public ParticleBuilder offsetZ(double offsetZ) {
        this.offsetZ = offsetZ;
        return this;
    }

    public ParticleBuilder extra(double extra) {
        this.extra = extra;
        return this;
    }

    public ParticleBuilder color(Color color) {
        this.color = color;
        return this;
    }

    public ParticleBuilder color(int r, int g, int b) {
        this.color = Color.fromRGB(r, g, b);
        return this;
    }

    public ParticleBuilder scope(ParticleConfig.ParticleScope scope) {
        this.scope = scope;
        return this;
    }

    public ParticleBuilder location(Location location) {
        this.location = location;
        return this;
    }

    public ParticleBuilder nearby() {
        return scope(ParticleConfig.ParticleScope.NEARBY);
    }

    public ParticleBuilder atLocation(Location location) {
        this.scope = ParticleConfig.ParticleScope.LOCATION;
        this.location = location;
        return this;
    }

    public static ParticleConfig fromString(String particleString) {
        String[] parts = particleString.split("\\|");
        ParticleBuilder builder = create();

        if (parts.length >= 1) {
            builder.particle(parts[0].trim());
        }
        if (parts.length >= 2) {
            try {
                builder.count(Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (parts.length >= 3) {
            try {
                builder.offsetX(Double.parseDouble(parts[2].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (parts.length >= 4) {
            try {
                builder.offsetY(Double.parseDouble(parts[3].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (parts.length >= 5) {
            try {
                builder.offsetZ(Double.parseDouble(parts[4].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (parts.length >= 6) {
            try {
                builder.extra(Double.parseDouble(parts[5].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (parts.length >= 7) {
            String[] rgb = parts[6].trim().split(",");
            if (rgb.length == 3) {
                try {
                    int r = Integer.parseInt(rgb[0].trim());
                    int g = Integer.parseInt(rgb[1].trim());
                    int b = Integer.parseInt(rgb[2].trim());
                    builder.color(r, g, b);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return builder.build();
    }

    @Override
    protected ValidationResult validateInternal() {
        List<String> errors = new ArrayList<>();

        if (particle == null) {
            errors.add("Particle must be specified");
        }

        if (count < 1) {
            errors.add("Count must be >= 1");
        }

        if (scope == ParticleConfig.ParticleScope.LOCATION && location == null) {
            errors.add("Location must be specified when scope is LOCATION");
        }

        if (particle == Particle.REDSTONE && color == null) {
            errors.add("Color must be specified for DUST particle");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    @Override
    protected ParticleConfig buildInternal() {
        ParticleConfig config = ParticleConfig.builder()
                .particle(particle)
                .count(count)
                .offsetX(offsetX)
                .offsetY(offsetY)
                .offsetZ(offsetZ)
                .extra(extra)
                .color(color)
                .scope(scope)
                .location(location)
                .build();

        config.setEnabled(enabled);
        config.setUpdateInterval(updateInterval);
        config.setPermanent(permanent);

        return config;
    }
}
