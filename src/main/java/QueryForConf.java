public class QueryForConf extends QueryBase{

    private final static String QuerySelectCategoryConferences = "SELECT id, title, alias FROM jos_categories WHERE alias LIKE '%konf%'";

    private final static String queryInsertCategoryRelationship = "INSERT INTO wp_term_taxonomy(term_id, taxonomy, description, parent, count) VALUES (?, 'category', '', 0, 0)";

    private final static String QuerySelectAllConferences = "SELECT id, title, introtext, alias, publish_up FROM jos_content WHERE catid = ?";

    private final static String QuerySelectAllAttachmentsForConferences = "SELECT id, filename, url, file_size, modification_date FROM jos_attachments WHERE article_id = ?";

    private final static String queryInsertRelationshipConferences = "INSERT INTO wp_term_relationships(object_id, term_taxonomy_id) VALUES(?,?)";

    public static String getQuerySelectCategoryConferences() {
        return QuerySelectCategoryConferences;
    }

    public static String getQueryInsertCategoryRelationship() {
        return queryInsertCategoryRelationship;
    }

    public static String getQuerySelectAllConferences() {
        return QuerySelectAllConferences;
    }

    public static String getQuerySelectAllAttachmentsForConferences() {
        return QuerySelectAllAttachmentsForConferences;
    }

    public static String getQueryInsertRelationshipConferences() {
        return queryInsertRelationshipConferences;
    }
}
