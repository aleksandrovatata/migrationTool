import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.*;
import java.time.LocalDate;
import java.util.*;
import java.sql.Date;

public class Diplomas {

    static QueryForDiplomas queryForDiplomas = new QueryForDiplomas();

    // key - Joomla category_id, value - Wordpress term_id
    Map<Integer, Integer> categoryIdsMapping = new HashMap();

    Map<Integer, Integer> diplomaIdsMapping = new HashMap();

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
            insertCategoryStatement.setNString(1, categoryName);
            insertCategoryStatement.setNString(2, categorySlug);

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
        var insertParentCategoryStatement = destinationDbConnection.prepareStatement(insertParentCategoryQueryText);

        while (selectParentCategoriesResultSet.next()) {
            var id = selectParentCategoriesResultSet.getInt("id");
            var wordpressId = categoryIdsMapping.get(id);

            var parentId = selectParentCategoriesResultSet.getInt("parent");
            var wordpressParentId = parentId == 0 ? 0 : categoryIdsMapping.get(parentId);

            insertParentCategoryStatement.setInt(1, wordpressId);
            insertParentCategoryStatement.setInt(2, wordpressParentId);

            insertParentCategoryStatement.executeUpdate();

        }
    }


    void SelectAllDiplomasFromJoomlaAndInsertThemInWP() throws SQLException {

        String selectDiplomasQueryText = queryForDiplomas.getQuerySelectAllDiplomas();
        var selectDiplomasStatement = sourceDbConnection.prepareStatement(selectDiplomasQueryText);
        var selectDiplomasResultSet = selectDiplomasStatement.executeQuery();

        var insertDiplomasQueryText = queryForDiplomas.getQueryInsertDiplomas();
        var insertDiplomasStatement = destinationDbConnection.prepareStatement(insertDiplomasQueryText, Statement.RETURN_GENERATED_KEYS);

        while(selectDiplomasResultSet.next())
        {
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
                var wordpressDiplomId = generatedKeys.getInt(1);
                diplomaIdsMapping.put(diplomaId, wordpressDiplomId);
            }
        }
    }

    void SelectAllRelationshipDiplomasFromJoomlaAndInsertThemInWP() {

    }

}
