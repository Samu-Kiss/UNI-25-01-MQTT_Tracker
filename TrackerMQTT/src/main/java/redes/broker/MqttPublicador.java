package redes.broker;
import org.eclipse.paho.client.mqttv3.*;


public class MqttPublicador {
    /**
     * Método para publicar un mensaje en un tópico MQTT.
     * @param topic El tópico donde se publicará el mensaje.
     * @param mensaje El mensaje a publicar.
     */
    public static void publicar(String topic, String mensaje){
        String broker = "tcp://broker.hivemq.com:1883"; // El puerto de mosquito es 1883
        String idCliente = "ClientePublicador";
        int QoS = 1;

        try{
            MqttClient cliente = new MqttClient(broker, idCliente);
            cliente.connect();

            MqttMessage msg = new MqttMessage(mensaje.getBytes());
            msg.setQos(QoS);
            cliente.publish(topic, msg);

            cliente.disconnect();
        }
        catch (MqttException e){
            e.getMessage();
        }
    }
}
