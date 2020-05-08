import com.ibm.icu.text.Transliterator;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigrationBase {
    Connection sourceDbConnection;
    Connection destinationDbConnection;

    public MigrationBase() {
        try {
            sourceDbConnection = Joomla.getConnection();
            destinationDbConnection = WordPress.getConnection();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected int InsertPost(String title, String slug, Date date) throws Exception {
        return InsertPostImplementation(title, "post", "publish", slug, "", 0, date);
    }

    protected int InsertAttachmentPost(String title, String guid, int parentId, Date date) throws Exception {
        return InsertPostImplementation(title, "attachment", "inherit", "", guid, parentId, date);
    }

    private int InsertPostImplementation(String title, String type, String status, String name, String guid, int parentId, Date date) throws Exception {
        var insertPostQueryText = QueryBase.getQueryInsertPost();
        var insertPostStatement = destinationDbConnection.prepareStatement(insertPostQueryText, Statement.RETURN_GENERATED_KEYS);

        insertPostStatement.setDate(1, date);
        insertPostStatement.setDate(2, date);
        insertPostStatement.setNString(3, "");
        insertPostStatement.setNString(4, title);
        insertPostStatement.setNString(5, status);
        insertPostStatement.setNString(6, name);
        insertPostStatement.setDate(7, date);
        insertPostStatement.setDate(8, date);
        insertPostStatement.setInt(9, parentId);
        insertPostStatement.setString(10, guid);
        insertPostStatement.setString(11, type);

        insertPostStatement.executeUpdate();

        var generatedKeys = insertPostStatement.getGeneratedKeys();
        if (generatedKeys.next()) {
            return generatedKeys.getInt(1);
        }

        throw new Exception("Cannot get generated key");
    }

    protected void AddAttachmentToPost(int postId, StringBuilder postContentBuilder, String filePath, String title, String type, Date date) throws Exception {
        if (StringUtils.isEmpty(filePath)) {
            return;
        }

        Path path = Paths.get(filePath);
        var fileName = type + path.getFileName().toString();
        var fileUrl = Configuration.GenerateDocumentUrl(fileName);

        int attachmentPostId = InsertAttachmentPost(title, fileUrl, postId, date);

        postContentBuilder.append(String.format("<!-- wp:file {\"id\":%s,\"href\":\"%s\"} -->", attachmentPostId, fileUrl));
        postContentBuilder.append("<div class=\"wp-block-file\">");
        postContentBuilder.append(String.format("<a href=\"%s\">%s</a>", fileUrl, title));
        postContentBuilder.append(String.format("<a href=\"%s\" class=\"wp-block-file__button\" download>Завантажити</a>", fileUrl));
        postContentBuilder.append("</div>");
        postContentBuilder.append("<!-- /wp:file -->");
    }

    protected String UpdateImageLinks(String content) {
        Pattern p = Pattern.compile("src\\s*=\\s*\"((?!.*http).+)\"");
        Matcher m = p.matcher(content);
        if (m.find()) {
            return m.replaceAll(String.format("src=\"%s/$1\"", Configuration.FilesBaseUrl));
        }

        return content;
    }

    protected String NormalizeSlug(String slug) {
        var CYRILLIC_TO_LATIN = "Ukrainian-Latin/BGN";
        Transliterator toLatin = Transliterator.getInstance(CYRILLIC_TO_LATIN);
        var result = toLatin.transliterate(slug);
        var categorySlugTrimmed = result.substring(0, Math.min(result.length(), 170));
        var categorySlugEncoded = URLEncoder.encode(categorySlugTrimmed, StandardCharsets.UTF_8);
        return StringUtils.stripEnd(categorySlugEncoded, "-");
    }

    protected void Cleanup() throws SQLException {
        var clearCacheQueryText = QueryBase.getQueryClearCache();
        var clearCacheStatement = destinationDbConnection.prepareStatement(clearCacheQueryText);
        clearCacheStatement.executeUpdate();

        var recalculateCategoryPostsCountQueryText = QueryBase.getQueryRecalculateCategoryPostsCount();
        var recalculateCategoryPostsCountStatement = destinationDbConnection.prepareStatement(recalculateCategoryPostsCountQueryText);
        recalculateCategoryPostsCountStatement.executeUpdate();
    }
}
