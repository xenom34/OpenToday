package ru.fazziclay.opentoday.app.items.item;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import ru.fazziclay.opentoday.annotation.RequireSave;
import ru.fazziclay.opentoday.annotation.SaveKey;
import ru.fazziclay.opentoday.app.App;
import ru.fazziclay.opentoday.app.TickSession;
import ru.fazziclay.opentoday.app.items.ItemsStorage;
import ru.fazziclay.opentoday.app.items.ItemsUtils;
import ru.fazziclay.opentoday.app.items.callback.OnItemStorageUpdate;
import ru.fazziclay.opentoday.callback.CallbackStorage;

public class FilterGroupItem extends TextItem implements ContainerItem, ItemsStorage {
    // START - Save
    public final static FilterGroupItemIETool IE_TOOL = new FilterGroupItemIETool();
    public static class FilterGroupItemIETool extends TextItem.TextItemIETool {
        @NonNull
        @Override
        public JSONObject exportItem(@NonNull Item item) throws Exception {
            FilterGroupItem filterGroupItem = (FilterGroupItem) item;

            JSONArray itemsArray = new JSONArray();
            for (ItemFilterWrapper wrapper : filterGroupItem.items) {
                itemsArray.put(wrapper.exportWrapper());
            }

            return super.exportItem(filterGroupItem)
                    .put("items", itemsArray);
        }

        @NonNull
        @Override
        public Item importItem(@NonNull JSONObject json, Item item) throws Exception {
            FilterGroupItem filterGroupItem = item != null ? (FilterGroupItem) item : new FilterGroupItem();
            super.importItem(json, filterGroupItem);

            // Items
            JSONArray itemsArray = json.optJSONArray("items");
            if (itemsArray == null) itemsArray = new JSONArray();
            int i = 0;
            while (i < itemsArray.length()) {
                JSONObject jsonWrapper = itemsArray.getJSONObject(i);
                ItemFilterWrapper wrapper = ItemFilterWrapper.importWrapper(jsonWrapper);
                wrapper.item.setController(filterGroupItem.groupItemController);
                filterGroupItem.items.add(wrapper);
                i++;
            }

            return filterGroupItem;
        }
    }
    // END - Save

    public static FilterGroupItem createEmpty() {
        return new FilterGroupItem("");
    }

    @NonNull @SaveKey(key = "items") @RequireSave private final List<ItemFilterWrapper> items = new ArrayList<>();
    @NonNull private final List<ItemFilterWrapper> activeItems = new ArrayList<>();
    @NonNull private final ItemController groupItemController = new FilterGroupItemController();
    @NonNull private final CallbackStorage<OnItemStorageUpdate> itemStorageUpdateCallbacks = new CallbackStorage<>();

    protected FilterGroupItem() {
        super();
    }

    // append
    public FilterGroupItem(String text) {
        super(text);
    }

    // Copy
    public FilterGroupItem(FilterGroupItem copy) {
        super(copy);
        for (ItemFilterWrapper copyWrapper : copy.items) {
            try {
                ItemFilterWrapper newWrapper = ItemFilterWrapper.importWrapper(copyWrapper.exportWrapper());
                newWrapper.item.setController(this.groupItemController);
                this.items.add(newWrapper);
            } catch (Exception e) {
                throw new RuntimeException("Copy exception", e);
            }
        }
    }

    @Nullable
    public ItemFilter getItemFilter(Item item) {
        for (ItemFilterWrapper wrapper : items) {
            if (wrapper.item == item) return wrapper.filter;
        }

        return null;
    }

    public Item[] getActiveItems() {
        List<Item> ret = new ArrayList<>();
        for (ItemFilterWrapper activeItem : activeItems) {
            ret.add(activeItem.item);
        }
        return ret.toArray(new Item[0]);
    }

    @NonNull
    @Override
    public Item[] getAllItems() {
        List<Item> ret = new ArrayList<>();
        for (ItemFilterWrapper wrapper : items) {
            ret.add(wrapper.item);
        }
        return ret.toArray(new Item[0]);
    }

    @Override
    public Item regenerateId() {
        super.regenerateId();
        for (ItemFilterWrapper item : items) {
            item.item.regenerateId();
        }
        return this;
    }

    // Item storage
    @Override
    public int size() {
        return items.size();
    }

