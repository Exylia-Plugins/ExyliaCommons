package net.exylia.commons.menu;

import net.exylia.commons.actions.ActionContext;
import net.exylia.commons.actions.ActionSource;
import net.exylia.commons.actions.GlobalActionManager;

/**
 * Adaptador para integrar el sistema de menús con el sistema global de acciones
 */
@Deprecated
public class MenuActionAdapter {

    /**
     * Ejecuta una acción desde un menú utilizando el sistema global
     * @param actionString String de acción
     * @param clickInfo Información del clic en menú
     * @return true si la acción fue ejecutada
     */
    public static boolean executeMenuAction(String actionString, MenuClickInfo clickInfo) {
        ActionContext context = new ActionContext(clickInfo.player(), ActionSource.MENU)
                .withData("clickType", clickInfo.clickType())
                .withData("slot", clickInfo.slot())
                .withData("menu", clickInfo.menu())
                .withData("menuItem", clickInfo.item());

        return GlobalActionManager.executeAction(actionString, context);
    }
}