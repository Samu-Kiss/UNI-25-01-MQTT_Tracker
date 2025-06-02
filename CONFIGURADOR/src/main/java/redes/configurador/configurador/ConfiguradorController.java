package redes.configurador.configurador;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import redes.configurador.configurador.model.Criptomoneda;
import redes.configurador.configurador.service.MQTTService;

import java.net.URL;
import java.util.ResourceBundle;

public class ConfiguradorController implements Initializable {
    @FXML
    private TextField simboloTextField;

    @FXML
    private TextField nombreTextField;

    private String mqttBrokerTextField = "broker.hivemq.com";

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

    private ObservableList<Criptomoneda> criptomonedas = FXCollections.observableArrayList();
    private ObservableList<Criptomoneda> criptomonedasActivas = FXCollections.observableArrayList();

    private MQTTService mqttService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
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

        // Deshabilitar botones hasta que se conecte al broker
        actualizarEstadoBotones(false);
    }

    @FXML
    private void onConectarButtonClick() {
        if (mqttService != null && mqttService.isConnected()) {
            mqttService.disconnect();
            conectarButton.setText("Conectar");
            estadoLabel.setText("Desconectado");
            actualizarEstadoBotones(false);
            return;
        }

        String brokerUrl = mqttBrokerTextField.trim();
        if (brokerUrl.isEmpty()) {
            mostrarAlerta("Error", "Ingrese la dirección del broker MQTT", Alert.AlertType.ERROR);
            return;
        }

        mqttService = new MQTTService(brokerUrl, "ConfiguradorESP32_" + System.currentTimeMillis());
        if (mqttService.connect()) {
            conectarButton.setText("Desconectar");
            estadoLabel.setText("Conectado a " + brokerUrl);
            actualizarEstadoBotones(true);
        } else {
            mostrarAlerta("Error", "No se pudo conectar al broker MQTT", Alert.AlertType.ERROR);
        }
    }

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

    @FXML
    private void onIniciarESP32ButtonClick() {
        if (mqttService != null && mqttService.isConnected()) {
            mqttService.iniciarESP32();
            estadoLabel.setText("ESP32 iniciado");
        }
    }

    @FXML
    private void onDetenerESP32ButtonClick() {
        if (mqttService != null && mqttService.isConnected()) {
            mqttService.detenerESP32();
            estadoLabel.setText("ESP32 detenido");
        }
    }

    private void actualizarCriptomonedasESP32() {
        if (mqttService != null && mqttService.isConnected()) {
            mqttService.actualizarCriptomonedas(criptomonedasActivas);
            estadoLabel.setText("Criptomonedas actualizadas");
        }
    }

    private void actualizarEstadoBotones(boolean conectado) {
        agregarButton.setDisable(!conectado);
        agregarActivasButton.setDisable(!conectado);
        quitarActivasButton.setDisable(!conectado);
        iniciarESP32Button.setDisable(!conectado);
        detenerESP32Button.setDisable(!conectado);
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}