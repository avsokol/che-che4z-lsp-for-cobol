/*
 * Copyright (c) 2019 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Broadcom, Inc. - initial API and implementation
 */
package com.ca.lsp.cobol.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Slf4j
public class CobolWorkspaceServiceImpl implements CobolWorkspaceService {
  private static final CobolWorkspaceServiceImpl INSTANCE = new CobolWorkspaceServiceImpl();

  private static final String COPYBOOK_FOLDER_NAME = "COPYBOOKS";
  private static final String URI_FILE_SEPARATOR = "/";
  private List<File> copybookList;
  private List<WorkspaceFolder> workspaceFolders;

  private CobolWorkspaceServiceImpl() {}

  /**
   * Create a list of Files for each copybook found in a specific folder under the workspace
   *
   * @param workspaceFolder is the folder sent from the client (represent the rootFolder of the
   *     workspace opened in the client)
   */
  private void createCopybookList(WorkspaceFolder workspaceFolder) {
    copybookList = new ArrayList<>();
    try (Stream<Path> copybookFoldersStream =
        Files.list(
            Paths.get(
                new URI(workspaceFolder.getUri() + URI_FILE_SEPARATOR + COPYBOOK_FOLDER_NAME)))) {
      copybookFoldersStream.map(Path::toFile).forEach(copybookList::add);
    } catch (URISyntaxException | IOException e) {
      log.error(e.getMessage());
    }
  }

  @Override
  public CompletableFuture<List<? extends SymbolInformation>> symbol(WorkspaceSymbolParams params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    throw new UnsupportedOperationException();
  }

  /**
   * Search in a list of given URI paths the presence of copybooks
   *
   * @param workspaceFolders list of URI paths
   */
  void scanWorkspaceForCopybooks(List<WorkspaceFolder> workspaceFolders) {
    setWorkspaceFolders(workspaceFolders);
    getWorkspaceFolders().forEach(this::createCopybookList);
  }

  /** @return List of copybooks */
  public List<File> getCopybookList() {
    return copybookList;
  }

  @Override
  public Path getURIByFileName(String fileName) {
    AtomicReference<Path> outputURIPath = new AtomicReference<>();
    getWorkspaceFolders()
        .forEach(
            workspaceFolder -> {
              try {
                File workspaceFolderFile =
                    new File(
                        new URI(
                            workspaceFolder.getUri() + URI_FILE_SEPARATOR + COPYBOOK_FOLDER_NAME));
                Path workspaceFolderPath = workspaceFolderFile.toPath();

                Stream<Path> pathStream =
                    Files.find(
                        workspaceFolderPath,
                        100,
                        (path, basicFileAttributes) -> {
                          File resFile = path.toFile();
                          outputURIPath.set(resFile.toPath());
                          return !resFile.isDirectory() && resFile.getName().contains(fileName);
                        });
                log.info("Number of matches" + pathStream.count());
              } catch (IOException | URISyntaxException e) {
                log.error(e.getMessage());
              }
            });

    return outputURIPath.get();
  }

  private List<WorkspaceFolder> getWorkspaceFolders() {
    return workspaceFolders;
  }

  private void setWorkspaceFolders(List<WorkspaceFolder> workspaceFolders) {
    this.workspaceFolders = workspaceFolders;
  }

  public static CobolWorkspaceServiceImpl getInstance() {
    return INSTANCE;
  }
}