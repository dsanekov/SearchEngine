package searchengine.model;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.stereotype.Service;
import searchengine.config.Site;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DBConnection {
    private static Connection connection;
    private static final String dbName = "search_engine";
    private static final String dbUser = "root";
    private static final String dbPass = "galaxyfit";
    private static StringBuffer deleteQuery = new StringBuffer();
    private static StringBuffer selectQuery = new StringBuffer();
    private static StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure("hibernate.cfg.xml").build();
    private static Metadata metadata = new MetadataSources(registry).getMetadataBuilder().build();
    private static SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();
    private static Session session = sessionFactory.openSession();

    private static synchronized Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/" + dbName +
                                "?user=" + dbUser + "&password=" + dbPass);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
    public static synchronized boolean thisPageExists(String path) throws SQLException{
        String query = "SELECT `id` FROM `page` WHERE `path` = '" + path + "'";
        ResultSet rs = getConnection().createStatement().executeQuery(query);
        System.out.println("Запрос на наличте страницы с path '" + path + "' в базе - " + rs.next());
        return rs.next();
    }

    public static synchronized void executeInsertSiteIndexing(searchengine.model.Site site){
        Transaction transaction = session.beginTransaction();
        session.save(site);
        transaction.commit();
        //sessionFactory.close();
    }

    public static synchronized List<Integer> getSitesIdForDeletion(List<Site> sites) throws SQLException{
        List<Integer> idList = new ArrayList<>();
        for(Site site : sites) {
            boolean isStart = selectQuery.length() == 0;
            selectQuery.append((isStart ? "SELECT id FROM site WHERE" :" OR")+(" url = '" + site.getUrl()+"'"));
        }
        ResultSet rs = DBConnection.getConnection().createStatement().executeQuery(selectQuery.toString());
        while (rs.next()) {
            System.out.println("Id сайта на удаление - " + rs.getInt("id"));
            idList.add(rs.getInt("id"));
        }
        clearSelectQuery();
        return idList;
    }
    public static synchronized void deleteInfoAboutSitesAndPages(List<Site> sites) throws SQLException {
        List<Integer> idList = getSitesIdForDeletion(sites);
        deleteInfoAboutSites(idList);
        deleteInfoAboutPages(idList);
    }

    public static synchronized void deleteInfoAboutSites(List<Integer> idList) throws SQLException{
        for(Integer id : idList) {
            boolean isStart = deleteQuery.length() == 0;
            deleteQuery.append((isStart ? "DELETE FROM site WHERE" :" OR")+(" id = '" + id +"'"));
        }
        if(deleteQuery.length() != 0){
            getConnection().createStatement().execute(deleteQuery.toString());
        }
        clearDeleteQuery();
    }
    public static synchronized void deleteInfoAboutPages(List<Integer> idList) throws SQLException{
        for(Integer id : idList) {
            boolean isStart = deleteQuery.length() == 0;
            deleteQuery.append((isStart ? "DELETE FROM page WHERE" :" OR")+(" site_id = '" + id +"'"));
        }
        if(deleteQuery.length() != 0){
            getConnection().createStatement().execute(deleteQuery.toString());
        }
        clearDeleteQuery();
    }

    public static synchronized void insertInfoAboutPage(Page page) throws SQLException{
        Transaction transaction = session.beginTransaction();
        session.save(page);
        transaction.commit();
        //sessionFactory.close();
    }
    private static void clearDeleteQuery(){
        deleteQuery = new StringBuffer();
    }
    private static void clearSelectQuery(){
        selectQuery = new StringBuffer();
    }
}
