package redes.configurador;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import redes.configurador.model.Criptomoneda;
import redes.configurador.service.MQTTService;

import java.net.URL;
import java.util.ResourceBundle;

public class ConfiguradorController implements Initializable {
    @FXML
    private TextField simboloTextField;

    @FXML
    private TextField nombreTextField;

    @FXML
    private TextField mqttBrokerTextField;

    @FXML
    private Button conectarButton;

    @FXML
    private Button agregarButton;

    @FXML
    private Button agregarActivasButton;

    @FXML
    private Button quitarActivasButton;

    @FXML
    private Button iniciarESP32Button;

    @FXML
    private Button detenerESP32Button;

    @FXML
    private TableView<Criptomoneda> criptomonedasTable;

    @FXML
    private TableColumn<Criptomoneda, String> simboloColumn;

    @FXML
    private TableColumn<Criptomoneda, String> nombreColumn;

    @FXML
    private TableView<Criptomoneda> criptomonedasActivasTable;

    @FXML
    private TableColumn<Criptomoneda, String> simboloActivasColumn;

    @FXML
    private TableColumn<Criptomoneda, String> nombreActivasColumn;

    @FXML
    private TableColumn<Criptomoneda, Double> valorCriticoColumn;

    @FXML
    private Label estadoLabel;

    @FXML
    private TextArea mensajesTextArea;

    private ObservableList<Criptomoneda> criptomonedas = FXCollections.observableArrayList();
    private ObservableList<Criptomoneda> criptomonedasActivas = FXCollections.observableArrayList();

    private MQTTService mqttService;

    /**
     * Inicializa el controlador y configura las tablas y botones.
     * Establece el broker MQTT por defecto y configura las columnas de las tablas.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Establecer valor predeterminado para el broker MQTT
        mqttBrokerTextField.setText("broker.hivemq.com");
        // Configurar tablas
        simboloColumn.setCellValueFactory(cellData -> cellData.getValue().simboloProperty());
        nombreColumn.setCellValueFactory(cellData -> cellData.getValue().nombreProperty());

        simboloActivasColumn.setCellValueFactory(cellData -> cellData.getValue().simboloProperty());
        nombreActivasColumn.setCellValueFactory(cellData -> cellData.getValue().nombreProperty());
        valorCriticoColumn.setCellValueFactory(new PropertyValueFactory<>("valorCritico"));

        // Permitir edición de valorCritico
        criptomonedasActivasTable.setEditable(true);
        valorCriticoColumn.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                return object == null ? "0.0" : object.toString();
            }

            @Override
            public Double fromString(String string) {
                try {
                    return Double.parseDouble(string);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
        }));

        valorCriticoColumn.setOnEditCommit(event -> {
            Criptomoneda moneda = event.getRowValue();
            moneda.setValorCritico(event.getNewValue());
            actualizarCriptomonedasESP32();
        });

        // Enlazar datos a tablas
        criptomonedasTable.setItems(criptomonedas);
        criptomonedasActivasTable.setItems(criptomonedasActivas);

        // Agregar criptomonedas por defecto
        inicializarCriptomonedasPorDefecto();

        // Deshabilitar botones hasta que se conecte al broker
        actualizarEstadoBotones(false);
    }

    /**
     * Inicializa las criptomonedas disponibles por defecto.
     * Agrega Bitcoin (BTC) y Solana (SOL) con valores críticos predefinidos.
     */
    private void inicializarCriptomonedasPorDefecto() {
        // Agregar criptomonedas disponibles por defecto
        Criptomoneda btc = new Criptomoneda("BTC", "Bitcoin");
        Criptomoneda sol = new Criptomoneda("SOL", "Solana");

        criptomonedas.add(btc);
        criptomonedas.add(sol);

        // Agregar a monedas activas con valores críticos por defecto
        btc.setValorCritico(50000.0);  // Valor crítico BTC: $50,000
        sol.setValorCritico(150.0);    // Valor crítico SOL: $150

        criptomonedasActivas.add(btc);
        criptomonedasActivas.add(sol);
    }

    /**
     * Maneja el evento de clic en el botón "Conectar".
     * Conecta o desconecta del broker MQTT según el estado actual.
     */
    @FXML
    private void onConectarButtonClick() {
        if (mqttService != null && mqttService.isConnected()) {
            mqttService.disconnect();
            conectarButton.setText("Conectar");
            estadoLabel.setText("Desconectado");
            actualizarEstadoBotones(false);
            return;
        }

        String brokerUrl = mqttBrokerTextField.getText().trim();
        if (brokerUrl.isEmpty()) {
            mostrarAlerta("Error", "Ingrese la dirección del broker MQTT", Alert.AlertType.ERROR);
            return;
        }

        mqttService = new MQTTService(brokerUrl, "ConfiguradorESP32_" + System.currentTimeMillis());

        // Configurar el listener para recibir mensajes del ESP32
        mqttService.addMessageListener(this::agregarMensaje);

        if (mqttService.connect()) {
            conectarButton.setText("Desconectar");
            estadoLabel.setText("Conectado a " + brokerUrl);
            actualizarEstadoBotones(true);
            agregarMensaje("Conectado al broker MQTT: " + brokerUrl);
        } else {
            mostrarAlerta("Error", "No se pudo conectar al broker MQTT", Alert.AlertType.ERROR);
        }
    }

