module com.example.multiplayergame2 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.multiplayergame2 to javafx.fxml;
    exports com.example.multiplayergame2;
}