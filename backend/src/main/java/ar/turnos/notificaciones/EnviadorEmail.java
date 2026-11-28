package ar.turnos.notificaciones;

/**
 * Puerto de salida de emails (design D7). Si el proveedor elegido en PA-03 es una API, se agrega otra
 * implementación sin tocar el procesador. Lanza una excepción no chequeada si el envío falla.
 */
public interface EnviadorEmail {

    void enviar(EmailSaliente email);
}
