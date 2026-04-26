import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Usage:
 *   java -Xmx2g DblpParsingDemo <dblp.xml|dblp.xml.gz> <dblp.dtd> --task=1|2 [--limit=1000000]
 */
public class DblpParsingDemo {

    /**
     * Graphe orienté pour stocker les paires (A → B) et leurs compteurs.
     * Permet de construire un graphe filtré avec un seuil sur les arêtes.
     * Détecte les composantes fortement connexes et calcule les diamètres.
     */
    static class OrientedGraph {
        // Compteurs des paires (A → B): pour chaque auteur A, une Map de B vers le compteur
        private Map<String, Map<String, Integer>> edgeCounters;
        // Graphe filtré: adjacence pour le graphe avec seuil ≥ 6
        private Map<String, Set<String>> filteredGraph;
        private Set<String> allAuthors;

        OrientedGraph() {
            edgeCounters = new HashMap<>();
            filteredGraph = new HashMap<>();
            allAuthors = new HashSet<>();
        }

        /**
         * Incrémente le compteur pour la paire (A → B).
         * Appelé en ligne pendant le parsing.
         */
        void incrementEdge(String from, String to) {
            if (from.equals(to)) return; // Pas d'auto-boucles

            allAuthors.add(from);
            allAuthors.add(to);

            edgeCounters.putIfAbsent(from, new HashMap<>());
            Map<String, Integer> outgoing = edgeCounters.get(from);
            outgoing.put(to, outgoing.getOrDefault(to, 0) + 1);
        }

        /**
         * Construit le graphe filtré avec les arêtes ayant un compteur >= `threshold`.
         */
        void buildFilteredGraph(int threshold) {
            filteredGraph.clear();

            for (Map.Entry<String, Map<String, Integer>> entry : edgeCounters.entrySet()) {
                String from = entry.getKey();
                for (Map.Entry<String, Integer> edge : entry.getValue().entrySet()) {
                    if (edge.getValue() >= threshold) {
                        // Ajouter "from" et "to" seulement s'ils participent au graphe filtré
                        filteredGraph.computeIfAbsent(from, k -> new HashSet<>()).add(edge.getKey());
                        filteredGraph.computeIfAbsent(edge.getKey(), k -> new HashSet<>());
                    }
                }
            }
        }

        List<Set<String>> findAllStronglyConnectedComponents() {
            Map<String, Integer> index = new HashMap<>();
            Map<String, Integer> lowlink = new HashMap<>();
            Deque<String> stack = new ArrayDeque<>();
            Set<String> onStack = new HashSet<>();
            List<Set<String>> components = new ArrayList<>();
            int[] currentIndex = {0};

            for (String node : filteredGraph.keySet()) {
                if (!index.containsKey(node)) {
                    strongConnect(node, index, lowlink, stack, onStack, components, currentIndex);
                }
            }

            components.sort((a, b) -> Integer.compare(b.size(), a.size()));
            return components;
        }

        /**
         * Algorithme de Tarjan pour trouver les composantes fortement connexes.
         */
        private void strongConnect(
                String node,
                Map<String, Integer> index,
                Map<String, Integer> lowlink,
                Deque<String> stack,
                Set<String> onStack,
                List<Set<String>> components,
                int[] currentIndex
        ) {
            index.put(node, currentIndex[0]);
            lowlink.put(node, currentIndex[0]);
            currentIndex[0]++;
            stack.push(node);
            onStack.add(node);

            for (String neighbor : filteredGraph.getOrDefault(node, Collections.emptySet())) {
                if (!index.containsKey(neighbor)) {
                    strongConnect(neighbor, index, lowlink, stack, onStack, components, currentIndex);
                    lowlink.put(node, Math.min(lowlink.get(node), lowlink.get(neighbor)));
                } else if (onStack.contains(neighbor)) {
                    lowlink.put(node, Math.min(lowlink.get(node), index.get(neighbor)));
                }
            }

            if (lowlink.get(node).equals(index.get(node))) {
                Set<String> component = new HashSet<>();
                String w;
                do {
                    w = stack.pop();
                    onStack.remove(w);
                    component.add(w);
                } while (!w.equals(node));
                components.add(component);
            }
        }

