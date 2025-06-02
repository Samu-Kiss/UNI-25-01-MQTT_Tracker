package redes.configurador.configurador.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Criptomoneda {
    private final StringProperty simbolo;
    private final StringProperty nombre;
    private final DoubleProperty valorCritico;

    public Criptomoneda(String simbolo, String nombre) {
        this.simbolo = new SimpleStringProperty(simbolo);
        this.nombre = new SimpleStringProperty(nombre);
        this.valorCritico = new SimpleDoubleProperty(0.0);
    }

    public StringProperty simboloProperty() {
        return simbolo;
    }

    public String getSimbolo() {
        return simbolo.get();
    }

    public void setSimbolo(String simbolo) {
        this.simbolo.set(simbolo);
    }

    public StringProperty nombreProperty() {
        return nombre;
    }

    public String getNombre() {
        return nombre.get();
    }

    public void setNombre(String nombre) {
        this.nombre.set(nombre);
    }

    public DoubleProperty valorCriticoProperty() {
        return valorCritico;
    }

    public double getValorCritico() {
        return valorCritico.get();
    }

    public void setValorCritico(double valorCritico) {
        this.valorCritico.set(valorCritico);
    }

    @Override
    public String toString() {
        return simbolo.get() + " - " + nombre.get();
    }
}
