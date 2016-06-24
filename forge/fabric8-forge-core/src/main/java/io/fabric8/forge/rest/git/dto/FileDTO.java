/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.forge.rest.git.dto;

import io.fabric8.utils.Base64Encoder;
import io.fabric8.utils.Files;
import io.fabric8.utils.Strings;
import io.fabric8.utils.XmlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Used to list contents of git
 */
public class FileDTO extends GitDTOSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(FileDTO.class);

    private final String type;
    private final long size;
    private final String name;
    private final String path;
    private final String encoding;
    private final String content;
    private String sha;
    private String url;
    private String gitUrl;
    private String htmlUrl;
    private String downloadUrl;
    private String[] xmlNamespaces;

    public FileDTO(String type, long size, String name, String path, String encoding, String content) {
        this.content = content;
        this.type = type;
        this.encoding = encoding;
        this.size = size;
        this.name = name;
        this.path = path;
    }

    public static FileDTO createFileDTO(File file, String parentPath, boolean includeContent) {
        String content = null;
        String encoding = null;
        boolean isFile = file.isFile();
        if (includeContent && isFile) {
            try {
                byte[] bytes = Files.readBytes(file);
                content = new String(Base64Encoder.encode(bytes));
                encoding = "base64";
            } catch (IOException e) {
                LOG.warn("Failed to load: " + file.getPath() + ". " + e, e);
            }
        }
        String type = file.isDirectory() ? "dir" : "file";
        long size = 0;
        if (isFile) {
            size = file.length();
        }
        String name = file.getName();
        String path = name;
        if (Strings.isNotBlank(parentPath)) {
            String separator = path.endsWith("/") ? "" : "/";
            path = parentPath + separator + name;
        }
        FileDTO fileDTO = new FileDTO(type, size, name, path, encoding, content);
        if (isFile && name.endsWith(".xml")) {
            // lets load the XML namespaces
            try {
                Set<String> uris = XmlHelper.getNamespaces(file);
                if (uris.size() > 0) {
                    String[] namespaces = uris.toArray(new String[uris.size()]);
                    fileDTO.setXmlNamespaces(namespaces);
                }
            } catch (Exception e) {
                LOG.warn("Failed to parse the XML namespaces in " + file + " due: " + e.getMessage() + ". This exception is ignored.", e);
            }
        }
        return fileDTO;
    }

    @Override
    public String toString() {
        return "FileDTO{" +
                "type='" + type + '\'' +
                ", size=" + size +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", sha='" + sha + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    public String getContent() {
        return content;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getEncoding() {
        return encoding;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public long getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String[] getXmlNamespaces() {
        return xmlNamespaces;
    }

    public void setXmlNamespaces(String[] xmlNamespaces) {
        this.xmlNamespaces = xmlNamespaces;
    }
}
