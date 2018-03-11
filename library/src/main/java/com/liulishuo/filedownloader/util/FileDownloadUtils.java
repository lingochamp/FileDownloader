/*
 * Copyright (c) 2015 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.filedownloader.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import com.liulishuo.filedownloader.BuildConfig;
import com.liulishuo.filedownloader.connection.FileDownloadConnection;
import com.liulishuo.filedownloader.download.CustomComponentHolder;
import com.liulishuo.filedownloader.exception.FileDownloadGiveUpRetryException;
import com.liulishuo.filedownloader.model.FileDownloadModel;
import com.liulishuo.filedownloader.services.FileDownloadService;
import com.liulishuo.filedownloader.stream.FileDownloadOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.liulishuo.filedownloader.model.FileDownloadModel.TOTAL_VALUE_IN_CHUNKED_RESOURCE;

/**
 * The utils for FileDownloader.
 */
@SuppressWarnings({"SameParameterValue", "WeakerAccess"})
public class FileDownloadUtils {

    private static int minProgressStep = 65536;
    private static long minProgressTime = 2000;

    /**
     * @param minProgressStep The minimum bytes interval in per step to sync to the file and the
     *                        database.
     *                        <p>
     *                        Used for adjudging whether is time to sync the downloaded so far bytes
     *                        to database and make sure sync the downloaded buffer to local file.
     *                        <p/>
     *                        More smaller more frequently, then download more slowly, but will more
     *                        safer in scene of the process is killed unexpected.
     *                        <p/>
     *                        Default 65536, which follow the value in
     *                        com.android.providers.downloads.Constants.
     * @see com.liulishuo.filedownloader.download.DownloadStatusCallback#onProgress(long)
     * @see #setMinProgressTime(long)
     */
    public static void setMinProgressStep(int minProgressStep) throws IllegalAccessException {
        if (isDownloaderProcess(FileDownloadHelper.getAppContext())) {
            FileDownloadUtils.minProgressStep = minProgressStep;
        } else {
            throw new IllegalAccessException("This value is used in the :filedownloader process,"
                    + " so set this value in your process is without effect. You can add "
                    + "'process.non-separate=true' in 'filedownloader.properties' to share the main"
                    + " process to FileDownloadService. Or you can configure this value in "
                    + "'filedownloader.properties' by 'download.min-progress-step'.");
        }
    }

    /**
     * @param minProgressTime The minimum millisecond interval in per time to sync to the file and
     *                        the database.
     *                        <p>
     *                        Used for adjudging whether is time to sync the downloaded so far bytes
     *                        to database and make sure sync the downloaded buffer to local file.
     *                        <p/>
     *                        More smaller more frequently, then download more slowly, but will more
     *                        safer in scene of the process is killed unexpected.
     *                        <p/>
     *                        Default 2000, which follow the value in
     *                        com.android.providers.downloads.Constants.
     * @see com.liulishuo.filedownloader.download.DownloadStatusCallback#onProgress(long)
     * @see #setMinProgressStep(int)
     */
    public static void setMinProgressTime(long minProgressTime) throws IllegalAccessException {
        if (isDownloaderProcess(FileDownloadHelper.getAppContext())) {
            FileDownloadUtils.minProgressTime = minProgressTime;
        } else {
            throw new IllegalAccessException("This value is used in the :filedownloader process,"
                    + " so set this value in your process is without effect. You can add "
                    + "'process.non-separate=true' in 'filedownloader.properties' to share the main"
                    + " process to FileDownloadService. Or you can configure this value in "
                    + "'filedownloader.properties' by 'download.min-progress-time'.");
        }
    }

    public static int getMinProgressStep() {
        return minProgressStep;
    }

    public static long getMinProgressTime() {
        return minProgressTime;
    }

    /**
     * Checks whether the filename looks legitimate.
     */
    @SuppressWarnings({"SameReturnValue", "UnusedParameters"})
    public static boolean isFilenameValid(String filename) {
//        filename = filename.replaceFirst("/+", "/"); // normalize leading
        // slashes
//        return filename.startsWith(Environment.getDownloadCacheDirectory()
//                .toString())
//                || filename.startsWith(Environment
//                .getExternalStorageDirectory().toString());
        return true;
    }

