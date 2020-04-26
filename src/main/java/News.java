import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class News extends MigrationBase {

    // key - Joomla category_id, value - Wordpress term_taxonomy_id
    Map<Integer, Integer> termTaxonomyIdsMapping = new HashMap<>();

    private int joomlaCategoryId;

    public void Migrate() throws Exception {
        SelectCategoryNewsFromJoomlaAndInsertItInWordpress();
        SelectContentNewsFromJoomlaAndInsertItInWordpress();
        Cleanup();
    }

    private void SelectCategoryNewsFromJoomlaAndInsertItInWordpress() throws SQLException {
        var selectCategoryNewsQueryText = QueryForNews.getQuerySelectCategoryNews();

        var selectCategoryNewsStatement = sourceDbConnection.prepareStatement(selectCategoryNewsQueryText);
        var selectCategoryNewsResultSet = selectCategoryNewsStatement.executeQuery();

        //insert all joomla zoo categories in wordpress categories
        var insertCategoryNewsQueryText = QueryForNews.getQueryInsertTagNews();
        var insertCategoryNewsStatement = destinationDbConnection.prepareStatement(insertCategoryNewsQueryText, Statement.RETURN_GENERATED_KEYS);

        while (selectCategoryNewsResultSet.next()) {
            var categoryId = selectCategoryNewsResultSet.getInt("id");
            var categoryName = selectCategoryNewsResultSet.getString("title");
            var categorySlug = selectCategoryNewsResultSet.getString("alias");

            joomlaCategoryId = categoryId;

            insertCategoryNewsStatement.setNString(1, categoryName);
            insertCategoryNewsStatement.setNString(2, categorySlug);

            insertCategoryNewsStatement.executeUpdate();

            var insertTermTaxonomyNewsQueryText = QueryForNews.getQueryInsertCategoryRelationship();
            var insertTermTaxonomyNewsStatement = destinationDbConnection.prepareStatement(insertTermTaxonomyNewsQueryText, Statement.RETURN_GENERATED_KEYS);

            var generatedKeys = insertCategoryNewsStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                var wordpressTermId = generatedKeys.getInt(1);
                insertTermTaxonomyNewsStatement.setInt(1, wordpressTermId);
                insertTermTaxonomyNewsStatement.executeUpdate();
            }

            var generatedKeysTaxonomy = insertTermTaxonomyNewsStatement.getGeneratedKeys();
            if (generatedKeysTaxonomy.next()) {
                var wordpressTermId = generatedKeysTaxonomy.getInt(1);
                termTaxonomyIdsMapping.put(joomlaCategoryId, wordpressTermId);
            }
        }
    }

    private void SelectContentNewsFromJoomlaAndInsertItInWordpress() throws Exception {

        var wordpressTermTaxonomyId = termTaxonomyIdsMapping.get(joomlaCategoryId);

        var selectContentNewsQueryText = QueryForNews.getQuerySelectAllNews();

        var selectContentNewsStatement = sourceDbConnection.prepareStatement(selectContentNewsQueryText);
        selectContentNewsStatement.setInt(1, joomlaCategoryId);
        var selectContentNewsResultSet = selectContentNewsStatement.executeQuery();

        var insertNewsRelationshipQueryText = QueryForNews.getQueryInsertRelationshipNews();
        var insertNewsRelationshipStatement = destinationDbConnection.prepareStatement(insertNewsRelationshipQueryText, Statement.RETURN_GENERATED_KEYS);

        var updatePostContentQueryText = QueryForNews.getQueryUpdatePostContent();
        var updatePostContentStatement = destinationDbConnection.prepareStatement(updatePostContentQueryText);

        while (selectContentNewsResultSet.next()) {

            var newsId = selectContentNewsResultSet.getInt("id");
            var newsTitle = selectContentNewsResultSet.getString("title");
            var newsContent = selectContentNewsResultSet.getString("introtext");
            var slug = selectContentNewsResultSet.getString("alias");

            int wordpressPostId = InsertPost(newsTitle, NormalizeSlug(slug));

            insertNewsRelationshipStatement.setInt(1, wordpressPostId);
            insertNewsRelationshipStatement.setInt(2, wordpressTermTaxonomyId);
            insertNewsRelationshipStatement.executeUpdate();

            StringBuilder newsPostContentBuilder = new StringBuilder();
            newsPostContentBuilder.append("<!-- wp:paragraph -->");
            newsPostContentBuilder.append(UpdateImageLinks(newsContent));
            newsPostContentBuilder.append("<!-- /wp:paragraph -->");

            var selectAttachmentsNewsQueryText = QueryForNews.getQuerySelectAllAttachmentsForNews();

            var selectAttachmentsNewsStatement = sourceDbConnection.prepareStatement(selectAttachmentsNewsQueryText);
            selectAttachmentsNewsStatement.setInt(1, newsId);
            var selectAttachmentsResultSet = selectAttachmentsNewsStatement.executeQuery();

            while (selectAttachmentsResultSet.next()) {

                var attachmentId = selectAttachmentsResultSet.getInt("id");
                var attachmentName = selectAttachmentsResultSet.getString("filename");
                var attachmentUrl = selectAttachmentsResultSet.getString("url");
                var attachmentSize = selectAttachmentsResultSet.getInt("file_size");
                var attachmentDate = selectAttachmentsResultSet.getDate("modification_date");

                AddAttachmentToPost(wordpressPostId, newsPostContentBuilder, attachmentUrl, attachmentName);
            }

            updatePostContentStatement.setNString(1, newsPostContentBuilder.toString());
            updatePostContentStatement.setInt(2, wordpressPostId);

            updatePostContentStatement.executeUpdate();
        }
    }

    private void AddAttachmentToPost(int postId, StringBuilder postContentBuilder, String filePath, String title) throws Exception {
        if (StringUtils.isEmpty(filePath)) {
            return;
        }

        Path path = Paths.get(filePath);
        var fileName = path.getFileName().toString();
        var fileUrl = Configuration.GenerateDocumentUrl(fileName);

        int attachmentPostId = InsertAttachmentPost(title, fileUrl, postId);

        postContentBuilder.append(String.format("<!-- wp:file {\"id\":%s,\"href\":\"%s\"} -->", attachmentPostId, fileUrl));
        postContentBuilder.append("<div class=\"wp-block-file\">");
        postContentBuilder.append(String.format("<a href=\"%s\">%s</a>", fileUrl, title));
        postContentBuilder.append(String.format("<a href=\"%s\" class=\"wp-block-file__button\" download>Завантажити</a>", fileUrl));
        postContentBuilder.append("</div>");
        postContentBuilder.append("<!-- /wp:file -->");
    }

    private String UpdateImageLinks(String content) {
        Pattern p = Pattern.compile("src\\s*=\\s*\"(.+?)\"");
        Matcher m = p.matcher(content);
        if (m.find()) {
            return m.replaceAll("src=\"/wp-content/themes/start-theme/$1\"");
        }

        return content;
    }
}
