package com.hhoa.kline.core.core.task.statemachine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.text.StringEscapeUtils;

public class Graph {
    public class Edge {
        Node from;
        Node to;
        String label;

        public Edge(Node from, Node to, String info) {
            this.from = from;
            this.to = to;
            this.label = info;
        }

        public boolean sameAs(Edge rhs) {
            if (this.from == rhs.from && this.to == rhs.to) {
                return true;
            }
            return false;
        }

        public Edge combine(Edge rhs) {
            String newlabel = this.label + "," + rhs.label;
            return new Edge(this.from, this.to, newlabel);
        }
    }

    public class Node {
        Graph parent;
        String id;
        List<Edge> ins;
        List<Edge> outs;

        public Node(String id) {
            this.id = id;
            this.parent = Graph.this;
            this.ins = new ArrayList<Graph.Edge>();
            this.outs = new ArrayList<Graph.Edge>();
        }

        public Graph getParent() {
            return parent;
        }

        public Node addEdge(Node to, String info) {
            Edge e = new Edge(this, to, info);
            outs.add(e);
            to.ins.add(e);
            return this;
        }

        public String getUniqueId() {
            return Graph.this.name + "." + id;
        }
    }

    private String name;
    private Graph parent;
    private Set<Graph.Node> nodes = new HashSet<Graph.Node>();
    private Set<Graph> subgraphs = new HashSet<Graph>();

    public Graph(String name, Graph parent) {
        this.name = name;
        this.parent = parent;
    }

    public Graph(String name) {
        this(name, null);
    }

    public Graph() {
        this("graph", null);
    }

    public String getName() {
        return name;
    }

    public Graph getParent() {
        return parent;
    }

    private Node newNode(String id) {
        Node ret = new Node(id);
        nodes.add(ret);
        return ret;
    }

    public Node getNode(String id) {
        for (Node node : nodes) {
            if (node.id.equals(id)) {
                return node;
            }
        }
        return newNode(id);
    }

    public Graph newSubGraph(String name) {
        Graph ret = new Graph(name, this);
        subgraphs.add(ret);
        return ret;
    }

    public void addSubGraph(Graph graph) {
        subgraphs.add(graph);
        graph.parent = this;
    }

    private static String wrapSafeString(String label) {
        if (label.indexOf(',') >= 0) {
            if (label.length() > 14) {
                label = label.replaceAll(",", ",\n");
            }
        }
        label = "\"" + StringEscapeUtils.escapeJava(label) + "\"";
        return label;
    }

