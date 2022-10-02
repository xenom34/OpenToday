package ru.fazziclay.opentoday.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.UUID;

import ru.fazziclay.opentoday.R;
import ru.fazziclay.opentoday.app.App;
import ru.fazziclay.opentoday.app.items.ItemStorage;
import ru.fazziclay.opentoday.app.items.ItemsTab;
import ru.fazziclay.opentoday.app.items.item.Item;
import ru.fazziclay.opentoday.ui.activity.MainActivity;
import ru.fazziclay.opentoday.ui.interfaces.NavigationHost;

public class ItemsEditorRootFragment extends Fragment implements NavigationHost {
    private static final int ROOT_CONTAINER_ID = R.id.changeOnLeftSwipe;
    private static final String EXTRA_TAB_ID = "items_editor_root_fragment_tabId";
    private TextView path;

    @NonNull
    public static ItemsEditorRootFragment create(@NonNull UUID tabId) {
        ItemsEditorRootFragment fragment = new ItemsEditorRootFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_TAB_ID, tabId.toString());
        fragment.setArguments(args);
        return fragment;
    }

    private UUID tabId;
    private ItemsTab tab;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        tabId = UUID.fromString(args.getString(EXTRA_TAB_ID));
        tab = App.get(requireContext()).getItemManager().getTab(tabId);

        if (savedInstanceState == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(ROOT_CONTAINER_ID, ItemsEditorFragment.createRoot(tabId))
                    .commit();

            getChildFragmentManager().addOnBackStackChangedListener(this::updatePath);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        path = new TextView(requireContext());
        path.setText("/");

        FrameLayout frameLayout = new FrameLayout(requireContext());
        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frameLayout.setId(ROOT_CONTAINER_ID);

        LinearLayout l = new LinearLayout(requireContext());
        l.setOrientation(LinearLayout.VERTICAL);
        l.addView(path);
        l.addView(frameLayout);

        return l;
    }

    private void updatePath() {
        StringBuilder s = new StringBuilder("/");
        int i = 0;
        while (i < getChildFragmentManager().getBackStackEntryCount()) {
            s.append(getChildFragmentManager().getBackStackEntryAt(i).getName()).append("/");
            i++;
        }
        path.setText(s.toString());
    }

    @Override
    public void navigate(Fragment fragment, boolean addToBackStack) {
        if (!(fragment instanceof ItemsEditorFragment)) throw new RuntimeException("Other fragments not allowed.");
        ItemsEditorFragment ief = (ItemsEditorFragment) fragment;

        FragmentTransaction transaction = getChildFragmentManager()
                .beginTransaction()
                .replace(ROOT_CONTAINER_ID, ief);

        String backStackName = null;
        UUID itemId = ief.getItemIdFromExtra();
        Item item = tab.getItemById(itemId);
        if (item != null) {
            backStackName = item.getText();
            updateItemStorageContext((ItemStorage) item);
        }

        if (addToBackStack) transaction.addToBackStack(backStackName);
        transaction.commit();
    }

    @Override
    public boolean popBackStack() {
        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            getChildFragmentManager().popBackStackImmediate();
            ItemsEditorFragment fragment = (ItemsEditorFragment) getChildFragmentManager().findFragmentById(ROOT_CONTAINER_ID);
            updateItemStorageContext(fragment.getItemStorage());
            return true;
        }
        return false;
    }

    private void updateItemStorageContext(ItemStorage itemStorage) {
        ((MainActivity) requireActivity()).setItemStorageInContext(itemStorage);
    }
}