    private void addItem(ItemFilterWrapper item) {
        if (item.item.getClass() == Item.class) {
            throw new RuntimeException("'Item' not allowed to add (add Item parents)");
        }
        item.item.setController(groupItemController);
        item.item.regenerateId();
        items.add(item);
        itemStorageUpdateCallbacks.run((callbackStorage, callback) -> callback.onAdded(item.item));
        if (!recalculate(new GregorianCalendar())) {
            visibleChanged();
        }
        save();
    }
    
    @Override
    public void addItem(Item item) {
        addItem(new ItemFilterWrapper(item, new ItemFilter()));
    }

    @Override
    public void deleteItem(Item item) {
        App.get().getItemManager().deselectItem(item); // TODO: 31.08.2022 other fix??  !!BUGFIX!!
        itemStorageUpdateCallbacks.run((callbackStorage, callback) -> callback.onDeleted(item));

        ItemFilterWrapper toDel = null;
        for (ItemFilterWrapper wrapper : items) {
            if (wrapper.item == item) toDel = wrapper;
        }

        items.remove(toDel);

        if (!recalculate(new GregorianCalendar())) {
            visibleChanged();
        }
        save();
    }

    @NonNull
    @Override
    public Item copyItem(Item item) {
        ItemFilter filter = getItemFilter(item);

        Item copy = ItemsRegistry.REGISTRY.getItemInfoByClass(item.getClass()).copy(item);
        ItemFilter copyFilter;
        try {
            copyFilter = filter.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Copy error", e);
        }
        addItem(new ItemFilterWrapper(copy, copyFilter));
        return copy;
    }

    @Override
    public void move(int positionFrom, int positionTo) {
        Item item = getAllItems()[positionFrom];
        Collections.swap(items, positionFrom, positionTo);
        itemStorageUpdateCallbacks.run((callbackStorage, callback) -> callback.onMoved(item, positionFrom));

        if (!recalculate(new GregorianCalendar())) {
            visibleChanged();
        }
        save();
    }

    @Override
    public int getItemPosition(Item item) {
        int i = 0;

        for (ItemFilterWrapper wrapper : items) {
            if (wrapper.item == item) return i;
            i++;
        }

        return -1; // List.indexOf()
    }

    @NonNull
    @Override
    public CallbackStorage<OnItemStorageUpdate> getOnUpdateCallbacks() {
        return itemStorageUpdateCallbacks;
    }

    @Override
    public Item getItemById(UUID itemId) {
        return ItemsUtils.getItemById(getAllItems(), itemId);
    }

    @Override
    public void tick(TickSession tickSession) {
        super.tick(tickSession);
        recalculate(tickSession.getGregorianCalendar());

        // NOTE: No use 'for-loop' (self-delete item in tick => ConcurrentModificationException)
        List<ItemFilterWrapper> tickList = activeItems; // TODO: 24.08.2022 add tick behavior
        int i = tickList.size() - 1;
        while (i >= 0) {
            tickList.get(i).item.tick(tickSession);
            i--;
        }
    }

    public boolean recalculate(GregorianCalendar gregorianCalendar) {
        boolean isUpdated = false;
        for (ItemFilterWrapper wrapper : items) {
            boolean fit = wrapper.filter.isFit(gregorianCalendar);
            if (fit) {
                if (!activeItems.contains(wrapper)) {
                    activeItems.add(wrapper);
                    isUpdated = true;
                }
            } else {
                if (activeItems.contains(wrapper)) {
                    activeItems.remove(wrapper);
                    isUpdated = true;
                }
            }
        }

        if (isUpdated) {
            visibleChanged();
            return true;
        }
        return false;
    }

    public static class ItemFilterWrapper {
        private final Item item;
        private final ItemFilter filter;

        public ItemFilterWrapper(Item item, ItemFilter filter) {
            this.item = item;
            this.filter = filter;
        }

        public JSONObject exportWrapper() throws Exception {
            return new JSONObject()
                    .put("item", ItemIEUtil.exportItem(item))
                    .put("filter", filter.export());
        }

        public static ItemFilterWrapper importWrapper(JSONObject json) throws Exception {
            return new ItemFilterWrapper(ItemIEUtil.importItem(json.getJSONObject("item")), ItemFilter.importFilter(json.getJSONObject("filter")));
        }
    }

