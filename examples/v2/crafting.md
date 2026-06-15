# Sistema de Crafting Personalizado (Base64)

## Inicialización

```java
CraftingAPI.initialize(this);
CraftingAPI.loadRecipes(Configs.get("recipes"), "recipes");

// onDisable
CraftingAPI.shutdown();
```

## YAML (Base64)

```yaml
recipes:
  diamond_sword_custom:
    enabled: true
    type: SHAPED
    permission: "server.craft.sword"
    shape:
      - " D "
      - " D "
      - " S "
    ingredients:
      D: "rO0ABXNyABpvcmcuYnVra2l0Lml..."  # Base64 del ItemStack
      S: "rO0ABXNyABpvcmcuYnVra2l0Lml..."
    result: "rO0ABXNyABpvcmcuYnVra2l0Lml..."  # Base64 del resultado
    result-amount: 1

  magic_dust:
    enabled: true
    type: SHAPELESS
    ingredients:
      1:
        base64: "rO0ABXNyABpvcmcuYnVra2l0Lml..."
        amount: 4
      2:
        base64: "rO0ABXNyABpvcmcuYnVra2l0Lml..."
        amount: 2
    result: "rO0ABXNyABpvcmcuYnVra2l0Lml..."
```

## Registro programático

```java
// Obtener Base64 de items in-game
ItemStack itemEnMano = player.getInventory().getItemInMainHand();
String base64 = CraftingAPI.toBase64(itemEnMano);

// Crear receta
CustomRecipe recipe = CustomRecipe.builder()
    .id("mi_receta")
    .key(new NamespacedKey(plugin, "mi_receta"))
    .type(RecipeType.SHAPED)
    .shape(List.of("AAA", "ABA", "AAA"))
    .ingredientMap(Map.of(
        'A', CraftingAPI.ingredientFromItem(diamante),
        'B', CraftingAPI.ingredientFromItem(estrella)
    ))
    .resultBase64(CraftingAPI.toBase64(resultado))
    .resultAmount(1)
    .build();

CraftingAPI.registerRecipe(recipe);
```

## Utilidades Base64

```java
// ItemStack -> Base64
String base64 = CraftingAPI.toBase64(itemStack);

// Base64 -> ItemStack
ItemStack item = CraftingAPI.fromBase64(base64);

// Crear ingrediente desde ItemStack
RecipeIngredient ing = CraftingAPI.ingredientFromItem(itemStack);
```

## Estructura

```
crafting/
├── api/CraftingAPI.java
├── core/
│   ├── CraftingManager.java
│   └── CraftingRegistry.java
├── listener/CraftingListener.java
├── model/
│   ├── CraftingResult.java
│   ├── CustomRecipe.java
│   ├── RecipeIngredient.java
│   └── RecipeType.java
├── processor/
│   ├── IngredientMatcher.java
│   └── RecipeProcessor.java
└── reload/CraftingAdapter.java
```
