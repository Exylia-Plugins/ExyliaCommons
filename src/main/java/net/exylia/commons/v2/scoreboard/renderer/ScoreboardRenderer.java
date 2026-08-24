package net.exylia.commons.v2.scoreboard.renderer;

import net.exylia.commons.v2.placeholders.context.PlaceholderContext;
import org.bukkit.entity.Player;

public interface ScoreboardRenderer {

    /**
     * Resuelve titulo y lineas dejando el resultado en {@code out}.
     * <p>
     * Se ejecuta fuera del hilo principal y no debe tocar la API de Bukkit mas
     * alla de leer datos del jugador. No envia packets: de eso se encarga
     * {@link SidebarHandle}.
     */
    void render(Player player, PlaceholderContext context, RenderBuffer out);

    /**
     * @return numero de lineas configuradas, usado para dimensionar el sidebar
     */
    int lineCount();
}
