public class QueryForDiplomas {

    private final static String querySelectAllCategories = "SELECT" +
            " id AS category_id," +
            " name AS category_name," +
            " alias AS category_slug" +
            " FROM jos_zoo_category";

    private final static String querySelectAllDiplomas = "SELECT" +
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

    private final static String queryInsertDiplomas =
            "INSERT INTO wp_posts(post_author, post_date, post_date_gmt, post_content, post_title, post_excerpt, post_status, comment_status, ping_status, post_password, post_name, to_ping, pinged, post_modified, post_modified_gmt, post_content_filtered, post_parent, guid, menu_order, post_type, post_mime_type) " +
            "VALUES (1, ?, ?, ?, ?, '', 'publish', 'closed', 'closed', '','','','',?,?,'',0,'',0,'post', '')";

    private final static String querySelectRelationshipDiplomas = "SELECT category_id, item_id FROM jos_zoo_category_item";

    private final static String queryInsertRelationshipDiplomas = "INSERT INTO wp_term_relationships(object_id, term_taxonomy_id) VALUES(?,?)";

    private final static String queryClearCache = "DELETE FROM wp_options WHERE option_name = 'category_children'";

    private final static String queryRecalculateCategoryPostsCount =
            "UPDATE wp_term_taxonomy SET count = (" +
            "SELECT COUNT(*) FROM wp_term_relationships rel " +
            "    LEFT JOIN wp_posts po ON (po.ID = rel.object_id) " +
            "    WHERE " +
            "        rel.term_taxonomy_id = wp_term_taxonomy.term_taxonomy_id " +
            "        AND " +
            "        wp_term_taxonomy.taxonomy NOT IN ('link_category')" +
            "        AND " +
            "        po.post_status IN ('publish', 'future')" +
            ")";

    public static String getQuerySelectAllDiplomas() {
        return querySelectAllDiplomas;
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

    public static String getQueryInsertDiplomas() {
        return queryInsertDiplomas;
    }

    public static String getQuerySelectRelationshipDiplomas() {
        return querySelectRelationshipDiplomas;
    }

    public static String getQueryInsertRelationshipDiplomas() {
        return queryInsertRelationshipDiplomas;
    }

    public static String getQueryClearCache() {
        return queryClearCache;
    }

    public static String getQueryRecalculateCategoryPostsCount() {
        return queryRecalculateCategoryPostsCount;
    }
}