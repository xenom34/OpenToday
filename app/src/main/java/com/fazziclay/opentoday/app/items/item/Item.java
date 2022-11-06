package com.fazziclay.opentoday.app.items.item;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fazziclay.opentoday.annotation.Getter;
import com.fazziclay.opentoday.annotation.RequireSave;
import com.fazziclay.opentoday.annotation.SaveKey;
import com.fazziclay.opentoday.annotation.Setter;
import com.fazziclay.opentoday.app.TickSession;
import com.fazziclay.opentoday.app.items.ID;
import com.fazziclay.opentoday.app.items.notification.ItemNotification;
import com.fazziclay.opentoday.app.items.notification.ItemNotificationIEUtil;
import com.fazziclay.opentoday.app.items.notification.ItemNotificationUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main app count (contain information) todo add javadoc to Item :)
 */
public abstract class Item implements ID {
    // START - Save
    public static class ItemIETool extends ItemImportExportTool {
        @NonNull
        @Override
        public JSONObject exportItem(@NonNull Item item) throws Exception {
            return new JSONObject()
                    .put("id", item.id.toString())
                    .put("viewMinHeight", item.viewMinHeight)
                    .put("viewBackgroundColor", item.viewBackgroundColor)
                    .put("viewCustomBackgroundColor", item.viewCustomBackgroundColor)
                    .put("minimize", item.minimize)
                    .put("notifications", ItemNotificationIEUtil.exportNotifications(item.notifications));
        }

        private final Item defaultValues = new Item(){};
        @NonNull
        @Override
        public Item importItem(@NonNull JSONObject json, Item item) throws Exception {
            applyId(item, json);
            item.viewMinHeight = json.optInt("viewMinHeight", defaultValues.viewMinHeight);
            item.viewBackgroundColor = json.optInt("viewBackgroundColor", defaultValues.viewBackgroundColor);
            item.viewCustomBackgroundColor = json.optBoolean("viewCustomBackgroundColor", defaultValues.viewCustomBackgroundColor);
            item.minimize = json.optBoolean("minimize", defaultValues.minimize);
            JSONArray jsonArray = json.optJSONArray("notifications");
            item.notifications = ItemNotificationIEUtil.importNotifications(jsonArray != null ? jsonArray : new JSONArray());
            return item;
        }

        private void applyId(Item o, JSONObject json) {
            String stringId = json.optString("id", null);
            if (stringId == null) {
                o.id = UUID.randomUUID();
            } else {
                try {
                    o.id = UUID.fromString(stringId);
                } catch (Exception e) {
                    o.id = UUID.randomUUID();
                }
            }
        }
    }
    // END - Save

    private static final String DEFAULT_BACKGROUND_COLOR = "#99999999";

    @Nullable @RequireSave @SaveKey(key = "id") private UUID id;
    @Nullable private ItemController controller;
    @SaveKey(key = "viewMinHeight") @RequireSave private int viewMinHeight = 0; // минимальная высота
    @SaveKey(key = "viewBackgroundColor") @RequireSave private int viewBackgroundColor = Color.parseColor(DEFAULT_BACKGROUND_COLOR); // фоновый цвет
    @SaveKey(key = "viewCustomBackgroundColor") @RequireSave private boolean viewCustomBackgroundColor = false; // юзаем ли фоновый цвет
    @SaveKey(key = "minimize") @RequireSave private boolean minimize = false;
    @NonNull @SaveKey(key = "notifications") @RequireSave private List<ItemNotification> notifications = new ArrayList<>();

    // Copy constructor
    public Item(@Nullable Item copy) {
        // unattached
        this.id = null;
        this.controller = null;

        // copy
        if (copy != null) {
            this.viewMinHeight = copy.viewMinHeight;
            this.viewBackgroundColor = copy.viewBackgroundColor;
            this.viewCustomBackgroundColor = copy.viewCustomBackgroundColor;
            this.minimize = copy.minimize;
            this.notifications = ItemNotificationUtil.copy(copy.notifications);
        }
    }

    public Item() {
        this(null);
    }

    // For fast get text (no cast to TextItem)
    public String getText() {
        return "{Item}";
    }

    public void delete() {
        if (isAttached()) controller.delete(this);
    }

    public void save() {
        if (isAttached()) controller.save(this);
    }

    public void visibleChanged() {
        if (isAttached()) controller.updateUi(this);
    }

    public boolean isAttached() {
        return controller != null;
    }

    /**
     * set controller and regenerate ids
     * @param itemController controller
     */
    public void attach(ItemController itemController) {
        this.controller = itemController;
        regenerateId();
    }

    public void detach() {
        this.controller = null;
        this.id = null;
    }

    public void tick(TickSession tickSession) {
        ItemNotificationUtil.tick(tickSession, notifications, this);
    }

    public Item regenerateId() {
        this.id = UUID.randomUUID();
        return this;
    }

    /**
     * Copy this item
     * @return copy
     */
    public Item copy() {
        return ItemsRegistry.REGISTRY.get(this.getClass()).copy(this);
    }

    // Getters & Setters
    @Nullable @Override @Getter public UUID getId() { return id; }

    public void setController(@Nullable ItemController controller) {
        this.controller = controller;
    }

    @Getter public int getViewMinHeight() { return viewMinHeight; }
    @Setter public void setViewMinHeight(int v) { this.viewMinHeight = v; }

    @Getter public int getViewBackgroundColor() { return viewBackgroundColor; }
    @Setter public void setViewBackgroundColor(int v) { this.viewBackgroundColor = v; }

    @Getter public boolean isViewCustomBackgroundColor() { return viewCustomBackgroundColor; }
    @Setter public void setViewCustomBackgroundColor(boolean v) { this.viewCustomBackgroundColor = v; }

    @Getter public boolean isMinimize() { return minimize; }
    @Setter public void setMinimize(boolean minimize) { this.minimize = minimize; }

    @Getter @NonNull public List<ItemNotification> getNotifications() { return notifications; }
}