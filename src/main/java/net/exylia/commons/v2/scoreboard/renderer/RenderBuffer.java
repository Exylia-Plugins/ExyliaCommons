package net.exylia.commons.v2.scoreboard.renderer;

import lombok.Getter;
import lombok.Setter;

/**
 * Buffer reutilizable por instancia donde el renderer deposita el resultado.
 * <p>
 * Existe para que un render no asigne ni una {@code List}, ni un array, ni un
 * record por ciclo. Con N jugadores actualizando cada segundo el diseño anterior
 * generaba 4 objetos por jugador por tick de update solo en el camino de render.
 * <p>
 * Es propiedad exclusiva de una {@code ScoreboardInstance} y solo se toca desde
 * el hilo que tiene el permiso de render de esa instancia.
 */
public class RenderBuffer {

    private String[] lines;

    @Getter
    private int lineCount;

    @Getter
    @Setter
    private String title = "";

    public RenderBuffer(int initialCapacity) {
        this.lines = new String[Math.max(1, initialCapacity)];
        this.lineCount = 0;
    }

    /**
     * @return el array interno; valido solo hasta {@link #getLineCount()}
     */
    public String[] lines() {
        return lines;
    }

    public void reset() {
        lineCount = 0;
    }

    /**
     * Añade una linea creciendo el array si hace falta.
     * El crecimiento solo ocurre cuando un placeholder expande a mas lineas de
     * las previstas; tras un par de ciclos el buffer se estabiliza y no vuelve
     * a asignar.
     */
    public void add(String line) {
        if (lineCount == lines.length) {
            String[] grown = new String[lines.length * 2];
            System.arraycopy(lines, 0, grown, 0, lines.length);
            lines = grown;
        }
        lines[lineCount++] = line;
    }
}