    /**
     * Maneja el evento de clic en el botón "Agregar".
     * Agrega una nueva criptomoneda a la lista de criptomonedas disponibles.
     */
    @FXML
    private void onAgregarButtonClick() {
        String simbolo = simboloTextField.getText().trim().toUpperCase();
        String nombre = nombreTextField.getText().trim();

        if (simbolo.isEmpty()) {
            mostrarAlerta("Error", "Ingrese el símbolo de la criptomoneda", Alert.AlertType.ERROR);
            return;
        }

        // Verificar si ya existe
        for (Criptomoneda c : criptomonedas) {
            if (c.getSimbolo().equals(simbolo)) {
                mostrarAlerta("Error", "La criptomoneda ya existe en la lista", Alert.AlertType.ERROR);
                return;
            }
        }

        criptomonedas.add(new Criptomoneda(simbolo, nombre.isEmpty() ? simbolo : nombre));
        simboloTextField.clear();
        nombreTextField.clear();
    }

    /**
     * Maneja el evento de clic en el botón "Agregar a activas".
     * Agrega la criptomoneda seleccionada a la lista de criptomonedas activas.
     */
    @FXML
    private void onAgregarActivasButtonClick() {
        Criptomoneda seleccionada = criptomonedasTable.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta("Error", "Seleccione una criptomoneda para agregar a las activas", Alert.AlertType.ERROR);
            return;
        }

        // Verificar si ya está en la lista de activas
        for (Criptomoneda c : criptomonedasActivas) {
            if (c.getSimbolo().equals(seleccionada.getSimbolo())) {
                mostrarAlerta("Error", "La criptomoneda ya está en la lista de activas", Alert.AlertType.ERROR);
                return;
            }
        }

        criptomonedasActivas.add(seleccionada);
        actualizarCriptomonedasESP32();
    }

    /**
     * Maneja el evento de clic en el botón "Quitar de activas".
     * Quita la criptomoneda seleccionada de la lista de criptomonedas activas.
     */
    @FXML
    private void onQuitarActivasButtonClick() {
        Criptomoneda seleccionada = criptomonedasActivasTable.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta("Error", "Seleccione una criptomoneda para quitar de las activas", Alert.AlertType.ERROR);
            return;
        }

        criptomonedasActivas.remove(seleccionada);
        actualizarCriptomonedasESP32();
    }

    /**
     * Maneja el evento de clic en el botón "Iniciar ESP32".
     * Envía un mensaje al ESP32 para iniciar la monitorización de criptomonedas.
     */
    @FXML
    private void onIniciarESP32ButtonClick() {
        if (mqttService != null && mqttService.isConnected()) {
            mqttService.iniciarESP32();
            estadoLabel.setText("ESP32 iniciado");
        }
    }

    /**
     * Maneja el evento de clic en el botón "Detener ESP32".
     * Envía un mensaje al ESP32 para detener la monitorización de criptomonedas.
     */
    @FXML
    private void onDetenerESP32ButtonClick() {
        if (mqttService != null && mqttService.isConnected()) {
            mqttService.detenerESP32();
            estadoLabel.setText("ESP32 detenido");
        }
    }

    /**
     * Actualiza la lista de criptomonedas en el ESP32.
     * Publica un mensaje con las criptomonedas activas y sus valores críticos.
     */
    private void actualizarCriptomonedasESP32() {
        if (mqttService != null && mqttService.isConnected()) {
            mqttService.actualizarCriptomonedas(criptomonedasActivas);
            estadoLabel.setText("Criptomonedas actualizadas");
        }
    }

    /**
     * Actualiza el estado de los botones según si está conectado al broker MQTT.
     * Deshabilita o habilita los botones según el estado de conexión.
     *
     * @param conectado true si está conectado, false si no lo está.
     */
    private void actualizarEstadoBotones(boolean conectado) {
        agregarButton.setDisable(!conectado);
        agregarActivasButton.setDisable(!conectado);
        quitarActivasButton.setDisable(!conectado);
        iniciarESP32Button.setDisable(!conectado);
        detenerESP32Button.setDisable(!conectado);
    }

    /**
     * Muestra una alerta con el título, mensaje y tipo especificado.
     *
     * @param titulo El título de la alerta.
     * @param mensaje El mensaje de la alerta.
     * @param tipo El tipo de alerta (ERROR, INFORMATION, CONFIRMATION, etc.).
     */
    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Agrega un mensaje al área de texto de mensajes.
     * Asegura que la actualización se realice en el hilo de JavaFX.
     *
     * @param mensaje El mensaje a agregar.
     */
    private void agregarMensaje(String mensaje) {
        // Asegurarse de que la actualización de la UI se haga en el hilo de JavaFX
        javafx.application.Platform.runLater(() -> {
            if (mensajesTextArea.getText().isEmpty()) {
                mensajesTextArea.setText(mensaje);
            } else {
                mensajesTextArea.appendText("\n" + mensaje);
            }
            // Hacer scroll al final para mostrar el mensaje más reciente
            mensajesTextArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}