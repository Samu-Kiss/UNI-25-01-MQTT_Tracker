package redes.broker;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


public class NotificadorTelegram {
    private static final String TOKEN = "8190410076:AAFLrSUGs8kTECEdyPJK3brFxbuw1wkQuXo";
    private static final String CHAT_ID = "7812011552";

    public static void enviarMensaje(String mensaje) {
        try {
            String urlString = String.format(
                    "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                    TOKEN, CHAT_ID, URLEncoder.encode(mensaje, "UTF-8")
            );

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Error al enviar mensaje Telegram: " + responseCode);
            }

        } catch (Exception e) {
            System.err.println("Excepción enviando mensaje Telegram: " + e.getMessage());
        }
    }
}
