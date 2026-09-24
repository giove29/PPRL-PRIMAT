"""Parita' Java/Python delle metriche: stessi scenari di
MultiSourceLinkageMetricsTest / PerformanceMetricsTest (LU).

Esecuzione (dalla cartella python_evaluation): python -m unittest test_eval_core
"""

import csv
import os
import tempfile
import unittest

from eval_core import evaluate, load_from_csv


def rec(party, rid, gid, cluster, dirty):
    return {"party": party, "record_id": rid, "global_id": gid,
            "cluster_id": cluster, "party_dirty": dirty}


class EvaluateTest(unittest.TestCase):

    def test_mixed_ground_truth_and_max_comparisons(self):
        # A clean (1), B dirty (3), C dirty (2): GT = 3 + 3 = 6, max = 33 con
        # A=3,B=4,C=2 nel test Java; qui A=1,B=3,C=2 -> 3+2+6 + 3 + 1 = 15.
        records = [
            rec("A", "a1", "g1", 1, False),
            rec("B", "b1", "g1", 1, True), rec("B", "b2", "g1", 1, True),
            rec("B", "b3", "g2", 2, True),
            rec("C", "c1", "g2", 2, True), rec("C", "c2", "g2", 2, True),
        ]
        r = evaluate(records)
        self.assertEqual(6, r["gt"])
        self.assertEqual(3 * 1 + 2 * 1 + 2 * 3 + 3 + 1, r["max_comparisons"])
        self.assertEqual(6, r["tp"])
        self.assertEqual(0, r["fp"])
        self.assertEqual(0, r["fn"])
        self.assertEqual(r["max_comparisons"], r["tp"] + r["fp"] + r["tn"] + r["fn"])

    def test_max_comparisons_three_parties_matches_java(self):
        records = ([rec("A", "a%d" % i, "", 100 + i, False) for i in range(3)]
                   + [rec("B", "b%d" % i, "", 200 + i, True) for i in range(4)]
                   + [rec("C", "c%d" % i, "", 300 + i, True) for i in range(2)])
        self.assertEqual(33, evaluate(records)["max_comparisons"])

    def test_all_clean_ignores_within_party(self):
        records = [rec("A", "a1", "g1", 1, False), rec("A", "a2", "g1", 1, False),
                   rec("B", "b1", "g1", 1, False)]
        r = evaluate(records)
        self.assertEqual(2, r["gt"])
        self.assertEqual(2, r["tp"])  # a1-b1, a2-b1; a1-a2 mai contata
        self.assertEqual(0, r["fp"])

    def test_single_dirty_party(self):
        records = [rec("A", "a%d" % i, "g1", 1, True) for i in range(4)]
        r = evaluate(records)
        self.assertEqual(6, r["gt"])
        self.assertEqual(6, r["tp"])
        self.assertEqual(6, r["max_comparisons"])
        self.assertEqual(0, r["tn"])

    def test_wrong_within_party_link_is_false_positive_only_if_dirty(self):
        dirty = [rec("A", "a1", "g1", 1, True), rec("A", "a2", "g2", 1, True)]
        clean = [rec("A", "a1", "g1", 1, False), rec("A", "a2", "g2", 1, False)]
        self.assertEqual(1, evaluate(dirty)["fp"])
        self.assertEqual(0, evaluate(clean)["fp"])

    def test_csv_without_party_dirty_column_falls_back_to_clean(self):
        with tempfile.TemporaryDirectory() as d:
            path = os.path.join(d, "old.csv")
            with open(path, "w", newline="", encoding="utf-8") as f:
                w = csv.writer(f)
                w.writerow(["cluster_id", "party", "record_id", "global_id"])
                w.writerow([1, "A", "a1", "g1"])
            records = load_from_csv(path)
        self.assertFalse(records[0]["party_dirty"])

    def test_csv_with_party_dirty_column(self):
        with tempfile.TemporaryDirectory() as d:
            path = os.path.join(d, "new.csv")
            with open(path, "w", newline="", encoding="utf-8") as f:
                w = csv.writer(f)
                w.writerow(["cluster_id", "party", "record_id", "global_id", "party_dirty"])
                w.writerow([1, "A", "a1", "g1", "true"])
                w.writerow([1, "B", "b1", "g1", "false"])
            records = load_from_csv(path)
        self.assertEqual([True, False], [r["party_dirty"] for r in records])


if __name__ == "__main__":
    unittest.main()
