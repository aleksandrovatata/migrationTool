public class QueryBase {

    private final static String queryInsertPost =
            "INSERT INTO wp_posts(post_author, post_date, post_date_gmt, post_content, post_title, post_excerpt, post_status, comment_status, ping_status, post_password, post_name, to_ping, pinged, post_modified, post_modified_gmt, post_content_filtered, post_parent, guid, menu_order, post_type, post_mime_type) " +
                    String.format("VALUES (%s, ?, ?, ?, ?, '', ?, 'closed', 'closed', '', ?, '', '', ?, ?,'',?, ?, 0, ?, '')", Configuration.AuthorUserId);

    private final static String queryUpdatePostContent = "UPDATE wp_posts SET post_content = ? WHERE id = ?";

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

    public static String getQueryInsertPost() {
        return queryInsertPost;
    }

    public static String getQueryUpdatePostContent() {
        return queryUpdatePostContent;
    }

    public static String getQueryClearCache() {
        return queryClearCache;
    }

    public static String getQueryRecalculateCategoryPostsCount() {
        return queryRecalculateCategoryPostsCount;
    }
}
