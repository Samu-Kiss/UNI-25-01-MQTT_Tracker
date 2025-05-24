package broker.mqtt;
import org.eclipse.paho.client.mqttv3.*;


public class MqttPublicador {
    public static void publicar(){
        String broker = "tcp://broker.hivemq.com:1883"; // El puerto de mosquito es 1883
        String idCliente = "ClientePublicador";
        String topic = "mensaje/prueba";
        String mensaje = "Esta es la primera prueba de funcionamiento";
        int QoS = 1;

        try{
            MqttClient cliente = new MqttClient(broker, idCliente);
            cliente.connect();

            MqttMessage msg = new MqttMessage(mensaje.getBytes());
            msg.setQos(QoS);
            cliente.publish(topic, msg);
            System.out.println("El mensaje fue publicado\n");

            cliente.disconnect();
        }
        catch (MqttException e){
            e.getMessage();
        }
    }
}
