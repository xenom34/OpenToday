package ru.fazziclay.opentoday.app.items;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.UUID;

import ru.fazziclay.opentoday.app.TickSession;
import ru.fazziclay.opentoday.app.items.callback.OnItemStorageUpdate;
import ru.fazziclay.opentoday.app.items.item.Item;
import ru.fazziclay.opentoday.callback.CallbackStorage;

/**
 * Interface items container
 * @author fazziclay
 * @see ItemsStorage#addItem(Item)
 * @see ItemsStorage#deleteItem(Item)
 * @see ItemsStorage#copyItem(Item)
 * @see ItemsStorage#getItemPosition(Item)
 * @see ItemsStorage#getItemById(UUID)
 * @see ItemsStorage#move(int, int)
 * @see ItemsStorage#getAllItems()
 * @see ItemsStorage#tick(TickSession)
 * @see ItemsStorage#save()
 * @see ItemsStorage#size()
 * @see ItemsStorage#getOnUpdateCallbacks()
 */
public interface ItemsStorage {
    /**
     * Add item to this ItemStorage
     * @param item item to add
     * @see Item
     */
    void addItem(Item item);

    /**
     * Delete item from ItemStorage
     * @param item item to delete
     */
    void deleteItem(Item item);

    /**
     * Copy item and automatically add to this ItemStorage
     * @param item item to copy
     * @return new item (copy)
     * @see Item
     */
    @NonNull Item copyItem(Item item);

    /**
     * Get item position in itemStorage
     * @param item item to find
     * @return position in ItemStorage, -1 if not found (default by List#indexOf)
     * @see Item
     * @see List#indexOf(Object)
     */
    int getItemPosition(Item item);

    /**
     * Get item by itemId
     * @param itemId itemId to find
     * @return item, if not found null
     * @see UUID
     * @see Item
     */
    @Nullable Item getItemById(UUID itemId);

    /**
     * Move items
     * @param positionFrom from
     * @param positionTo to
     */
    void move(int positionFrom, int positionTo);

    /**
     * Get all items in this ItemStorage (only root)
     * @return item in user-position
     * @see Item
     */
    @NonNull Item[] getAllItems();

    /**
     * Tick function
     * Call every seconds for user-like
     * @param tickSession see tickSession javaDoc
     * @see TickSession
     */
    void tick(TickSession tickSession);

    /**
     * Save data (Call to up (to up, to up, to up)) -> call save to ItemManager => Save all
     * @see ItemManager#save()
     */
    void save();

    /**
     * Get items count in this ItemStorage (only root)
     * @return count of items
     */
    int size();

    /**
     * Get OnItemStorageUpdate CallbackStorage
     * @return callbackStorage
     * @see OnItemStorageUpdate
     * @see CallbackStorage
     */
    @NonNull CallbackStorage<OnItemStorageUpdate> getOnUpdateCallbacks();
}
