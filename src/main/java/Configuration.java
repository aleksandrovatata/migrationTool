import org.apache.commons.lang3.StringUtils;
import java.time.LocalDate;

public class Configuration {
    final static String FilesBaseUrl = "http://wpfolder:99";

    final static int AuthorUserId = 1;

    static String GenerateDocumentUrl(String fileName) {
        var now = LocalDate.now();
        String monthSegment = StringUtils.leftPad(String.valueOf(now.getMonthValue()), 2, '0');

        return String.format("%s/wp-content/uploads/%s/%s/%s", FilesBaseUrl, now.getYear(), monthSegment, fileName);
    }
}
