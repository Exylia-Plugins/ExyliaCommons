---

````md
# Menú de Input de Ítems — Item Input

---

## Archivo: `menus.yml`

```yml
item-input-menu:
  title: '&6Configurar Kit'
  type: ITEM_INPUT
  size: 54

  filler:
    global:
      material: BLACK_STAINED_GLASS_PANE
      name: ' '

  editable_slots: '10-16,19-25,28-34'

  items:
    confirm:
      material: EMERALD
      name: '&aConfirmar'
      slot: 49
      click_actions:
        - 'close'

    cancel:
      material: BARRIER
      name: '&cCancelar'
      slot: 45
      click_actions:
        - 'close'
```

---

## Uso en Java

### Abrir el menú y recoger los ítems al cerrar

```java
public void open(Player player) {
    ConfigurationSection config = getConfig().getConfigurationSection("item-input-menu");

    MenuAPI.openItemInput(player, config, items -> {
        // Llamado en el hilo principal al cerrar el menú
        items.forEach((slot, item) -> {
            player.sendMessage("Slot " + slot + ": " + item.getType().name());
        });
    });
}
```

---

### Leer los ítems en cualquier momento (sin callback)

```java
MenuAPI.getActiveMenu(player).ifPresent(menu -> {
    if (menu instanceof ItemInputMenu inputMenu) {
        Map<Integer, ItemStack> items = inputMenu.getEditableItems();
    }
});
```

---

## Flujo

1. El jugador abre el menú — los slots editables están vacíos
2. El jugador arrastra ítems desde su inventario a los slots editables
3. Al cerrar (o al pulsar "Confirmar"), el callback recibe un `Map<Integer, ItemStack>` con cada slot y su ítem
4. Los slots no editables están bloqueados como en cualquier otro menú

---

## Comportamiento de slots editables

| Acción                          | Resultado                                  |
|---------------------------------|--------------------------------------------|
| Click sobre slot editable       | Permitido (mueve ítems normalmente)        |
| Drag sobre slots editables      | Permitido si todos los slots son editables |
| Shift-click desde inventario    | Va al primer slot editable vacío           |
| Click sobre slot no editable    | Bloqueado, ejecuta `click_actions`         |

````

---
