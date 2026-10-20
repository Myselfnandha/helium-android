package net.imput.helium.database;

import androidx.annotation.NonNull;
import java.util.Map;

/**
 * BookmarkFolder: Hierarchical folder entity for Bookmark organization (PRD M09).
 * Enforces folder relationship validation and circular nesting prevention.
 */
public final class BookmarkFolder {

    public static final long ROOT_FOLDER_ID = 0L;

    private final long mId;
    private final long mParentId;
    private final String mName;
    private final long mCreatedAt;

    public BookmarkFolder(long id, long parentId, @NonNull String name, long createdAt) {
        mId = id;
        mParentId = parentId;
        mName = name;
        mCreatedAt = createdAt;
    }

    public long getId() {
        return mId;
    }

    public long getParentId() {
        return mParentId;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public long getCreatedAt() {
        return mCreatedAt;
    }

    public boolean isRoot() {
        return mParentId == ROOT_FOLDER_ID;
    }

    /**
     * Prevents circular parent relationships when moving a folder (PRD M09).
     * Returns true if moving targetFolderId under destinationParentId would cause a cycle.
     */
    public static boolean wouldCauseCycle(long targetFolderId, long destinationParentId, @NonNull Map<Long, Long> folderParentMap) {
        if (targetFolderId == destinationParentId) {
            return true;
        }

        Long current = destinationParentId;
        while (current != null && current != ROOT_FOLDER_ID) {
            if (current == targetFolderId) {
                return true;
            }
            current = folderParentMap.get(current);
        }
        return false;
    }
}
