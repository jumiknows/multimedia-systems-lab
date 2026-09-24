package ca.ernestwong.multimedia.audio;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public final class HuffmanCodec {
    private HuffmanCodec() {
    }

    public static Model build(int[] symbols, int nominalBitsPerSymbol) {
        if (symbols == null || symbols.length == 0) {
            throw new IllegalArgumentException("At least one symbol is required");
        }
        if (nominalBitsPerSymbol <= 0) {
            throw new IllegalArgumentException("Nominal bits per symbol must be positive");
        }

        Map<Integer, Long> frequencies = new HashMap<Integer, Long>();
        for (int symbol : symbols) {
            Long current = frequencies.get(symbol);
            frequencies.put(symbol, current == null ? 1L : current + 1L);
        }

        PriorityQueue<Node> queue = new PriorityQueue<Node>(new NodeComparator());
        for (Map.Entry<Integer, Long> entry : frequencies.entrySet()) {
            queue.add(Node.leaf(entry.getKey(), entry.getValue()));
        }
        while (queue.size() > 1) {
            Node left = queue.remove();
            Node right = queue.remove();
            queue.add(Node.branch(left, right));
        }

        Node root = queue.remove();
        Map<Integer, String> codes = new HashMap<Integer, String>();
        assignCodes(root, "", codes);

        long encodedBits = 0;
        double entropy = 0.0;
        for (Map.Entry<Integer, Long> entry : frequencies.entrySet()) {
            encodedBits += entry.getValue() * codes.get(entry.getKey()).length();
            double probability = entry.getValue() / (double) symbols.length;
            entropy -= probability * log2(probability);
        }
        long originalBits = (long) symbols.length * nominalBitsPerSymbol;
        double averageCodeLength = encodedBits / (double) symbols.length;
        double ratio = encodedBits == 0 ? 1.0 : originalBits / (double) encodedBits;
        Statistics statistics = new Statistics(
            symbols.length,
            frequencies.size(),
            originalBits,
            encodedBits,
            entropy,
            averageCodeLength,
            ratio
        );
        return new Model(root, codes, frequencies, statistics);
    }

    public static EncodedData encode(int[] symbols, Model model) {
        if (symbols == null || model == null) {
            throw new IllegalArgumentException("Symbols and model are required");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int currentByte = 0;
        int bitsInCurrentByte = 0;
        int bitLength = 0;

        for (int symbol : symbols) {
            String code = model.codes().get(symbol);
            if (code == null) {
                throw new IllegalArgumentException("The model has no code for symbol " + symbol);
            }
            for (int index = 0; index < code.length(); index++) {
                currentByte <<= 1;
                if (code.charAt(index) == '1') {
                    currentByte |= 1;
                }
                bitsInCurrentByte++;
                bitLength++;
                if (bitsInCurrentByte == 8) {
                    output.write(currentByte);
                    currentByte = 0;
                    bitsInCurrentByte = 0;
                }
            }
        }

        if (bitsInCurrentByte > 0) {
            currentByte <<= 8 - bitsInCurrentByte;
            output.write(currentByte);
        }
        return new EncodedData(output.toByteArray(), bitLength, symbols.length);
    }

    public static int[] decode(EncodedData encoded, Model model) {
        if (encoded == null || model == null) {
            throw new IllegalArgumentException("Encoded data and model are required");
        }
        int[] result = new int[encoded.symbolCount()];
        if (model.root().isLeaf()) {
            for (int index = 0; index < result.length; index++) {
                result[index] = model.root().symbol();
            }
            return result;
        }

        Node current = model.root();
        int outputIndex = 0;
        byte[] bytes = encoded.bytes();
        for (int bitIndex = 0; bitIndex < encoded.bitLength(); bitIndex++) {
            int byteIndex = bitIndex / 8;
            int shift = 7 - (bitIndex % 8);
            int bit = (bytes[byteIndex] >>> shift) & 1;
            current = bit == 0 ? current.left() : current.right();
            if (current == null) {
                throw new IllegalArgumentException("The encoded bit stream does not match the model");
            }
            if (current.isLeaf()) {
                if (outputIndex >= result.length) {
                    throw new IllegalArgumentException("The encoded bit stream contains extra symbols");
                }
                result[outputIndex] = current.symbol();
                outputIndex++;
                current = model.root();
            }
        }
        if (outputIndex != result.length || current != model.root()) {
            throw new IllegalArgumentException("The encoded bit stream ended before all symbols were decoded");
        }
        return result;
    }

    public static List<SymbolFrequency> mostCommonSymbols(Model model, int limit) {
        List<SymbolFrequency> values = new ArrayList<SymbolFrequency>();
        for (Map.Entry<Integer, Long> entry : model.frequencies().entrySet()) {
            values.add(new SymbolFrequency(
                entry.getKey(),
                entry.getValue(),
                model.codes().get(entry.getKey())
            ));
        }
        Collections.sort(values, new Comparator<SymbolFrequency>() {
            @Override
            public int compare(SymbolFrequency first, SymbolFrequency second) {
                int frequencyOrder = Long.compare(second.frequency(), first.frequency());
                if (frequencyOrder != 0) {
                    return frequencyOrder;
                }
                return Integer.compare(first.symbol(), second.symbol());
            }
        });
        return values.subList(0, Math.min(Math.max(limit, 0), values.size()));
    }

    private static void assignCodes(Node node, String prefix, Map<Integer, String> codes) {
        if (node.isLeaf()) {
            codes.put(node.symbol(), prefix.isEmpty() ? "0" : prefix);
            return;
        }
        assignCodes(node.left(), prefix + "0", codes);
        assignCodes(node.right(), prefix + "1", codes);
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2.0);
    }

    public record Statistics(
        int symbolCount,
        int distinctSymbols,
        long originalBits,
        long encodedBits,
        double entropyBitsPerSymbol,
        double averageCodeLength,
        double compressionRatio
    ) {
    }

    public record EncodedData(byte[] bytes, int bitLength, int symbolCount) {
        public EncodedData {
            bytes = bytes.clone();
            if (bitLength < 0 || symbolCount < 0) {
                throw new IllegalArgumentException("Bit length and symbol count cannot be negative");
            }
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }
    }

    public record SymbolFrequency(int symbol, long frequency, String code) {
    }

    public static final class Model {
        private final Node root;
        private final Map<Integer, String> codes;
        private final Map<Integer, Long> frequencies;
        private final Statistics statistics;

        private Model(
            Node root,
            Map<Integer, String> codes,
            Map<Integer, Long> frequencies,
            Statistics statistics
        ) {
            this.root = root;
            this.codes = Collections.unmodifiableMap(new HashMap<Integer, String>(codes));
            this.frequencies = Collections.unmodifiableMap(new HashMap<Integer, Long>(frequencies));
            this.statistics = statistics;
        }

        private Node root() {
            return root;
        }

        public Map<Integer, String> codes() {
            return codes;
        }

        public Map<Integer, Long> frequencies() {
            return frequencies;
        }

        public Statistics statistics() {
            return statistics;
        }
    }

    private static final class Node {
        private final int symbol;
        private final long frequency;
        private final int smallestSymbol;
        private final Node left;
        private final Node right;

        private Node(int symbol, long frequency, int smallestSymbol, Node left, Node right) {
            this.symbol = symbol;
            this.frequency = frequency;
            this.smallestSymbol = smallestSymbol;
            this.left = left;
            this.right = right;
        }

        private static Node leaf(int symbol, long frequency) {
            return new Node(symbol, frequency, symbol, null, null);
        }

        private static Node branch(Node left, Node right) {
            return new Node(
                0,
                left.frequency + right.frequency,
                Math.min(left.smallestSymbol, right.smallestSymbol),
                left,
                right
            );
        }

        private boolean isLeaf() {
            return left == null && right == null;
        }

        private int symbol() {
            return symbol;
        }

        private Node left() {
            return left;
        }

        private Node right() {
            return right;
        }
    }

    private static final class NodeComparator implements Comparator<Node> {
        @Override
        public int compare(Node first, Node second) {
            int frequencyOrder = Long.compare(first.frequency, second.frequency);
            if (frequencyOrder != 0) {
                return frequencyOrder;
            }
            return Integer.compare(first.smallestSymbol, second.smallestSymbol);
        }
    }
}
