package ru.fazziclay.opentoday.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ru.fazziclay.opentoday.app.App;
import ru.fazziclay.opentoday.app.items.ItemManager;
import ru.fazziclay.opentoday.app.items.ItemsStorage;
import ru.fazziclay.opentoday.app.items.item.CycleListItem;
import ru.fazziclay.opentoday.app.items.item.FilterGroupItem;
import ru.fazziclay.opentoday.app.items.item.GroupItem;
import ru.fazziclay.opentoday.app.items.item.Item;
import ru.fazziclay.opentoday.app.items.tab.Tab;
import ru.fazziclay.opentoday.ui.UI;
import ru.fazziclay.opentoday.ui.activity.MainActivity;
import ru.fazziclay.opentoday.ui.dialog.DialogEditItemFilter;
import ru.fazziclay.opentoday.ui.interfaces.IVGEditButtonInterface;
import ru.fazziclay.opentoday.ui.interfaces.NavigationHost;
import ru.fazziclay.opentoday.ui.item.ItemStorageDrawer;
import ru.fazziclay.opentoday.util.L;

public class ItemsEditorFragment extends Fragment {
    private static final int RES_FILTER_BUTTON_IMAGE = android.R.drawable.stat_notify_voicemail;
    private static final String EXTRA_TAB_ID = "items_editor_fragment_tabId";
    private static final String EXTRA_ITEM_ID = "items_editor_fragment_itemId";
    private static final String EXTRA_PREVIEW_MODE = "items_editor_fragment_previewMode";
    private static final String TAG = "ItemsEditorFragment";

    private MainActivity activity;
    private NavigationHost navigationHost;
    private NavigationHost rootNavigationHost;
    private ItemManager itemManager;
    private ItemsStorage itemsStorage;
    private boolean previewMode;
    private ItemStorageDrawer itemStorageDrawer;

    private UUID tabId;
    private UUID itemId;
    private boolean isRoot;

    private Tab tab;
    private Item item;
    private final List<Runnable> onCreateListeners = new ArrayList<>();

    public static ItemsEditorFragment createRoot(UUID tab) {
        return ItemsEditorFragment.create(tab, null, false);
    }

    public static ItemsEditorFragment createItem(UUID tab, UUID item) {
        return ItemsEditorFragment.create(tab, item, false);
    }

    private static ItemsEditorFragment create(UUID tab, UUID item, boolean previewMode) {
        ItemsEditorFragment fragment = new ItemsEditorFragment();
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_PREVIEW_MODE, previewMode);
        args.putString(EXTRA_TAB_ID, tab.toString());
        if (item != null) args.putString(EXTRA_ITEM_ID, item.toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.o(TAG, "onCreate", L.nn(savedInstanceState));
        activity = (MainActivity) requireActivity();
        navigationHost = (NavigationHost) getParentFragment();
        rootNavigationHost = (NavigationHost) UI.findFragmentInParents(this, MainRootFragment.class);
        itemManager = App.get(requireContext()).getItemManager();

        Bundle args = getArguments();
        previewMode = args.getBoolean(EXTRA_PREVIEW_MODE);
        tabId = UUID.fromString(args.getString(EXTRA_TAB_ID));
        tab = itemManager.getTab(tabId);

        isRoot = !args.containsKey(EXTRA_ITEM_ID);
        if (isRoot) {
            this.itemsStorage = tab;

        } else {
            itemId = UUID.fromString(args.getString(EXTRA_ITEM_ID));
            item = tab.getItemById(itemId);

            if (item instanceof ItemsStorage) {
                itemsStorage = (ItemsStorage) item;
            } else {
                throw new RuntimeException("Cannot get ItemStorage from item. Item=" + item + "; id=" + itemId + "; tab=" + tab + "; tabId="+tabId);
            }
        }

        this.itemStorageDrawer = new ItemStorageDrawer(activity, itemManager, itemsStorage, item -> rootNavigationHost.navigate(ItemEditorFragment.edit(tabId, item.getId()), true), previewMode, new IVGEditButtonInterface() {
            @Override
            public void onGroupEdit(GroupItem groupItem) {
                navigationHost.navigate(createItem(tabId, groupItem.getId()), true);
            }

            @Override
            public void onCycleListEdit(CycleListItem cycleListItem) {
                navigationHost.navigate(createItem(tabId, cycleListItem.getId()), true);
            }

            @Override
            public void onFilterGroupEdit(FilterGroupItem filterGroupItem) {
                navigationHost.navigate(createItem(tabId, filterGroupItem.getId()), true);
            }
        });

        if (item instanceof FilterGroupItem) {
            applyFilterGroupViewPatch((FilterGroupItem) item);
        }

        itemStorageDrawer.create();
        runOnCreateListeners();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        L.o(TAG, "onCreateView", L.nn(savedInstanceState));
        return itemStorageDrawer.getView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.itemStorageDrawer != null) this.itemStorageDrawer.destroy();
    }

    public ItemsStorage getItemStorage() {
        return itemsStorage;
    }

    public UUID getTabId() {
        return tabId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void addOnCreateListener(Runnable o) {
        onCreateListeners.add(o);
    }

    @Nullable
    public UUID getItemIdFromExtra() {
        if (getArguments().containsKey(EXTRA_ITEM_ID)) {
            return UUID.fromString(getArguments().getString(EXTRA_ITEM_ID));
        }
        return null;
    }

    private void applyFilterGroupViewPatch(FilterGroupItem filterGroupItem) {
        itemStorageDrawer.setItemViewWrapper((item, view) -> {
            LinearLayout layout = new LinearLayout(view.getContext());
            layout.addView(view);
            view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            ImageButton filter = new ImageButton(view.getContext());
            filter.setImageResource(RES_FILTER_BUTTON_IMAGE);
            filter.setOnClickListener(v -> new DialogEditItemFilter(activity, filterGroupItem.getItemFilter(item), filterGroupItem::save).show());
            layout.addView(filter);
            filter.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 70, 0));

            return layout;
        });
    }

    private void runOnCreateListeners() {
        for (Runnable e : onCreateListeners) {
            e.run();
        }
    }
}
