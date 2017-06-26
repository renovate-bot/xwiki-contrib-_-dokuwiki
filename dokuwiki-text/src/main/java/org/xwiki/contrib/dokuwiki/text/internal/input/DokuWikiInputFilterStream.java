/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.dokuwiki.text.internal.input;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.dokuwiki.text.input.DokuWikiInputProperties;
import org.xwiki.contrib.dokuwiki.text.internal.DokuWikiFilter;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.AbstractBeanInputFilterStream;
import org.xwiki.filter.input.FileInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.InputStreamInputSource;

import javax.inject.Named;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;

/**
 * @version $Id: 41df1dab66b03111214dbec56fee8dbd44747638 $
 */
@Component
@Named(DokuWikiInputProperties.FILTER_STREAM_TYPE_STRING)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DokuWikiInputFilterStream extends AbstractBeanInputFilterStream<DokuWikiInputProperties, DokuWikiFilter> {
    @Override
    protected void read(Object filter, DokuWikiFilter proxyFilter) throws FilterException {
        InputSource inputSource = this.properties.getSource();
        if (inputSource instanceof FileInputSource) {
            File sourceFolder = ((FileInputSource) inputSource).getFile();
            readFolder(sourceFolder, filter, proxyFilter);
        } else if (inputSource instanceof InputStreamInputSource) {
            try {
                CompressorInputStream input = new CompressorStreamFactory()
                        .createCompressorInputStream(((InputStreamInputSource) inputSource).getInputStream());
                ArchiveInputStream archiveInputStream = new ArchiveStreamFactory()
                        .createArchiveInputStream(new BufferedInputStream(input));
                ArchiveEntry archiveEntry = archiveInputStream.getNextEntry();
                DirectoryTree rootDirectory = new DirectoryTree(new Folder("root", "root"));
                while (archiveEntry != null) {
                    String entryName = archiveEntry.getName();
                    rootDirectory.addElement(entryName, archiveEntry.isDirectory());
                    archiveEntry = archiveInputStream.getNextEntry();
                }
                Folder dataDirectory = rootDirectory.getCommonRoot();
                Folder pagesDirectory = dataDirectory.childs.get(dataDirectory.childs.indexOf(new Folder("pages", "root/data/pages")));
                proxyFilter.beginWikiSpace("Main", FilterEventParameters.EMPTY);
                readPageFolderStream(pagesDirectory, archiveInputStream, filter, proxyFilter);
                proxyFilter.endWikiSpace("Main", FilterEventParameters.EMPTY);
            } catch (Exception e) {
                throw new FilterException("Unsupported input stream [" + inputSource.getClass() + "]");
            }
        } else {
            throw new FilterException("Unsupported input source [" + inputSource.getClass() + "]");
        }
    }

    private void readPageFolderStream(Folder pagesFolderTree, ArchiveInputStream archiveInputStream, Object filter, DokuWikiFilter proxyFilter) throws FilterException {
        for (Folder i : pagesFolderTree.childs) {
            proxyFilter.beginWikiSpace(i.toString(), FilterEventParameters.EMPTY);
            readPageFolderStream(i, archiveInputStream, filter, proxyFilter);
            proxyFilter.endWikiSpace(i.toString(), FilterEventParameters.EMPTY);
        }
        for (Folder j : pagesFolderTree.leafs) {
            String leafName = j.toString().replace(".txt", "");
            proxyFilter.beginWikiDocument(leafName, FilterEventParameters.EMPTY);
            proxyFilter.endWikiDocument(leafName, FilterEventParameters.EMPTY);
        }
    }

    private void readFolder(File sourceFolder, Object filter, DokuWikiFilter proxyFilter) throws FilterException {
        if (sourceFolder.isDirectory()) {
            File pageFolder = new File(sourceFolder, "pages");
            if (pageFolder.exists() && pageFolder.isDirectory()) {
                proxyFilter.beginWikiSpace("Main", FilterEventParameters.EMPTY);
                readPagesFolder(pageFolder, filter, proxyFilter);
                proxyFilter.endWikiSpace("Main", FilterEventParameters.EMPTY);
            } else {
                throw new FilterException("Can't locate Pages folder: Invalid Package");
            }
        } else
            throw new FilterException("Input folder is not a directory");
    }

    private void readPagesFolder(File pages, Object filter, DokuWikiFilter proxyFilter) throws FilterException {
        File[] files = pages.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String folder = file.getName();
                    proxyFilter.beginWikiSpace(folder, FilterEventParameters.EMPTY);
                    readPagesFolder(file, filter, proxyFilter);
                    proxyFilter.endWikiSpace(folder, FilterEventParameters.EMPTY);
                } else if (file.isFile() && file.getName().endsWith(".txt")) {
                    String fileName = file.getName().replace(".txt", "");
                    proxyFilter.beginWikiDocument(fileName, FilterEventParameters.EMPTY);
                    proxyFilter.endWikiDocument(fileName, FilterEventParameters.EMPTY);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.properties.getSource().close();
    }

    //helper functions
//    private static URL concatenate(URL baseUrl, String extraPath) throws URISyntaxException,
//            MalformedURLException {
//        URI uri = baseUrl.toURI();
//        String newPath = uri.getPath() + '/' + extraPath;
//        URI newUri = uri.resolve(newPath);
//        return newUri.toURL();
//    }
}


