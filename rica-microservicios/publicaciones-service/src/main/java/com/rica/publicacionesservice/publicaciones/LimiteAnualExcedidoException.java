package com.rica.publicacionesservice.publicaciones;

public class LimiteAnualExcedidoException extends RuntimeException {

    public LimiteAnualExcedidoException(String mensaje) {
        super(mensaje);
    }

}
