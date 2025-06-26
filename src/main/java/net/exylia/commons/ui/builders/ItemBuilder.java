
package net.exylia.commons.ui.builders;

import net.exylia.commons.ui.items.MenuItem;
import org.bukkit.Material;

import java.util.List;

/**
 * Base builder for menu items
 */
public abstract class ItemBuilder<T extends ItemBuilder<T>> {

    protected MenuItem item;

    protected ItemBuilder(MenuItem item) {
        this.item = item;
    }

    protected ItemBuilder(Material material) {
        this.item = new MenuItem(material);
    }

    protected ItemBuilder(String materialString) {
        this.item = new MenuItem(materialString);
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T name(String name) {
        item.setName(name);
        return self();
    }

    public T lore(String... lore) {
        item.setLore(lore);
        return self();
    }

    public T lore(List<String> lore) {
        item.setLoreList(lore);
        return self();
    }

    public T amount(int amount) {
        item.setAmount(amount);
        return self();
    }

    public T amount(String amountString) {
        item.setAmount(amountString);
        return self();
    }

    public T glow(boolean glowing) {
        item.setGlowing(glowing);
        return self();
    }

    public T hideAttributes() {
        item.hideAllAttributes();
        return self();
    }

    public T dynamicUpdate(boolean dynamic) {
        item.setDynamicUpdate(dynamic);
        return self();
    }

    public T updateInterval(long interval) {
        item.setUpdateInterval(interval);
        return self();
    }

    public MenuItem build() {
        return item;
    }
}