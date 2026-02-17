package net.exylia.commons.v2.crafting.core;

import net.exylia.commons.v2.crafting.model.CustomRecipe;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CraftingRegistry {

    private final Map<String, CustomRecipe> recipesById = new ConcurrentHashMap<>();
    private final Map<NamespacedKey, CustomRecipe> recipesByKey = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> recipesByGroup = new ConcurrentHashMap<>();

    public void register(CustomRecipe recipe) {
        recipesById.put(recipe.getId(), recipe);
        if (recipe.getKey() != null) {
            recipesByKey.put(recipe.getKey(), recipe);
        }
        if (recipe.getGroup() != null && !recipe.getGroup().isEmpty()) {
            recipesByGroup.computeIfAbsent(recipe.getGroup(), k -> ConcurrentHashMap.newKeySet())
                    .add(recipe.getId());
        }
    }

    public void unregister(String id) {
        CustomRecipe recipe = recipesById.remove(id);
        if (recipe != null) {
            if (recipe.getKey() != null) {
                recipesByKey.remove(recipe.getKey());
            }
            if (recipe.getGroup() != null && !recipe.getGroup().isEmpty()) {
                Set<String> groupRecipes = recipesByGroup.get(recipe.getGroup());
                if (groupRecipes != null) {
                    groupRecipes.remove(id);
                    if (groupRecipes.isEmpty()) {
                        recipesByGroup.remove(recipe.getGroup());
                    }
                }
            }
        }
    }

    public void clear() {
        recipesById.clear();
        recipesByKey.clear();
        recipesByGroup.clear();
    }

    public Optional<CustomRecipe> getById(String id) {
        return Optional.ofNullable(recipesById.get(id));
    }

    public Optional<CustomRecipe> getByKey(NamespacedKey key) {
        return Optional.ofNullable(recipesByKey.get(key));
    }

    public List<CustomRecipe> getByGroup(String group) {
        Set<String> ids = recipesByGroup.get(group);
        if (ids == null) return Collections.emptyList();
        return ids.stream()
                .map(recipesById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Collection<CustomRecipe> getAll() {
        return Collections.unmodifiableCollection(recipesById.values());
    }

    public Collection<CustomRecipe> getEnabled() {
        return recipesById.values().stream()
                .filter(CustomRecipe::isEnabled)
                .collect(Collectors.toList());
    }

    public int size() {
        return recipesById.size();
    }

    public boolean contains(String id) {
        return recipesById.containsKey(id);
    }
}
