package com.trajan.negentropy.server.backup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class BackupServiceTest {

    @Autowired
    private BackupService backupService;

    @Test
    public void backupAndRestoreTest() {
        // perform backup
        backupService.backup();

        // get backup files
        List<File> backupFiles = BackupUtils.getBackupFileList(backupService.getBackupPath());
        assertThat(backupFiles).isNotEmpty();

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

}
