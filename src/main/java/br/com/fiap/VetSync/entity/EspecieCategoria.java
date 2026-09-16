package br.com.fiap.VetSync.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.text.Normalizer;

public enum EspecieCategoria {

    CAO("Cão"),
    GATO("Gato"),
    EQUINO("Equino"),
    BOVINO("Bovino"),
    SUINO("Suíno"),
    OVINO("Ovino"),
    CAPRINO("Caprino"),
    AVE("Ave"),
    REPTIL("Réptil"),
    ANFIBIO("Anfíbio"),
    PEIXE("Peixe"),
    ROEDOR("Roedor"),
    COELHO("Coelho"),
    FURAO("Furão"),
    PRIMATA("Primata"),
    FELINO_SILVESTRE("Felino Silvestre"),
    CANIDEO_SILVESTRE("Canídeo Silvestre"),
    MARSUPIAL("Marsupial"),
    SILVESTRE("Animal Silvestre"),
    OUTRO(null);

    private final String nomeOficial;

    EspecieCategoria(String nomeOficial) {
        this.nomeOficial = nomeOficial;
    }

    public String getNomeOficial() {
        return nomeOficial;
    }

    private static String semAcento(String valor) {
        String normalizado = Normalizer.normalize(valor, Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{M}", "");
    }

    @JsonCreator
    public static EspecieCategoria fromString(String valor) {
        if (valor == null) return null;
        String chave = semAcento(valor.trim().toLowerCase());
        return switch (chave) {
            case "cao", "cachorro", "canino" -> CAO;
            case "gato", "felino", "gata" -> GATO;
            case "equino", "cavalo", "egua", "potro" -> EQUINO;
            case "bovino", "boi", "vaca", "gado" -> BOVINO;
            case "suino", "porco", "porca", "leitao" -> SUINO;
            case "ovino", "ovelha", "carneiro" -> OVINO;
            case "caprino", "cabra", "bode" -> CAPRINO;
            case "ave", "passaro", "papagaio", "calopsita", "canario", "periquito" -> AVE;
            case "reptil", "lagarto", "cobra", "serpente", "tartaruga", "jabuti", "iguana" -> REPTIL;
            case "anfibio", "sapo", "ra", "salamandra" -> ANFIBIO;
            case "peixe", "peixes" -> PEIXE;
            case "roedor", "hamster", "rato", "ratinho", "porquinho-da-india", "porquinho da india" -> ROEDOR;
            case "coelho", "coelha" -> COELHO;
            case "furao" -> FURAO;
            case "primata", "macaco", "sagui" -> PRIMATA;
            case "felino silvestre", "gato-do-mato", "onca", "jaguatirica" -> FELINO_SILVESTRE;
            case "canideo silvestre", "lobo-guara", "lobo guara", "raposa" -> CANIDEO_SILVESTRE;
            case "marsupial", "gamba", "sarue" -> MARSUPIAL;
            case "silvestre", "animal silvestre", "fauna silvestre" -> SILVESTRE;
            default -> OUTRO;
        };
    }
}