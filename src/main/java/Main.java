public class Main {

    public static void main(String[] args) throws Exception {
        Diplomas diplomas = new Diplomas();
        diplomas.Migrate();

        News news = new News();
        news.Migrate();

        System.out.println("Migration completed.");
    }
}
