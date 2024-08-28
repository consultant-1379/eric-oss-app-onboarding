/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.ae.apponboarding.v2.utils;

import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.COLON_CHAR;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.NEW_LINE;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.REPLACEMENT_FILENAME_SUFFIX;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.REPLACEMENT_TEXT;
import static com.ericsson.oss.ae.apponboarding.v2.utils.Constants.SPACE_CHAR;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File utils.
 */
public class FileUtils {

    /**
     * Read file to bytes.
     *
     * @param filePathInResourcesDirectory
     *            the file path in resources directory
     * @return the byte [ ]
     * @throws IOException
     *             the io exception
     */
    public static byte[] readFileToBytes(final String filePathInResourcesDirectory) throws IOException {
        return Files.readAllBytes(new File(FileUtils.class.getClassLoader().getResource(filePathInResourcesDirectory).getFile()).toPath());
    }

    public static void renameFileInJobDir(final String tempFolderLocation, final UUID jobId, final Path sourcePath) throws IOException {
        final Path baseDirPath = Paths.get(tempFolderLocation, jobId.toString());
        final Path sourceFilePath = baseDirPath.resolve(sourcePath);
        final Path targetFilePath = baseDirPath.resolve(sourcePath + REPLACEMENT_FILENAME_SUFFIX);
        Files.move(sourceFilePath, targetFilePath);
    }

    public static void replaceFileWithContentInJobDir(final String tempFolderLocation, final UUID jobId, final Path sourcePath,
                                                      final byte[] newContent)
            throws IOException {
        if (newContent != null) {
            renameFileInJobDir(tempFolderLocation, jobId, sourcePath);
            final Path baseDirPath = Paths.get(tempFolderLocation, jobId.toString());
            final Path sourceFilePath = baseDirPath.resolve(sourcePath);
            Files.write(sourceFilePath, newContent);
        } else {
            throw new RuntimeException("Null is not accepted as file content");
        }
    }

    public static void replaceGivenTextInFileInJobDir(final String tempFolderLocation, final UUID jobId, final Path sourcePath,
                                                      final String contentToReplace) throws IOException {
        replaceTextWithGivenTextInFileInJobDir(tempFolderLocation, jobId, sourcePath, contentToReplace, REPLACEMENT_TEXT);
    }

    public static void replaceTextWithGivenTextInFileInJobDir(final String tempFolderLocation, final UUID jobId, final Path sourcePath,
                                                              final String contentToReplace, final String newContent) throws IOException {
        if (contentToReplace != null) {
            final Path baseDirPath = Paths.get(tempFolderLocation, jobId.toString());
            final Path sourceFilePath = baseDirPath.resolve(sourcePath);
            final String fileData = Files.readString(sourceFilePath);

            renameFileInJobDir(tempFolderLocation, jobId, sourcePath);
            Files.write(sourceFilePath, fileData.replace(contentToReplace, newContent).getBytes());
        } else {
            throw new RuntimeException("Null is not accepted as replacement content");
        }
    }

    public static void replaceYamlArrayInFileInJobDir(final String tempFolderLocation, final UUID jobId, final String sourcePath,
                                                      final String arrayName)
            throws IOException {
        if (!StringUtils.isBlank(arrayName)) {
            final Path baseDirPath = Paths.get(tempFolderLocation, jobId.toString());
            final Path sourceFilePath = baseDirPath.resolve(sourcePath);

            String fileData = Files.readString(sourceFilePath);
            final String replacementText = arrayName + COLON_CHAR + SPACE_CHAR + REPLACEMENT_TEXT + NEW_LINE + REPLACEMENT_TEXT;
            fileData = fileData.replace(arrayName, replacementText);

            renameFileInJobDir(tempFolderLocation, jobId, Path.of(sourcePath));
            Files.write(sourceFilePath, fileData.getBytes());
        } else {
            throw new RuntimeException("Null is not accepted as array name");
        }
    }

