package b2.BackBlaze2;

import b2.BackBlaze2.listeners.ProgressRequestBody;
import b2.BackBlaze2.models.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.squareup.okhttp.*;
import b2.BackBlaze2.models.Callback;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

/**
 * <a href="https://www.backblaze.com/b2/docs/">Official BackBlaze B2 Docs.</a>
 */
public class B2 {
    public static final String BUCKET_TYPE_PUBLIC = "allPublic";
    public static final String BUCKET_TYPE_PRIVATE = "allPrivate";
    private B2Info info;
    private final OkHttpClient client;
    private final Gson gson;
    private String USER_AGENT = "Mozilla/5.0 (Linux; Android 8.0.0; SAMSUNG-SM-G950N/KSU3CRJ1 Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/8.2 Chrome/63.0.3239.111 Mobile Safari/537.36";
    /**
     * Initialize B2.
     *
     * @param accountId      The identifier for the account.
     * @param applicationKey The application Key for the account.
     */
    public B2(final String accountId, final String applicationKey) {
        gson = new Gson();
        client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Constants.AUTHORIZATION_URL)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", Utils.credToBasic64(accountId, applicationKey))
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                info = gson.fromJson(response.body().string(), B2Info.class);
                System.out.println("인증 성공!!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("인증 실패!!" + e.getMessage());

        }
    }

    /**
     * Used to log in to the B2 API.
     * Returns an authorization token that can be used for account-level operations,
     * and a URL that should be used as the base URL for subsequent API calls.
     *
     * @return B2Info instance
     */
    public B2Info getInfo() {
        return info;
    }


    /**
     * Creates a new bucket. A bucket belongs to the account used to create it.
     *
     * @param b2Bucket Set your bucketName "The name to give the new bucket.",
     *                 bucketType "allPublic or allPrivate"
     * @return Newly created bucket.
     * @see <a href="https://www.backblaze.com/b2/docs/b2_create_bucket.html">Create Bucket</a>
     */
    public B2Bucket createBucket(B2Bucket b2Bucket) {

        b2Bucket.setAccountId(info.getAccountId());
        Request request = new Request
                .Builder()
                .url(info.getApiUrl() + Constants.CREATE_BUCKET_URL)
                .method("POST", RequestBody.create(MediaType.parse("application/json"), gson.toJson(b2Bucket)))
                .addHeader("Authorization", info.getAuthorizationToken())
                .addHeader("Accept", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {


                // System.out.println("Response:" + response.body().string());
                // System.out.println(response.body().toString());

                // b2BucketResp = gson.fromJson(response.body().string(), B2Bucket.class);
                JSONObject json = new JSONObject(response.body().string());

                String bucketID = json.getString("bucketId");
                b2Bucket.setBucketId(bucketID);
                         
                System.out.println("버킷 생성 성공!!");
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("버킷 생성 실패!! " + e.getMessage());
        }

        return b2Bucket;
    }

    /**
     * Deletes the bucket specified. Only buckets that contain no version of any files can be deleted.
     *
     * @param bucketId The Id of the bucket to delete.
     * @return Deleted Bucket.
     * @see <a href="https://www.backblaze.com/b2/docs/b2_delete_bucket.html">Delete Bucket</a>
     */
    public B2Bucket deleteBucket(final String bucketId) {

        B2Bucket b2BucketResp = null;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("accountId", info.getAccountId());
        jsonObject.addProperty("bucketId", bucketId);
        Request request = new Request
                .Builder()
                .url(info.getApiUrl() + Constants.DELETE_BUCKET_URL)
                .method("POST", RequestBody.create(MediaType.parse("application/json"), gson.toJson(jsonObject)))
                .addHeader("Authorization", info.getAuthorizationToken())
                .addHeader("Accept", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {

                b2BucketResp = gson.fromJson(response.body().string(), B2Bucket.class);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return b2BucketResp;
    }

    /**
     * Update an existing bucket.
     *
     * @param bucketId   The Id of the bucket to update.
     * @param bucketType The bucketType of the bucket to delete "allPublic or allPrivate"
     * @return Updated Bucket.
     * @see <a href="https://www.backblaze.com/b2/docs/b2_update_bucket.html">Update Bucket</a>
     */
    public B2Bucket updateBucket(final String bucketId, final String bucketType) {

        B2Bucket b2BucketResp = null;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("accountId", info.getAccountId());
        jsonObject.addProperty("bucketId", bucketId);
        jsonObject.addProperty("bucketType", bucketType);
        Request request = new Request
                .Builder()
                .url(info.getApiUrl() + Constants.UPDATE_BUCKET_URL)
                .method("POST", RequestBody.create(MediaType.parse("application/json"), gson.toJson(jsonObject)))
                .addHeader("Authorization", info.getAuthorizationToken())
                .addHeader("Accept", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {

                b2BucketResp = gson.fromJson(response.body().string(), B2Bucket.class);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return b2BucketResp;
    }

    /**
     * Lists buckets associated with an account, in alphabetical order by bucket ID.
     *
     * @return All buckets created by an account
     * @see <a href="https://www.backblaze.com/b2/docs/b2_list_buckets.html">List Buckets</a>
     */
    public List<B2Bucket> getBuckets() {

        List<B2Bucket> buckets = null;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("accountId", info.getAccountId());
        Request request = new Request
                .Builder()
                .url(info.getApiUrl() + Constants.GET_BUCKETS_URL)
                .method("POST", RequestBody.create(MediaType.parse("application/json"), gson.toJson(jsonObject)))
                .addHeader("Authorization", info.getAuthorizationToken())
                .addHeader("Accept", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                buckets = gson.fromJson(response.body().string(), B2BucketsResponse.class).getBuckets();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return buckets;
    }

    /**
     * Gets an URL to use for uploading files.
     *
     * @param bucketId The ID of the bucket that you want to upload to.
     * @return Upload Information.
     * @see <a href="https://www.backblaze.com/b2/docs/b2_get_upload_url.html">Get Upload Url</a>
     */
    public B2UploadInfo getUploadInfo(final String bucketId) {

        B2UploadInfo b2UploadInfo = new B2UploadInfo();
        b2UploadInfo.setAuthorizationToken(info.getAuthorizationToken());
        b2UploadInfo.setBucketId(bucketId);

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("bucketId", bucketId);
        Request request = new Request
                .Builder()
                .url(info.getApiUrl() + Constants.GET_UPLOAD_URL)
                .method("POST", RequestBody.create(MediaType.parse("application/json"), gson.toJson(jsonObject)))
                .addHeader("Authorization", info.getAuthorizationToken())
                .addHeader("Accept", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {

                JSONObject json = new JSONObject(response.body().string());

                String uploadUrl = json.getString("uploadUrl");
                b2UploadInfo.setUploadUrl(uploadUrl);

                System.out.println("업로드 정보 가져오기 성공!!");
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("업로드 정보 가져오기 실패!!" + e.getMessage());
        }

        return b2UploadInfo;
    }


    /**
     * Uploads one file to B2, returning its unique file ID.
     *
     * @param file         The file to upload
     * @param b2UploadInfo Upload Info returned by {@link #getUploadInfo(String)}.
     * @return Information about the uploaded file
     * @see <a href="hhttps://www.backblaze.com/b2/docs/b2_upload_file.html">Upload File</a>
     */
    public B2File uploadFile(File file, B2UploadInfo b2UploadInfo) {

        B2File b2File = null;

        

        try {

            RequestBody requestBody = new MultipartBuilder()
        .type(MultipartBuilder.FORM)
        .addFormDataPart("10MB", "10MB.txt", RequestBody.create(MediaType.parse(Files.probeContentType(file.toPath())), file))
        .build();

            Request request = new Request
                    .Builder()
                    .post(requestBody)
                    .url(b2UploadInfo.getUploadUrl())
                    .addHeader("Authorization", b2UploadInfo.getAuthorizationToken())
                    .addHeader("X-Bz-File-Name", file.getName())
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("X-Bz-Content-Sha1", getFileHash(file))
                    .addHeader("Content-Type", "b2/x-auto")
                    .build();

                    try {
                        Response response = client.newCall(request).execute();
                    } catch(SocketException e) {
                        System.out.println(e.getMessage());
                    }
            
 
            //     @Override
            //     public void onFailure(Call call, IOException e) {
            //         System.err.println("Error Occurred");
            //     }
     
            //     @Override
            //     public void onResponse(Call call, Response response) throws IOException {
            //         ResponseBody body = response.body();
            //         if (body != null) {
            //             System.out.println("Response:" + body.string());
            //         }
            //     }
            // }););
            // if (response.isSuccessful() && response.body() != null) {
            //     System.out.println("성공!!");
            //     b2File = gson.fromJson(response.body().string(), B2File.class);
            // }
        } catch (IOException e ) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException error) {

        }
        return b2File;
    }

    /**
     * Uploads one file to B2 with progress information, returning its unique file ID.
     *
     * @param file             The file to upload
     * @param b2UploadInfo     Upload Info returned by {@link #getUploadInfo(String)}.
     * @param progressListener For checking upload progress.
     * @return Information about the uploaded file
     * @see <a href="hhttps://www.backblaze.com/b2/docs/b2_upload_file.html">Upload File</a>
     */
    public B2File uploadFile(File file, B2UploadInfo b2UploadInfo, ProgressRequestBody.ProgressListener progressListener) {
        B2File b2File = null;
        try {


            RequestBody requestBody = new MultipartBuilder()
            .type(MultipartBuilder.FORM)
            .addFormDataPart("10MB", "10MB.txt",
            RequestBody.create(MediaType.parse(Files.probeContentType(file.toPath())), file)).build();

            Request request = new Request
                    .Builder()
                    
                    .post(requestBody)
                    // .method("POST", new ProgressRequestBody(RequestBody.create(MediaType.parse(Files.probeContentType(file.toPath())), file), progressListener, file.length()))
                    .url(b2UploadInfo.getUploadUrl())
                    .addHeader("Authorization", b2UploadInfo.getAuthorizationToken())
                    .addHeader("X-Bz-File-Name", file.getName())
                    .addHeader("User-Agent", USER_AGENT)
                  
                    // .addHeader("X-Bz-Content-Sha1", Utils.getFileSha1Hash(file))
                    .build();

            Response response = client.newCall(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                System.out.println("성공!!");
                b2File = gson.fromJson(response.body().string(), B2File.class);
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        
        // catch(NullPointerException e) {
        //     System.out.println("비어있음!!" + e.getStackTrace() + e.getCause() + e.getClass() + e.getLocalizedMessage() + e.getSuppressed());
        // }
        return b2File;
    }

    /**
     * Deletes one version of a file from B2.
     *
     * @param fileName The name of the file.
     * @param fileId   The ID of the file.
     * @return Information about delete file.
     * @see <a href="https://www.backblaze.com/b2/docs/b2_delete_file_version.html">Delete File Version</a>
     */
    public B2File deleteFileVersion(final String fileName, final String fileId) {
        B2File b2File = null;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("fileName", fileName);
        jsonObject.addProperty("fileId", fileId);

        Request request = new Request
                .Builder()
                .method("POST", RequestBody.create(MediaType.parse("application/json"), gson.toJson(jsonObject)))
                .url(info.getApiUrl() + Constants.DELETE_FILE_VERSION_URL)
                .addHeader("Authorization", info.getAuthorizationToken())
                .addHeader("Accept", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                b2File = gson.fromJson(response.body().string(), B2File.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return b2File;
    }

    /**
     * Gets information about one file stored in B2.
     *
     * @param fileId The ID of the file.
     * @return Information about the file.
     * @see <a href="https://www.backblaze.com/b2/docs/b2_get_file_info.html">Get File Info</a>
     */
    public B2File getFileInfo(final String fileId) {
        B2File b2File = null;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("fileId", fileId);
        Request request = new Request
                .Builder()
                .method("POST", RequestBody.create(MediaType.parse("application/json"), gson.toJson(jsonObject)))
                .url(info.getApiUrl() + Constants.GET_FILE_INFO_URL)
                .addHeader("Authorization", info.getAuthorizationToken())
                .addHeader("Accept", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                b2File = gson.fromJson(response.body().string(), B2File.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return b2File;

    }

    /**
     * Hides a file so that downloading by name will not find the file,
     * but previous versions of the file are still stored. See File Versions about what it means to hide a file.
     *
     * @param bucketId The bucket containing the file to hide.
     * @param fileName The name of the file to hide.
     * @return Details about this action.
     * @see <a href="https://www.backblaze.com/b2/docs/b2_hide_file.html">Hide File</a>
     */
    public B2FileAction hideFile(final String bucketId, final String fileName) {
        B2FileAction b2FileAction = null;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("bucketId", bucketId);
        jsonObject.addProperty("fileName", fileName);
        Request request = new Request
                .Builder()
                .method("POST", RequestBody.create(MediaType.parse("application/json"), gson.toJson(jsonObject)))
                .url(info.getApiUrl() + Constants.HIDE_FILE_URL)
                .addHeader("Authorization", info.getAuthorizationToken())
                .addHeader("Accept", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                b2FileAction = gson.fromJson(response.body().string(), B2FileAction.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return b2FileAction;

    }


    /**
     * Lists the names of all files in a bucket, starting at a given name.
     *
     * @param bucketId      The bucket to look for file names in.
     * @param startFileName The first file name to return. If there is a file with this name,
     *                      it will be returned in the list. If not,
     *                      the first file name after this the first one after this name.
     * @param maxFileCount  The maximum number of files to return from this call.
     *                      The default value is 100, and the maximum allowed is 1000.
     * @return List of all files  names in a specific bucket.
     * @see <a href="https://www.backblaze.com/b2/docs/b2_list_file_names.html">List File Names</a>
     */
    public B2FilesResponse getFilesNames(final String bucketId, final String startFileName, final int maxFileCount) {
        B2FilesResponse b2FilesResponse = null;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("bucketId", bucketId);
        if (startFileName != null) {
            jsonObject.addProperty("startFileName", startFileName);
        }
        if (maxFileCount != 0) {
            jsonObject.addProperty("maxFileCount", maxFileCount);
        }
        Request request = new Request
                .Builder()
                .method("POST", RequestBody.create(MediaType.parse("application/json"), gson.toJson(jsonObject)))
                .url(info.getApiUrl() + Constants.GET_LIST_FILE_NAMES)
                .addHeader("Authorization", info.getAuthorizationToken())
                .addHeader("Accept", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                b2FilesResponse = gson.fromJson(response.body().string(), B2FilesResponse.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return b2FilesResponse;

    }

    /**
     * Lists all of the versions of all of the files contained in one bucket,
     * in alphabetical order by file name, and by reverse of date/time uploaded for versions of files with the same name.
     *
     * @param bucketId      The bucket to look for file names in.
     * @param startFileName The first file name to return. If there is a file with this name,
     *                      it will be returned in the list. If not,
     *                      the first file name after this the first one after this name.
     * @param startFileId   The first file ID to return. (See startFileName.)
     * @param maxFileCount  The maximum number of files to return from this call.
     *                      The default value is 100, and the maximum allowed is 1000.
     * @return List of all files versions in a specific bucket.
     * @see <a href="https://www.backblaze.com/b2/docs/b2_list_file_names.html">List File Names</a>
     */
    public B2FilesResponse getFilesVersions(final String bucketId, final String startFileName, final String startFileId, final int maxFileCount) {
        B2FilesResponse b2FilesResponse = null;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("bucketId", bucketId);
        if (startFileName != null) {
            jsonObject.addProperty("startFileName", startFileName);
        }
        if (startFileId != null) {
            jsonObject.addProperty("startFileId", startFileId);
        }
        if (maxFileCount != 0) {
            jsonObject.addProperty("maxFileCount", maxFileCount);
        }

        Request request = new Request
                .Builder()
                .method("POST", RequestBody.create(MediaType.parse("application/json"), gson.toJson(jsonObject)))
                .url(info.getApiUrl() + Constants.GET_LIST_FILE_VERSIONS)
                .addHeader("Authorization", info.getAuthorizationToken())
                .addHeader("Accept", "application/json")
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                b2FilesResponse = gson.fromJson(response.body().string(), B2FilesResponse.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return b2FilesResponse;
    }

    /**
     * Downloads one file from B2.
     *
     * @param fileId The Id of the file to download.
     * @return An InputStream of the file.
     * @see <a href="https://www.backblaze.com/b2/docs/b2_download_file_by_id.html">Download File By Id</a>
     */
    public InputStream downloadFileById(final String fileId) {
        InputStream inputStream = null;
        Request request = new Request
                .Builder()
                .url(info.getDownloadUrl() + Constants.DOWNLOAD_FILE_BY_ID + "?fileId=" + fileId)
                .addHeader("Authorization", info.getAuthorizationToken())
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                inputStream = response.body().byteStream();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inputStream;
    }

    /**
     * Downloads a private file by providing the name of the bucket and the name of the file.
     *
     * @param fileName   The name of file to download.
     * @param bucketName The name of the bucket containing the file.
     * @return InputStream of the file.
     * @see <a href="https://www.backblaze.com/b2/docs/b2_download_file_by_name.html">Download file By Name</a>
     */
    public InputStream downloadPrivateFileByName(final String fileName, final String bucketName) {
        InputStream inputStream = null;
        Request request = new Request
                .Builder()
                .url(info.getDownloadUrl() + "/file/" + bucketName + "/" + fileName)
                .addHeader("Authorization", info.getAuthorizationToken())
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                inputStream = response.body().byteStream();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inputStream;
    }

    /**
     * Downloads a public file by providing the name of the bucket and the name of the file.
     *
     * @param fileName   The name of file to download.
     * @param bucketName The name of the bucket containing the file.
     * @return InputStream of the file.
     * @see <a href="https://www.backblaze.com/b2/docs/b2_download_file_by_name.html">Download file By Name</a>
     */
    public InputStream downloadPublicFileByName(final String fileName, final String bucketName) {
        InputStream inputStream = null;
        Request request = new Request
                .Builder()
                .url(info.getDownloadUrl() + "/file/" + bucketName + "/" + fileName)
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                inputStream = response.body().byteStream();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inputStream;
    }

    private String getFileHash(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        FileInputStream fis = new FileInputStream(file);
        byte[] dataBytes = new byte[1024];
        int nread = 0;

        while ((nread = fis.read(dataBytes)) != -1) md.update(dataBytes, 0, nread);

        byte[] mdBytes = md.digest();
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < mdBytes.length; i++) sb.append(Integer.toString((mdBytes[i] & 0xff) + 0x100, 16).substring(1));

        return sb.toString();
    }

}