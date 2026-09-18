package br.com.fiap.VetSync.entity;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RacaCatalogo {

    private static final Map<EspecieCategoria, List<String>> RACAS = Map.ofEntries(
            Map.entry(EspecieCategoria.CAO, List.of(
                    "Fila Brasileiro", "Vira-lata Caramelo", "Vira-lata", "Rastreador Brasileiro",
                    "Fox Paulistinha (Terrier Brasileiro)", "Labrador Retriever", "Golden Retriever", "Bulldog Inglês",
                    "Cocker Spaniel Inglês", "Springer Spaniel Inglês", "Beagle", "Border Collie",
                    "Collie (Pastor Escocês)", "Old English Sheepdog (Bobtail)", "Yorkshire Terrier",
                    "Jack Russell Terrier", "Parson Russell Terrier", "Staffordshire Bull Terrier", "Bull Terrier",
                    "West Highland White Terrier", "Cairn Terrier", "Scottish Terrier", "Airedale Terrier", "Whippet",
                    "Greyhound", "Basset Hound", "Bloodhound (Cão de Santo Humberto)", "Cavalier King Charles Spaniel",
                    "King Charles Spaniel", "Corgi Galês de Pembroke", "Corgi Galês de Cardigan", "Bearded Collie",
                    "Shetland Sheepdog", "Setter Inglês", "Setter Irlandês", "Wolfhound Irlandês", "Terrier Irlandês",
                    "Kerry Blue Terrier", "Manchester Terrier", "Norfolk Terrier", "Norwich Terrier",
                    "Dandie Dinmont Terrier", "Bedlington Terrier", "Sealyham Terrier", "Skye Terrier",
                    "Welsh Terrier", "Lakeland Terrier", "Glen of Imaal Terrier", "Deerhound Escocês", "Otterhound",
                    "Boston Terrier", "Bulldog Francês", "Poodle (Caniche)", "Basset Artésien Normand",
                    "Basset Bleu de Gascogne", "Braque Francês", "Épagneul Breton", "Beauceron", "Pastor dos Pirineus",
                    "Berger Picard", "Dogue de Bordeaux", "Barbet Francês", "Briard", "Papillon",
                    "Grand Basset Griffon Vendéen", "Petit Basset Griffon Vendéen", "Pastor Alemão", "Rottweiler",
                    "Doberman", "Boxer", "Dachshund (Teckel)", "Schnauzer Miniatura", "Schnauzer Standard",
                    "Schnauzer Gigante", "Pinscher Miniatura", "Weimaraner", "Pointer Alemão de Pelo Curto",
                    "Spitz Alemão", "Leonberger", "Dogue Alemão (Great Dane)", "Hovawart", "Cane Corso",
                    "Maremmano-Abruzzese (Pastor de Maremma)", "Spinone Italiano", "Bracco Italiano",
                    "Volpino Italiano", "Lagotto Romagnolo", "Cirneco dell'Etna", "Bergamasco", "Mastim Espanhol",
                    "Podengo Ibicenco", "Galgo Espanhol", "Perro de Agua Español", "Pastor Catalão",
                    "Cão de Água Português", "Rafeiro do Alentejo", "Podengo Português", "Cão da Serra da Estrela",
                    "Cão de Castro Laboreiro", "Perdigueiro Português", "Pastor Belga Malinois",
                    "Pastor Belga Tervueren", "Pastor Belga Groenendael", "Pastor Belga Laekenois", "Keeshond",
                    "Schapendoes", "Kooikerhondje", "Stabyhoun", "Bouvier des Flandres", "São Bernardo",
                    "Boiadeiro de Berna (Bernese Mountain Dog)", "Appenzeller", "Entlebucher", "Vizsla", "Puli",
                    "Pumi", "Komondor", "Kuvasz", "Owczarek Polski Nizinny", "Chart Polski", "Borzoi", "Samoieda",
                    "Laika Siberiana", "Pastor do Cáucaso", "Terrier Preto Russo", "Tsvetnaya Bolonka",
                    "Husky Siberiano", "Elkhound Norueguês", "Lundehund Norueguês", "Buhund Norueguês",
                    "Spitz Finlandês", "Vallhund Sueco", "Broholmer", "Sarplaninac", "Pastor da Croácia", "Tornjak",
                    "Akita Inu", "Shiba Inu", "Shikoku", "Kai Ken", "Kishu", "Hokkaido", "Tosa Inu", "Spitz Japonês",
                    "Chow Chow", "Shar Pei", "Pequinês", "Shih Tzu", "Lhasa Apso", "Pug", "Cão Crestado Chinês",
                    "Jindo Coreano", "Sapsali Coreano", "Mastim Tibetano", "Terrier Tibetano", "Spaniel Tibetano",
                    "Ridgeback Tailandês", "Bangkaew Tailandês", "Aspin (Vira-lata Filipino)", "Rajapalayam",
                    "Chippiparai", "Kombai", "Mudhol Hound", "Rampur Greyhound", "Kanni", "Cão de Canaã",
                    "Galgo Afegão", "Pastor da Ásia Central (Alabai)", "Bankhar Mongol", "Kangal", "Akbash",
                    "Pastor de Anatólia", "Kintamani Balinês", "Boerboel", "Ridgeback da Rodésia", "Pharaoh Hound",
                    "Basenji", "Sloughi", "Azawakh", "Africanis", "American Staffordshire Terrier", "American Bully",
                    "Bulldog Americano", "Pit Bull Terrier Americano", "American Water Spaniel", "Malamute do Alasca",
                    "Chesapeake Bay Retriever", "Catahoula Leopard Dog", "Pastor Australiano", "Rat Terrier",
                    "Toy Fox Terrier", "Black and Tan Coonhound", "Bluetick Coonhound", "Plott Hound",
                    "Treeing Walker Coonhound", "Cão Esquimó Americano", "Terra Nova (Newfoundland)",
                    "Nova Scotia Duck Tolling Retriever", "Xoloitzcuintle", "Chihuahua", "Cão sem Pelo do Peru",
                    "Dogo Argentino", "Cimarrón Uruguayo", "Australian Cattle Dog (Blue Heeler)", "Kelpie Australiano",
                    "Dingo", "Silky Terrier Australiano", "Terrier Australiano"
            )),
            Map.entry(EspecieCategoria.GATO, List.of(
                    "Persa", "Siamês", "Maine Coon", "Angorá Turco", "Van Turco", "Sphynx", "Ragdoll",
                    "British Shorthair", "British Longhair", "Bengal", "Munchkin", "Vira-lata (SRD)",
                    "American Shorthair", "American Curl", "American Wirehair", "American Bobtail",
                    "American Ringtail", "Azul Russo (Russian Blue)", "Abissínio", "Somali",
                    "Birmanês (Sacred Birman)", "Burmês", "Bombay", "Tonquinês", "Oriental Shorthair",
                    "Oriental Longhair", "Javanês", "Balinês", "Colorpoint Shorthair", "Havana Brown", "Korat",
                    "Korn Ja", "Chartreux", "Exótico de Pelo Curto (Exotic Shorthair)", "Devon Rex", "Cornish Rex",
                    "Selkirk Rex", "LaPerm", "Manx", "Cymric", "Egyptian Mau",
                    "Norueguês da Floresta (Norwegian Forest Cat)", "Siberiano", "Ocicat", "Savannah", "Toyger",
                    "Bengal Selvagem (Asian Leopard hybrid)", "Ragamuffin", "Nebelung", "Peterbald",
                    "Donskoy (Sphynx Russo)", "Kurilian Bobtail", "Bobtail Japonês (Japanese Bobtail)", "Khao Manee",
                    "Singapura", "Serengeti", "Chausie", "Pixie-bob", "California Spangled", "Highlander",
                    "Scottish Fold", "Scottish Straight", "British Semi-longhair", "Cheetoh", "Sokoke",
                    "Chantilly-Tiffany", "Cyprus Aphrodite", "Kucing Malaysia", "Arabian Mau", "Aegean Cat (Grego)",
                    "Celtic Shorthair", "German Rex", "Ural Rex", "Minskin", "Dwelf", "Lykoi", "Elf Cat", "Skookum",
                    "Napoleon (Minuet)", "Suqutranese", "York Chocolate", "Snowshoe", "Oregon Rex (histórico)",
                    "Foldex"
            )),
            Map.entry(EspecieCategoria.EQUINO, List.of(
                    "Mangalarga Marchador", "Mangalarga Paulista", "Campolina", "Crioulo (Cavalo Crioulo)",
                    "Brasileiro de Hipismo", "Pantaneiro", "Pônei Brasileiro", "Nordestino", "Pônei Baixadeiro",
                    "Puro Sangue Inglês (Thoroughbred)", "Quarto de Milha", "Árabe (Puro Sangue Árabe)",
                    "Andaluz (PRE)", "Lusitano", "Appaloosa", "Pônei", "Clydesdale", "Shire", "Percheron",
                    "Frison (Friesian)", "Hanoveriano", "Trakehner", "Holsteiner", "Oldemburgo", "Westfaliano",
                    "Lipizzano", "Camargue", "Ardennes", "Breton", "Comtois", "Selle Français", "Anglo-Árabe",
                    "Konik Polski", "Haflinger", "Noriker", "Fjord (Fjord Norueguês)", "Gotland Russ",
                    "Islandês (Icelandic Horse)", "Shetland Pony", "Welsh Pony", "Dartmoor Pony", "Exmoor Pony",
                    "New Forest Pony", "Connemara Pony", "Highland Pony", "Fell Pony", "Dales Pony",
                    "Gypsy Vanner (Cob Cigano)", "Hackney", "Cleveland Bay", "Suffolk Punch", "Akhal-Teke",
                    "Don (Cavalo Don)", "Orlov Trotter", "Kabardin", "Mongol",
                    "Przewalski (Cavalo Selvagem da Mongólia)", "Marwari", "Kathiawari", "Manipuri Pony",
                    "Caspian Horse", "Miniatura Americano (American Miniature Horse)", "Mustang Americano", "Morgan",
                    "Tennessee Walking Horse", "Saddlebred Americano", "Paint Horse", "Pinto",
                    "Standardbred Americano", "Peruvian Paso", "Paso Fino", "Criollo Argentino", "Falabella",
                    "Australian Stock Horse", "Brumby"
            )),
            Map.entry(EspecieCategoria.BOVINO, List.of(
                    "Nelore", "Gir", "Guzerá", "Girolando", "Senepol", "Tabapuã", "Sindi", "Curraleiro Pé-Duro",
                    "Caracu", "Pantaneiro", "Mocho Nacional", "Holandês (Holstein)", "Jersey",
                    "Angus (Aberdeen Angus)", "Brahman", "Charolês (Charolais)", "Hereford", "Simental (Simmental)",
                    "Limousin", "Gelbvieh", "Chianina", "Marchigiana", "Piemontese", "Romagnola", "Maremmana",
                    "Podolica", "Normando (Normande)", "Salers", "Blonde d'Aquitaine", "Parthenaise", "Montbéliarde",
                    "Tarentaise", "Pardo Suíço (Brown Swiss)", "Fleckvieh", "Highland Escocês (Highland Cattle)",
                    "Galloway", "Belted Galloway", "Devon", "Sussex", "South Devon", "Dexter", "Kerry", "Shorthorn",
                    "Beefmaster", "Santa Gertrudis", "Brangus", "Braford", "Red Angus", "Texas Longhorn", "Wagyu",
                    "Watusi (Ankole-Watusi)", "N'Dama", "Boran", "Sahiwal", "Nguni", "Corriente", "Criollo",
                    "Pinzgauer", "Murray Grey"
            )),
            Map.entry(EspecieCategoria.SUINO, List.of(
                    "Large White", "Landrace", "Duroc", "Pietrain", "Piau", "Moura", "Caruncho", "Canastra",
                    "Nilo-Sul-Rio-Grandense", "Hampshire", "Berkshire", "Chester White", "Poland China", "Yorkshire",
                    "Tamworth", "Mangalitsa (Porco Lanudo Húngaro)", "Kunekune", "Meishan", "Iberico (Porco Ibérico)",
                    "Gloucestershire Old Spot", "Middle White", "Welsh Pig", "Vietnamita Barrigudo (Pot-bellied Pig)",
                    "Wild Boar (Javali Europeu)", "Bísaro", "Negro Canario", "Chato Murciano"
            )),
            Map.entry(EspecieCategoria.OVINO, List.of(
                    "Santa Inês", "Morada Nova", "Bergamácia", "Somalis Brasileira", "Crioula Lanada", "Dorper",
                    "Suffolk", "Texel", "Ile de France", "Merino Australiano", "Merino Espanhol (Rambouillet)",
                    "Hampshire Down", "Romney", "Corriedale", "Border Leicester", "Lincoln", "Cheviot",
                    "Blackface Escocesa", "Karakul", "Awassi", "Dorset", "Katahdin", "Poll Dorset", "White Dorper",
                    "Finn Sheep", "Jacob Sheep", "Shropshire", "Wensleydale", "Leicester Longwool", "Southdown",
                    "East Friesian", "Barbados Blackbelly", "Gulf Coast Native", "Navajo-Churro",
                    "Valais Blacknose (Nariz Preto de Valais)", "Herdwick", "Swaledale", "Soay"
            )),
            Map.entry(EspecieCategoria.CAPRINO, List.of(
                    "Boer", "Saanen", "Anglo-Nubiana", "Toggenburg", "Moxotó", "Canindé", "Marota", "Repartida",
                    "Azul (Caprino Azul)", "Alpina", "Murciano-Granadina", "Angorá (Cabra Angorá)", "Kalahari Red",
                    "Bhuj", "Jamnapari", "Beetal", "Black Bengal", "Kiko", "Nubian", "Pigmeu (Nigerian Dwarf)",
                    "La Mancha", "Oberhasli", "Golden Guernsey", "Damasco (Damascus)", "Barbari", "Sirohi"
            )),
            Map.entry(EspecieCategoria.AVE, List.of(
                    "Calopsita", "Periquito Australiano", "Canário Belga", "Canário-da-terra", "Agapornis",
                    "Papagaio Verdadeiro", "Araruna (Ararinha-azul)", "Arara Canindé", "Arara Vermelha", "Cacatua",
                    "Galinha D'angola", "Codorna", "Galinha Caipira", "Ring-neck (Periquito-de-colar)",
                    "Sabiá-laranjeira (protegido)", "Diamante de Gould", "Coleiro", "Curió", "Bicudo",
                    "Cacatua-da-crista-amarela", "Cacatua-rosa (Galah)", "Cacatua-negra", "Ecletus", "Eclectus",
                    "Ninfa (Cockatiel)", "Rosela", "Agapornis-roseicollis", "Agapornis-fischer",
                    "Agapornis-personatus", "Papagaio-do-congo (African Grey)", "Papagaio-de-cabeça-azul",
                    "Papagaio-charão", "Papagaio-de-peito-roxo", "Amazona-aestiva", "Tucano-toco",
                    "Tucano-de-bico-preto", "Arara-azul-de-lear", "Arara-azul-grande (Hyacinth)", "Conure Jandaia",
                    "Conure-sol (Sun Conure)", "Conure-de-cabeça-preta (Nanday)", "Periquitão (Nhandaia)", "Maritaca",
                    "Sagui-piri-piri (Periquito-de-encontro-amarelo)", "Canário Gloster", "Canário Yorkshire",
                    "Canário Timbrado Espanhol", "Canário Lizard", "Canário Raça Roller", "Faisão Dourado",
                    "Faisão Prateado", "Faisão Lady Amherst", "Pavão Comum", "Ganso-de-toulouse", "Ganso-branco",
                    "Pato-mudo (Pato-mosqueado)", "Marreco-de-pequim (Pekin Duck)", "Galinha Leghorn",
                    "Galinha Rhode Island Red", "Galinha Sussex", "Galinha Orpington", "Galinha Brahma",
                    "Galinha Cochin", "Galinha Silkie", "Galinha Polonesa", "Galinha Plymouth Rock",
                    "Galinha Wyandotte", "Peru (Meleagris)", "Avestruz", "Emu", "Ema", "Diamante-mandarim",
                    "Pombo-correio", "Pombo-magnum", "Rola-turca", "Mandarim (Diamante Mandarim)", "Estrilda",
                    "Bengalinho", "Papa-capim"
            )),
            Map.entry(EspecieCategoria.REPTIL, List.of(
                    "Jabuti Piranga", "Jabuti-tinga (Jabuti-do-cerrado)", "Tartaruga Tigre D'água",
                    "Tartaruga-de-orelha-vermelha (Red-eared slider)", "Iguana Verde", "Jiboia (Boa constrictor)",
                    "Corn Snake (Serpente-do-milho)", "Gecko Leopardo", "Cágado", "Cágado-de-barbicha",
                    "Tartaruga-das-patas-vermelhas", "Tartaruga-de-couro-mole (Softshell)",
                    "Tartaruga Sulcata (Sulcata Tortoise)", "Tartaruga Grega (Testudo graeca)",
                    "Tartaruga Estrela-indiana", "Tartaruga de Hermann", "Tartaruga-russa (Testudo horsfieldii)",
                    "Dragão Barbudo (Pogona)", "Camaleão-do-véu (Veiled Chameleon)",
                    "Camaleão-pantera (Panther Chameleon)", "Camaleão-de-jackson", "Teiú (Salvator merianae)",
                    "Monitor-do-nilo", "Monitor-das-savanas", "Dragão-de-komodo (não doméstico)",
                    "Píton-real (Ball Python)", "Píton-tapete (Carpet Python)", "Píton-reticulada",
                    "Píton-verde-arborícola", "Cascavel (não recomendada como pet)", "Serpente-rei (King Snake)",
                    "Serpente-do-leite (Milk Snake)", "Gecko-tokay", "Gecko-crestado (Crested Gecko)",
                    "Gecko-diurno-de-madagascar", "Anolis-verde", "Uromastyx", "Lagarto-de-vidro",
                    "Escorpião-da-cauda (não réptil, mas comum em terrários)", "Tartaruga-mapa (Map Turtle)",
                    "Tartaruga-almiscarada (Musk Turtle)", "Cobra-do-nariz-de-porco"
            )),
            Map.entry(EspecieCategoria.ANFIBIO, List.of(
                    "Perereca-verde", "Perereca-de-olhos-vermelhos (Agalychnis callidryas)", "Rã Touro (Bullfrog)",
                    "Salamandra", "Axolote (Ajolote)", "Sapo-de-chifre (Pacman Frog)", "Sapo-cururu", "Rã-de-vidro",
                    "Salamandra-tigre (Tiger Salamander)", "Tritão-de-crista (Crested Newt)",
                    "Rã-touro-africana (Pyxicephalus)", "Sapo-do-vinagre", "Perereca-de-são-tomé",
                    "Rã-arborícola-cinza", "Salamandra-de-fogo (Fire Salamander)", "Rã-venenosa-dourada (Dendrobates)",
                    "Rã-venenosa-azul", "Sapo-comum-europeu", "Anfiuma", "Proteus (Olm)"
            )),
            Map.entry(EspecieCategoria.PEIXE, List.of(
                    "Betta", "Kinguio (Goldfish)", "Carpa Koi", "Tetra Neon", "Acará Disco (Discus)", "Guppy", "Oscar",
                    "Molinésia", "Platy", "Espada (Xiphophorus)", "Barbo-sumatrano", "Corydoras",
                    "Cascudo (Ancistrus/Pleco)", "Ciclídeo Africano (Malawi/Tanganica)",
                    "Acará Bandeira (Angelfish/Escalar)", "Zebrafish (Paulistinha)", "Rasbora",
                    "Loach Palhaço (Clown Loach)", "Tetra Cardinal", "Tetra Limão", "Kissing Gourami",
                    "Gourami Azul", "Gourami Beta-splendens", "Peixe-palhaço (marinho)",
                    "Peixe-cirurgião (Tang, marinho)", "Peixe-anjo-marinho (Angelfish marinho)",
                    "Peixe-borboleta (marinho)", "Peixe-leão (Lionfish, marinho)", "Cavalo-marinho",
                    "Arraia-de-água-doce", "Arowana", "Peixe-lua-de-água-doce", "Bagre (Catfish)", "Lambari", "Piau",
                    "Tilápia Ornamental"
            )),
            Map.entry(EspecieCategoria.ROEDOR, List.of(
                    "Hamster Sírio", "Hamster Anão Russo (Campbell)", "Hamster Anão Winter White",
                    "Hamster Roborovski", "Hamster Chinês", "Porquinho da Índia (Cobaia) de Pelo Curto",
                    "Porquinho da Índia Abissínio", "Porquinho da Índia Peruano",
                    "Porquinho da Índia Skinny (sem pelo)", "Chinchila", "Rato Twister",
                    "Rato Fancy (Rattus norvegicus domesticus)", "Rato Dumbo", "Camundongo Fancy (Fancy Mouse)",
                    "Gerbil (Esquilo-da-mongólia)", "Esquilo-da-sibéria (Chipmunk)", "Degu",
                    "Esquilo-voador (Sugar Glider - marsupial, mas criado junto)", "Rato-do-deserto (Jerboa)",
                    "Cavia-selvagem", "Prairie Dog (Cão-da-pradaria)"
            )),
            Map.entry(EspecieCategoria.COELHO, List.of(
                    "Mini Lion (Leão)", "Holandês (Dutch Rabbit)", "Fuzzy Lop", "Angorá Inglês", "Angorá Francês",
                    "Angorá Gigante", "Rex", "Netherland Dwarf", "Ariete (Lop)", "Holland Lop", "Mini Rex",
                    "Flemish Giant (Gigante de Flandres)", "New Zealand White", "Californiano (Californian Rabbit)",
                    "Chinchilla Rabbit", "English Spot", "Havana", "Himalayan Rabbit", "Polish Rabbit",
                    "Britannia Petite", "Belgian Hare", "Silver Fox", "American Fuzzy Lop", "Cashmere Lop",
                    "Jersey Wooly", "Harlequin"
            )),
            Map.entry(EspecieCategoria.FURAO, List.of(
                    "Furão Standard (Cor Sable/Fitch)", "Furão Albino", "Furão Angorá", "Furão Champagne",
                    "Furão Cinnamon", "Furão Chocolate", "Furão Black Sable", "Furão Panda", "Furão Silver",
                    "Furão Black-eyed White"
            )),
            Map.entry(EspecieCategoria.PRIMATA, List.of(
                    "Sagui-de-tufo-branco", "Sagui-de-tufo-preto", "Macaco-prego", "Mico-leão-dourado",
                    "Mico-leão-preto", "Bugio (Alouatta)", "Macaco-aranha", "Macaco-da-noite (Aotus)", "Titi (Sauá)",
                    "Sagui-anão (Pygmy Marmoset)", "Macaco-esquilo (Saimiri)", "Uacari (Cacajao)", "Macaco Rhesus",
                    "Macaco Cynomolgus", "Babuíno", "Macaco-japonês", "Mandril", "Mangabey", "Colobo",
                    "Lêmure-de-cauda-anelada", "Lêmure-marrom", "Galago (Bush Baby)", "Loris-lento"
            )),
            Map.entry(EspecieCategoria.FELINO_SILVESTRE, List.of(
                    "Jaguatirica", "Gato-do-mato-pequeno (Leopardus tigrinus)", "Gato-do-mato-grande", "Gato-palheiro",
                    "Gato-mourisco (Puma yagouaroundi)", "Puma (Onça-parda)", "Onça-pintada", "Leão", "Tigre",
                    "Leopardo", "Guepardo (Chita)", "Lince-boreal", "Lince-ibérico", "Caracal", "Serval",
                    "Gato-do-deserto (Sand Cat)", "Gato-selvagem-europeu", "Gato-de-pallas", "Gato-leopardo-asiático",
                    "Gato-pescador", "Gato-marmoreado", "Gato-dourado-africano", "Ocelote", "Margay"
            )),
            Map.entry(EspecieCategoria.CANIDEO_SILVESTRE, List.of(
                    "Lobo-guará", "Raposa-do-campo (Lycalopex vetulus)", "Cachorro-do-mato (Cerdocyon thous)",
                    "Cachorro-vinagre (Speothos venaticus)", "Lobo-cinzento", "Coiote", "Chacal-dourado",
                    "Raposa-vermelha", "Raposa-do-ártico", "Fennec (Raposa-do-deserto)", "Raposa-cinzenta",
                    "Dingo (silvestre)", "Licaon (Cão-selvagem-africano)", "Raposa-das-pampas"
            )),
            Map.entry(EspecieCategoria.MARSUPIAL, List.of(
                    "Gambá-de-orelha-branca", "Cuíca", "Gambá-de-orelha-preta", "Canguru-vermelho", "Canguru-cinzento",
                    "Wallaby", "Coala", "Vombate (Wombat)", "Quokka", "Diabo-da-tasmânia", "Bandicoot",
                    "Petauro-do-açúcar (Sugar Glider)", "Opossum-da-virgínia"
            )),
            Map.entry(EspecieCategoria.SILVESTRE, List.of(
                    "Tamanduá-mirim", "Tamanduá-bandeira", "Paca", "Cutia", "Ouriço-cacheiro (Coendou)", "Preá",
                    "Capivara", "Tatu-galinha", "Tatu-canastra", "Quati", "Furão-europeu (Fuinha)", "Texugo",
                    "Ouriço-europeu (Hedgehog)", "Porco-espinho", "Esquilo (silvestre)", "Morcego-frugívoro", "Anta",
                    "Veado-catingueiro", "Javali", "Zorrilho (Gambá-fedorento)"
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