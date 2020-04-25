import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.sql.Date;

public class Diplomas {

    static QueryForDiplomas queryForDiplomas = new QueryForDiplomas();

    // key - Joomla category_id, value - Wordpress term_id
    Map<Integer, Integer> categoryIdsMapping = new HashMap();

    Map<Integer, Integer> diplomaIdsMapping = new HashMap();

    // key - Joomla category_id, value - Wordpress term_taxonomy_id
    Map<Integer, Integer> termTaxonomyIdsMapping = new HashMap();

    Connection sourceDbConnection;
    Connection destinationDbConnection;

    public Diplomas() {
        try {
            sourceDbConnection = new Joomla().getConnection();
            destinationDbConnection = new WordPress().getConnection();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void Migrate() throws SQLException {
        SelectAllCategoriesFromJoomlaAndInsertThemInWP();
        SelectAllRelationshipFromJoomlaAndInsertThemInWP();
        SelectAllDiplomasFromJoomlaAndInsertThemInWP();
        SelectAllRelationshipDiplomasFromJoomlaAndInsertThemInWP();

        Cleanup();
    }
    void SelectAllCategoriesFromJoomlaAndInsertThemInWP() throws SQLException {

        var selectCategoriesQueryText = queryForDiplomas.getQuerySelectAllCategories();

        var selectCategoriesStatement = sourceDbConnection.prepareStatement(selectCategoriesQueryText);
        var selectCategoriesResultSet = selectCategoriesStatement.executeQuery();

        //insert all joomla zoo categories in wordpress categories
        var insertCategoryQueryText = queryForDiplomas.getQueryInsertCategories();
        var insertCategoryStatement = destinationDbConnection.prepareStatement(insertCategoryQueryText, Statement.RETURN_GENERATED_KEYS);

        while (selectCategoriesResultSet.next()) {
            var categoryId = selectCategoriesResultSet.getInt("category_id");
            var categoryName = selectCategoriesResultSet.getString("category_name");
            var categorySlug = selectCategoriesResultSet.getString("category_slug");

            var categorySlugTrimmed = categorySlug.substring(0, Math.min(categorySlug.length(), 40));
            var categorySlugUrlEncoded = URLEncoder.encode(categorySlugTrimmed, StandardCharsets.UTF_8);

            insertCategoryStatement.setNString(1, categoryName);
            insertCategoryStatement.setNString(2, categorySlugUrlEncoded);

            insertCategoryStatement.executeUpdate();

            var generatedKeys = insertCategoryStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                var wordpressTermId = generatedKeys.getInt(1);
                categoryIdsMapping.put(categoryId, wordpressTermId);
                //System.out.println(String.format("Joomla category_id: %s, Wordpress term_id: %s", categoryId, wordpressTermId));
            }
        }
    }

    void SelectAllRelationshipFromJoomlaAndInsertThemInWP() throws SQLException {
        //select all relashionships from zoo joomla and insert them in wp_term_taxonomy
        var selectParentCategoriesQueryText = queryForDiplomas.getQuerySelectCategoryRelationship();

        var selectParentCategoriesStatement = sourceDbConnection.prepareStatement(selectParentCategoriesQueryText);
        var selectParentCategoriesResultSet = selectParentCategoriesStatement.executeQuery();

        var insertParentCategoryQueryText = queryForDiplomas.getQueryInsertCategoryRelationship();
        var insertParentCategoryStatement = destinationDbConnection.prepareStatement(insertParentCategoryQueryText, Statement.RETURN_GENERATED_KEYS);

        while (selectParentCategoriesResultSet.next()) {
            var joomlaCategoryId = selectParentCategoriesResultSet.getInt("id");
            var wordpressTermId = categoryIdsMapping.get(joomlaCategoryId);

            var joomlaParentCategoryId = selectParentCategoriesResultSet.getInt("parent");
            var wordpressParentTermId = joomlaParentCategoryId == 0 ? 0 : categoryIdsMapping.get(joomlaParentCategoryId);

            insertParentCategoryStatement.setInt(1, wordpressTermId);
            insertParentCategoryStatement.setInt(2, wordpressParentTermId);

            insertParentCategoryStatement.executeUpdate();

            var generatedKeys = insertParentCategoryStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                var termTaxonomyId = generatedKeys.getInt(1);
                termTaxonomyIdsMapping.put(joomlaCategoryId, termTaxonomyId);
            }
        }
    }

