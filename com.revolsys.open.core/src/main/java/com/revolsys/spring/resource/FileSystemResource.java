/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.revolsys.spring.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.revolsys.util.WrappedException;

/**
 * {@link Resource} implementation for {@code java.io.File} handles.
 * Obviously supports resolution as File, and also as URL.
 * Implements the extended {@link WritableResource} interface.
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see java.io.File
 */
public class FileSystemResource extends AbstractResource {

  private final File file;

  private final String path;

  /**
   * Create a new {@code FileSystemResource} from a {@link File} handle.
   * <p>Note: When building relative resources via {@link #createRelative},
   * the relative path will apply <i>at the same directory level</i>:
   * e.g. new File("C:/dir1"), relative path "dir2" -> "C:/dir2"!
   * If you prefer to have relative paths built underneath the given root
   * directory, use the {@link #FileSystemResource(String) constructor with a file path}
   * to append a trailing slash to the root path: "C:/dir1/", which
   * indicates this directory as root for all relative paths.
   * @param file a File handle
   */
  public FileSystemResource(final File file) {
    Assert.notNull(file, "File must not be null");
    this.file = file;
    this.path = StringUtils.cleanPath(file.getPath());
  }

  /**
   * Create a new {@code FileSystemResource} from a file path.
   * <p>Note: When building relative resources via {@link #createRelative},
   * it makes a difference whether the specified resource base path here
   * ends with a slash or not. In the case of "C:/dir1/", relative paths
   * will be built underneath that root: e.g. relative path "dir2" ->
   * "C:/dir1/dir2". In the case of "C:/dir1", relative paths will apply
   * at the same directory level: relative path "dir2" -> "C:/dir2".
   * @param path a file path
   */
  public FileSystemResource(final String path) {
    Assert.notNull(path, "Path must not be null");
    this.file = new File(path);
    this.path = StringUtils.cleanPath(path);
  }

  /**
   * This implementation returns the underlying File's length.
   */
  @Override
  public long contentLength() throws IOException {
    return this.file.length();
  }

  @Override
  public void copyFrom(final InputStream in) {
    final File file = getFile();
    final File parent = file.getParentFile();
    if (!parent.exists()) {
      parent.mkdirs();
    }
    super.copyFrom(in);
  }

  /**
   * This implementation creates a FileSystemResource, applying the given path
   * relative to the path of the underlying file of this resource descriptor.
   * @see org.springframework.util.StringUtils#applyRelativePath(String, String)
   */
  @Override
  public Resource createRelative(final String relativePath) {
    final String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
    return new FileSystemResource(pathToUse);
  }

  @Override
  public boolean delete() {
    return this.file.delete();
  }

  /**
   * This implementation compares the underlying File references.
   */
  @Override
  public boolean equals(final Object obj) {
    return obj == this
      || obj instanceof FileSystemResource && this.path.equals(((FileSystemResource)obj).path);
  }

  /**
   * This implementation returns whether the underlying file exists.
   * @see java.io.File#exists()
   */
  @Override
  public boolean exists() {
    return this.file.exists();
  }

  /**
   * This implementation returns a description that includes the absolute
   * path of the file.
   * @see java.io.File#getAbsolutePath()
   */
  @Override
  public String getDescription() {
    return "file [" + this.file.getAbsolutePath() + "]";
  }

  /**
   * This implementation returns the underlying File reference.
   */
  @Override
  public File getFile() {
    return this.file;
  }

  /**
   * This implementation returns the name of the file.
   * @see java.io.File#getName()
   */
  @Override
  public String getFilename() {
    return this.file.getName();
  }

  /**
   * This implementation opens a FileInputStream for the underlying file.
   * @see java.io.FileInputStream
   */
  @Override
  public InputStream getInputStream() {
    try {
      return new FileInputStream(this.file);
    } catch (final FileNotFoundException e) {
      throw new WrappedException(e);
    }
  }

  /**
   * This implementation opens a FileOutputStream for the underlying file.
   * @see java.io.FileOutputStream
   */
  public OutputStream getOutputStream() {
    try {
      return new FileOutputStream(this.file);
    } catch (final FileNotFoundException e) {
      throw new WrappedException(e);
    }
  }

  @Override
  public Resource getParent() {
    final File parentFile = getFile().getParentFile();
    if (parentFile == null) {
      return null;
    } else {
      return new FileSystemResource(this.file);
    }
  }

  /**
   * Return the file path for this resource.
   */
  public final String getPath() {
    return this.path;
  }

  /**
   * This implementation returns a URI for the underlying file.
   * @see java.io.File#toURI()
   */
  @Override
  public URI getURI() throws IOException {
    return this.file.toURI();
  }

  /**
   * This implementation returns a URL for the underlying file.
   * @see java.io.File#toURI()
   */
  @Override
  public URL getURL() {
    try {
      return this.file.toURI().toURL();
    } catch (final MalformedURLException e) {
      throw new WrappedException(e);
    }
  }

  // implementation of WritableResource

  /**
   * This implementation returns the hash code of the underlying File reference.
   */
  @Override
  public int hashCode() {
    return this.path.hashCode();
  }

  /**
   * This implementation checks whether the underlying file is marked as readable
   * (and corresponds to an actual file with content, not to a directory).
   * @see java.io.File#canRead()
   * @see java.io.File#isDirectory()
   */
  @Override
  public boolean isReadable() {
    return this.file.canRead() && !this.file.isDirectory();
  }

  /**
   * This implementation checks whether the underlying file is marked as writable
   * (and corresponds to an actual file with content, not to a directory).
   * @see java.io.File#canWrite()
   * @see java.io.File#isDirectory()
   */
  public boolean isWritable() {
    return this.file.canWrite() && !this.file.isDirectory();
  }

  @Override
  public OutputStream newOutputStream() {
    return getOutputStream();
  }

}