    public String generateGraphViz(String indent) {
        StringBuilder sb = new StringBuilder();
        if (this.parent == null) {
            sb.append("digraph " + name + " {\n")
                    .append(
                            String.format(
                                    "graph [ label=%s, fontsize=24, fontname=Helvetica];%n",
                                    wrapSafeString(name)))
                    .append("node [fontsize=12, fontname=Helvetica];\n")
                    .append("edge [fontsize=9, fontcolor=blue, fontname=Arial];\n");
        } else {
            sb.append("subgraph cluster_" + name + " {\nlabel=\"" + name + "\"\n");
        }
        for (Graph g : subgraphs) {
            String ginfo = g.generateGraphViz(indent + "  ");
            sb.append(ginfo).append("\n");
        }
        for (Node n : nodes) {
            sb.append(
                    String.format(
                            "%s%s [ label = %s ];%n",
                            indent, wrapSafeString(n.getUniqueId()), n.id));
            List<Edge> combinedOuts = combineEdges(n.outs);
            for (Edge e : combinedOuts) {
                sb.append(
                        String.format(
                                "%s%s -> %s [ label = %s ];%n",
                                indent,
                                wrapSafeString(e.from.getUniqueId()),
                                wrapSafeString(e.to.getUniqueId()),
                                wrapSafeString(e.label)));
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    public String generateGraphViz() {
        return generateGraphViz("");
    }

    /** 生成 Mermaid 文本：无子图时用 {@code stateDiagram-v2}（状态语义、短状态名、较易读）；含子图时退回 {@code flowchart TD}。 */
    public String generateMermaid(String indent) {
        if (this.parent == null && this.subgraphs.isEmpty()) {
            return generateMermaidStateDiagramV2();
        }
        return generateMermaidFlowchart(indent);
    }

    public String generateMermaid() {
        return generateMermaid("");
    }

    /** 扁平状态图：{@code stateDiagram-v2}，节点用状态短名，边按源状态排序并分组空行。 */
    private String generateMermaidStateDiagramV2() {
        StringBuilder sb = new StringBuilder();
        sb.append("%% ").append(name).append("\n");
        sb.append("stateDiagram-v2\n");
        sb.append("  direction LR\n");
        List<Node> sorted = new ArrayList<>(nodes);
        sorted.sort(Comparator.comparing(n -> n.id));
        for (Node n : sorted) {
            List<Edge> combinedOuts = combineEdges(n.outs);
            if (combinedOuts.isEmpty()) {
                continue;
            }
            combinedOuts.sort(Comparator.comparing(e -> e.to.id));
            for (Edge e : combinedOuts) {
                String fromId = sanitizeMermaidId(e.from.id);
                String toId = sanitizeMermaidId(e.to.id);
                String lbl = escapeMermaidStateTransitionLabel(e.label);
                sb.append("  ").append(fromId).append(" --> ").append(toId).append(" : ");
                appendQuotedIfNeeded(sb, lbl);
                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String generateMermaidFlowchart(String indent) {
        StringBuilder sb = new StringBuilder();
        if (this.parent == null) {
            sb.append("%% ").append(name).append("\n");
            sb.append("flowchart TD\n");
        } else {
            String subgraphId = sanitizeMermaidId("cluster_" + name);
            sb.append(indent)
                    .append("subgraph ")
                    .append(subgraphId)
                    .append("[\"")
                    .append(escapeMermaidNodeText(name))
                    .append("\"]\n");
            sb.append(indent).append("  direction TB\n");
        }
        for (Graph g : subgraphs) {
            sb.append(g.generateMermaid(indent + "  "));
        }
        String lineIndent = (indent.isEmpty() && parent == null) ? "  " : indent;
        List<Node> sorted = new ArrayList<>(nodes);
        sorted.sort(Comparator.comparing(n -> n.id));
        for (Node n : sorted) {
            String mid = sanitizeMermaidId(n.getUniqueId());
            sb.append(lineIndent)
                    .append(mid)
                    .append("[\"")
                    .append(escapeMermaidNodeText(n.id))
                    .append("\"]\n");
            List<Edge> combinedOuts = combineEdges(n.outs);
            combinedOuts.sort(Comparator.comparing(e -> e.to.getUniqueId()));
            for (Edge e : combinedOuts) {
                String fromId = sanitizeMermaidId(e.from.getUniqueId());
                String toId = sanitizeMermaidId(e.to.getUniqueId());
                String edgeLabel = escapeMermaidFlowchartEdgeLabel(e.label);
                sb.append(lineIndent)
                        .append(fromId)
                        .append(" -->|")
                        .append(edgeLabel)
                        .append("| ")
                        .append(toId)
                        .append("\n");
            }
            sb.append("\n");
        }
        if (this.parent != null) {
            sb.append(indent).append("end\n");
        }
        return sb.toString();
    }

    private static void appendQuotedIfNeeded(StringBuilder sb, String lbl) {
        if (lbl.isEmpty()) {
            return;
        }
        if (mustQuoteStateTransitionLabel(lbl)) {
            sb.append('"').append(lbl.replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        } else {
            sb.append(lbl);
        }
    }

    private static boolean mustQuoteStateTransitionLabel(String lbl) {
        if (lbl.indexOf(':') >= 0 || lbl.indexOf(' ') >= 0) {
            return true;
        }
        return lbl.indexOf(',') >= 0;
    }

    private static String sanitizeMermaidId(String uniqueId) {
        String s = uniqueId.replaceAll("[^a-zA-Z0-9_]", "_");
        if (s.isEmpty()) {
            return "_node";
        }
        if (Character.isDigit(s.charAt(0))) {
            return "_" + s;
        }
        return s;
    }

    /** 节点显示文本（flowchart 方括号内）。 */
    private static String escapeMermaidNodeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "#quot;");
    }

    /** stateDiagram 边上说明（冒号后）。 */
    private static String escapeMermaidStateTransitionLabel(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\n", " ").replace("\r", "").replace(",", ", ");
    }

    /** flowchart 边上 |…| 内文本。 */
    private static String escapeMermaidFlowchartEdgeLabel(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("|", "/").replace("\n", " ").replace("\r", "").replace(",", ", ");
    }

    public void save(String filepath) throws IOException {
        try (OutputStreamWriter fout =
                new OutputStreamWriter(new FileOutputStream(filepath), Charset.forName("UTF-8"))) {
            fout.write(generateMermaid());
        }
    }

    public static List<Edge> combineEdges(List<Edge> edges) {
        List<Edge> ret = new ArrayList<Edge>();
        for (Edge edge : edges) {
            boolean found = false;
            for (int i = 0; i < ret.size(); i++) {
                Edge current = ret.get(i);
                if (edge.sameAs(current)) {
                    ret.set(i, current.combine(edge));
                    found = true;
                    break;
                }
            }
            if (!found) {
                ret.add(edge);
            }
        }
        return ret;
    }
}