        /**
         * Calcule le diamètre d'une composante (plus long plus court chemin).
         * Limitation: distance calculée en restant dans la sous-communauté.
         */
        int computeDiameter(Set<String> component) {
            if (component.size() <= 1) return 0;

            int maxDistance = 0;

            // Pour chaque paire (u, v) dans la composante
            List<String> nodes = new ArrayList<>(component);
            for (int i = 0; i < nodes.size(); i++) {
                // BFS depuis nodes.get(i)
                int[] distances = bfsDistance(nodes.get(i), component);
                for (int d : distances) {
                    if (d > maxDistance && d != Integer.MAX_VALUE) {
                        maxDistance = d;
                    }
                }
            }

            return maxDistance;
        }

        /**
         * BFS pour calculer les distances depuis un nœud source,
         * en restant dans la sous-communauté.
         */
        private int[] bfsDistance(String source, Set<String> component) {
            Map<String, Integer> distances = new HashMap<>();
            Queue<String> queue = new LinkedList<>();

            queue.offer(source);
            distances.put(source, 0);

            while (!queue.isEmpty()) {
                String u = queue.poll();
                for (String v : filteredGraph.getOrDefault(u, new HashSet<>())) {
                    if (component.contains(v) && !distances.containsKey(v)) {
                        distances.put(v, distances.get(u) + 1);
                        queue.offer(v);
                    }
                }
            }

            // Convertir en tableau
            List<String> nodes = new ArrayList<>(component);
            int[] result = new int[nodes.size()];
            for (int i = 0; i < nodes.size(); i++) {
                result[i] = distances.getOrDefault(nodes.get(i), Integer.MAX_VALUE);
            }
            return result;
        }

    }

    /**
     * Structure Union-Find pour maintenir les communautés d'auteurs en ligne.
     * (Gardée pour compatibilité avec d'autres tâches)
     */
    static class UnionFind {
        private Map<String, String> parent;
        private Map<String, Integer> rank;
        private Map<String, Integer> size;

        UnionFind() {
            parent = new HashMap<>();
            rank = new HashMap<>();
            size = new HashMap<>();
        }

        /**
         * Trouve le représentant (racine) de l'ensemble contenant x,
         * avec compression de chemin.
         */
        String find(String x) {
            if (!parent.containsKey(x)) {
                parent.put(x, x);
                rank.put(x, 0);
                size.put(x, 1);
            }

            if (!parent.get(x).equals(x)) {
                parent.put(x, find(parent.get(x))); // compression de chemin
            }
            return parent.get(x);
        }

        /**
         * Fusionne les ensembles contenant x et y.
         * Retourne true si l'union a eu lieu, false s'ils étaient déjà dans le même ensemble.
         */
        boolean union(String x, String y) {
            String rx = find(x);
            String ry = find(y);

            if (rx.equals(ry)) {
                return false;
            }

            if (rank.get(rx) < rank.get(ry)) {
                parent.put(rx, ry);
                size.put(ry, size.get(ry) + size.get(rx));
            } else if (rank.get(rx) > rank.get(ry)) {
                parent.put(ry, rx);
                size.put(rx, size.get(rx) + size.get(ry));
            } else {
                parent.put(ry, rx);
                size.put(rx, size.get(rx) + size.get(ry));
                rank.put(rx, rank.get(rx) + 1);
            }
            return true;
        }

        /**
         * Retourne la taille de la communauté contenant x.
         */
        int getSize(String x) {
            String rx = find(x);
            return size.get(rx);
        }

        /**
         * Retourne le nombre de communautés distinctes.
         */
        int getNumCommunities() {
            Set<String> roots = new HashSet<>();
            for (String author : parent.keySet()) {
                roots.add(find(author));
            }
            return roots.size();
        }

