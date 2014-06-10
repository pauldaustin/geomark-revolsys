/*
 *    ImageI/O-Ext - OpenSource Java Image translation Library
 *    http://www.geo-solutions.it/
 *    http://java.net/projects/imageio-ext/
 *    (C) 2007 - 2009, GeoSolutions
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    either version 3 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.imageio.gdalframework;

import it.geosolutions.imageio.stream.input.FileImageInputStreamExtImpl;
import it.geosolutions.imageio.utilities.ImageIOUtilities;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdalconst.gdalconst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The abstract service provider interface (SPI) for {@link GDALImageReader}s.
 * 
 * @author Daniele Romagnoli, GeoSolutions.
 * @author Simone Giannecchini, GeoSolutions.
 */
public abstract class GDALImageReaderSpi extends ImageReaderSpi {

  private static final Logger LOGGER = LoggerFactory.getLogger(GDALImageReaderSpi.class.toString());
  static {
    GDALUtilities.loadGDAL();
  }

  /**
   * <code>List</code> of gdal formats supported by this plugin.
   */
  private final List<String> supportedFormats;

  public GDALImageReaderSpi(final String vendorName, final String version,
    final String[] names, final String[] suffixes, final String[] MIMETypes,
    final String readerClassName, final Class<?>[] inputTypes,
    final String[] writerSpiNames,
    final boolean supportsStandardStreamMetadataFormat,
    final String nativeStreamMetadataFormatName,
    final String nativeStreamMetadataFormatClassName,
    final String[] extraStreamMetadataFormatNames,
    final String[] extraStreamMetadataFormatClassNames,
    final boolean supportsStandardImageMetadataFormat,
    final String nativeImageMetadataFormatName,
    final String nativeImageMetadataFormatClassName,
    final String[] extraImageMetadataFormatNames,
    final String[] extraImageMetadataFormatClassNames,
    final Collection<String> supportedFormats) {

    super(
      vendorName,
      version,
      names,
      suffixes,
      MIMETypes,
      readerClassName, // readerClassName
      inputTypes,
      writerSpiNames, // writer Spi Names
      supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName,
      nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames,
      extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat,
      nativeImageMetadataFormatName, nativeImageMetadataFormatClassName,
      extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
    this.supportedFormats = new ArrayList<String>(supportedFormats);
  }

  /**
   * Checks if the provided input can be decoded by the specific SPI. When
   * building a new plugin, remember to implement the
   * <code>getSupportedFormat</coded> abstract method.
   * 
   * @return 
   * 		<code>true</code> if the input can be successfully decoded.
   */
  @Override
  public boolean canDecodeInput(Object input) throws IOException {
    if (input == null) {
      return false;
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Can Decode Input called with object " + input != null ? input.toString()
        : "null");
    }

    File sourceFile = null;

    // if input source is a string,
    // convert input from String to File then try URL
    if (input instanceof String) {
      final File file = new File((String)input);
      if (!file.exists() || !file.canRead()) {
        // /check for URL
        input = new URL((String)input);
      } else {
        input = file;
      }
    }

    // if input source is an URL, open an InputStream
    if (input instanceof URL) {
      input = ImageIOUtilities.urlToFile((URL)input);
    }
    if (input instanceof FileImageInputStreamExtImpl) {
      input = ((FileImageInputStreamExtImpl)input).getTarget();
    }

    // if input source is a File,
    // convert input from File to FileInputStream
    if (input instanceof File) {
      sourceFile = (File)input;
    }

    if (sourceFile == null || !sourceFile.exists() || !sourceFile.canRead()) {
      return false;
    }

    boolean isInputDecodable = false;
    // Checking if this specific SPI can decode the provided input
    Dataset ds = null;
    try {
      ds = GDALUtilities.acquireDataSet(sourceFile.getAbsolutePath(),
        gdalconst.GA_ReadOnly);
      isInputDecodable = isDecodable(ds);

    } catch (final Throwable e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(e.getLocalizedMessage(), e);
      }
    } finally {
      if (ds != null) {
        try {
          // Closing the dataset
          GDALUtilities.closeDataSet(ds);
        } catch (final Throwable e) {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(e.getLocalizedMessage(), e);
          }
        }
      }
    }
    return isInputDecodable;
  }

  /**
   * Methods returning the formats which are supported by a plugin.
   * 
   * The right value to be returned may be found using the GDAL command:
   * <code> gdalinfo --formats</code> which lists all the supported formats.
   * 
   * As an instance, the result of this command may be:
   * 
   * VRT (rw+): Virtual Raster GTiff (rw+): GeoTIFF NITF (rw+): National
   * Imagery Transmission Format HFA (rw+): Erdas Imagine Images (.img)
   * SAR_CEOS (ro): CEOS SAR Image CEOS (ro): CEOS Image
   * .........................................
   * 
   * You need to set the String returned as the first word (as an instance:
   * "HFA", if you are building a plugin for the Erdas Image Images)
   * 
   * In some circumstances, GDAL provides more than 1 driver to manage a
   * specific format. As an instance, in order to handle HDF4 files, GDAL
   * provides two drivers: HDF4 and HDF4Image (which supports Dataset
   * creation). The HDF4ImageReader will be capable of manage both formats.
   * 
   */
  public List<String> getSupportedFormats() {
    return Collections.unmodifiableList(this.supportedFormats);
  }

  /**
   * This method tells us if this driver is available or not.
   * 
   * @return <code>true</code> if the driver is available,
   *         <code>false</code> otherwise.
   */
  public boolean isAvailable() {
    // check if gdal is available
    if (!GDALUtilities.isGDALAvailable()) {
      return false;
    }
    // now check that all the drivers are available
    final List<String> supportedFormats = getSupportedFormats();
    final Iterator<String> it = supportedFormats.iterator();
    if (!it.hasNext()) {
      return false;
    }
    while (it.hasNext()) {
      final String formatName = it.next();
      if (!GDALUtilities.isDriverAvailable(formatName)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the provided Dataset was opened by a Driver supporting the same
   * formats which are supported by the specific ImageReaderSpi.
   * 
   * There is a trivial example: Suppose we are implementing a plugin for HDF4
   * format and suppose we are testing the <code>canDecodeInput</code> with
   * a NITF file as input. GDAL will successfully open the NITF file. However,
   * it will use the NITF driver instead of the HDF4 driver. Since NITF is not
   * supported by the HDF4ImageReaderSpi, this method will return return
   * <code>false</code>.
   * 
   * @param dataset
   *                The input dataset
   * 
   * @return <code>true</code> if the format is supported.
   *         <code>false</code> otherwise.
   */
  protected boolean isDecodable(final Dataset dataset) {
    if (dataset != null) {
      final Driver driver = dataset.GetDriver();

      // retrieving the format of the provided input.
      // We use the "Description" of the driver which has opened the
      // input.
      final String sDriver = driver.getShortName();

      // checking if this format is supported by the specific SPI */
      return getSupportedFormats().contains(sDriver);
    }
    return false;
  }

  /**
   * Allows to deregister GDAL based spi in case GDAL libraries are
   * unavailable.
   */
  @Override
  public void onRegistration(final ServiceRegistry registry,
    final Class<?> category) {
    super.onRegistration(registry, category);
    if (!GDALUtilities.isGDALAvailable()) {
      final IIORegistry iioRegistry = (IIORegistry)registry;
      final Class<ImageReaderSpi> spiClass = ImageReaderSpi.class;
      final Iterator<ImageReaderSpi> iter = iioRegistry.getServiceProviders(
        spiClass, true);
      while (iter.hasNext()) {
        final ImageReaderSpi provider = iter.next();
        if (provider instanceof GDALImageReaderSpi) {
          registry.deregisterServiceProvider(provider);
        }
      }
    }
  }
}
