import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class Conferences extends MigrationBase{

    // key - Joomla category_id, value - Wordpress term_taxonomy_id
    Map<Integer, Integer> termTaxonomyIdsMapping = new HashMap<>();

    private int joomlaCategoryId;

    public void Migrate() throws Exception {
        SelectCategoryNewsFromJoomlaAndInsertItInWordpress();
        SelectContentNewsFromJoomlaAndInsertItInWordpress();
        Cleanup();
    }

    private void SelectCategoryNewsFromJoomlaAndInsertItInWordpress() throws SQLException {
        var selectCategoryNewsQueryText = QueryForConf.getQuerySelectCategoryConferences();

        var selectCategoryNewsStatement = sourceDbConnection.prepareStatement(selectCategoryNewsQueryText);
        var selectCategoryNewsResultSet = selectCategoryNewsStatement.executeQuery();

        //insert all joomla zoo categories in wordpress categories
        var insertCategoryNewsQueryText = QueryBase.getQueryInsertCategories();
        var insertCategoryNewsStatement = destinationDbConnection.prepareStatement(insertCategoryNewsQueryText, Statement.RETURN_GENERATED_KEYS);

        while (selectCategoryNewsResultSet.next()) {
            var categoryId = selectCategoryNewsResultSet.getInt("id");
            var categoryName = selectCategoryNewsResultSet.getString("title");
            var categorySlug = selectCategoryNewsResultSet.getString("alias");

            joomlaCategoryId = categoryId;

            insertCategoryNewsStatement.setNString(1, categoryName);
            insertCategoryNewsStatement.setNString(2, categorySlug);

            insertCategoryNewsStatement.executeUpdate();

            var insertTermTaxonomyNewsQueryText = QueryForConf.getQueryInsertCategoryRelationship();
            var insertTermTaxonomyNewsStatement = destinationDbConnection.prepareStatement(insertTermTaxonomyNewsQueryText, Statement.RETURN_GENERATED_KEYS);

            var generatedKeys = insertCategoryNewsStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                var wordpressTermId = generatedKeys.getInt(1);
                insertTermTaxonomyNewsStatement.setInt(1, wordpressTermId);
                insertTermTaxonomyNewsStatement.executeUpdate();
            }

            var generatedKeysTaxonomy = insertTermTaxonomyNewsStatement.getGeneratedKeys();
            if (generatedKeysTaxonomy.next()) {
                var wordpressTermTaxonomyId = generatedKeysTaxonomy.getInt(1);
                termTaxonomyIdsMapping.put(joomlaCategoryId, wordpressTermTaxonomyId);
            }
        }
    }

    private void SelectContentNewsFromJoomlaAndInsertItInWordpress() throws Exception {

        var wordpressTermTaxonomyId = termTaxonomyIdsMapping.get(joomlaCategoryId);

        var selectContentNewsQueryText = QueryForConf.getQuerySelectAllConferences();

        var selectContentNewsStatement = sourceDbConnection.prepareStatement(selectContentNewsQueryText);
        selectContentNewsStatement.setInt(1, joomlaCategoryId);
        var selectContentNewsResultSet = selectContentNewsStatement.executeQuery();

        var insertNewsRelationshipQueryText = QueryForConf.getQueryInsertRelationshipConferences();
        var insertNewsRelationshipStatement = destinationDbConnection.prepareStatement(insertNewsRelationshipQueryText, Statement.RETURN_GENERATED_KEYS);

        var updatePostContentQueryText = QueryBase.getQueryUpdatePostContent();
        var updatePostContentStatement = destinationDbConnection.prepareStatement(updatePostContentQueryText);

        while (selectContentNewsResultSet.next()) {

            var newsId = selectContentNewsResultSet.getInt("id");
            var newsTitle = selectContentNewsResultSet.getString("title");
            var newsContent = selectContentNewsResultSet.getString("introtext");
            var slug = selectContentNewsResultSet.getString("alias");
            var dateTime = selectContentNewsResultSet.getDate("publish_up");

            int wordpressPostId = InsertPost(newsTitle, NormalizeSlug(slug), dateTime);

            insertNewsRelationshipStatement.setInt(1, wordpressPostId);
            insertNewsRelationshipStatement.setInt(2, wordpressTermTaxonomyId);
            insertNewsRelationshipStatement.executeUpdate();

            StringBuilder newsPostContentBuilder = new StringBuilder();
            newsPostContentBuilder.append("<!-- wp:paragraph -->");
            newsPostContentBuilder.append(UpdateImageLinks(newsContent));
            newsPostContentBuilder.append("<!-- /wp:paragraph -->");

            var selectAttachmentsNewsQueryText = QueryForConf.getQuerySelectAllAttachmentsForConferences();

            var selectAttachmentsNewsStatement = sourceDbConnection.prepareStatement(selectAttachmentsNewsQueryText);
            selectAttachmentsNewsStatement.setInt(1, newsId);
            var selectAttachmentsResultSet = selectAttachmentsNewsStatement.executeQuery();

            while (selectAttachmentsResultSet.next()) {

                var attachmentId = selectAttachmentsResultSet.getInt("id");
                var attachmentName = selectAttachmentsResultSet.getString("filename");
                var attachmentUrl = selectAttachmentsResultSet.getString("url");
                var attachmentSize = selectAttachmentsResultSet.getInt("file_size");
                var attachmentDate = selectAttachmentsResultSet.getDate("modification_date");

                AddAttachmentToPost(wordpressPostId, newsPostContentBuilder, attachmentUrl, attachmentName, "", dateTime);
            }

            updatePostContentStatement.setNString(1, newsPostContentBuilder.toString());
            updatePostContentStatement.setInt(2, wordpressPostId);

            updatePostContentStatement.executeUpdate();
        }
    }

}
