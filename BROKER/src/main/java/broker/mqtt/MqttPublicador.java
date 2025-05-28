package broker.mqtt;
import org.eclipse.paho.client.mqttv3.*;


public class MqttPublicador {
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
            System.out.printf("El mensaje fue publicado con el topico %s y el precio: %s\n", topic, mensaje);

            cliente.disconnect();
        }
        catch (MqttException e){
            e.getMessage();
        }
    }
}
