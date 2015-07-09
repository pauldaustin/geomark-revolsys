package com.revolsys.io;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;

import com.revolsys.spring.SpringUtil;

public interface MapWriterFactory extends FileIoFactory {
  default MapWriter createMapWriter(final OutputStream out) {
    final java.io.Writer writer = FileUtil.createUtf8Writer(out);
    return createMapWriter(writer);
  }

  default MapWriter createMapWriter(final OutputStream out, final Charset charset) {
    final OutputStreamWriter writer = new OutputStreamWriter(out, charset);
    return createMapWriter(writer);
  }

  default MapWriter createMapWriter(final Path path) {
    final PathResource resource = new PathResource(path);
    return createMapWriter(resource);
  }

  default MapWriter createMapWriter(final Resource resource) {
    final java.io.Writer writer = SpringUtil.getWriter(resource);
    return createMapWriter(writer);
  }

  MapWriter createMapWriter(final Writer out);
}
