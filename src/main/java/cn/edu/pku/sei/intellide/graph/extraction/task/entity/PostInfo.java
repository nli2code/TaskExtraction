package cn.edu.pku.sei.intellide.graph.extraction.task.entity;

import cn.edu.pku.sei.structureAlignment.util.DoubleValue;
import cn.edu.pku.sei.structureAlignment.util.Matrix;
import org.neo4j.cypher.internal.frontend.v2_3.ast.functions.Str;
import java.util.*;

public class PostInfo {
    public String title;
    public String question;
    public String answer;
    public List<String> standardTasks;
    public List<String> candidatePhrases;
    public List<String> filteredPhrases;
    public HashMap<String, Matrix<DoubleValue>> resultMap;
    public HashSet<String> selectedPhrases;
    public Double precision;
    public Double recall;

    public PostInfo(String title, String question, String answer, List<String> standardTasks) {
        this.title = title;
        this.question = question;
        this.answer = answer;
        this.standardTasks = standardTasks;
        this.candidatePhrases = new ArrayList<>();
        this.filteredPhrases = new ArrayList<>();
        this.resultMap = new HashMap<>();
        this.selectedPhrases = new HashSet<>();
        this.precision = 0.0;
        this.recall = 0.0;
    }

}
