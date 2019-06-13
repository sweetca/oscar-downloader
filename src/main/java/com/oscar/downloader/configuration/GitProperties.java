package com.oscar.downloader.configuration;

import com.oscar.downloader.utils.StorageUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Configuration
@ConfigurationProperties(prefix = "application.git")
@Setter
@Getter
public class GitProperties {

    private String repositoryDir;

    private String storageLimit;

    private boolean terminalClone;

    private int maxParallelClones;

    private int maxParallelRepoProcessors;

    private boolean commitsDiff;

    public long getStorageLimitBytes() {
        return isNotBlank(storageLimit) ? StorageUtils.parseStorageLimit(storageLimit) : 0;
    }

}