    void SelectAllDiplomasFromJoomlaAndInsertThemInWP() throws SQLException {

        String selectDiplomasQueryText = queryForDiplomas.getQuerySelectAllDiplomas();
        var selectDiplomasStatement = sourceDbConnection.prepareStatement(selectDiplomasQueryText);
        var selectDiplomasResultSet = selectDiplomasStatement.executeQuery();

        var insertDiplomasQueryText = queryForDiplomas.getQueryInsertDiplomas();
        var insertDiplomasStatement = destinationDbConnection.prepareStatement(insertDiplomasQueryText, Statement.RETURN_GENERATED_KEYS);

        while (selectDiplomasResultSet.next()) {
            var diplomaId = selectDiplomasResultSet.getInt("diploma_id");
            var diplomaTopic = selectDiplomasResultSet.getString("diploma_topic");
            var diplomaSupervisor = selectDiplomasResultSet.getString("diploma_supervisor");
            var studentName = selectDiplomasResultSet.getString("student_name");

            Date date = Date.valueOf(LocalDate.now());
            insertDiplomasStatement.setDate(1, date);
            insertDiplomasStatement.setDate(2, date);
            insertDiplomasStatement.setNString(3, String.format("<!-- wp:paragraph --><p><b>%s</b></p> <p>Тема дипломної роботи: <i>%s</i></p> <p>Науковий керівник: %s</p><!-- /wp:paragraph -->", studentName, diplomaTopic, diplomaSupervisor));
            insertDiplomasStatement.setNString(4, String.format("%s - %s", studentName, diplomaTopic));
            insertDiplomasStatement.setDate(5, date);
            insertDiplomasStatement.setDate(6, date);

            insertDiplomasStatement.executeUpdate();

            var generatedKeys = insertDiplomasStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                var wordpressDiplomaId = generatedKeys.getInt(1);
                diplomaIdsMapping.put(diplomaId, wordpressDiplomaId);
            }
        }
    }

    void SelectAllRelationshipDiplomasFromJoomlaAndInsertThemInWP() throws SQLException {

        String selectDiplomasRelationshipQueryText = queryForDiplomas.getQuerySelectRelationshipDiplomas();
        var selectDiplomasRelationshipStatement = sourceDbConnection.prepareStatement(selectDiplomasRelationshipQueryText);
        var selectDiplomasRelationshipResultSet = selectDiplomasRelationshipStatement.executeQuery();

        var insertDiplomasRelationshipQueryText = queryForDiplomas.getQueryInsertRelationshipDiplomas();
        var insertDiplomasRelationshipStatement = destinationDbConnection.prepareStatement(insertDiplomasRelationshipQueryText, Statement.RETURN_GENERATED_KEYS);

        while (selectDiplomasRelationshipResultSet.next()) {
            var joomlaItemId = selectDiplomasRelationshipResultSet.getInt("item_id");
            var wordpressPostId = diplomaIdsMapping.get(joomlaItemId);

            var joomlaCategoryId = selectDiplomasRelationshipResultSet.getInt("category_id");
            var wordpressTermTaxonomyId = termTaxonomyIdsMapping.get(joomlaCategoryId);

            insertDiplomasRelationshipStatement.setInt(1, wordpressPostId);
            insertDiplomasRelationshipStatement.setInt(2, wordpressTermTaxonomyId);

            insertDiplomasRelationshipStatement.executeUpdate();
        }
    }

    private void Cleanup() throws SQLException {
        var clearCacheQueryText = queryForDiplomas.getQueryClearCache();
        var clearCacheStatement = destinationDbConnection.prepareStatement(clearCacheQueryText);
        clearCacheStatement.executeUpdate();

        var recalculateCategoryPostsCountQueryText = queryForDiplomas.getQueryRecalculateCategoryPostsCount();
        var recalculateCategoryPostsCountStatement = destinationDbConnection.prepareStatement(recalculateCategoryPostsCountQueryText);
        recalculateCategoryPostsCountStatement.executeUpdate();
    }
}