    public static class ItemFilter implements Cloneable {
        private IntegerValue year = null;
        private IntegerValue month = null;
        private IntegerValue dayOfMonth = null;
        private IntegerValue dayOfWeek = null;
        private IntegerValue weekOfYear = null;
        private IntegerValue dayOfYear = null;
        private IntegerValue hour = null;
        private IntegerValue minute = null;
        private IntegerValue second = null;


        public boolean isFit(GregorianCalendar calendar) {
            return check(calendar, year, Calendar.YEAR) &&
                    check(calendar, month, Calendar.MONTH) &&
                    check(calendar, dayOfMonth, Calendar.DAY_OF_MONTH) &&
                    check(calendar, dayOfWeek, Calendar.DAY_OF_WEEK) &&
                    check(calendar, dayOfYear, Calendar.DAY_OF_YEAR) &&
                    check(calendar, weekOfYear, Calendar.WEEK_OF_YEAR) &&
                    check(calendar, hour, Calendar.HOUR_OF_DAY) &&
                    check(calendar, minute, Calendar.MINUTE) &&
                    check(calendar, second, Calendar.SECOND);
        }

        public boolean check(Calendar calendar, IntegerValue integerValue, int field) {
            if (integerValue != null) {
                return integerValue.isFit(calendar.get(field));
            }
            return true;
        }

        public JSONObject export() throws JSONException {
            JSONObject j = new JSONObject();
            if (year != null) { j.put("year", year.exportI()); }
            if (month != null) { j.put("month", month.exportI()); }
            if (dayOfWeek != null) { j.put("dayOfWeek", dayOfWeek.exportI()); }
            if (dayOfMonth != null) { j.put("dayOfMonth", dayOfMonth.exportI()); }
            if (weekOfYear != null) { j.put("weekOfYear", weekOfYear.exportI()); }
            if (dayOfYear != null) { j.put("dayOfYear", dayOfYear.exportI()); }
            if (hour != null) { j.put("hour", hour.exportI()); }
            if (minute != null) { j.put("minute", minute.exportI()); }
            if (second != null) { j.put("second", second.exportI()); }

            return j;
        }

        public static ItemFilter importFilter(JSONObject json) {
            ItemFilter i = new ItemFilter();

            i.year = IntegerValue.importI(json.optJSONObject("year"));
            i.month = IntegerValue.importI(json.optJSONObject("month"));
            i.dayOfWeek = IntegerValue.importI(json.optJSONObject("dayOfWeek"));
            i.dayOfMonth = IntegerValue.importI(json.optJSONObject("dayOfMonth"));
            i.weekOfYear = IntegerValue.importI(json.optJSONObject("weekOfYear"));
            i.dayOfYear = IntegerValue.importI(json.optJSONObject("dayOfYear"));
            i.hour = IntegerValue.importI(json.optJSONObject("hour"));
            i.minute = IntegerValue.importI(json.optJSONObject("minute"));
            i.second = IntegerValue.importI(json.optJSONObject("second"));

            return i;
        }

        public abstract static class Value implements Cloneable {
            private boolean isInvert = false;

            public boolean isInvert() {
                return isInvert;
            }

            public void setInvert(boolean invert) {
                isInvert = invert;
            }

            public JSONObject exportI() throws JSONException {
                return new JSONObject()
                        .put("isInvert", isInvert);
            }

            @NonNull
            @Override
            protected Value clone() throws CloneNotSupportedException {
                return (Value) super.clone();
            }
        }

        public static class IntegerValue extends Value implements Cloneable {
            private int shift = 0;
            private int value = 0;
            private String mode;

            public boolean isFit(int i) {
                boolean isFit = true;
                if (shift != 0) {
                    i = i + shift;
                }

                if (mode == null) {
                    isFit = i == value;
                } else {
                    switch (mode) {
                        case "==":
                            isFit = i == value;
                            break;
                        case ">":
                            isFit = i > value;
                            break;
                        case "<":
                            isFit = i < value;
                            break;
                        case ">=":
                            isFit = i >= value;
                            break;
                        case "<=":
                            isFit = i <= value;
                            break;

                        case "%":
                            if (value != 0) {
                                isFit = i % value == 0;
                            } else {
                                isFit = false;
                            }
                            break;

                    }
                }

                return (isInvert() != isFit);
            }

