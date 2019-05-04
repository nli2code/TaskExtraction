package cn.edu.pku.sei.intellide.graph.extraction.task;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import cn.edu.pku.sei.intellide.graph.extraction.KnowledgeExtractor;
import cn.edu.pku.sei.intellide.graph.extraction.qa.StackOverflowExtractor;

import cn.edu.pku.sei.intellide.graph.extraction.task.entity.*;
import cn.edu.pku.sei.intellide.graph.extraction.task.graph.FrequentGraphMiner;
import cn.edu.pku.sei.intellide.graph.extraction.task.graph.GraphBuilder;
import cn.edu.pku.sei.intellide.graph.extraction.task.parser.*;
import cn.edu.pku.sei.structureAlignment.util.DoubleValue;
import cn.edu.pku.sei.structureAlignment.util.Matrix;
import cn.edu.pku.sei.summarization.Summarization;
import de.parsemis.graph.Graph;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;

import javafx.geometry.Pos;
import org.apache.commons.text.similarity.CosineSimilarity;
import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.eclipse.jdt.core.dom.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import scala.Int;

public class TaskExtractor extends KnowledgeExtractor{
    public static final Label TASK = Label.label("Task");
    public static final String TASK_TEXT = "text";
    public static final String TASK_LEVEL = "level";
    public static final Integer MIN_PROOFSCORE = -10;
    public static final Integer MAX_NUMBER_OF_POSTS = 5000;
    public static final RelationshipType FUNCTIONALFEATURE = RelationshipType.withName("functionalFeature");
    public static final RelationshipType GENERALIZATION = RelationshipType.withName("generalization");

    public FrequentGraphMiner miner = new FrequentGraphMiner();
    public GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(new File("E:/graphs/graph-POI"));

    public HashMap<String, Integer> phraseHashMap = new HashMap<>();

    public static Integer postWithCode = 0;
    public static Integer phraseMatchCode = 0;

    public static Integer postCount = 0;
    public static Integer notNullResult = 0;
    public static Integer notNullMatrix = 0;
    public static Integer allStandardPhrases = 0;
    public static Integer allStandardPhrasesWithoutNullMatrix = 0;
    public static Double sumPrecision = 0.0;
    public static Double sumRecall = 0.0;

    @Override
    public void extraction(){
        Map<Long, String> questionMap = getNodesFromNeo4j(StackOverflowExtractor.QUESTION);
        Map<Long, String> answerMap = getNodesFromNeo4j(StackOverflowExtractor.ANSWER);

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

//        System.out.println("extract task of level 2");
//        for (Map.Entry<Long, String> entry : questionMap.entrySet()) {
//            if (phraseHashMap.size() > MAX_NUMBER_OF_POSTS)
//                break;
//            extractPhrase(entry.getKey(), pipeline);
//        }
//        for (Map.Entry<Long, String> entry : answerMap.entrySet()) {
//            if (phraseHashMap.size() > MAX_NUMBER_OF_POSTS * 2)
//                break;
//            extractPhrase(entry.getKey(), pipeline);
//        }

        //PdfFile
//        extractPhrase(29679L, pipeline);

        for (Integer curSheet=0; curSheet<10; curSheet++)
            parsePostsFromExcel("E:\\data_for_functional_feature\\poi\\poi1-10.xls", curSheet, pipeline);
        for (Integer curSheet=10; curSheet<20; curSheet++)
            parsePostsFromExcel("E:\\data_for_functional_feature\\poi\\poi11-20.xls", curSheet, pipeline);
        System.out.println("postCount: " + postCount);
        System.out.println("notNullResult: " + notNullResult);
        System.out.println("notNullMatrix: " + notNullMatrix);
        System.out.println("sumPrecision: " + sumPrecision);
        System.out.println("sumRecall: " + sumRecall);
        System.out.println("allStandardPhrases: " + allStandardPhrases);
        System.out.println("allStandardPhrasesWithnotNullMatrix: " + allStandardPhrasesWithoutNullMatrix);

//        System.out.println("extract task of level 1");
//        extractTask(questionMap, answerMap, pipeline);

//        printTaskOfLevel2("phraseHashMap.txt");
        System.out.println("end");
    }

