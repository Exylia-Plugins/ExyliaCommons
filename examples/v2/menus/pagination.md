---

````md
# Menú con Paginación — Items Browser

---

## Archivo: `menus.yml`

```yml
items-browser:
  title: '&6Items &7(%current_page%/%total_pages%)'
  type: PAGINATION
  size: 54

  filler:
    global:
      material: BLACK_STAINED_GLASS_PANE
      name: ' '

  pagination:
    slots: '10-16,19-25,28-34,37-43'

    item_template:
      material: '%item_material%'
      name: '%item_display_name%'
      lore:
        - ''
        - '&7Click para obtener'
      click_actions:
        - 'command: give %player_name% %item_id% 1'

    navigation:
      previous:
        slot: 48
        material: ARROW
        name: '&e← Anterior'
        click_actions:
          - 'previous_page'
      next:
        slot: 50
        material: ARROW
        name: '&eSiguiente →'
        click_actions:
          - 'next_page'
      info:
        slot: 49
        material: PAPER
        name: '&7Página %current_page%/%total_pages%'

  items:
    close:
      material: BARRIER
      name: '&cCerrar'
      slot: 45
      click_actions:
        - 'close'
````

---

## Uso en Java

### Abrir el menú con paginación

```java
public void open(Player player) {
    ConfigurationSection config = getConfig().getConfigurationSection("items-browser");

    List<Material> materials = Arrays.stream(Material.values())
        .filter(Material::isItem)
        .filter(m -> !m.isAir())
        .toList();

    MenuData menuData = MenuAPI.parse(config)
        .withPaginationData(materials, material ->
            PlaceholderContext.create()
                .put("item_material", material.name())
                .put("item_display_name", formatName(material.name()))
                .put("item_id", material.name().toLowerCase())
        );

    MenuAPI.open(player, menuData);
}
```

---

### Formatear nombres

```java
private String formatName(String name) {
    return Arrays.stream(name.split("_"))
        .map(s -> s.charAt(0) + s.substring(1).toLowerCase())
        .collect(Collectors.joining(" "));
}
```

---

## ★ Insight

* `withPaginationData()` usa **genéricos `<T>`**: acepta cualquier tipo de lista (Material, DTOs de DB, etc.)
* `Function<T, PlaceholderContext>` te da control total sobre qué placeholders inyectar por cada item
* El método es **fluent** (retorna `this`) para permitir encadenamiento

---