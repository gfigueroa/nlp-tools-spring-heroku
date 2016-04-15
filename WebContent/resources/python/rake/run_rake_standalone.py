# coding=utf-8
import rake
import logging
import random

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def run_rake_standalone(stop_word_list, text):
	extractor = rake.Rake(stop_word_list)  # It uses SmartStoplist, you can choose FoxStopList instead
	keywords = extractor.run(text)

	return keywords


if __name__ == "__main__":
	text = "A new approach to the decomposition of Boolean functions that depend on n variables and are represented in " \
		   "various forms is considered. The approach is based on the method of q-partitioning of minterms and on the " \
		   "introduced concept of a decomposition clone. The theorem on simple disjunctive decomposition of full and " \
		   "partial functions is formulated. The approach proposed is illustrated by examples."

	extractor = rake.Rake("SmartStoplist.txt")

	keywords = extractor.run(text)
	print keywords

	print "*** Candidate Phrases ***"
	print extractor.phrase_list

	co_occ_graph = extractor.co_occ_graph
	print
	extractor.print_co_occ_graph()

	'''
	weight = random.uniform(-0.1, 0.1)
	for phrase in extractor.phrase_list:
		extractor.modify_phrase_edge_weights(phrase, weight)

	print
	extractor.print_co_occ_graph()

	keywords2 = extractor.rerun()
	print keywords2
	'''