    public static void revertRenamedFile(final String tempFolderLocation, final UUID jobId, final Path originalPath) throws IOException {
        final Path baseDirPath = Paths.get(tempFolderLocation, jobId.toString());
        final Path sourceFilePath = baseDirPath.resolve(originalPath + REPLACEMENT_FILENAME_SUFFIX);
        final Path targetFilePath = baseDirPath.resolve(originalPath);
        if (Files.exists(sourceFilePath)) {
            Files.move(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void copyFileToFolder(final Path targetDirPath, final String fileName, final String sourceFileLocation)
            throws URISyntaxException, IOException {
        final Path targetFilePath = targetDirPath.resolve(fileName);
        final Path sourceFilePath = Paths.get(FileUtils.class.getClassLoader().getResource(sourceFileLocation).toURI());
        Files.createDirectories(targetDirPath);
        Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void createNewFileInFolder(final Path newFilePath, final String content) throws IOException {
        Files.createDirectories(newFilePath.getParent());
        if (Files.notExists(newFilePath)) {
            Files.write(newFilePath, content.getBytes());
        }
    }

    public static void cleanUpFilesCreated(final String tempFolderLocation, final UUID jobId) throws IOException {
        final Path folderToDelete = Paths.get(tempFolderLocation, jobId.toString());
        if (Files.exists(folderToDelete)) {
            try (final Stream<Path> walk = Files.walk(folderToDelete)) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }

            Files.deleteIfExists(folderToDelete);
        }

        removeFile(Constants.TEST_CSAR_NAME, tempFolderLocation);
        removeFile(Constants.INVALID_ARCHIVE_CSAR_NAME, tempFolderLocation);
    }

    public static void deleteFilesInDirectory(final String directory, final UUID jobId) throws IOException {
        final Path folderToDelete = Paths.get(directory, jobId.toString());
        if (Files.exists(folderToDelete)) {
            try (final Stream<Path> walk = Files.walk(folderToDelete)) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(file -> {
                    try {
                        Files.deleteIfExists(file.toPath());
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            Files.deleteIfExists(folderToDelete);
        }
    }

    public static void cleanUpTempAllDirectories(final String tempFolderLocation) {
        Path tempFolderPath = Paths.get(tempFolderLocation);
        try {
            Files.walkFileTree(tempFolderPath, new CustomFileVisitor(tempFolderPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void removeFile(final String fileName, final String folderLocation) throws IOException {
        final Path folderPath = Path.of(folderLocation);
        final Path filePath = folderPath.resolve(fileName);
        Files.deleteIfExists(filePath);
    }

    private record CustomFileVisitor(Path tmpFolder) implements FileVisitor<Path> {
        private static final Logger logger = LoggerFactory.getLogger(CustomFileVisitor.class);

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            // Handle file visit
            Path parentDir = file.getParent();
            if (parentDir.toAbsolutePath().equals(tmpFolder.toAbsolutePath()) && file.getFileName().toString().endsWith(".csar")) {
                Files.deleteIfExists(file);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            // Handle pre-visit directory
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            // Handle post-visit directory
            if (dir.toAbsolutePath().equals(tmpFolder.toAbsolutePath()) || isUUID(dir.getFileName().toString())) {
                if (!dir.toAbsolutePath().equals(tmpFolder.toAbsolutePath())) {
                    deleteFilesInDirectory(tmpFolder.toString(), UUID.fromString(dir.getFileName().toString()));
                }
                return FileVisitResult.CONTINUE; // Continue traversal if the directory name is a UUID
            } else {
                return FileVisitResult.SKIP_SUBTREE; // Skip traversal if the directory name is not a UUID
            }
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            // Handle visit failure
            if (exc instanceof AccessDeniedException) {
                logger.info("Unable to delete. Access denied for file: {}", file);
                return FileVisitResult.CONTINUE;
            } else {
                throw new RuntimeException(exc);
            }
        }

        private boolean isUUID(String string) {
            try {
                UUID.fromString(string);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
}
