import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

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

    protected int InsertPost(String title, String slug) throws Exception {
        return InsertPostImplementation(title, "post", "publish", slug,"",0);
    }

    protected int InsertAttachmentPost(String title, String guid, int parentId) throws Exception {
        return InsertPostImplementation(title, "attachment", "inherit", "", guid, parentId);
    }

    private int InsertPostImplementation(String title, String type, String status, String name, String guid, int parentId) throws Exception {
        var insertPostQueryText = QueryBase.getQueryInsertPost();
        var insertPostStatement = destinationDbConnection.prepareStatement(insertPostQueryText, Statement.RETURN_GENERATED_KEYS);

        Date date = Date.valueOf(LocalDate.now());
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

    protected String NormalizeSlug(String slug) {
        var categorySlugTrimmed = slug.substring(0, Math.min(slug.length(), 33));
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
