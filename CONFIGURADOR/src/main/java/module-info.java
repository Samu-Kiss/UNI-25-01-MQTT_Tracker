module redes.configurador.configurador {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.eclipse.paho.client.mqttv3;


    opens redes.configurador.configurador to javafx.fxml;
    exports redes.configurador.configurador;
}