import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class Diplomas {

    public void Diplomas() throws SQLException {

        Joomla joomla = new Joomla();
        WordPress wordPress = new WordPress();
        QueryForDiplomas queryForDiplomas = new QueryForDiplomas();

        // key - Joomla category_id, value - Wordpress term_id
        Map<Integer, Integer> categoryIdsMapping = new HashMap();

        var sourceDbConnection = joomla.getConnection();
        var destinationDbConnection = wordPress.getConnection();

        //select all zoo categories from joomla
        var selectCategoriesQueryText = queryForDiplomas.getQuerySelectAllCategories();

        var selectCategoriesStatement = sourceDbConnection.prepareStatement(selectCategoriesQueryText);
        var selectCategoriesResultSet = selectCategoriesStatement.executeQuery();

        //insert all joomla zoo categories in wordpress categories
        var insertCategoryQueryText = queryForDiplomas.getQueryInsertCategories();
        var insertCategoryStatement = destinationDbConnection.prepareStatement(insertCategoryQueryText, Statement.RETURN_GENERATED_KEYS);

        while(selectCategoriesResultSet.next()) {
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

        //select all relashionships from zoo joomla and insert them in wp_term_taxonomy
        var selectParentCategoriesQueryText = queryForDiplomas.getQuerySelectCategoryRelationship();

        var selectParentCategoriesStatement = sourceDbConnection.prepareStatement(selectParentCategoriesQueryText);
        var selectParentCategoriesResultSet = selectParentCategoriesStatement.executeQuery();

        var insertParentCategoryQueryText = queryForDiplomas.getQueryInsertCategoryRelationship();
        var insertParentCategoryStatement = destinationDbConnection.prepareStatement(insertParentCategoryQueryText);

        while(selectParentCategoriesResultSet.next()) {
            var id = selectParentCategoriesResultSet.getInt("id");
            var wordpressId = categoryIdsMapping.get(id);

            var parentId = selectParentCategoriesResultSet.getInt("parent");
            var wordpressParentId = parentId == 0 ? 0: categoryIdsMapping.get(parentId);

            insertParentCategoryStatement.setInt(1, wordpressId);
            insertParentCategoryStatement.setInt(2, wordpressParentId);

            insertParentCategoryStatement.executeUpdate();

        }

        // var joomlaCategoryId = 67;
        // var wordpressTermId = categoryIdsMapping.get(joomlaCategoryId);

        /*String selectDiplomasQueryText = query.getQueryCreateTable();


        var selectDiplomasStatement = sourceDbConnection.prepareStatement(selectDiplomasQueryText);
        var selectDiplomasResultSet = selectDiplomasStatement.executeQuery();

        while(selectDiplomasResultSet.next())
        {
            var diplomaId = selectDiplomasResultSet.getInt("diploma_id");
            var diplomaTopic = selectDiplomasResultSet.getString("diploma_topic");
            var diplomaSupervisor = selectDiplomasResultSet.getString("diploma_supervisor");
            var studentName = selectDiplomasResultSet.getString("student_name");
        }*/

        System.out.println("Migration completed.");
    }
}