        /**
         * Retourne les 10 plus grandes communautés.
         */
        List<Integer> getTop10Sizes() {
            Map<String, Integer> communitySize = new HashMap<>();
            for (String author : parent.keySet()) {
                String root = find(author);
                communitySize.put(root, size.get(root));
            }

            return communitySize.values().stream()
                    .sorted(Collections.reverseOrder())
                    .limit(10)
                    .toList();
        }

        /**
         * Retourne un histogramme des tailles de communautés.
         */
        Map<Integer, Integer> getSizeHistogram() {
            Map<String, Integer> communitySize = new HashMap<>();
            for (String author : parent.keySet()) {
                String root = find(author);
                communitySize.put(root, size.get(root));
            }

            Map<Integer, Integer> histogram = new TreeMap<>();
            for (Integer community : communitySize.values()) {
                histogram.put(community, histogram.getOrDefault(community, 0) + 1);
            }
            return histogram;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("""
                Usage:
                  java -Xmx2g DblpParsingDemo <dblp.xml|dblp.xml.gz> <dblp.dtd> --task=1|2 [--limit=1000000]

                Exemple:
                  java -Xmx2g DblpParsingDemo dblp.xml.gz dblp.dtd --task=2 --limit=500000
                """);
            System.exit(2);
        }

        Path xmlPath = Paths.get(args[0]);
        Path dtdPath = Paths.get(args[1]);

