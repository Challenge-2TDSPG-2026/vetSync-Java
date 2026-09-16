package br.com.fiap.VetSync.entity;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RacaCatalogo {

    private static final Map<EspecieCategoria, List<String>> RACAS = Map.ofEntries(
            Map.entry(EspecieCategoria.CAO, List.of(
                    "Labrador Retriever", "Golden Retriever", "Pastor Alemão", "Bulldog Francês", "Bulldog Inglês",
                    "Poodle", "Yorkshire Terrier", "Shih Tzu", "Beagle", "Rottweiler", "Border Collie",
                    "Chihuahua", "Dachshund", "Boxer", "Pug", "Dálmata", "Husky Siberiano", "Akita",
                    "Cocker Spaniel", "Maltês", "Lhasa Apso", "Schnauzer", "Pinscher", "Basset Hound",
                    "Doberman", "Fila Brasileiro", "Vira-lata"
            )),
            Map.entry(EspecieCategoria.GATO, List.of(
                    "Persa", "Siamês", "Maine Coon", "Angorá", "Sphynx", "Ragdoll", "British Shorthair",
                    "Bengal", "Munchkin", "Vira-lata"
            )),
            Map.entry(EspecieCategoria.EQUINO, List.of(
                    "Mangalarga Marchador", "Quarto de Milha", "Puro Sangue Inglês", "Crioulo", "Campolina",
                    "Árabe", "Andaluz", "Appaloosa", "Lusitano", "Brasileiro de Hipismo", "Pônei"
            )),
            Map.entry(EspecieCategoria.BOVINO, List.of(
                    "Nelore", "Gir", "Holandês", "Jersey", "Angus", "Brahman", "Girolando", "Guzerá", "Senepol", "Charolês"
            )),
            Map.entry(EspecieCategoria.SUINO, List.of(
                    "Large White", "Landrace", "Duroc", "Pietrain", "Piau", "Moura"
            )),
            Map.entry(EspecieCategoria.OVINO, List.of(
                    "Santa Inês", "Dorper", "Morada Nova", "Suffolk", "Texel", "Bergamácia"
            )),
            Map.entry(EspecieCategoria.CAPRINO, List.of(
                    "Boer", "Saanen", "Anglo-Nubiana", "Toggenburg", "Moxotó", "Canindé"
            )),
            Map.entry(EspecieCategoria.AVE, List.of(
                    "Calopsita", "Periquito Australiano", "Canário Belga", "Agapornis", "Papagaio Verdadeiro",
                    "Araruna", "Cacatua", "Galinha D'angola", "Codorna", "Galinha Caipira"
            )),
            Map.entry(EspecieCategoria.REPTIL, List.of(
                    "Jabuti Piranga", "Tartaruga Tigre D'água", "Iguana Verde", "Jiboia", "Corn Snake",
                    "Gecko Leopardo", "Cágado"
            )),
            Map.entry(EspecieCategoria.ANFIBIO, List.of(
                    "Perereca", "Rã Touro", "Salamandra", "Axolote"
            )),
            Map.entry(EspecieCategoria.PEIXE, List.of(
                    "Betta", "Kinguio", "Carpa", "Tetra Neon", "Acará Disco", "Guppy", "Oscar"
            )),
            Map.entry(EspecieCategoria.ROEDOR, List.of(
                    "Hamster Sírio", "Hamster Anão Russo", "Porquinho da Índia", "Chinchila", "Rato Twister", "Gerbil"
            )),
            Map.entry(EspecieCategoria.COELHO, List.of(
                    "Mini Lion", "Holandês", "Fuzzy Lop", "Angorá", "Rex", "Netherland Dwarf", "Ariete"
            )),
            Map.entry(EspecieCategoria.FURAO, List.of(
                    "Furão Standard", "Furão Angorá"
            )),
            Map.entry(EspecieCategoria.PRIMATA, List.of(
                    "Sagui-de-tufo-branco", "Macaco-prego", "Mico-leão-dourado", "Bugio"
            )),
            Map.entry(EspecieCategoria.FELINO_SILVESTRE, List.of(
                    "Jaguatirica", "Gato-do-mato", "Puma", "Onça-pintada"
            )),
            Map.entry(EspecieCategoria.CANIDEO_SILVESTRE, List.of(
                    "Lobo-guará", "Raposa-do-campo", "Cachorro-do-mato"
            )),
            Map.entry(EspecieCategoria.MARSUPIAL, List.of(
                    "Gambá-de-orelha-branca", "Cuíca"
            )),
            Map.entry(EspecieCategoria.SILVESTRE, List.of(
                    "Tamanduá-mirim", "Paca", "Cutia", "Ouriço-cacheiro", "Preá"
            )),
            Map.entry(EspecieCategoria.OUTRO, List.of())
    );

    public static List<String> listar(EspecieCategoria categoria) {
        if (categoria == null) return List.of();
        return RACAS.getOrDefault(categoria, List.of());
    }

    public static List<String> sugerir(EspecieCategoria categoria, String texto) {
        List<String> base = listar(categoria);
        if (texto == null || texto.isBlank()) return base;
        String alvo = normalizar(texto);
        return base.stream()
                .filter(r -> normalizar(r).contains(alvo))
                .collect(Collectors.toList());
    }

    private static String normalizar(String valor) {
        String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase().trim();
    }
}