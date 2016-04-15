# coding=utf-8
import rake
from rankup_db import DatabaseManager
import logging
import collections

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def getKeywords(extractor, sentence):
    keywords = extractor.run(sentence)
    return keywords


def runRakeBulk():

    extractor = rake.Rake("SmartStoplist.txt")  # It uses SmartStoplist, you can choose FoxStopList instead

    dbm = DatabaseManager(extracted_keywords_table="RankUp_Extracted_Keyword_Final2_Rake")

    for abstract_source in dbm.ABSTRACT_SOURCES:

        if abstract_source != "Kaggle":
            continue

        logger.info("Running RAKE (bulk) on {0}...".format(abstract_source))
        logger.info("Retrieving abstract ids...")
        all_abstracts = dbm.get_abstracts(abstract_source=abstract_source) # {(id: text)} dictionary

        # If Kaggle, use sample only
        abstracts = {}
        if abstract_source == 'Kaggle':
            sample_percentage = 0.1
            sample_size = int(len(all_abstracts) * sample_percentage)
            skip = len(all_abstracts) / sample_size
            all_abstracts = collections.OrderedDict(sorted(all_abstracts.items()))
            for i in range(sample_size):
                index = list(all_abstracts.keys())[i * skip]
                abstracts[index] = all_abstracts[index]
        else:
            abstracts = all_abstracts

        i = 0
        for abstract_id in abstracts:
            i += 1
            logger.info("Extracting keywords from abstract {0} ({1}/{2})...".format(abstract_id, i, len(abstracts)))

            text = abstracts[abstract_id]
            keywords = getKeywords(extractor, text)
            dbm.insert_keywords(abstract_id, keywords)


if __name__ == "__main__":
    runRakeBulk()