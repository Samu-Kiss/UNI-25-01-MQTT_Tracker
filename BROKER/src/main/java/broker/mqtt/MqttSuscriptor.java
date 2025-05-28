package broker.mqtt;
import org.eclipse.paho.client.mqttv3.*;

public class MqttSuscriptor {
    public static void suscribirse(String topic){
        String broker = "tcp://broker.hivemq.com:1883";
        String idCliente = "ClienteSuscriptor";

        try{
            MqttClient cliente = new MqttClient(broker, idCliente);
            cliente.connect();

            // Se usa una función flecha para tomar el mensaje que le llega al suscriptor (En Bytes) y pasarlo a texto.
            cliente.subscribe(topic, (tema, mensaje) ->{
                System.out.printf("Un mensaje fue recibido con el topico %s y el precio: %s\n", tema, new String(mensaje.getPayload()));
            });
            //System.out.printf("Usted se sucribió al tema: %s\n", topic);

        }
        catch(MqttException e){
            e.getMessage();
        }
    }
}
