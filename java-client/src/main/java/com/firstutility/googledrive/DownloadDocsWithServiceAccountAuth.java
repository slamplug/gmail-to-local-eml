package com.firstutility.googledrive;

import static java.lang.String.format;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

public class DownloadDocsWithServiceAccountAuth {

    private static final String APPLICATION_NAME = "Drive API Quickstart";

    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/drive-java-quickstart");

    private static FileDataStoreFactory DATA_STORE_FACTORY;

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static HttpTransport HTTP_TRANSPORT;

    private static final List<String> SCOPES = Arrays.asList(
            DriveScopes.DRIVE,
            DriveScopes.DRIVE_FILE);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public static Credential authorize() throws IOException, GeneralSecurityException, URISyntaxException {

        URL resource = DownloadDocsWithServiceAccountAuth.class.getResource("/0b958b9c19d9.p12");

        Credential credential  = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setServiceAccountId("sl-1234567890@famous-plexus-167910.iam.gserviceaccount.com")
                .setServiceAccountScopes(SCOPES)
                .setServiceAccountPrivateKeyFromP12File(Paths.get(resource.toURI()).toFile())
                .build();

        return credential;
    }

    public static Drive getDriveService() throws IOException, GeneralSecurityException, URISyntaxException {
        Credential credential = authorize();
        return new Drive.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static File getFolder(Drive service, final String folderName) throws IOException {

        FileList result = service.files().list()
                .setQ(format("mimeType = 'application/vnd.google-apps.folder' and name = '%s'", folderName))
                .execute();

        List<File> files = result.getFiles();

        return (files == null || files.size() == 0) ? null : files.get(0);
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException, URISyntaxException {

        Drive service = getDriveService();

        /*FileList result = service.files().list()
                .setPageSize(1000)
                //.setFields("nextPageToken, files(id, name, parents)")
                .execute();

        List<File> files = result.getFiles();

        if (files == null || files.size() == 0) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (File file : files) {
                System.out.printf("%s, %s, (%s)\n", file.getParents(), file.getName(), file.getId());
            }
        }*/

        File emailFolder = getFolder(service, "emails");

        FileList result2 = service.files().list()
                .setQ(format("'%s' in parents and trashed = false", emailFolder.getId()))
                .setPageSize(100)
                .execute();

        List<File> files2 = result2.getFiles();

        if (files2 == null || files2.size() == 0) {
            System.out.println("No files found.");
        } else {
            //System.out.println("Files:");
            for (File file2 : files2) {
                System.out.printf("%s, %s, (%s)\n", file2.getParents(), file2.getName(), file2.getId());

                OutputStream outputStream = new FileOutputStream(
                        new java.io.File(format("/Users/slamplugh/Desktop/%s", file2.getName())));
                service.files().get(file2.getId())
                        .executeMediaAndDownloadTo(outputStream);
            }
        }
    }
}
