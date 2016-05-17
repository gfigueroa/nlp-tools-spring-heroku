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

    text = "For pt. I. see Vestn. KhGPU, no. 81, p. 15-18 (2000). The paper presents the results of development of an " \
           "object-oriented systemological method used to design complex systems. A formal system representation, as " \
           "well as an axiomatics of the calculus of systems as functional flow-type objects based on a " \
           "Node-Function-Object class hierarchy are proposed. A formalized NFO/UFO analysis algorithm and CASE tools " \
           "used to support it are considered"

    extractor = rake.Rake("SmartStoplist.txt")

    keywords = extractor.run(text)
    print keywords

    co_occ_graph = extractor.co_occ_graph

    print
    extractor.print_co_occ_graph()

    extractor.modify_edge_weight("based", "objects", 1)
    extractor.modify_edge_weight("based", "objects", 1.0)
    extractor.modify_edge_weight("based", "objects", 1.2)
    extractor.co_occ_graph = {}

    print
    extractor.print_co_occ_graph()

    keywords2 = extractor.rerun()
    print keywords2