module com.example.kurs {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires org.json;

    opens com.example.kurs to javafx.fxml;
    exports com.example.kurs;
}