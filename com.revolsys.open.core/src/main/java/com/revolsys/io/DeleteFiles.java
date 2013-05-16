package com.revolsys.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class DeleteFiles {

  private static final ResourcePatternResolver RESOLVER = new PathMatchingResourcePatternResolver();

  private List<String> filePatterns = new ArrayList<String>();

  private boolean deleteDirectories = true;

  @PostConstruct
  public void deleteFiles() {
    for (String filePattern : filePatterns) {
      if (!filePattern.startsWith("file:")) {
        filePattern = "file:" + filePattern;
      }
      try {
        for (final Resource resource : RESOLVER.getResources(filePattern)) {
          final File file = resource.getFile();
          if (file.isDirectory()) {
            if (deleteDirectories) {
              if (!FileUtil.deleteDirectory(file, true)) {
                throw new RuntimeException("Unable to delete directory: "
                  + file);
              }
            }
          } else {
            if (!file.delete()) {
              throw new RuntimeException("Unable to delete file: " + file);
            }
          }
        }
      } catch (final Throwable e) {
        throw new RuntimeException("Cannot delete files: " + filePattern, e);
      }
    }
  }

  public void setDeleteDirectories(boolean deleteDirectories) {
    this.deleteDirectories = deleteDirectories;
  }

  public boolean isDeleteDirectories() {
    return deleteDirectories;
  }

  public List<String> getFilePatterns() {
    return filePatterns;
  }

  public void setFilePattern(final String filePattern) {
    this.filePatterns.add(filePattern);
  }

  public void setFilePatterns(final List<String> filePatterns) {
    this.filePatterns = new ArrayList<String>(filePatterns);
  }
}