public class QueryForDiplomas {

    private final static String querySelectAllCategories = "SELECT" +
            " id AS category_id," +
            " name AS category_name," +
            " alias AS category_slug" +
            " FROM jos_zoo_category";

    private final static String queryCreateTable = "SELECT" +
            " i.id AS diploma_id," +
            " trim(si_t.value) AS diploma_topic," +
            " trim(si_s.value) AS diploma_supervisor," +
            " trim(i.name) AS student_name" +
            " FROM jos_zoo_item i" +
            " JOIN jos_zoo_search_index si_t ON si_t.item_id = i.id AND si_t.element_id = '8174565e-3438-44f1-b79e-7f3be74079a1'" +
            " JOIN jos_zoo_search_index si_s ON si_s.item_id = i.id AND si_s.element_id = 'f653d1ec-5614-4053-89b8-332e807c945e'" +
            " ORDER BY student_name";

    private final static String querySelectCategoryRelationship = "SELECT id, parent FROM jos_zoo_category";

    private final static String queryInsertCategoryRelationship = "INSERT INTO wp_term_taxonomy(term_id, taxonomy, description, parent, count) VALUES (?, 'category', '', ?, 0)";

    private final static String queryInsertCategories = "INSERT INTO wp_terms(name, slug, term_group) VALUES (?, ? ,0)";

    public static String getQueryCreateTable() {
        return queryCreateTable;
    }

    public static String getQuerySelectAllCategories() {
        return querySelectAllCategories;
    }

    public static String getQueryInsertCategories() {
        return queryInsertCategories;
    }

    public static String getQuerySelectCategoryRelationship() {
        return querySelectCategoryRelationship;
    }

    public static String getQueryInsertCategoryRelationship() {
        return queryInsertCategoryRelationship;
    }
}