            public JSONObject exportI() throws JSONException {
                return super.exportI()
                        .put("value", value)
                        .put("mode", mode)
                        .put("shift", shift);
            }

            public static IntegerValue importI(JSONObject json) {
                if (json == null) {
                    return null;
                }
                IntegerValue integerValue = new IntegerValue();
                integerValue.value = json.optInt("value", 0);
                integerValue.setInvert(json.optBoolean("isInvert", false));
                integerValue.mode = json.optString("mode", "==");
                integerValue.shift = json.optInt("shift", 0);
                return integerValue;
            }

            public int getValue() {
                return value;
            }

            public void setValue(int value) {
                this.value = value;
            }

            public String getMode() {
                return mode;
            }

            public void setMode(String mode) {
                this.mode = mode;
            }

            public int getShift() {
                return shift;
            }

            public void setShift(int shift) {
                this.shift = shift;
            }

            @NonNull
            @Override
            protected IntegerValue clone() throws CloneNotSupportedException {
                return (IntegerValue) super.clone();
            }
        }

        public IntegerValue getYear() {
            return year;
        }

        public void setYear(IntegerValue year) {
            this.year = year;
        }

        public IntegerValue getMonth() {
            return month;
        }

        public void setMonth(IntegerValue month) {
            this.month = month;
        }

        public IntegerValue getDayOfMonth() {
            return dayOfMonth;
        }

        public void setDayOfMonth(IntegerValue dayOfMonth) {
            this.dayOfMonth = dayOfMonth;
        }

        public IntegerValue getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(IntegerValue dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public IntegerValue getDayOfYear() {
            return dayOfYear;
        }

        public void setDayOfYear(IntegerValue dayOfYear) {
            this.dayOfYear = dayOfYear;
        }

        public IntegerValue getWeekOfYear() {
            return weekOfYear;
        }

        public void setWeekOfYear(IntegerValue weekOfYear) {
            this.weekOfYear = weekOfYear;
        }

        public IntegerValue getHour() {
            return hour;
        }

        public void setHour(IntegerValue hour) {
            this.hour = hour;
        }

        public IntegerValue getMinute() {
            return minute;
        }

        public void setMinute(IntegerValue minute) {
            this.minute = minute;
        }

        public IntegerValue getSecond() {
            return second;
        }

        public void setSecond(IntegerValue second) {
            this.second = second;
        }
        
        @NonNull
        @Override
        protected ItemFilter clone() throws CloneNotSupportedException {
            ItemFilter c = (ItemFilter) super.clone();
            c.year = this.year != null ? this.year.clone() : null;
            c.month = this.month != null ? this.month.clone() : null;
            c.dayOfMonth = this.dayOfMonth != null ? this.dayOfMonth.clone() : null;
            c.dayOfYear = this.dayOfYear != null ? this.dayOfYear.clone() : null;
            c.dayOfWeek = this.dayOfWeek != null ? this.dayOfWeek.clone() : null;
            c.weekOfYear = this.weekOfYear != null ? this.weekOfYear.clone() : null;
            c.hour = this.hour != null ? this.hour.clone() : null;
            c.minute = this.minute != null ? this.minute.clone() : null;
            c.second = this.second != null ? this.second.clone() : null;

            return c;
        }
    }

    private class FilterGroupItemController extends ItemController {
        @Override
        public void delete(Item item) {
            App.get().getItemManager().deselectItem(item); // TODO: 31.08.2022 other fix??  !!BUGFIX!!

            itemStorageUpdateCallbacks.run((callbackStorage, callback) -> callback.onDeleted(item));


            ItemFilterWrapper toDelete = null;
            for (ItemFilterWrapper wrapper : items) {
                if (wrapper.item == item) {
                    toDelete = wrapper;
                    break;
                }
            }
            recalculate(new GregorianCalendar());

            items.remove(toDelete);
            activeItems.remove(toDelete);
            FilterGroupItem.this.visibleChanged();
            FilterGroupItem.this.save();
        }

        @Override
        public void save(Item item) {
            FilterGroupItem.this.save();
        }

        @Override
        public void updateUi(Item item) {
            itemStorageUpdateCallbacks.run((callbackStorage, callback) -> callback.onUpdated(item));
            FilterGroupItem.this.visibleChanged();
        }
    }
}
