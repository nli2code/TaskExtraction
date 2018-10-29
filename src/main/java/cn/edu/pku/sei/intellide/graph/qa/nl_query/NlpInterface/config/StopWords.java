package cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.config;

import cn.edu.pku.sei.intellide.graph.qa.nl_query.NlpInterface.ir.LuceneIndex;
import org.apache.commons.io.FileUtils;
import org.tartarus.snowball.ext.EnglishStemmer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StopWords {
    private static EnglishStemmer stemmer = new EnglishStemmer();
    public static Set<String> englishStopWords = new HashSet<>();
    static {
        List<String> lines=new ArrayList<>();
        try {
            String filepath =StopWords.class.getResource("/stopwords_chinese.txt").getPath();
            filepath = new File(filepath).getParentFile().getPath();
            filepath = new File(filepath).getParentFile().getPath();
            //filepath = new File(filepath).getParentFile().getPath();
            filepath = filepath+"\\config\\stopwords_chinese.txt";
            filepath = filepath.substring(6);
            lines= FileUtils.readLines(new File(filepath),"utf-8");
            //lines= FileUtils.readLines(new File("/data/stopwords_chinese.txt"),"utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        englishStopWords.addAll(lines);
    }
    public static boolean isStopWord(String word){
        return englishStopWords.contains(word);
    }
}
