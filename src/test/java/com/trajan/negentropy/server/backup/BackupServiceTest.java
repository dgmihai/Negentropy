package com.trajan.negentropy.server.backup;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BackupServiceTest {

    @Autowired
    private BackupService backupService;

    @Test
    public void backupAndRestoreTest() {
        // perform backup
        backupService.backup();

        // get backup files
        List<File> backupFiles = BackupUtils.getBackupFileList(backupService.getBackupPath());
        assertFalse(backupFiles.isEmpty());

        // perform restore
        backupService.restore(1);
    }

    @Test
    public void restoreInvalidIndexTest() {
        backupService.restore(-1);
        backupService.restore(0);

        List<File> backupFiles = BackupUtils.getBackupFileList(backupService.getBackupPath());
        backupService.restore(backupFiles.size() + 1);
    }

    @AfterAll
    public void tearDown() {
        BackupUtils.deleteBackupFiles(backupService.getBackupPath(), 0);
        assertTrue(BackupUtils.getBackupFileList(backupService.getBackupPath())
                .isEmpty());
    }

}
