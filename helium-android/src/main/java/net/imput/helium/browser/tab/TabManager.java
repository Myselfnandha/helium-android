package net.imput.helium.browser.tab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TabManager: Controls browser multi-tab lifecycle, tab switching,
 * memory optimization for background tabs, and tab listener dispatch.
 */
public final class TabManager {

    public interface TabListener {
        void onTabCreated(@NonNull Tab tab);
        void onTabSelected(@NonNull Tab tab);
        void onTabClosed(@NonNull Tab tab);
        void onTabCountChanged(int count);
    }

    private static volatile TabManager sInstance;

    private final List<Tab> mTabs = new ArrayList<>();
    private final List<TabListener> mListeners = new CopyOnWriteArrayList<>();
    private String mActiveTabId;

    public static TabManager getInstance() {
        if (sInstance == null) {
            synchronized (TabManager.class) {
                if (sInstance == null) {
                    sInstance = new TabManager();
                }
            }
        }
        return sInstance;
    }

    private TabManager() {}

    public synchronized void addListener(@NonNull TabListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public synchronized void removeListener(@NonNull TabListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Creates a new tab and sets it as the active tab.
     */
    @NonNull
    public synchronized Tab createTab(@NonNull String url, boolean isIncognito) {
        Tab tab = new Tab(url, isIncognito);
        mTabs.add(tab);
        mActiveTabId = tab.getId();

        for (TabListener l : mListeners) {
            l.onTabCreated(tab);
            l.onTabCountChanged(mTabs.size());
            l.onTabSelected(tab);
        }
        return tab;
    }

    /**
     * Switches the active tab to the specified ID.
     */
    public synchronized boolean selectTab(@NonNull String tabId) {
        Tab target = findTabById(tabId);
        if (target == null) {
            return false;
        }

        mActiveTabId = tabId;
        target.touch();

        for (TabListener l : mListeners) {
            l.onTabSelected(target);
        }
        return true;
    }

    /**
     * Closes the tab with the specified ID. If it was active, switches to the nearest tab.
     */
    public synchronized boolean closeTab(@NonNull String tabId) {
        Tab target = findTabById(tabId);
        if (target == null) {
            return false;
        }

        int index = mTabs.indexOf(target);
        mTabs.remove(target);
        target.destroy();

        for (TabListener l : mListeners) {
            l.onTabClosed(target);
            l.onTabCountChanged(mTabs.size());
        }

        if (tabId.equals(mActiveTabId)) {
            if (!mTabs.isEmpty()) {
                int nextIndex = Math.min(index, mTabs.size() - 1);
                Tab nextTab = mTabs.get(nextIndex);
                selectTab(nextTab.getId());
            } else {
                mActiveTabId = null;
            }
        }

        return true;
    }

    @Nullable
    public synchronized Tab getActiveTab() {
        if (mActiveTabId == null || mTabs.isEmpty()) {
            return null;
        }
        return findTabById(mActiveTabId);
    }

    @Nullable
    public synchronized Tab findTabById(@NonNull String tabId) {
        for (Tab tab : mTabs) {
            if (tab.getId().equals(tabId)) {
                return tab;
            }
        }
        return null;
    }

    @NonNull
    public synchronized List<Tab> getTabs() {
        return Collections.unmodifiableList(new ArrayList<>(mTabs));
    }

    public synchronized int getTabCount() {
        return mTabs.size();
    }

    /**
     * Closes all tabs, releasing memory for all associated WebViews.
     */
    public synchronized void closeAllTabs() {
        for (Tab tab : mTabs) {
            tab.destroy();
            for (TabListener l : mListeners) {
                l.onTabClosed(tab);
            }
        }
        mTabs.clear();
        mActiveTabId = null;
        for (TabListener l : mListeners) {
            l.onTabCountChanged(0);
        }
    }

    /**
     * Suspends background tab timers and JavaScript execution to conserve battery and CPU.
     */
    public synchronized void pauseBackgroundTabs() {
        for (Tab tab : mTabs) {
            if (!tab.getId().equals(mActiveTabId) && tab.getWebView() != null) {
                tab.getWebView().onPause();
            }
        }
    }

    /**
     * Resumes the active tab's execution.
     */
    public synchronized void resumeActiveTab() {
        Tab active = getActiveTab();
        if (active != null && active.getWebView() != null) {
            active.getWebView().onResume();
        }
    }
}
