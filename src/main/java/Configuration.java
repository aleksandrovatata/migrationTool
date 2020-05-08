public class Configuration {
    public final static String FilesBaseUrl = "http://wpfolder:99";

    public final static int AuthorUserId = 1;

    public static String GenerateDocumentUrl(String fileName) {
        return String.format("%s/attachments/%s", FilesBaseUrl, fileName);
    }
}
