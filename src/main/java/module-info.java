module com.example.proxyrotator {
    requires java.sql;
    requires mysql.connector.j;
    requires javafx.fxml;
    requires java.prefs;
    requires org.json;
    requires jdk.jsobject;
    requires javafx.controls;

    opens com.example.proxyrotator to javafx.fxml;
    opens com.example.proxyrotator.Controllers to javafx.fxml;

    exports com.example.proxyrotator;
}