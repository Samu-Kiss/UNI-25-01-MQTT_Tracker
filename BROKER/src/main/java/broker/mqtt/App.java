package broker.mqtt;


public class App {
    public static void main(String[] args) {
        //MqttSuscriptor.suscribirse();
        //MqttPublicador.publicar();

        consumoApi prueba = new consumoApi();
        prueba.consumir("SOLAUD");
    }
}
