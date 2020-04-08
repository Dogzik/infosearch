#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from argparse import ArgumentParser
from codecs import open as codopen
from array import array
from bisect import bisect_left
from zlib import crc32


class Index:
    def __hash_word(self, word):
        return crc32(bytes(word, "utf-8")) % self.__SIZE

    def __init__(self, index_file):
        self.__SIZE = 100000
        self.__doc_ids = [array('h', []) for _ in range(self.__SIZE)]
        with codopen(index_file, mode="r", encoding="utf-8") as documents:
            for document in documents:
                doc_id_str, title, body = document.rstrip("\n").split("\t")
                doc_id = int(doc_id_str)
                for word in set(title.split() + body.split()):
                    self.__doc_ids[self.__hash_word(word)].append(doc_id)

    def get_doc_ids(self, word):
        return self.__doc_ids[self.__hash_word(word)]


class QueryParser:
    def __init__(self):
        self.__idx = 0
        self.__tokens = []

    def __skip(self, token):
        if self.__tokens[self.__idx] == token:
            self.__idx += 1
            return True
        else:
            return False

    def __parse_atom(self):
        if self.__skip("("):
            res = self.__parse_or()
            self.__skip(")")
            return res
        else:
            word = self.__tokens[self.__idx]
            self.__idx += 1
            return QueryTree.WordNode(word)

    def __parse_and(self):
        res = self.__parse_atom()
        while self.__skip(" "):
            res = QueryTree.AndNode(res, self.__parse_atom())
        return res

    def __parse_or(self):
        res = self.__parse_and()
        while self.__skip("|"):
            res = QueryTree.OrNode(res, self.__parse_and())
        return res

    @staticmethod
    def __split_string(query_str):
        delimiters = ["(", ")", "|", " "]
        res = []
        cur_token = ""
        for c in query_str:
            if c in delimiters:
                if cur_token != "":
                    res.append(cur_token)
                    cur_token = ""
                res.append(c)
            else:
                cur_token += c
        if cur_token != "":
            res.append(cur_token)
        return res

    def parse(self, query_str):
        self.__tokens = self.__split_string(query_str) + ["\n"]
        self.__idx = 0
        return self.__parse_or()


class QueryTree:
    class WordNode:
        def __init__(self, word):
            self.__word = word

        def search(self, index):
            return index.get_doc_ids(self.__word)

    class AndNode:
        def __init__(self, left, right):
            self.__left = left
            self.__right = right

        def search(self, index):
            left_res = self.__left.search(index)
            right_res = self.__right.search(index)
            res = array('h', [])
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
            self.__left = left
            self.__right = right

        def search(self, index):
            left_res = self.__left.search(index)
            right_res = self.__right.search(index)
            res = array('h', [])
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

    def __init__(self, query_str):
        self.query = QueryParser().parse(query_str)

    def search(self, index):
        return self.query.search(index)


class SearchResults:
    def __init__(self):
        self.__results = []

    def add(self, found):
        self.__results.append(found)

    def print_submission(self, objects_file, submission_file):
        with codopen(submission_file, mode="w", encoding="utf-8") as out:
            out.write("ObjectId,Relevance\n")
            with codopen(objects_file, mode='r', encoding='utf-8') as objects_fh:
                objects_fh.readline()
                for line in objects_fh:
                    oid, qid, doc_id = map(int, line.split(','))
                    query_result = self.__results[qid - 1]
                    insert_pos = bisect_left(query_result, doc_id)
                    if (insert_pos < len(query_result)) and (query_result[insert_pos] == doc_id):
                        res = 1
                    else:
                        res = 0
                    out.write("{},{}\n".format(oid, res))


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

    # Process queries.
    search_results = SearchResults()
    with codopen(args.queries_file, mode="r", encoding="utf-8") as queries_fh:
        for line in queries_fh:
            _, query = line.rstrip("\n").split("\t")

            # Parse query.
            query_tree = QueryTree(query)

            # Search and save results.
            search_results.add(query_tree.search(index))

    # Generate submission file.
    search_results.print_submission(args.objects_file, args.submission_file)


if __name__ == "__main__":
    main()
