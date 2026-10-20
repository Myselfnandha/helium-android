package net.imput.helium;

import java.util.HashMap;
import java.util.Map;
import net.imput.helium.database.BookmarkFolder;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for PRD M09: BookmarkFolder hierarchy and circular cycle detection.
 */
public class BookmarkFolderTest {

    @Test
    public void testFolderAttributes() {
        BookmarkFolder folder = new BookmarkFolder(10, BookmarkFolder.ROOT_FOLDER_ID, "Tech Blogs", System.currentTimeMillis());
        assertEquals(10, folder.getId());
        assertEquals(BookmarkFolder.ROOT_FOLDER_ID, folder.getParentId());
        assertEquals("Tech Blogs", folder.getName());
        assertTrue(folder.isRoot());
    }

    @Test
    public void testCycleDetectionOnFolderMove() {
        // Hierarchy: Root -> Folder 1 -> Folder 2 -> Folder 3
        //                   Folder 4
        Map<Long, Long> parentMap = new HashMap<>();
        parentMap.put(1L, BookmarkFolder.ROOT_FOLDER_ID);
        parentMap.put(2L, 1L);
        parentMap.put(3L, 2L);
        parentMap.put(4L, BookmarkFolder.ROOT_FOLDER_ID);

        // 1. Moving a folder into itself causes a cycle
        assertTrue("Self-parenting causes cycle", BookmarkFolder.wouldCauseCycle(1L, 1L, parentMap));

        // 2. Moving Folder 1 into its child Folder 2 causes a cycle
        assertTrue("Moving parent into direct child causes cycle", BookmarkFolder.wouldCauseCycle(1L, 2L, parentMap));

        // 3. Moving Folder 1 into its grandchild Folder 3 causes a cycle
        assertTrue("Moving parent into grandchild causes cycle", BookmarkFolder.wouldCauseCycle(1L, 3L, parentMap));

        // 4. Moving Folder 2 into sibling Folder 4 is safe
        assertFalse("Moving to sibling folder is valid", BookmarkFolder.wouldCauseCycle(2L, 4L, parentMap));

        // 5. Moving Folder 3 to Root is safe
        assertFalse("Moving to root is valid", BookmarkFolder.wouldCauseCycle(3L, BookmarkFolder.ROOT_FOLDER_ID, parentMap));
    }
}
