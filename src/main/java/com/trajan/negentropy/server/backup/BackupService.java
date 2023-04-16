package com.trajan.negentropy.server.backup;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
@Getter
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${backup.max_backups}")
    private int maxBackups;

    @Value("${backup.path}")
    private String backupPath;

    @Value("${backup.interval:0 0 * * * *}") // runs every hour by default
    private String backupInterval;

    @Value("${backup.on_startup:false}")
    private boolean backupOnStartup;

    private long lastBackupModificationTime = 0;

    @PostConstruct
    public void startup() {
        if (backupOnStartup) {
            backup();
        }
    }

    @Scheduled(cron = "${backup.interval}")
    public void backup() {
        try {
            // create backup file name
            String backupFileName = BackupUtils.getBackupFileName(backupPath, new Date());

            // create backup file
            File backupFile = new File(backupFileName);
            backupFile.getParentFile().mkdirs();
            backupFile.createNewFile();

            // check if backup file has been modified since last backup
            if (backupFile.lastModified() <= lastBackupModificationTime) {
                logger.info("No changes detected since last backup, skipping backup");
                return;
            }

            // create backup
            jdbcTemplate.execute("BACKUP TO '" + backupFileName + "'");

            // delete old backups
            BackupUtils.deleteBackupFiles(backupPath, maxBackups);

            // update last backup modification time
            lastBackupModificationTime = backupFile.lastModified();

            logger.info("Backup successful: {}", backupFileName);
        } catch (IOException | RuntimeException e) {
            logger.error("Backup failed: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error occurred during backup: {}", e.getMessage(), e);
        }
    }

    public void restore(int backupIndex) {
        try {
            // get backup file list
            List<File> backupFiles = BackupUtils.getBackupFileList(backupPath);

            if (backupIndex < 1 || backupIndex > backupFiles.size()) {
                logger.error("Invalid backup index: {}", backupIndex);
                return;
            }

            // get backup file
            File backupFile = backupFiles.get(backupIndex - 1);

            // restore backup
            jdbcTemplate.execute(String.format("RUNSCRIPT FROM '%s'", backupFile.getAbsolutePath()));

            logger.info("Database restored from backup file: {}", backupFile.getName());
        } catch (Exception e) {
            logger.error("Failed to restore database from backup: {}", backupIndex, e);
        }
    }
}
