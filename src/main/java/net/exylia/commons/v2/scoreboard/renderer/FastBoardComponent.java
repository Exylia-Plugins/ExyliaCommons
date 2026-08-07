package net.exylia.commons.v2.scoreboard.renderer;

import net.exylia.commons.v2.scoreboard.fastboard.FastBoardBase;
import net.exylia.commons.v2.scoreboard.fastboard.FastReflection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class FastBoardComponent extends FastBoardBase<Component> {

    private static final MethodHandle ADVENTURE_CONVERTER;
    private static final MethodHandle CHAT_SERIALIZER_METHOD;
    private static final Object EMPTY_COMPONENT;
    private static final boolean USE_PAPER_ADVENTURE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle adventureConverter = null;
            boolean usePaperAdventure = false;

            try {
                Class<?> paperAdventureClass = Class.forName("io.papermc.paper.adventure.PaperAdventure");
                Method asVanillaMethod = paperAdventureClass.getDeclaredMethod("asVanilla", Component.class);
                adventureConverter = lookup.unreflect(asVanillaMethod);
                usePaperAdventure = true;
            } catch (Throwable ignored) {
            }

            ADVENTURE_CONVERTER = adventureConverter;
            USE_PAPER_ADVENTURE = usePaperAdventure;

            if (!usePaperAdventure) {
                Class<?> componentClass = FastReflection.nmsClass("network.chat", "IChatBaseComponent", "Component");
                Class<?> serializerClass;

                try {
                    serializerClass = FastReflection.nmsClass("network.chat", "Component$Serializer");
                } catch (ClassNotFoundException e) {
                    serializerClass = FastReflection.nmsClass("network.chat", "IChatBaseComponent$ChatSerializer");
                }

                String methodName = FastReflection.nmsOptionalClass("network.chat", "Component$Serializer").isPresent()
                    ? "fromJson" : "a";

                CHAT_SERIALIZER_METHOD = lookup.findStatic(
                    serializerClass,
                    methodName,
                    java.lang.invoke.MethodType.methodType(componentClass, String.class)
                );
                EMPTY_COMPONENT = CHAT_SERIALIZER_METHOD.invoke("\"\"");
            } else {
                CHAT_SERIALIZER_METHOD = null;
                EMPTY_COMPONENT = adventureConverter.invoke(Component.empty());
            }
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    public FastBoardComponent(Player player) {
        super(player);
    }

    @Override
    protected void sendLineChange(int score) throws Throwable {
        Component line = getLineByScore(score);
        sendTeamPacket(score, TeamMode.UPDATE, line, null);
    }

    @Override
    protected Object toMinecraftComponent(Component component) throws Throwable {
        if (component == null) {
            return EMPTY_COMPONENT;
        }

        if (USE_PAPER_ADVENTURE) {
            return ADVENTURE_CONVERTER.invoke(component);
        }

        String json = GsonComponentSerializer.gson().serialize(component);
        return CHAT_SERIALIZER_METHOD.invoke(json);
    }

    @Override
    protected String serializeLine(Component component) {
        if (component == null) {
            return "";
        }
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    @Override
    protected Component emptyLine() {
        return Component.empty();
    }
}
