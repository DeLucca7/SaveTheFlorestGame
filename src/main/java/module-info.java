module com.game.savetheflorest {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.game.savetheflorest to javafx.fxml;
    exports com.game.savetheflorest;
}