    public Map<Long,String> getNodesFromNeo4j(Label label) {
        Map<Long,String> textMap = new LinkedHashMap<Long,String>();
//        GraphDatabaseService db = this.getDb();
        try(Transaction tx = db.beginTx()){
            ResourceIterator<Node> nodes = db.findNodes(label);
            while(nodes.hasNext()){
                Node node = nodes.next();
                String text = "";
                if (label == StackOverflowExtractor.QUESTION || label == StackOverflowExtractor.ANSWER) {
                    text = (String)node.getProperty("body");
                }
                if (label == TaskExtractor.TASK) {
                    String level = (String)node.getProperty(TASK_LEVEL);
                    if (level.equals("2")) {
                        text = (String)node.getProperty(TaskExtractor.TASK_TEXT);
                    }
                }
                if(!text.equals("")){
                    textMap.put(node.getId(),text);
                }
            }
            tx.success();
        }
        return textMap;
    }

    public Double getPhraseSimilarity(String standard, String result) {
        HashSet<String> standardSet = new HashSet<>(Arrays.asList(standard.toLowerCase().split(" ")));
        HashSet<String> resultSet = new HashSet<>(Arrays.asList(result.toLowerCase().split(" ")));

        Double sumSimilarity = 0.0;
        for (String standardWord: standardSet) {
            Double maxSimilarity = 0.0;
            for (String resultWord: resultSet) {
                if (standardWord.compareTo(resultWord) == 0)
                    maxSimilarity = Math.max(maxSimilarity, 1.0);
                else if (standardWord.length()>=5 && resultWord.length()>=5) {
                    if (standardWord.substring(0,5).compareTo(resultWord.substring(0,5)) == 0)
                        maxSimilarity = Math.max(maxSimilarity, 0.9);
                }
            }
            sumSimilarity += maxSimilarity;
        }
        BigDecimal bigDecimal = new BigDecimal(sumSimilarity / standardSet.size());
        Double similarity = bigDecimal.setScale(5,   BigDecimal.ROUND_HALF_UP).doubleValue();

        return similarity;
    }

     public PostInfo getPrecisionAndRecall(PostInfo post) {
//        Double sumSimilarity = 0.0;
//        for (String standard: post.standardTasks) {
//            Double maxSimilarity = 0.0;
//            for (String result: post.selectedPhrases) {
//                maxSimilarity = Math.max(maxSimilarity, getPhraseSimilarity(standard, result));
//            }
//            sumSimilarity += maxSimilarity;
//        }
//        BigDecimal bigDecimal = new BigDecimal(sumSimilarity / post.standardTasks.size());
//        Double precision = bigDecimal.setScale(3,BigDecimal.ROUND_HALF_UP).doubleValue();

         HashSet<String> precisionSet = new HashSet<>();
         HashSet<String> recallSet = new HashSet<>();

         for (String standard: post.standardTasks) {
            Double maxSimilarity = 0.0;
            for (String result: post.selectedPhrases) {
                Double phraseSimilarity = getPhraseSimilarity(standard, result);
                if (phraseSimilarity >= 0.5)
                    precisionSet.add(result);
                maxSimilarity = Math.max(maxSimilarity, phraseSimilarity);
            }
            if (maxSimilarity >= 0.5)
                recallSet.add(standard);
         }
         if (post.selectedPhrases.size() > 0)
            post.precision = 1.0 * precisionSet.size() / post.selectedPhrases.size();
         if (post.standardTasks.size() > 0)
            post.recall = 1.0 * recallSet.size() / post.standardTasks.size();

         return post;
    }

