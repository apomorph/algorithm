package main

import (
	"fmt"
	"math"
	"strings"
)

func Tf(doc []string, term string) float64 {
	var termFrequency float64
	for _, str := range doc {
		if strings.EqualFold(str, term) {
			termFrequency++
		}
	}
	return termFrequency / float64(len(doc))
}

func Df(docs [][]string, term string) int {
	var n int
	if term != "" {
		for _, doc := range docs {
			for _, word := range doc {
				if strings.EqualFold(word, term) {
					n++
					break
				}
			}
		}
	} else {
		fmt.Println("term不能为空")
	}
	return n
}

func Idf(docs [][]string, term string) float64 {
	return math.Log(float64(len(docs)) / float64(Df(docs, term)+1))
}

func TfIdf(doc []string, docs [][]string, term string) float64 {
	return Tf(doc, term) * Idf(docs, term)
}

func main() {
	doc1 := []string{"人工", "智能", "成为", "互联网", "大会", "焦点"}
	doc2 := []string{"谷歌", "推出", "开源", "人工", "智能", "系统", "工具"}
	doc3 := []string{"互联网", "的", "未来", "在", "人工", "智能"}
	doc4 := []string{"谷歌", "开源", "机器", "学习", "工具"}

	// docs := [][]string{{"人工", "智能", "成为", "互联网", "大会", "焦点"},
	// 	{"谷歌", "推出", "开源", "人工", "智能", "系统", "工具"},
	// 	{"互联网", "的", "未来", "在", "人工", "智能"},
	// 	{"谷歌", "开源", "机器", "学习", "工具"}}

	docs := [][]string{doc1, doc2, doc3, doc4}

	fmt.Println(Tf(doc2, "谷歌"))
	fmt.Println(Df(docs, "谷歌"))
	fmt.Printf("tf-idf(谷歌) = %v", TfIdf(doc2, docs, "谷歌"))

}
