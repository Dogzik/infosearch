#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from argparse import ArgumentParser
from codecs import open as codopen
from array import array
from bisect import bisect_left
from zlib import crc32


class Index:
    def __init__(self, index_file):
        self.SIZE = 55000
        self.doc_ids = [array('h', []) for _ in range(self.SIZE)]
        with codopen(index_file, mode="r", encoding="utf-8") as documents:
            for document in documents:
                doc_id_str, title, body = document.rstrip("\n").split("\t")
                doc_id = int(doc_id_str)
                for word in set(title.split() + body.split()):
                    word_hash = crc32(bytes(word, "utf-8")) % self.SIZE
                    self.doc_ids[word_hash].append(doc_id)


class QueryTree:
    class WordNode:
        def __init__(self, word):
            self.word = word

        def search(self, index):
            return index.doc_ids[crc32(bytes(self.word, "utf-8")) % index.SIZE]

    class AndNode:
        def __init__(self, left, right):
            self.left = left
            self.right = right

        def search(self, index):
            left_res = self.left.search(index)
            right_res = self.right.search(index)
            res = array('h', 0)
            i, j = 0, 0
            while (i < len(left_res)) and (j < len(right_res)):
                if left_res[i] < right_res[j]:
                    i += 1
                elif left_res[i] > right_res[j]:
                    j += 1
                else:
                    res.append(left_res[i])
                    i += 1
                    j += 1
            return res

    class OrNode:
        def __init__(self, left, right):
            self.left = left
            self.right = right

        def search(self, index):
            left_res = self.left.search(index)
            right_res = self.right.search(index)
            res = array('h', 0)
            i, j = 0, 0
            while (i < len(left_res)) and (j < len(right_res)):
                if left_res[i] < right_res[j]:
                    res.append(left_res[i])
                    i += 1
                elif left_res[i] > right_res[j]:
                    res.append(right_res[j])
                    j += 1
                else:
                    res.append(left_res[i])
                    i += 1
                    j += 1
            while i < len(left_res):
                res.append(left_res[i])
                i += 1
            while j < len(right_res):
                res.append(right_res[j])
                j += 1
            return res

    def __init__(self, query):
        # TODO: parse query and create query tree
        self.query = self.WordNode(query)

    def search(self, index):
        return self.query.search(index)


class SearchResults:
    def __init__(self):
        self.results = []

    def add(self, found):
        self.results.append(found)

    def print_submission(self, objects_file, submission_file):
        with codopen(submission_file, mode="w", encoding="utf-8") as out:
            out.write("ObjectId,Relevance\n")
            with codopen(objects_file, mode='r', encoding='utf-8') as objects_fh:
                for line in objects_fh[1:]:
                    oid, qid, doc_id = map(int, line.split(','))
                    query_result = self.results[qid - 1]
                    insert_pos = bisect_left(query_result, doc_id)
                    if (insert_pos < len(query_result)) and (query_result[insert_pos] == doc_id):
                        res = 1
                    else:
                        res = 0
                    out.write("{},{}".format(oid, res))


def main():
    # Command line arguments.
    parser = ArgumentParser(description="Homework 2: Boolean Search")
    parser.add_argument("--queries_file", required=True, help="queries.numerate.txt")
    parser.add_argument("--objects_file", required=True, help="objects.numerate.txt")
    parser.add_argument("--docs_file", required=True, help="docs.tsv")
    parser.add_argument("--submission_file", required=True, help="output file with relevances")
    args = parser.parse_args()

    # Build index.
    index = Index(args.docs_file)

    # # Process queries.
    # search_results = SearchResults()
    # with codecs.open(args.queries_file, mode="r", encoding="utf-8") as queries_fh:
    #     for line in queries_fh:
    #         _, query = line.rstrip("\n").split("\t")
    #
    #         Parse query.
    #         query_tree = QueryTree(query)
    #
    #         Search and save results.
    #         search_results.add(query_tree.search(index))
    #
    # # Generate submission file.
    # search_results.print_submission(args.objects_file, args.submission_file)


if __name__ == "__main__":
    main()
