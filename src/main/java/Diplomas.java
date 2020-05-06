import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class Diplomas extends MigrationBase {

    // key - Joomla category_id, value - Wordpress term_id
    Map<Integer, Integer> categoryIdsMapping = new HashMap<>();

    Map<Integer, Integer> diplomaIdsMapping = new HashMap<>();

    // key - Joomla category_id, value - Wordpress term_taxonomy_id
    Map<Integer, Integer> termTaxonomyIdsMapping = new HashMap<>();

    public void Migrate() throws Exception {
        SelectAllCategoriesFromJoomlaAndInsertThemInWP();
        SelectAllRelationshipFromJoomlaAndInsertThemInWP();
        SelectAllDiplomasFromJoomlaAndInsertThemInWP();
        SelectAllRelationshipDiplomasFromJoomlaAndInsertThemInWP();

        Cleanup();
    }

    void SelectAllCategoriesFromJoomlaAndInsertThemInWP() throws SQLException {

        var selectCategoriesQueryText = QueryForDiplomas.getQuerySelectAllCategories();

        var selectCategoriesStatement = sourceDbConnection.prepareStatement(selectCategoriesQueryText);
        var selectCategoriesResultSet = selectCategoriesStatement.executeQuery();

        //insert all joomla zoo categories in wordpress categories
        var insertCategoryQueryText = QueryForDiplomas.getQueryInsertCategories();
        var insertCategoryStatement = destinationDbConnection.prepareStatement(insertCategoryQueryText, Statement.RETURN_GENERATED_KEYS);

        while (selectCategoriesResultSet.next()) {
            var categoryId = selectCategoriesResultSet.getInt("category_id");
            var categoryName = selectCategoriesResultSet.getString("category_name");
            var categorySlug = selectCategoriesResultSet.getString("category_slug");

            insertCategoryStatement.setNString(1, categoryName);
            insertCategoryStatement.setNString(2, NormalizeSlug(categorySlug));

            insertCategoryStatement.executeUpdate();

            var generatedKeys = insertCategoryStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                var wordpressTermId = generatedKeys.getInt(1);
                categoryIdsMapping.put(categoryId, wordpressTermId);
            }
        }
    }

    void SelectAllRelationshipFromJoomlaAndInsertThemInWP() throws SQLException {
        //select all relashionships from zoo joomla and insert them in wp_term_taxonomy
        var selectParentCategoriesQueryText = QueryForDiplomas.getQuerySelectCategoryRelationship();

        var selectParentCategoriesStatement = sourceDbConnection.prepareStatement(selectParentCategoriesQueryText);
        var selectParentCategoriesResultSet = selectParentCategoriesStatement.executeQuery();

        var insertParentCategoryQueryText = QueryForDiplomas.getQueryInsertCategoryRelationship();
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

    void SelectAllDiplomasFromJoomlaAndInsertThemInWP() throws Exception {

        String selectDiplomasQueryText = QueryForDiplomas.getQuerySelectAllDiplomas();
        var selectDiplomasStatement = sourceDbConnection.prepareStatement(selectDiplomasQueryText);
        var selectDiplomasResultSet = selectDiplomasStatement.executeQuery();

        var updatePostContentQueryText = QueryForDiplomas.getQueryUpdatePostContent();
        var updatePostContentStatement = destinationDbConnection.prepareStatement(updatePostContentQueryText);

        while (selectDiplomasResultSet.next()) {
            var diplomaId = selectDiplomasResultSet.getInt("diploma_id");
            var diplomaElements = selectDiplomasResultSet.getString("diploma_elements");
            var diplomaTopic = selectDiplomasResultSet.getString("diploma_topic");
            var diplomaSupervisor = selectDiplomasResultSet.getString("diploma_supervisor");
            var studentName = selectDiplomasResultSet.getString("student_name");
            var slug = selectDiplomasResultSet.getString("alias");

            String diplomaPostTitle = String.format("%s - %s", studentName, diplomaTopic);

            int wordpressDiplomaId = InsertPost(diplomaPostTitle, NormalizeSlug(slug));
            diplomaIdsMapping.put(diplomaId, wordpressDiplomaId);

            StringBuilder diplomaPostContentBuilder = new StringBuilder();
            diplomaPostContentBuilder.append("<!-- wp:paragraph -->");
            diplomaPostContentBuilder.append(String.format("<p><b>%s</b></p>", studentName));
            diplomaPostContentBuilder.append(String.format("<p>Тема дипломної роботи: <i>%s</i></p>", diplomaTopic));
            diplomaPostContentBuilder.append(String.format("<p>Науковий керівник: %s</p>", diplomaSupervisor));
            diplomaPostContentBuilder.append("<!-- /wp:paragraph -->");

            var presentationFilePath = ParseDocumentName(diplomaElements, "312d6aeb-7e50-4d4a-99fd-9d0380cddf87");
            AddAttachmentToPost(wordpressDiplomaId, diplomaPostContentBuilder, presentationFilePath, "presentation");

            var diplomaUkFilePath = ParseDocumentName(diplomaElements, "243f9373-527c-4bbb-9ba2-6eccd140ab4e");
            AddAttachmentToPost(wordpressDiplomaId, diplomaPostContentBuilder, diplomaUkFilePath, "diploma-uk");

            var diplomaRuFilePath = ParseDocumentName(diplomaElements, "27b10641-7d14-40a8-ac12-53b38e8af657");
            AddAttachmentToPost(wordpressDiplomaId, diplomaPostContentBuilder, diplomaRuFilePath, "diploma-ru");

            var diplomaEnFilePath = ParseDocumentName(diplomaElements, "62a079d4-5a28-4197-a54d-4f024429c2f0");
            AddAttachmentToPost(wordpressDiplomaId, diplomaPostContentBuilder, diplomaEnFilePath, "diploma-en");

            updatePostContentStatement.setNString(1, diplomaPostContentBuilder.toString());
            updatePostContentStatement.setInt(2, wordpressDiplomaId);

            updatePostContentStatement.executeUpdate();
        }
    }

    private void SelectAllRelationshipDiplomasFromJoomlaAndInsertThemInWP() throws SQLException {

        String selectDiplomasRelationshipQueryText = QueryForDiplomas.getQuerySelectRelationshipDiplomas();
        var selectDiplomasRelationshipStatement = sourceDbConnection.prepareStatement(selectDiplomasRelationshipQueryText);
        var selectDiplomasRelationshipResultSet = selectDiplomasRelationshipStatement.executeQuery();

        var insertDiplomasRelationshipQueryText = QueryForDiplomas.getQueryInsertRelationshipDiplomas();
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

    private String ParseDocumentName(String xml, String documentIdentifier) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        Document doc = dBuilder.parse(is);

        var xPathExpression = String.format("/elements/download[@identifier='%s']/file", documentIdentifier);
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.compile(xPathExpression).evaluate(doc, XPathConstants.NODESET);
        if (nodeList.getLength() == 0) {
            return null;
        }

        Node node = nodeList.item(0);
        return node.getTextContent();
    }
}
