import mysql.connector
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class DatabaseManager(object):
    """
    Class to handle all connections, queries, and inserts to RankUp testing database for keyword extraction
    """

    # Connection parameters
    _MYSQL_CONFIG = {
        'user': 'root',
        'password': 'bakayarou00',
        'host': '140.114.77.17',
        'database': 'New_RankUp_Tests',
        'raise_on_warnings': False,
    }

    # Static members
    ABSTRACT_SOURCES = [
        "Hulth 2003",
        "IEEE Explore",
        "Journal of Applied Physics",
        "Journal of Psychiatric Practice",
        "Kaggle",
        "VLDB Journal",
        "finalpool_type0_score=3",
        "finalpool_type0_score>=2 AND score<3"
    ]

    def __init__(self, extracted_keywords_table, abstract_table="Abstract"):
        """
        :type extracted_keywords_table: the name of the table where the extracted keywords will be inserted
        """
        self.abstract_table = abstract_table
        self.extracted_keywords_table = extracted_keywords_table
        self._cnx = mysql.connector.connect(**self._MYSQL_CONFIG)
        self._cnx.set_charset('utf8')

    def get_abstracts(self, abstract_source):
        """
        Get all abstract ids and texts from the given data source
        :param abstract_source:
        :return: a dictionary of (abstract_id: text) belonging to the given data source
        """
        query = "SELECT Abstract_Id, Abstract_Text " \
                "FROM {0} " \
                "WHERE Abstract_Source = '{1}' AND " \
                "Abstract_Type = 'Testing'".format(self.abstract_table, abstract_source)

        logger.debug("get_abstracts() query: {0}".format(query))

        cursor = self._cnx.cursor()
        cursor.execute(query)
        abstracts = {}
        for (abstract_id, abstract_text) in cursor:
            abstracts[abstract_id] = abstract_text

        cursor.close()

        return abstracts


    def insert_keywords(self, abstract_id, keywords):
        """
        Insert list of keywords for a given abstract with their corresponding scores to the extracted_keywords_table
        :param abstract_id: The abstract id whose keywords will be inserted to the DB
        :param keywords: A list of (keyword: score) tuples
        """

        cursor = self._cnx.cursor()

        for keyword, score in keywords:

            insertString = u"INSERT INTO {0}(" \
                           "Abstract_Id, " \
                           "Keyword, " \
                           "Learning_Rate, " \
                           "SE_Threshold, " \
                           "Convergence_Scheme, " \
                           "Score, " \
                           "Method, " \
                           "Use_Whole_TR_Graph, " \
                           "Postprocess, " \
                           "Error_Detecting_Approach, " \
                           "Expected_Score_Value, " \
                           "Feature_Lower_Bound, " \
                           "Feature_Upper_Bound, " \
                           "Set_Assignment_Approach, " \
                           "Convergence_Rule, " \
                           "Revert_Graphs" \
                           ")\n" \
                           "VALUES ({1}, '{2}', {3}, {4}, '{5}', {6}, '{7}', {8}, {9}, '{10}', '{11}', {12}, {13}, " \
                           "'{14}', '{15}', {16})".format(self.extracted_keywords_table,
                                                          abstract_id,
                                                          keyword,
                                                          -1,
                                                          -1,
                                                          "null",
                                                          score,
                                                          "RAKE",
                                                          False,
                                                          False,
                                                          "null",
                                                          "null",
                                                          -1,
                                                          -1,
                                                          "null",
                                                          "null",
                                                          False
                                                          )

            try:
                cursor.execute(insertString)
            except Exception, e:
                logger.error(("Error in insert_keywords():", e))

        self._cnx.commit()
        cursor.close()