    public void parsePostsFromExcel(String filePath, Integer curSheet, StanfordCoreNLP pipeline) {
        File excelFile = new File(filePath);
        InputStream inputStream;
        HSSFWorkbook workbook;
        OutputStream outputStream;
        try {
            inputStream = new FileInputStream(excelFile);
            workbook = new HSSFWorkbook(inputStream);
            if (inputStream != null)
                inputStream.close();
            outputStream = new FileOutputStream(excelFile);
            HSSFSheet sheet = workbook.getSheetAt(curSheet);
            sheet.getRow(0).createCell(5).setCellValue("candidatePhrases");
            sheet.getRow(0).createCell(6).setCellValue("filteredPhrases");
            sheet.getRow(0).createCell(7).setCellValue("selectedPhrases");
            sheet.getRow(0).createCell(8).setCellValue("precision");
            sheet.getRow(0).createCell(9).setCellValue("recall");
            sheet.getRow(0).createCell(10).setCellValue("resultMap");
            for (int i = 1; i < sheet.getLastRowNum(); i += 2) {
                HSSFRow row = sheet.getRow(i);
                String title = row.getCell(1).getStringCellValue();
                String question = row.getCell(3).getStringCellValue();
                String standardTasks = "";
                HSSFCell cell = row.getCell(4);
                if (cell != null)
                    standardTasks = cell.getStringCellValue();
                if (standardTasks == "")
                    continue;
                postCount++;
                cell = sheet.getRow(i + 1).getCell(3);
                String answer = "";
                if (cell != null)
                    answer = cell.getStringCellValue();
                PostInfo post = new PostInfo(title, question, answer, Arrays.asList(standardTasks.split(", ")));
                post = extractPhrase(post, pipeline);
                HSSFCell candidatePhrasesCell = row.createCell(5);
                HSSFCell filteredPhrasesCell = row.createCell(6);
                HSSFCell selectedPhrasesCell = row.createCell(7);
                HSSFCell precisionCell = row.createCell(8);
                HSSFCell recallCell = row.createCell(9);
                String candidatePhrases = "";
                for (Integer k=0; k<post.candidatePhrases.size(); k++) {
                    candidatePhrases =  candidatePhrases + "[" + (k+1) + "] "+ post.candidatePhrases.get(k) + "\n";
                }
                candidatePhrasesCell.setCellValue(candidatePhrases);
                filteredPhrasesCell.setCellValue(post.filteredPhrases.toString());
                Integer cellIndex = 10;
                Integer notNull = 0;
                for (Object key : post.resultMap.keySet()) {
                    Matrix<DoubleValue> matrix = post.resultMap.get(key);
                    if (matrix.getM() > 0)
                        notNull = 1;
                    for (Integer n = 0; n < matrix.getN(); n++) {
                        int index = matrix.getColumnMax(n, 0, 0.8);
                        if (index != -1)
                            post.selectedPhrases.add(post.candidatePhrases.get(n));
                    }
                    row.createCell(cellIndex).setCellValue(key.toString());
                    sheet.getRow(i+1).createCell(cellIndex).setCellValue(matrix.printToString(0.0));
                    cellIndex++;
                }
                selectedPhrasesCell.setCellValue(post.selectedPhrases.toString());
                if (post.selectedPhrases.size() > 0)
                    notNullResult++;
                if (notNull == 1) {
                    notNullMatrix++;
                    allStandardPhrasesWithoutNullMatrix += post.standardTasks.size();
                }
                allStandardPhrases += post.standardTasks.size();
                post = getPrecisionAndRecall(post);
                precisionCell.setCellValue(post.precision);
                recallCell.setCellValue(post.recall);
                sumPrecision += post.precision;
                sumRecall += post.recall;
//                System.out.println("-------------------------------------------------------------------------");
            }
            workbook.write(outputStream);
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> parseCode(String code) {
        // remove tags
        code = code.replace("<pre>", "");
        code = code.replace("</pre>", "");

        // 转义字符
        code = code.replace("&nbsp;", " ");
        code = code.replace("&lt;", "<");
        code = code.replace("&gt;", ">");
        code = code.replace("&amp;", "&");
        code = code.replace("&apos;", "'");
        code = code.replace("&quot;", "\"");
        code = code.replace("&;", "&");
//        System.out.println(code);

        // extract method body
        ASTParser parser = ASTParser.newParser(AST.JLS10);
        parser.setSource(code.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        List<String> methods = new ArrayList<>();
        if (cu.types().size() > 0) {
//            System.out.println("ClassName:");
            TypeDeclaration typeDec = (TypeDeclaration) cu.types().get(0);
//            System.out.println(typeDec.getName());
            MethodDeclaration methodDec[] = typeDec.getMethods();
//            System.out.println("Method:");
            for (MethodDeclaration method: methodDec) {
                Block body = method.getBody();
                String methodBody = body.toString();
                methodBody = methodBody.substring(methodBody.indexOf("{") + 1, methodBody.lastIndexOf("}"));
                methods.add(methodBody);
//                System.out.println(body.toString());
            }
        }
        else {
            methods.add(code);
        }
        return methods;
    }

    public PostInfo extractPhrase(PostInfo post, StanfordCoreNLP pipeline) {
            Document doc = Jsoup.parse(post.title + ". " + post.question + ". " + post.answer);

            // analyze code
            Elements codes = doc.getElementsByTag("pre");

            List<String> phraseList = new ArrayList<>();
            //analyze text
            doc.select("pre").remove();
            String text = doc.text();
            List<String> sentences = splitText(text, pipeline);
            for (String sentence: sentences) {
                Tree tree = NLPParser.parseGrammaticalTree(sentence);
                PhraseInfo[] verbPhrases = PhraseExtractor.extractVerbPhrases(tree);
                if (verbPhrases == null)
                    continue;
                for (PhraseInfo phraseInfo : verbPhrases) {
                    PhraseFilter.filter(phraseInfo, sentence);
                    Integer proofScore = phraseInfo.getProofScore();
                    post.candidatePhrases.add(phraseInfo.getText() + " (" + proofScore + ")");
                    phraseList.add(phraseInfo.getText());

                    if (proofScore >= MIN_PROOFSCORE) {
                        phraseHashMap.put(phraseInfo.getText(), proofScore);
                        post.filteredPhrases.add(phraseInfo.getText());
                    }
                }
            }
            //analyze code
            for (Element codeElement: codes) {
                List<String> methods = parseCode(codeElement.toString());
                for (String code: methods) {
//                    System.out.println("Code:");
//                    System.out.println(code);
                    Summarization summarization = new Summarization(code, phraseList);
                    Matrix<DoubleValue> matrix = summarization.getMatrix();
//                    System.out.println("Matrix:");
//                    matrix.print(0.0);

//                    Integer rows = matrix.getM();
//                    Integer columns = matrix.getN();
//                    String matrixToString = "";
//                    for (Integer i=0; i<rows; i++) {
//                        for (Integer j=0; j<columns; j++) {
//                            matrixToString += matrix.getValue(i, j) + " ";
//                        }
//                        matrixToString += "\n";
//                    }
//                    System.out.println(matrixToString);

                    post.resultMap.put(code, matrix);
                }
            }
            System.out.println("-------------------------------------------------------");
            return post;
    }

    public void extractPhrase(long id, StanfordCoreNLP pipeline) {
//        GraphDatabaseService db = this.getDb();
        try(Transaction tx = db.beginTx()){
            Node textNode = db.getNodeById(id);
            if (textNode.hasProperty("questionId"))
                System.out.println(textNode.getProperty("questionId"));
            else if (textNode.hasProperty("parentQuestionId"))
                System.out.println(textNode.getProperty("parentQuestionId"));
            String body = textNode.getProperty("body").toString();
            Document doc = Jsoup.parse(body);

            // analyze code
            Elements codes = doc.getElementsByTag("code");
//            HashSet<String> phraseFromCode = new HashSet<>();
//            if (codes.size() > 0)
//            {
//                postWithCode++;
//                phraseFromCode= extractPhraseFromCode(codes);
//                System.out.println(phraseFromCode);
//            }
            String code =
                    "   FileInputStream fis = new FileInputStream(\"e:\\\\projects\\\\1.docx\");\n" +
                            "   XWPFDocument doc = new XWPFDocument(OPCPackage.open(fis));\n" +
                            "   fis.close();\n" +
                            "   XWPFTable table = doc.createTable();\n" +
                            "\n" +
                            "//added to satisfy poi docx->pdf converter and avoid future npe on getCTTbl().getTblGrid()...\n" +
                            "   CTTblGrid ctg = table.getCTTbl().getTblGrid();\n" +
                            "   table.getCTTbl().setTblGrid(ctg);\n" +
                            "\n" +
                            "   fillTable(table);\n" +
                            "\n" +
                            "   OutputStream pdfFile = new FileOutputStream(new File(\"e:\\\\projects\\\\1.pdf\"));\n" +
                            "   PdfOptions options= PdfOptions.create().fontEncoding(\"UTF-8\");\n" +
                            "   PdfConverter.getInstance().convert(doc, pdfFile, options);\n";
            List<String> phraseList = new ArrayList<>();

            //analyze text
            doc.select("code").remove();
            String text = doc.text();
            List<String> sentences = splitText(text, pipeline);
            for (String sentence: sentences) {
                Tree tree = NLPParser.parseGrammaticalTree(sentence);
                PhraseInfo[] verbPhrases = PhraseExtractor.extractVerbPhrases(tree);
                if (verbPhrases == null)
                    continue;
                for (PhraseInfo phraseInfo : verbPhrases) {
                    PhraseFilter.filter(phraseInfo, sentence);
                    Integer proofScore = phraseInfo.getProofScore();
                    System.out.println(phraseInfo.getText());
                    System.out.println(proofScore);

                    // analyze code
//                    if (phraseFromCode.size() > 0)
//                        proofScore = filterWithCode(phraseInfo, phraseFromCode, id);
                    phraseList.add(phraseInfo.getText());

                    if (proofScore >= MIN_PROOFSCORE) {
//                        Node taskNode = db.createNode(TASK);
//                        taskNode.setProperty(TASK_TEXT, phraseInfo.getText());
//                        taskNode.setProperty(TASK_LEVEL, "2");
//                        textNode.createRelationshipTo(taskNode, FUNCTIONALFEATURE);
//                        System.out.println(sentence);
//                        System.out.println(phraseInfo.getText());
                        phraseHashMap.put(phraseInfo.getText(), proofScore);
                    }
                }
            }
            //analyze code
            Summarization summarization = new Summarization(code, phraseList);
            Matrix<DoubleValue> matrix = summarization.getMatrix();
            matrix.print(0.0);

            System.out.println("-------------------------------------------------------");
            tx.success();
        }
    }

    public static HashSet<String> extractPhraseFromCode(Elements codes) {
        HashSet<String> phraseSet = new HashSet<>();
        for (Element code: codes)
        {
            String[] names = code.toString().split("([^a-zA-Z])+");
            if (names.length == 0)
                continue;
            for (String name: names) {
                if (name.matches("[a-z]+.*[A-Z]+.*"))
                    phraseSet.add(name.toLowerCase());
            }
        }
        return phraseSet;
    }

    public static Integer filterWithCode(PhraseInfo phraseInfo, HashSet<String> phraseFromCode, Long nodeId) {
        String text = phraseInfo.getText();
        Integer proofScore = phraseInfo.getProofScore();
        if (!text.contains(" "))
            return proofScore;
        text = text.replace(" ", "");
        if (phraseFromCode.contains(text.toLowerCase()))
        {
//            System.out.println(proofScore);
//            System.out.println(phraseInfo.getText());
//            nodeSet.add(nodeId);
            phraseMatchCode++;
            return proofScore + 10;
        }
        else
            return proofScore;
    }

    public static List<String> splitText(String text, StanfordCoreNLP pipeline) {
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        List<String> sentencesText = new ArrayList<>();
        for (CoreMap sentence: sentences) {
            sentencesText.add(sentence.toString());
        }
        return sentencesText;
    }

    public void extractTask(Map<Long, String> questionMap, Map<Long, String> answerMap, StanfordCoreNLP pipeline) {
//        GraphDatabaseService db = this.getDb();
        miner.setQuestionMap(questionMap);
        miner.setAnswerMap(answerMap);
        try(Transaction tx = db.beginTx()){
            for (Map.Entry<Long, String> entry : questionMap.entrySet()) {
//                if (miner.getObjSize() > MAX_NUMBER_OF_POSTS)
//                    break;
                parseNode(entry.getKey(), pipeline);
            }
            for (Map.Entry<Long, String> entry : answerMap.entrySet()) {
//                if (miner.getObjSize() > MAX_NUMBER_OF_POSTS * 2)
//                    break;
                parseNode(entry.getKey(), pipeline);
            }
            HashSet<String> tasks = miner.mineGraph();
//            Map<Long, String> phraseMap = getNodesFromNeo4j(TaskExtractor.TASK);
//            for (String task: tasks) {
//                Node taskNode = db.createNode(TASK);
//                taskNode.setProperty(TASK_TEXT, task);
//                taskNode.setProperty(TASK_LEVEL, "1");
//                for (Map.Entry<Long, String> entry : phraseMap.entrySet()) {
//                    Node phraseNode = db.getNodeById(entry.getKey());
//                    String phrase = (String)phraseNode.getProperty(TASK_TEXT);
//                    if (phrase.contains(task)) {
//                        phraseNode.createRelationshipTo(taskNode, GENERALIZATION);
//                    }
//                }
//            }
            //print task of level 1
            printTaskOfLevel1(tasks, "tasks.txt");
            tx.success();
        }
    }

    public void parseNode(long id, StanfordCoreNLP pipeline) {
//        GraphDatabaseService db = this.getDb();
        org.neo4j.graphdb.Node textNode = db.getNodeById(id);
        String body = textNode.getProperty("body").toString();
        Document doc = Jsoup.parse(body);
        // analyze code
//        Elements codes = doc.getElementsByTag("code");
//        HashSet<String> phraseFromCode = new HashSet<>();
//        if (codes.size() > 0)
//            phraseFromCode= extractPhraseFromCode(codes);
        //analyze text
        doc.select("code").remove();
        String text = doc.text();
        List<String> sentences = splitText(text, pipeline);
        for (String sentence: sentences) {
            Tree tree = NLPParser.parseGrammaticalTree(sentence);
            PhraseInfo[] verbPhrases = PhraseExtractor.extractVerbPhrases(tree);
            if (verbPhrases == null)
                continue;
            for (PhraseInfo phraseInfo : verbPhrases) {
                PhraseFilter.filter(phraseInfo, sentence);
                Integer proofScore = phraseInfo.getProofScore();
                // analyze code
//                if (phraseFromCode.size() > 0)
//                    proofScore = filterWithCode(phraseInfo, phraseFromCode, id);
                if (proofScore >= MIN_PROOFSCORE) {
                    phraseHashMap.put(phraseInfo.getText(), proofScore);
                    Tree stemmedPhraseTree = Stemmer.stemTree(Tree.valueOf(phraseInfo.getSyntaxTree()));
                    VerbalPhraseStructureInfo vp = new VerbalPhraseStructureInfo(stemmedPhraseTree);
                    GraphInfo graphInfo = GraphBuilder.buildGraphInfoFromStructure(vp);
                    miner.addToObj(graphInfo);
                    Graph<NodeInfo, Integer> g = GraphBuilder.convertToParsemisGraph(graphInfo);
                    Iterator<de.parsemis.graph.Node<NodeInfo, Integer>> ite = g.nodeIterator();
                    while (ite.hasNext()) {
                        de.parsemis.graph.Node<NodeInfo, Integer> n = ite.next();
                    }
                }
            }
        }
    }

    public void printTaskOfLevel1(HashSet<String> tasks, String fileName) {
        File resultFile = new File("E:/data_for_functional_feature/" + fileName);
        System.out.println("size of tasks: " + tasks.size());
        try {
            resultFile.createNewFile();
            FileWriter fileWriter = new FileWriter(resultFile);
            for (String task: tasks)
                fileWriter.write(task + "\r\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printTaskOfLevel2(String fileName) {
        File resultFile = new File("E:/data_for_functional_feature/" + fileName);
        System.out.println("size of phraseHashMap: " + phraseHashMap.size());
        try {
            resultFile.createNewFile();
            FileWriter fileWriter = new FileWriter(resultFile);
            for (Map.Entry<String, Integer> entry: phraseHashMap.entrySet())
                fileWriter.write(entry.getKey() + "__________" + entry.getValue() + "\r\n");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}