        String task = "2";
        long limit = Long.MAX_VALUE; // optionnel: s'arrêter après N publications
        for (int i = 2; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--limit")) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Argument --limit nécessite une valeur");
                }
                limit = Long.parseLong(args[++i]);
            } else if (a.startsWith("--limit=")) {
                limit = Long.parseLong(a.substring("--limit=".length()));
            } else if (a.equals("--task")) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Argument --task nécessite une valeur");
                }
                task = args[++i];
                if (!task.equals("1") && !task.equals("2")) {
                    throw new IllegalArgumentException("Argument --task doit être 1 ou 2");
                }
            } else if (a.startsWith("--task=")) {
                task = a.substring("--task=".length());
                if (!task.equals("1") && !task.equals("2")) {
                    throw new IllegalArgumentException("Argument --task doit être 1 ou 2");
                }
            } else {
                throw new IllegalArgumentException("Argument inconnu: " + a);
            }
        }

        if (!Files.exists(xmlPath)) throw new FileNotFoundException("XML introuvable: " + xmlPath);
        if (!Files.exists(dtdPath)) throw new FileNotFoundException("DTD introuvable: " + dtdPath);

        // Paramètres d'expansion d'entités XML
        System.setProperty("jdk.xml.entityExpansionLimit", "0");
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");
        System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "0");
        System.setProperty("jdk.xml.maxParameterEntitySizeLimit", "0");

        System.out.println("XML: " + xmlPath);
        System.out.println("DTD: " + dtdPath);
        System.out.println("Task: " + task);
        if (limit != Long.MAX_VALUE) System.out.println("Limit: " + limit);

        if (task.equals("1")) {
            runTask1(xmlPath, dtdPath, limit);
        } else {
            runTask2(xmlPath, dtdPath, limit);
        }
    }

    /**
     * Lance le script Python pour générer le PNG de l'histogramme.
     * Cherche automatiquement python3 ou python selon le système.
     */
    static void runPythonHistogram(Path csvPath, Path pngPath) throws IOException, InterruptedException {
        // Le script Python se trouve dans le répertoire parent du dossier de sortie data_out
        Path scriptPath = csvPath.toAbsolutePath().getParent().getParent().resolve("generate_histogram.py").normalize();

        if (!Files.exists(scriptPath)) {
            throw new IOException("Script Python introuvable: " + scriptPath);
        }

        // Déterminer la commande Python disponible
        String pythonCmd = findPythonCommand();
        if (pythonCmd == null) {
            throw new IOException("Aucune commande Python trouvée (python3 / python)");
        }

        Path pythonPath = Path.of(pythonCmd);
        if (!pythonPath.isAbsolute()) {
            pythonPath = Path.of(System.getProperty("user.dir")).resolve(pythonPath).normalize();
        }
        String resolvedPython = pythonPath.toString();

        ProcessBuilder pb = new ProcessBuilder(resolvedPython, scriptPath.toString(), csvPath.toString(), pngPath.toString());
        pb.redirectErrorStream(true); // merge stdout + stderr
        pb.directory(csvPath.getParent().toFile());

        System.out.println("[INFO] Lancement: " + pythonCmd + " " + scriptPath + " " + csvPath);

        Process process = pb.start();

        // Lire la sortie du process en temps réel
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[Python] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Le script Python a échoué avec le code: " + exitCode);
        }

        if (!Files.exists(pngPath)) {
            throw new IOException("Le PNG n'a pas été généré: " + pngPath);
        }

        System.out.printf("[OK] Histogramme PNG généré: %s%n", pngPath);
    }

    static void runTask1(Path xmlPath, Path dtdPath, long limit) throws Exception {
        System.out.println("=== Tâche 1 : communautés de co-publication (graphe non orienté) ===\n");
        long pubCount = 0;
        long reportEvery = 100_000;
        UnionFind uf = new UnionFind();

        try (DblpPublicationGenerator gen = new DblpPublicationGenerator(xmlPath, dtdPath, 256)) {
            while (pubCount < limit) {
                Optional<DblpPublicationGenerator.Publication> opt = gen.nextPublication();
                if (opt.isEmpty()) break;

                pubCount++;
                DblpPublicationGenerator.Publication p = opt.get();
                List<String> authors = p.authors;
                if (authors == null || authors.isEmpty()) {
                    continue;
                }

                Set<String> uniqueAuthors = new LinkedHashSet<>(authors);
                if (uniqueAuthors.size() == 1) {
                    uf.find(uniqueAuthors.iterator().next());
                } else {
                    Iterator<String> it = uniqueAuthors.iterator();
                    String first = it.next();
                    uf.find(first);
                    while (it.hasNext()) {
                        uf.union(first, it.next());
                    }
                }

                if (pubCount % reportEvery == 0) {
                    System.out.printf(Locale.US, "Publication #%,d traitée%n", pubCount);
                    List<Integer> top10 = uf.getTop10Sizes();
                    System.out.printf("  Communautés: %,d, Top 10 tailles: %s%n",
                            uf.getNumCommunities(), top10);
                }
            }
        }

        System.out.println("\n=== Fin du parsing ===");
        System.out.printf(Locale.US, "Total publications traitées: %,d%n", pubCount);
        System.out.printf(Locale.US, "Nombre de communautés: %,d%n", uf.getNumCommunities());
        System.out.printf(Locale.US, "Top 10 tailles: %s%n", uf.getTop10Sizes());
        generateTask1Report(uf, xmlPath);
    }

    static Path getOutputDir(Path xmlPath) throws IOException {
        Path absoluteXmlPath = xmlPath.toAbsolutePath();
        Path outputDir = absoluteXmlPath.getParent();
        if (outputDir == null) {
            outputDir = Paths.get(".").toAbsolutePath();
        }
        Path dataOut = outputDir.resolve("data_out");
        if (!Files.exists(dataOut)) {
            Files.createDirectories(dataOut);
        }
        return dataOut;
    }

    static void runTask2(Path xmlPath, Path dtdPath, long limit) throws Exception {
        System.out.println("=== Tâche 2 : graphe orienté avec seuil >= 6 ===\n");
        long pubCount = 0;
        long reportEvery = 100_000;
        OrientedGraph graph = new OrientedGraph();

        try (DblpPublicationGenerator gen = new DblpPublicationGenerator(xmlPath, dtdPath, 256)) {
            while (pubCount < limit) {
                Optional<DblpPublicationGenerator.Publication> opt = gen.nextPublication();
                if (opt.isEmpty()) break;

                pubCount++;
                DblpPublicationGenerator.Publication p = opt.get();

                List<String> authors = p.authors;
                if (authors == null || authors.size() < 2) {
                    continue;
                }

                String firstAuthor = authors.get(0);
                for (int i = 1; i < authors.size(); i++) {
                    graph.incrementEdge(firstAuthor, authors.get(i));
                }

                if (pubCount % reportEvery == 0) {
                    System.out.printf(Locale.US, "Publication #%,d traitée%n", pubCount);
                }
            }
        }

        System.out.println("\n=== Fin du parsing ===");
        System.out.printf(Locale.US, "Total publications traitées: %,d%n", pubCount);

        System.out.println("\nConstruction du graphe filtré (seuil >= 6)...");
        graph.buildFilteredGraph(6);

        System.out.println("Détection des composantes fortement connexes...");
        List<Set<String>> allComponents = graph.findAllStronglyConnectedComponents();
        int totalCommunities = allComponents.size();
        List<Set<String>> top10Communities = allComponents.stream().limit(10).collect(Collectors.toList());

        System.out.printf("Nombre total de CFC: %d%n", totalCommunities);
        generateCommunityReport(graph, top10Communities, xmlPath);
    }

    static void generateTask1Report(UnionFind uf, Path xmlPath) throws IOException {
        Path outputDir = getOutputDir(xmlPath);

        Map<Integer, Integer> sizeHistogram = uf.getSizeHistogram();
        Path csvPath = outputDir.resolve("communautes_tache1_data.csv");
        Path txtPath = outputDir.resolve("communautes_tache1_histogram.txt");

        if (Files.exists(csvPath)) {
            Files.delete(csvPath);
            System.out.println("[INFO] Suppression: " + csvPath);
        }
        if (Files.exists(txtPath)) {
            Files.delete(txtPath);
            System.out.println("[INFO] Suppression: " + txtPath);
        }

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(csvPath))) {
            writer.println("Taille de communauté,Fréquence");
            for (Map.Entry<Integer, Integer> entry : sizeHistogram.entrySet()) {
                writer.printf("%d,%d%n", entry.getKey(), entry.getValue());
            }
        }
        System.out.printf("[OK] CSV généré: %s%n", csvPath);

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(txtPath))) {
            writer.println("=======================================================");
            writer.println("   HISTOGRAMME DES TAILLES DE COMMUNAUTÉS (Tâche 1)");
            writer.println("=======================================================");
            writer.println();
            writer.println("Taille de communauté, Fréquence");
            for (Map.Entry<Integer, Integer> entry : sizeHistogram.entrySet()) {
                writer.printf("%d -> %d%n", entry.getKey(), entry.getValue());
            }
            writer.println();
            writer.printf("Nombre total de communautés : %,d%n", uf.getNumCommunities());
            writer.printf("Top 10 tailles : %s%n", uf.getTop10Sizes());
        }
        System.out.printf("[OK] Rapport généré: %s%n", txtPath);

        Path pngPath = outputDir.resolve("communautes_tache1_histogram.png");
        try {
            runPythonHistogram(csvPath, pngPath);
        } catch (Exception e) {
            System.err.println("[ERREUR] Impossible de générer le PNG: " + e.getMessage());
        }
    }

    /**
     * Cherche la commande Python disponible sur le système.
     * Essaie python3 en premier, puis python.
     */
    private static String findPythonCommand() {
        for (String cmd : new String[]{"venv/bin/python3", "python3", "python"}) {
            try {
                Process p = new ProcessBuilder(cmd, "--version")
                        .redirectErrorStream(true)
                        .start();
                int exit = p.waitFor();
                if (exit == 0) {
                    return cmd;
                }
            } catch (IOException | InterruptedException ignored) {

            }
        }
        return null;
    }

    /**
     * Génère un rapport sur les 10 plus grandes communautés:
     * - Histogramme des tailles
     * - Détails (taille, diamètre, auteurs)
     */
    static void generateCommunityReport(OrientedGraph graph, List<Set<String>> top10Communities, Path xmlPath) throws Exception {
        Path outputDir = getOutputDir(xmlPath);

        // === Générer l'histogramme des tailles de communautés ===
        Map<Integer, Integer> sizeHistogram = new TreeMap<>();
        for (Set<String> community : top10Communities) {
            int size = community.size();
            sizeHistogram.put(size, sizeHistogram.getOrDefault(size, 0) + 1);
        }

        Path csvPath = outputDir.resolve("communautes_tache2_data.csv");
        Path txtPath = outputDir.resolve("communautes_tache2_histogram.txt");

        // Supprimer les fichiers existants
        if (Files.exists(csvPath)) {
            Files.delete(csvPath);
            System.out.println("[INFO] Suppression: " + csvPath);
        }
        if (Files.exists(txtPath)) {
            Files.delete(txtPath);
            System.out.println("[INFO] Suppression: " + txtPath);
        }

        // Exporter en CSV
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(csvPath))) {
            writer.println("Taille de communauté,Fréquence");
            for (Map.Entry<Integer, Integer> entry : sizeHistogram.entrySet()) {
                writer.printf("%d,%d%n", entry.getKey(), entry.getValue());
            }
        }
        System.out.printf("[OK] CSV généré: %s%n", csvPath);

        // === Rapport textuel détaillé ===
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(txtPath))) {
            writer.println("=======================================================");
            writer.println("   RAPPORT DES 10 PLUS GRANDES COMMUNAUTÉS");
            writer.println("   (Graphe orienté avec seuil ≥ 6 publications)");
            writer.println("=======================================================");
            writer.println();

            for (int i = 0; i < top10Communities.size(); i++) {
                Set<String> community = top10Communities.get(i);
                int size = community.size();
                int diameter = graph.computeDiameter(community);

                writer.println("---[COMMUNAUTÉ " + (i + 1) + "]---");
                writer.printf("Taille: %d%n", size);
                writer.printf("Diamètre: %d%n", diameter);
                writer.println("Auteurs:");

                // Afficher les auteurs triés alphabétiquement
                List<String> sortedAuthors = community.stream()
                        .sorted()
                        .collect(Collectors.toList());
                for (String author : sortedAuthors) {
                    writer.printf("  - %s%n", author);
                }
                writer.println();
            }

            writer.println("=======================================================");
            writer.println("   RÉSUMÉ");
            writer.println("=======================================================");
            writer.printf("CFC: %d%n", graph.findAllStronglyConnectedComponents().size());
            int totalAuthors = top10Communities.stream().mapToInt(Set::size).sum();
            writer.printf("Nombre total d'auteurs dans ces 10 communautés: %d%n", totalAuthors);
            int maxDiameter = top10Communities.stream()
                    .mapToInt(graph::computeDiameter)
                    .max()
                    .orElse(0);
            writer.printf("Diamètre maximal: %d%n", maxDiameter);
        }
        System.out.printf("[OK] Rapport généré: %s%n", txtPath);

        // Générer le PNG de l'histogramme via le script Python
        Path pngPath = outputDir.resolve("communautes_tache2_histogram.png");
        try {
            runPythonHistogram(csvPath, pngPath);
        } catch (Exception e) {
            System.err.println("[ERREUR] Impossible de générer le PNG: " + e.getMessage());
        }

        // Afficher aussi à la console
        System.out.println();
        System.out.println("=======================================================");
        System.out.println("   RÉSUMÉ DES 10 PLUS GRANDES COMMUNAUTÉS");
        System.out.println("=======================================================");
        for (int i = 0; i < top10Communities.size(); i++) {
            Set<String> community = top10Communities.get(i);
            int diameter = graph.computeDiameter(community);
            System.out.printf("Communauté %2d: Taille=%4d, Diamètre=%2d%n",
                    i + 1, community.size(), diameter);
        }
    }
}