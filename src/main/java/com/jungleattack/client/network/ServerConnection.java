package com.jungleattack.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.jungleattack.client.controller.ClientController;
import com.jungleattack.client.protocol.Message;
import com.jungleattack.client.util.ClientConstants;

/**
 * La conexión con el servidor. Su hilo lee una línea JSON por mensaje y se la pasa al controlador.
 * Este hilo NUNCA toca Swing: el controlador es el que pasa el mensaje al hilo de Swing.
 */
public class ServerConnection implements Runnable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final ClientController controller;
    private volatile boolean closing;

    // Abre el socket contra el servidor real y prepara lector/escritor de texto UTF-8.
    public ServerConnection(String host, int port, ClientController controller) throws IOException {
        Socket s = new Socket();
        BufferedReader reader;
        PrintWriter writer;
        try {
            s.connect(new InetSocketAddress(host, port), ClientConstants.CONNECT_TIMEOUT_MS);
            reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
        } catch (IOException e) {
            s.close();
            throw e;
        }
        this.socket = s;
        this.in = reader;
        this.out = writer;
        this.controller = controller;
    }

    /** Para los tests: sin socket (StringReader y StringWriter). */
    public ServerConnection(BufferedReader in, PrintWriter out, ClientController controller) {
        this.socket = null;
        this.in = in;
        this.out = out;
        this.controller = controller;
    }

    /** Crea el hilo de red. */
    public void start() {
        Thread thread = new Thread(this, "network");
        thread.setDaemon(true);
        thread.start();
    }

    /** Lee cada línea, la convierte en Message y se la pasa al controlador. */
    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    controller.onMessage(Message.fromJson(line));
                } catch (RuntimeException e) {
                    System.err.println("Mensaje inválido del servidor: " + line);
                }
            }
        } catch (IOException e) {
            // el socket se cerró: se avisa abajo
        } finally {
            if (!closing) {
                controller.onDisconnected();
            }
        }
    }

    /** Manda un mensaje: un JSON por línea. */
    public synchronized void send(Message message) {
        out.print(message.toJson() + "\n");
        out.flush();
    }

    /** Cierra la conexión a propósito (no cuenta como desconexión). */
    public void close() {
        closing = true;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // ya estaba cerrado
            }
        }
    }
}
