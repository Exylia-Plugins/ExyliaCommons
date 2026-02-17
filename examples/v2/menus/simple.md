---

````md
# Sistema de Menús — Ejemplo Básico

---

## Archivo: `menus.yml`

```yml
simple-menu:
  title: '&6&lMenú Principal'
  type: SIMPLE
  size: 27

  filler:
    global:
      material: GRAY_STAINED_GLASS_PANE
      name: ' '

  items:
    info:
      material: DIAMOND
      name: '&bInfo'
      lore:
        - '&7Hola %player_name%'
      slot: 13
      click_actions:
        - 'message: &aClick!'

    close:
      material: BARRIER
      name: '&cCerrar'
      slot: 22
      click_actions:
        - 'close'
````

---

## Uso en Java

### Inicialización

```java
MenuAPI.initialize(this);
```

---

### Abrir el menú

```java
ConfigurationSection config = getConfig().getConfigurationSection("simple-menu");
MenuAPI.open(player, config);
```

---

## Flujo

1. El plugin carga `menus.yml`
2. Se inicializa el sistema con `MenuAPI.initialize(this)`
3. Se obtiene la sección del menú desde la config
4. Se abre con `MenuAPI.open(player, config)`

---

Si quieres, en el siguiente paso puedo ayudarte a documentar **acciones (`click_actions`)**, **tipos de menú**, o **sistema de placeholders dentro del menú**.