    private static String defaultSaveRootPath;

    public static String getDefaultSaveRootPath() {
        if (!TextUtils.isEmpty(defaultSaveRootPath)) {
            return defaultSaveRootPath;
        }

        if (FileDownloadHelper.getAppContext().getExternalCacheDir() == null) {
            return Environment.getDownloadCacheDirectory().getAbsolutePath();
        } else {
            //noinspection ConstantConditions
            return FileDownloadHelper.getAppContext().getExternalCacheDir().getAbsolutePath();
        }
    }

    public static String getDefaultSaveFilePath(final String url) {
        return generateFilePath(getDefaultSaveRootPath(), generateFileName(url));
    }

    public static String generateFileName(final String url) {
        return md5(url);
    }

    /**
     * @see #getTargetFilePath(String, boolean, String)
     */
    public static String generateFilePath(String directory, String filename) {
        if (filename == null) {
            throw new IllegalStateException("can't generate real path, the file name is null");
        }

        if (directory == null) {
            throw new IllegalStateException("can't generate real path, the directory is null");
        }

        return formatString("%s%s%s", directory, File.separator, filename);
    }

    /**
     * The path is used as the default directory in the case of the task without set path.
     *
     * @param path default root path for save download file.
     * @see com.liulishuo.filedownloader.BaseDownloadTask#setPath(String, boolean)
     */
    public static void setDefaultSaveRootPath(final String path) {
        defaultSaveRootPath = path;
    }

    /**
     * @param targetPath The target path for the download task.
     * @return The temp path is {@code targetPath} in downloading status; The temp path is used for
     * storing the file not completed downloaded yet.
     */
    public static String getTempPath(final String targetPath) {
        return FileDownloadUtils.formatString("%s.temp", targetPath);
    }

    /**
     * @param url  The downloading URL.
     * @param path The absolute file path.
     * @return The download id.
     */
    public static int generateId(final String url, final String path) {
        return CustomComponentHolder.getImpl().getIdGeneratorInstance()
                .generateId(url, path, false);
    }

    /**
     * @param url  The downloading URL.
     * @param path If {@code pathAsDirectory} is {@code true}, {@code path} would be the absolute
     *             directory to place the file;
     *             If {@code pathAsDirectory} is {@code false}, {@code path} would be the absolute
     *             file path.
     * @return The download id.
     */
    public static int generateId(final String url, final String path,
                                 final boolean pathAsDirectory) {
        return CustomComponentHolder.getImpl().getIdGeneratorInstance()
                .generateId(url, path, pathAsDirectory);
    }

