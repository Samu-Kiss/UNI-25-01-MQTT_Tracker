module redes.trackermqtt {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires org.eclipse.paho.client.mqttv3;


    opens redes.configurador to javafx.fxml;
    exports redes.configurador;
}