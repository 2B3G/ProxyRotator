module com.example.proxyrotator {
    requires java.sql;
    requires mysql.connector.j;
    requires javafx.fxml;
    requires javafx.web;
    requires java.prefs;
    requires org.json;
    requires jdk.jsobject;

    opens com.example.proxyrotator to javafx.fxml;
    opens com.example.proxyrotator.Controllers to javafx.fxml;

    exports com.example.proxyrotator;
}