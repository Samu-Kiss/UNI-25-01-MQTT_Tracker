package redes.configurador.configurador.service;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import redes.configurador.configurador.model.Criptomoneda;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MQTTService {
    private MqttClient client;
    private String broker;
    private String clientId;
    private boolean connected = false;

    public MQTTService(String broker, String clientId) {
        this.broker = broker;
        this.clientId = clientId;
    }

    public boolean connect() {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
            connected = true;
            return true;
        } catch (MqttException e) {
            System.err.println("Error connecting to MQTT broker: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                connected = false;
            }
        } catch (MqttException e) {
            System.err.println("Error disconnecting from MQTT broker: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void enviarComando(String comando) {
        if (!connected) {
            return;
        }

        try {
            MqttMessage message = new MqttMessage(comando.getBytes(StandardCharsets.UTF_8));
            message.setQos(2);
            client.publish("esp32/comandos", message);
        } catch (MqttException e) {
            System.err.println("Error sending command to ESP32: " + e.getMessage());
        }
    }

    public void actualizarCriptomonedas(List<Criptomoneda> monedas) {
        if (!connected) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"criptomonedas\":[");

        for (int i = 0; i < monedas.size(); i++) {
            Criptomoneda moneda = monedas.get(i);
            sb.append("{\"simbolo\":\"")
              .append(moneda.getSimbolo())
              .append("\",\"valorCritico\":")
              .append(moneda.getValorCritico())
              .append("}");

            if (i < monedas.size() - 1) {
                sb.append(",");
            }
        }

        sb.append("]}");

        try {
            MqttMessage message = new MqttMessage(sb.toString().getBytes(StandardCharsets.UTF_8));
            message.setQos(2);
            client.publish("esp32/criptomonedas", message);
        } catch (MqttException e) {
            System.err.println("Error sending cryptocurrencies to ESP32: " + e.getMessage());
        }
    }

    public void iniciarESP32() {
        enviarComando("start");
    }

    public void detenerESP32() {
        enviarComando("stop");
    }
}
