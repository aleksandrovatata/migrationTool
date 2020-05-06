public class Main {

    public static void main(String[] args) throws Exception {
        Diplomas diplomas = new Diplomas();
        diplomas.Migrate();

        News news = new News();
        news.Migrate();

        Conferences conferences = new Conferences();
        conferences.Migrate();

        System.out.println("Migration completed.");
    }
}