    public static String md5(String string) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }


    public static String getStack() {
        return getStack(true);
    }

    public static String getStack(final boolean printLine) {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        return getStack(stackTrace, printLine);
    }

    public static String getStack(final StackTraceElement[] stackTrace, final boolean printLine) {
        if ((stackTrace == null) || (stackTrace.length < 4)) {
            return "";
        }

        StringBuilder t = new StringBuilder();

        for (int i = 3; i < stackTrace.length; i++) {
            if (!stackTrace[i].getClassName().contains("com.liulishuo.filedownloader")) {
                continue;
            }
            t.append("[");
            t.append(stackTrace[i].getClassName()
                    .substring("com.liulishuo.filedownloader".length()));
            t.append(":");
            t.append(stackTrace[i].getMethodName());
            if (printLine) {
                t.append("(").append(stackTrace[i].getLineNumber()).append(")]");
            } else {
                t.append("]");
            }
        }
        return t.toString();
    }

    private static Boolean isDownloaderProcess;

    /**
     * @param context the context
     * @return {@code true} if the FileDownloadService is allowed to run on the current process,
     * {@code false} otherwise.
     */
    public static boolean isDownloaderProcess(final Context context) {
        if (isDownloaderProcess != null) {
            return isDownloaderProcess;
        }

        boolean result = false;
        do {
            if (FileDownloadProperties.getImpl().processNonSeparate) {
                result = true;
                break;
            }

            int pid = android.os.Process.myPid();
            final ActivityManager activityManager = (ActivityManager) context.
                    getSystemService(Context.ACTIVITY_SERVICE);

            if (activityManager == null) {
                FileDownloadLog.w(FileDownloadUtils.class, "fail to get the activity manager!");
                return false;
            }

            final List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList =
                    activityManager.getRunningAppProcesses();

            if (null == runningAppProcessInfoList || runningAppProcessInfoList.isEmpty()) {
                FileDownloadLog
                        .w(FileDownloadUtils.class, "The running app process info list from"
                                + " ActivityManager is null or empty, maybe current App is not "
                                + "running.");
                return false;
            }

            for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcessInfoList) {
                if (processInfo.pid == pid) {
                    result = processInfo.processName.endsWith(":filedownloader");
                    break;
                }
            }

        } while (false);

        isDownloaderProcess = result;
        return isDownloaderProcess;
    }

    public static String[] convertHeaderString(final String nameAndValuesString) {
        final String[] lineString = nameAndValuesString.split("\n");
        final String[] namesAndValues = new String[lineString.length * 2];

        for (int i = 0; i < lineString.length; i++) {
            final String[] nameAndValue = lineString[i].split(": ");
            /**
             * @see Headers#toString()
             * @see Headers#name(int)
             * @see Headers#value(int)
             */
            namesAndValues[i * 2] = nameAndValue[0];
            namesAndValues[i * 2 + 1] = nameAndValue[1];
        }

        return namesAndValues;
    }

    public static long getFreeSpaceBytes(final String path) {
        long freeSpaceBytes;
        final StatFs statFs = new StatFs(path);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            freeSpaceBytes = statFs.getAvailableBytes();
        } else {
            //noinspection deprecation
            freeSpaceBytes = statFs.getAvailableBlocks() * (long) statFs.getBlockSize();
        }

        return freeSpaceBytes;
    }

    public static String formatString(final String msg, Object... args) {
        return String.format(Locale.ENGLISH, msg, args);
    }

    private static final String INTERNAL_DOCUMENT_NAME = "filedownloader";
    private static final String OLD_FILE_CONVERTED_FILE_NAME = ".old_file_converted";

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void markConverted(final Context context) {
        final File file = getConvertedMarkedFile(context);
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Boolean filenameConverted = null;

    /**
     * @return Whether has converted all files' name from 'filename'(in old architecture) to
     * 'filename.temp', if it's in downloading state.
     * <p>
     * If {@code true}, You can check whether the file has completed downloading with
     * {@link File#exists()} directly.
     * <p>
     * when {@link FileDownloadService#onCreate()} is invoked, This value will be assigned to
     * {@code true} only once since you upgrade the filedownloader version to 0.3.3 or higher.
     */
    public static boolean isFilenameConverted(final Context context) {
        if (filenameConverted == null) {
            filenameConverted = getConvertedMarkedFile(context).exists();
        }

        return filenameConverted;
    }

    public static File getConvertedMarkedFile(final Context context) {
        return new File(context.getFilesDir().getAbsolutePath() + File.separator
                + INTERNAL_DOCUMENT_NAME, OLD_FILE_CONVERTED_FILE_NAME);
    }

    private static final Pattern CONTENT_DISPOSITION_QUOTED_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*\"([^\"]*)\"");
    // note on http://www.ietf.org/rfc/rfc1806.txt
    private static final Pattern CONTENT_DISPOSITION_NON_QUOTED_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*(.*)");

    public static long parseContentRangeFoInstanceLength(String contentRange) {
        if (contentRange == null) return -1;

        final String[] session = contentRange.split("/");
        if (session.length >= 2) {
            try {
                return Long.parseLong(session[1]);
            } catch (NumberFormatException e) {
                FileDownloadLog.w(FileDownloadUtils.class, "parse instance length failed with %s",
                        contentRange);
            }
        }

        return -1;
    }

    /**
     * The same to com.android.providers.downloads.Helpers#parseContentDisposition.
     * </p>
     * Parse the Content-Disposition HTTP Header. The format of the header
     * is defined here: http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html
     * This header provides a filename for content that is going to be
     * downloaded to the file system. We only support the attachment type.
     */
    public static String parseContentDisposition(String contentDisposition) {
        if (contentDisposition == null) {
            return null;
        }

        try {
            Matcher m = CONTENT_DISPOSITION_QUOTED_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                return m.group(1);
            }

            m = CONTENT_DISPOSITION_NON_QUOTED_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                return m.group(1);
            }
        } catch (IllegalStateException ex) {
            // This function is defined as returning null when it can't parse the header
        }
        return null;
    }

    /**
     * @param path            If {@code pathAsDirectory} is true, the {@code path} would be the
     *                        absolute directory to settle down the file;
     *                        If {@code pathAsDirectory} is false, the {@code path} would be the
     *                        absolute file path.
     * @param pathAsDirectory whether the {@code path} is a directory.
     * @param filename        the file's name.
     * @return the absolute path of the file. If can't find by params, will return {@code null}.
     */
    public static String getTargetFilePath(String path, boolean pathAsDirectory, String filename) {
        if (path == null) {
            return null;
        }

        if (pathAsDirectory) {
            if (filename == null) {
                return null;
            }

            return FileDownloadUtils.generateFilePath(path, filename);
        } else {
            return path;
        }
    }

    /**
     * The same to {@link File#getParent()}, for non-creating a file object.
     *
     * @return this file's parent pathname or {@code null}.
     */
    public static String getParent(final String path) {
        int length = path.length(), firstInPath = 0;
        if (File.separatorChar == '\\' && length > 2 && path.charAt(1) == ':') {
            firstInPath = 2;
        }
        int index = path.lastIndexOf(File.separatorChar);
        if (index == -1 && firstInPath > 0) {
            index = 2;
        }
        if (index == -1 || path.charAt(length - 1) == File.separatorChar) {
            return null;
        }
        if (path.indexOf(File.separatorChar) == index
                && path.charAt(firstInPath) == File.separatorChar) {
            return path.substring(0, index + 1);
        }
        return path.substring(0, index);
    }

    private static final String FILEDOWNLOADER_PREFIX = "FileDownloader";

    public static String getThreadPoolName(String name) {
        return FILEDOWNLOADER_PREFIX + "-" + name;
    }

    public static boolean isNetworkNotOnWifiType() {
        final ConnectivityManager manager = (ConnectivityManager) FileDownloadHelper.getAppContext()
                .
                        getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            FileDownloadLog.w(FileDownloadUtils.class, "failed to get connectivity manager!");
            return true;
        }

        //noinspection MissingPermission, because we check permission accessable when invoked
        final NetworkInfo info = manager.getActiveNetworkInfo();

        return info == null || info.getType() != ConnectivityManager.TYPE_WIFI;
    }

    public static boolean checkPermission(String permission) {
        final int perm = FileDownloadHelper.getAppContext()
                .checkCallingOrSelfPermission(permission);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    public static long convertContentLengthString(String s) {
        if (s == null) return -1;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String findEtag(final int id, FileDownloadConnection connection) {
        if (connection == null) {
            throw new RuntimeException("connection is null when findEtag");
        }

        final String newEtag = connection.getResponseHeaderField("Etag");

        if (FileDownloadLog.NEED_LOG) {
            FileDownloadLog.d(FileDownloadUtils.class, "etag find %s for task(%d)", newEtag, id);
        }

        return newEtag;
    }

    // accept range is effect by  response code and Accept-Ranges header field.
    public static boolean isAcceptRange(int responseCode, FileDownloadConnection connection) {
        if (responseCode == HttpURLConnection.HTTP_PARTIAL
                || responseCode == FileDownloadConnection.RESPONSE_CODE_FROM_OFFSET) return true;

        final String acceptRanges = connection.getResponseHeaderField("Accept-Ranges");
        return "bytes".equals(acceptRanges);
    }

    // because of we using one of two HEAD method to request or using range:0-0 to trial connection
    // only if connection api not support, so we test content-range first and then test
    // content-length.
    public static long findInstanceLengthForTrial(FileDownloadConnection connection) {
        long length = findInstanceLengthFromContentRange(connection);
        if (length < 0) {
            length = TOTAL_VALUE_IN_CHUNKED_RESOURCE;
            FileDownloadLog.w(FileDownloadUtils.class, "don't get instance length from"
                    + "Content-Range header");
        }
        // the response of HEAD method is not very canonical sometimes(it depends on server
        // implementation)
        // so that it's uncertain the content-length is the same as the response of GET method if
        // content-length=0, so we have to filter this case in here.
        if (length == 0 && FileDownloadProperties.getImpl().trialConnectionHeadMethod) {
            length = TOTAL_VALUE_IN_CHUNKED_RESOURCE;
        }

        return length;
    }

    public static long findInstanceLengthFromContentRange(FileDownloadConnection connection) {
        return parseContentRangeFoInstanceLength(getContentRangeHeader(connection));
    }

    private static String getContentRangeHeader(FileDownloadConnection connection) {
        return connection.getResponseHeaderField("Content-Range");
    }

    public static long findContentLength(final int id, FileDownloadConnection connection) {
        long contentLength = convertContentLengthString(
                connection.getResponseHeaderField("Content-Length"));
        final String transferEncoding = connection.getResponseHeaderField("Transfer-Encoding");

        if (contentLength < 0) {
            final boolean isEncodingChunked = transferEncoding != null && transferEncoding
                    .equals("chunked");
            if (!isEncodingChunked) {
                // not chunked transfer encoding data
                if (FileDownloadProperties.getImpl().httpLenient) {
                    // do not response content-length either not chunk transfer encoding,
                    // but HTTP lenient is true, so handle as the case of transfer encoding chunk
                    contentLength = TOTAL_VALUE_IN_CHUNKED_RESOURCE;
                    if (FileDownloadLog.NEED_LOG) {
                        FileDownloadLog
                                .d(FileDownloadUtils.class, "%d response header is not legal but "
                                        + "HTTP lenient is true, so handle as the case of "
                                        + "transfer encoding chunk", id);
                    }
                } else {
                    throw new FileDownloadGiveUpRetryException("can't know the size of the "
                            + "download file, and its Transfer-Encoding is not Chunked "
                            + "either.\nyou can ignore such exception by add "
                            + "http.lenient=true to the filedownloader.properties");
                }
            } else {
                contentLength = TOTAL_VALUE_IN_CHUNKED_RESOURCE;
            }
        }

        return contentLength;
    }

    public static long findContentLengthFromContentRange(FileDownloadConnection connection) {
        final String contentRange = getContentRangeHeader(connection);
        long contentLength = parseContentLengthFromContentRange(contentRange);
        if (contentLength  < 0) contentLength = TOTAL_VALUE_IN_CHUNKED_RESOURCE;
        return contentLength;
    }

    public static long parseContentLengthFromContentRange(String contentRange) {
        if (contentRange == null || contentRange.length() == 0) return -1;
        final String pattern = "bytes (\\d+)-(\\d+)/\\d+";
        try {
            final Pattern r = Pattern.compile(pattern);
            final Matcher m = r.matcher(contentRange);
            if (m.find()) {
                final long rangeStart = Long.parseLong(m.group(1));
                final long rangeEnd = Long.parseLong(m.group(2));
                return rangeEnd - rangeStart + 1;
            }
        } catch (Exception e) {
            FileDownloadLog.e(FileDownloadUtils.class, e, "parse content length"
                    + " from content range error");
        }
        return -1;
    }

    public static String findFilename(FileDownloadConnection connection, String url) {
        String filename = FileDownloadUtils.parseContentDisposition(connection.
                getResponseHeaderField("Content-Disposition"));

        if (TextUtils.isEmpty(filename)) {
            filename = FileDownloadUtils.generateFileName(url);
        }

        return filename;
    }

    public static FileDownloadOutputStream createOutputStream(final String path)
            throws IOException {

        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException("found invalid internal destination path, empty");
        }

        //noinspection ConstantConditions
        if (!FileDownloadUtils.isFilenameValid(path)) {
            throw new RuntimeException(
                    FileDownloadUtils.formatString("found invalid internal destination filename"
                            + " %s", path));
        }

        File file = new File(path);

        if (file.exists() && file.isDirectory()) {
            throw new RuntimeException(
                    FileDownloadUtils.formatString("found invalid internal destination path[%s],"
                            + " & path is directory[%B]", path, file.isDirectory()));
        }
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException(
                        FileDownloadUtils.formatString("create new file error  %s",
                                file.getAbsolutePath()));
            }
        }

        return CustomComponentHolder.getImpl().createOutputStream(file);
    }

    public static boolean isBreakpointAvailable(final int id, final FileDownloadModel model) {
        return isBreakpointAvailable(id, model, null);
    }

    /**
     * @return can resume by break point
     */
    public static boolean isBreakpointAvailable(final int id, final FileDownloadModel model,
                                                final Boolean outputStreamSupportSeek) {
        if (model == null) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog.d(FileDownloadUtils.class, "can't continue %d model == null", id);
            }
            return false;
        }

        if (model.getTempFilePath() == null) {
            if (FileDownloadLog.NEED_LOG) {
                FileDownloadLog
                        .d(FileDownloadUtils.class, "can't continue %d temp path == null", id);
            }
            return false;
        }

        return isBreakpointAvailable(id, model, model.getTempFilePath(), outputStreamSupportSeek);
    }

    public static boolean isBreakpointAvailable(final int id, final FileDownloadModel model,
                                                final String path,
                                                final Boolean outputStreamSupportSeek) {
        boolean result = false;

        do {
            if (path == null) {
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadUtils.class, "can't continue %d path = null", id);
                }
                break;
            }

            File file = new File(path);
            final boolean isExists = file.exists();
            final boolean isDirectory = file.isDirectory();

            if (!isExists || isDirectory) {
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadUtils.class,
                            "can't continue %d file not suit, exists[%B], directory[%B]",
                            id, isExists, isDirectory);
                }
                break;
            }

            final long fileLength = file.length();
            final long currentOffset = model.getSoFar();

            if (model.getConnectionCount() <= 1 && currentOffset == 0) {
                // the sofar is stored on connection table
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadUtils.class,
                            "can't continue %d the downloaded-record is zero.",
                            id);
                }
                break;
            }

            final long totalLength = model.getTotal();
            if (fileLength < currentOffset
                    || (totalLength != TOTAL_VALUE_IN_CHUNKED_RESOURCE  // not chunk transfer
                    && (fileLength > totalLength || currentOffset >= totalLength))
                    ) {
                // dirty data.
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadUtils.class, "can't continue %d dirty data"
                                    + " fileLength[%d] sofar[%d] total[%d]",
                            id, fileLength, currentOffset, totalLength);
                }
                break;
            }

            if (outputStreamSupportSeek != null && !outputStreamSupportSeek
                    && totalLength == fileLength) {
                if (FileDownloadLog.NEED_LOG) {
                    FileDownloadLog.d(FileDownloadUtils.class, "can't continue %d, because of the "
                                    + "output stream doesn't support seek, but the task has "
                                    + "already pre-allocated, so we only can download it from the"
                                    + " very beginning.",
                            id);
                }
                break;
            }

            result = true;
        } while (false);


        return result;
    }

    public static void deleteTaskFiles(String targetFilepath, String tempFilePath) {
        deleteTempFile(tempFilePath);
        deleteTargetFile(targetFilepath);
    }

    public static void deleteTempFile(String tempFilePath) {
        if (tempFilePath != null) {
            final File tempFile = new File(tempFilePath);
            if (tempFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
        }
    }

    public static void deleteTargetFile(String targetFilePath) {
        if (targetFilePath != null) {
            final File targetFile = new File(targetFilePath);
            if (targetFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                targetFile.delete();
            }
        }
    }

    public static boolean isNeedSync(long bytesDelta, long timestampDelta) {
        return bytesDelta > FileDownloadUtils.getMinProgressStep()
                && timestampDelta > FileDownloadUtils.getMinProgressTime();
    }

    public static String defaultUserAgent() {
        return formatString("FileDownloader/%s", BuildConfig.VERSION_NAME);
    }
}
