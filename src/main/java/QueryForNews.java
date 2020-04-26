public class QueryForNews extends QueryBase {

    private final static String QuerySelectCategoryNews = "SELECT id, title, alias FROM jos_categories WHERE alias LIKE '%news%'";

    private final static String QueryInsertTagNews = "INSERT INTO wp_terms(name, slug, term_group) VALUES (?, ? ,0)";

    private final static String queryInsertCategoryRelationship = "INSERT INTO wp_term_taxonomy(term_id, taxonomy, description, parent, count) VALUES (?, 'post_tag', '', 0, 0)";

    private final static String QuerySelectAllNews = "SELECT id, title, introtext, alias FROM jos_content WHERE catid = ?";

    private final static String QuerySelectAllAttachmentsForNews = "SELECT id, filename, url, file_size, modification_date FROM jos_attachments WHERE article_id = ?";

    private final static String queryInsertRelationshipNews = "INSERT INTO wp_term_relationships(object_id, term_taxonomy_id) VALUES(?,?)";

    public static String getQuerySelectCategoryNews() {
        return QuerySelectCategoryNews;
    }

    public static String getQueryInsertTagNews() {
        return QueryInsertTagNews;
    }

    public static String getQueryInsertCategoryRelationship() {
        return queryInsertCategoryRelationship;
    }

    public static String getQuerySelectAllNews() {
        return QuerySelectAllNews;
    }

    public static String getQuerySelectAllAttachmentsForNews() {
        return QuerySelectAllAttachmentsForNews;
    }

    public static String getQueryInsertRelationshipNews() {
        return queryInsertRelationshipNews;
    }
}
