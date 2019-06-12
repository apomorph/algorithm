package com.yegoo.searcher.searcher;

import java.util.Arrays;
import java.util.List;

/**
 * tf-idf权重计算
 */
public class TfIdfCal {

    /**
     * 词项频率 term frequency
     *  单词在文档中的出现次数 / 文档的总次数
     *  Lucene采用√￣(单词在文档中的出现次数)
     * @param doc
     * @param term
     * @return
     */
    public double tf(List<String> doc, String term) {
        double termFrequency = 0;
        for (String str : doc) {
            if (str.equalsIgnoreCase(term)) {
                termFrequency++;
            }
        }
        return termFrequency / doc.size();
    }

    /**
     * 文档频率 document frequency
     * 文档集中包含某个词的所有文档数目
     * @param docs
     * @param term
     * @return
     */
    public int df(List<List<String>> docs, String term) {
        int n = 0;
        if (term != null && term != "") {
            for (List<String> doc : docs) {
                for (String word : doc) {
                    if (term.equalsIgnoreCase(word)) {
                        n++;
                        break;
                    }
                }
            }
        } else {
            System.out.println("term不能为null或空串");
        }
        return n;
    }

    /**
     * 逆文档频率 inverse document frequency
     * log(文档集总的文档数 / 包含某个词的文档数 + 1)
     * @param docs
     * @param term
     * @return
     */
    public double idf(List<List<String>> docs, String term) {
        return Math.log(docs.size() / (double)df(docs, term) + 1);
    }

    /**
     * 词频-逆文档频率
     * tf-idf = tf * idf
     * @param doc
     * @param docs
     * @param term
     * @return
     */
    public double tfIdf(List<String> doc, List<List<String>> docs, String term) {
        return tf(doc, term) * idf(docs, term);
    }

    public static void main(String[] args) {
        List<String> doc1 = Arrays.asList("人工", "智能", "成为", "互联网", "大会", "焦点");
        List<String> doc2 = Arrays.asList("谷歌", "推出", "开源", "人工", "智能", "系统", "工具");
        List<String> doc3 = Arrays.asList("互联网", "的", "未来", "在", "人工", "智能");
        List<String> doc4 = Arrays.asList("谷歌", "开源", "机器", "学习", "工具");

        List<List<String>> documents = Arrays.asList(doc1, doc2, doc3, doc4);

        TfIdfCal tfIdfCal = new TfIdfCal();
        System.out.println(tfIdfCal.tf(doc2, "谷歌"));
        System.out.println(tfIdfCal.df(documents, "谷歌"));
        System.out.println("tf-idf(谷歌) = " + tfIdfCal.tfIdf(doc2, documents, "谷歌"));

    }
}
