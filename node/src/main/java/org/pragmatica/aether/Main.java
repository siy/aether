package org.pragmatica.aether;

public record Main(String[] args) {
    public static void main(String[] args) {
        new Main(args).run();
    }

    private void run() {

    }
}
