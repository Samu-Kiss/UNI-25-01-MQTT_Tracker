package redes.configurador.service;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import redes.configurador.model.Criptomoneda;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MQTTService {
    private MqttClient client;
    private String broker;
    private String clientId;
    private boolean connected = false;
    private final List<Consumer<String>> messageListeners = new ArrayList<>();
    private final List<String> subscribedTopics = new ArrayList<>();

    /**
     * Constructor para inicializar el servicio MQTT con el broker y el ID del cliente.
     *
     * @param broker   Dirección del broker MQTT (ejemplo: "tcp://broker.hivemq.com:1883").
     * @param clientId ID único del cliente MQTT.
     */
    public MQTTService(String broker, String clientId) {
        this.broker = broker;
        this.clientId = clientId;
    }

    /**
     * Método para conectar al broker MQTT.
     * Configura el cliente, establece las opciones de conexión y suscribe a los tópicos necesarios.
     *
     * @return true si la conexión fue exitosa, false en caso contrario.
     */
    public boolean connect() {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            
            // Agregar el protocolo si no está presente
            String brokerUrl = broker;
            if (!broker.startsWith("tcp://") && !broker.startsWith("ssl://")) {
                brokerUrl = "tcp://" + broker + ":1883";
            }
        
            client = new MqttClient(brokerUrl, clientId, persistence);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);

            // Configurar callback para recibir mensajes
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("Conexión perdida con el broker MQTT: " + cause.getMessage());
                    connected = false;
                    notifyListeners("ESP32/estado", "Conexión perdida con el broker MQTT");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    notifyListeners(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // No es necesario implementar para esta aplicación
                }
            });

            client.connect(options);

            // Suscribirse a tópicos del ESP32
            subscribeToTopic("ESP32/estado");
            subscribeToTopic("ESP32/datos");

            connected = true;
            return true;
        } catch (MqttException e) {
            System.err.println("Error connecting to MQTT broker: " + e.getMessage());
            return false;
        }
    }

    /**
     * Método para desconectar del broker MQTT.
     * Desuscribe de todos los tópicos y cierra la conexión.
     */
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                // Desuscribirse de todos los tópicos antes de desconectar
                for (String topic : subscribedTopics) {
                    try {
                        client.unsubscribe(topic);
                    } catch (MqttException e) {
                        System.err.println("Error al desuscribirse del tópico " + topic + ": " + e.getMessage());
                    }
                }
                subscribedTopics.clear();
                client.disconnect();
                connected = false;
            }
        } catch (MqttException e) {
            System.err.println("Error disconnecting from MQTT broker: " + e.getMessage());
        }
    }

    /**
     * Método para suscribirse a un tópico específico.
     * Si ya está suscrito, no hace nada.
     *
     * @param topic Tópico al que se desea suscribir.
     */
    public void subscribeToTopic(String topic) {
        if (!connected) {
            return;
        }

        try {
            client.subscribe(topic, 1);  // QoS 1 para garantizar la entrega al menos una vez
            if (!subscribedTopics.contains(topic)) {
                subscribedTopics.add(topic);
            }
        } catch (MqttException e) {
            System.err.println("Error al suscribirse al tópico " + topic + ": " + e.getMessage());
        }
    }

    /**
     * Método para desuscribirse de un tópico específico.
     * Si no está suscrito, no hace nada.
     *
     * @param topic Tópico del que se desea desuscribir.
     */
    public void unsubscribeFromTopic(String topic) {
        if (!connected) {
            return;
        }

        try {
            client.unsubscribe(topic);
            subscribedTopics.remove(topic);
        } catch (MqttException e) {
            System.err.println("Error al desuscribirse del tópico " + topic + ": " + e.getMessage());
        }
    }

    /**
     * Método para agregar un listener que recibirá mensajes del ESP32.
     * Evita duplicados en la lista de listeners.
     *
     * @param listener Consumer que procesará los mensajes recibidos.
     */
    public void addMessageListener(Consumer<String> listener) {
        if (!messageListeners.contains(listener)) {
            messageListeners.add(listener);
        }
    }

    /**
     * Método para eliminar un listener de la lista de mensajes.
     * Si el listener no está registrado, no hace nada.
     *
     * @param listener Consumer que se desea eliminar de la lista de listeners.
     */
    public void removeMessageListener(Consumer<String> listener) {
        messageListeners.remove(listener);
    }

    /**
     * Método para notificar a todos los listeners registrados sobre un nuevo mensaje.
     * Formatea el mensaje con el tópico y el contenido.
     *
     * @param topic   Tópico del mensaje recibido.
     * @param message Contenido del mensaje recibido.
     */
    private void notifyListeners(String topic, String message) {
        String formattedMessage = "[" + topic + "] " + message;
        for (Consumer<String> listener : messageListeners) {
            listener.accept(formattedMessage);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Método para enviar un comando al ESP32.
     * Publica el comando en el tópico "esp32/comandos".
     *
     * @param comando Comando a enviar al ESP32.
     */
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

    /**
     * Método para actualizar la lista de criptomonedas en el ESP32.
     * Publica un mensaje JSON con las criptomonedas y sus valores críticos.
     *
     * @param monedas Lista de criptomonedas a enviar al ESP32.
     */
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

    /**
     * Método para iniciar o detener el ESP32.
     * Envía un comando "start" al ESP32.
     */
    public void iniciarESP32() {
        enviarComando("start");
    }

    /**
     * Método para detener el ESP32.
     * Envía un comando "stop" al ESP32.
     */
    public void detenerESP32() {
        enviarComando("stop");
    }
}