import random
import re

from timmy import db_access


class MarkovProcessor:
    def __init__(self):
        self.db = None
        self.bad_words = set()
        self.bad_pairs = set()
        self.alternate_words = set()
        self.alternate_pairs = set()

        self.clean_string_pattern = re.compile('[^a-z]', re.IGNORECASE)
        self.all_upper_pattern = re.compile('^[A-Z]+$')
        self.starts_upper_pattern = re.compile('^[A-Z]+[a-z]+$')

    def init(self):
        self.db = db_access.connection_pool

        self._load_bad_words()
        self._load_bad_pairs()
        self._load_alternate_words()
        self._load_alternate_pairs()

    def store_line(self, line_type: str, line: str):
        if self.db is None:
            self.init()

        conn = self.db.get_connection()
        insert_statement = "INSERT INTO markov_processing_queue (`type`, `text`, `created`) VALUES (%(line_type)s, " \
                           "%(line)s, NOW())"

        cursor = conn.cursor()
        cursor.execute(insert_statement, {
            'line_type': line_type,
            'line': line
        })

    def processing_loop(self):
        if self.db is None:
            self.init()

        select_statement = "SELECT `id`, `type`, `text` FROM markov_processing_queue"
        delete_statement = "DELETE FROM `markov_processing_queue` WHERE `id` = %(id)s"

        conn = self.db.get_connection()
        select_cursor = conn.cursor(dictionary=True)
        delete_cursor = conn.cursor()

        select_cursor.execute(select_statement)
        for row in select_cursor:
            if row['type'] == 'emote' or row['type'] == 'say':
                self._process_markov(row['text'], row['type'])
            elif row['type'] == 'novel':
                self._process_markov4(row['text'], row['type'])
                self._process_markov(row['text'], 'say')

            delete_cursor.execute(delete_statement, {'id': row['id']})

    def _process_markov(self, message: str, message_type: str):
        known_replacements = {}
        full_message = message.split()

        for i in range(-1, len(full_message) + 1):
            words = [
                [i - 1, ""],
                [i, ""],
                [i + 1, ""]
            ]

            self._internal_markov_processing(full_message, known_replacements, words)
            self._store_triad(words[0][1], words[1][1], words[2][1], message_type)

    def _internal_markov_processing(self, full_message, known_replacements, words):
        for word in words:
            word[1] = self.__set_word(word[0], full_message)
            word[1] = self.__replace_word(known_replacements, word[1])

        for j, k in zip(range(len(words)), range(len(words))[1:]):
            words[j][1], words[k][1] = self.__replace_pair(known_replacements, words[j][1], words[k][1])

        for word in words:
            word[1] = word[1][:50]
            if 0 <= word[0] < len(full_message):
                full_message[word[0]] = word[1]

    def _process_markov4(self, message: str, message_type: str):
        known_replacements = {}
        full_message = message.split()

        for i in range(-1, len(full_message) + 1):
            words = [
                [i - 2, ""],
                [i - 1, ""],
                [i, ""],
                [i + 1, ""]
            ]

            self._internal_markov_processing(full_message, known_replacements, words)
            self._store_quad(words[0][1], words[1][1], words[2][1], words[3][1], message_type)

    def __replace_pair(self, known_replacements, word1, word2):
        pair = "{} {}".format(word1, word2)
        if pair in known_replacements:
            word1, word2 = known_replacements[pair].split()
        else:
            rep = self._replace_bad_pair(pair)
            known_replacements[pair] = rep
            word1, word2 = rep.split()
        return word1, word2

    def __replace_word(self, known_replacements, word):
        if word in known_replacements:
            word = known_replacements[word]
        else:
            rep = self._replace_bad_word(word)
            known_replacements[word] = rep
            word = rep

        return word

    @staticmethod
    def __set_word(offset1, words):
        if offset1 < 0:
            word1 = ""
        elif offset1 >= len(words):
            word1 = ""
        else:
            word1 = words[offset1]
        return word1

    def _store_triad(self, word1, word2, word3, message_type):
        first = self.get_markov_word_id(word1)
        second = self.get_markov_word_id(word2)
        third = self.get_markov_word_id(word3)

        conn = self.db.get_connection()

        if message_type != 'emote':
            message_type = 'say'

        add_triad_expression = "INSERT INTO `markov3_{}_data` (`first_id`, `second_id`, `third_id`, `count`) VALUES " \
                               "(%(first)s, %(second)s, %(third)s, 1) ON DUPLICATE KEY UPDATE " \
                               "count = count + 1".format(message_type)

        cursor = conn.cursor()
        cursor.execute(add_triad_expression, {
            'first': first,
            'second': second,
            'third': third
        })

    def _store_quad(self, word1, word2, word3, word4, message_type):
        first = self.get_markov_word_id(word1)
        second = self.get_markov_word_id(word2)
        third = self.get_markov_word_id(word3)
        fourth = self.get_markov_word_id(word4)

        conn = self.db.get_connection()

        if message_type == 'novel':
            return

        add_quad_expression = "INSERT INTO `markov4_{}_data` (`first_id`, `second_id`, `third_id`, `fourth_id`, " \
                              "`count`) VALUES (%(first)s, %(second)s, %(third)s, %(fourth)s, 1) ON DUPLICATE KEY " \
                              "UPDATE count = count + 1".format(message_type)

        cursor = conn.cursor()
        cursor.execute(add_quad_expression, {
            'first': first,
            'second': second,
            'third': third,
            'fourth': fourth
        })

    def get_markov_word_id(self, word: str):
        conn = self.db.get_connection()

        word = word[:50]

        checkword_statement = "SELECT id FROM markov_words WHERE word = %(word)s"
        addword_statement = "INSERT INTO markov_words SET word = %(word)s"

        cursor = conn.cursor(dictionary=True)
        cursor.execute(checkword_statement, {'word': word})

        result = cursor.fetchone()

        if result is not None:
            return result['id']
        else:
            cursor.execute(addword_statement, {'word': word})

            return cursor.lastrowid

    def get_markov_word_by_id(self, id: int):
        conn = self.db.get_connection()

        select_statement = "SELECT word FROM markov_words WHERE id = %(id)s"

        cursor = conn.cursor(dictionary=True)
        cursor.execute(select_statement, {'id': id})

        result = cursor.fetchone()

        if result is not None:
            return result['word']
        else:
            return None


    def _load_bad_words(self):
        conn = self.db.get_connection()
        select_statement = "SELECT `word` FROM `bad_words`"

        cursor = conn.cursor(dictionary=True)
        cursor.execute(select_statement)

        for row in cursor:
            self.bad_words.add(re.compile('(\\W|\\b)({})(\\W|\\b)'.format(re.escape(row['word'])), re.IGNORECASE))

    def _load_bad_pairs(self):
        conn = self.db.get_connection()
        select_statement = "SELECT `word_one`, `word_two` FROM `bad_pairs`"

        cursor = conn.cursor(dictionary=True)
        cursor.execute(select_statement)

        for row in cursor:
            self.bad_pairs.add((
                re.compile('(\\W|\\b)({})(\\W|\\b)'.format(re.escape(row['word_one'])), re.IGNORECASE),
                re.compile('(\\W|\\b)({})(\\W|\\b)'.format(re.escape(row['word_two'])), re.IGNORECASE),
            ))

    def _load_alternate_words(self):
        conn = self.db.get_connection()
        select_statement = "SELECT `word` FROM `alternate_words`"

        cursor = conn.cursor(dictionary=True)
        cursor.execute(select_statement)

        for row in cursor:
            self.alternate_words.add(row['word'])

    def _load_alternate_pairs(self):
        conn = self.db.get_connection()
        select_statement = "SELECT `word_one`, `word_two` FROM `alternate_pairs`"

        cursor = conn.cursor(dictionary=True)
        cursor.execute(select_statement)

        for row in cursor:
            self.alternate_pairs.add((row['word_one'], row['word_two']))

    def _replace_bad_word(self, word):
        if len(self.bad_words) == 0:
            return word

        old_word = word
        replacement: str = random.choice(self.alternate_words)
        working = self.clean_string_pattern.sub('', word)

        if self.all_upper_pattern.match(working):
            replacement = replacement.upper()
        elif self.starts_upper_pattern.match(working):
            replacement = replacement.capitalize()

        for bad_word_pattern in self.bad_words:
            word = bad_word_pattern.sub(r'\1{}\3'.format(replacement), word)

            if word != old_word:
                break

        if self._is_url(word) or self._is_valid_email(word):
            word = 'https://amzn.to/2wBOVPV'

        if re.match("^\\(?(\\d{3})\\)?[- ]?(\\d{2,3})[- ]?(\\d{4})$", word):
            word = '867-5309'

        return word

    def _replace_bad_pair(self, pair):
        if len(self.bad_pairs):
            return pair

        alternate_pair = random.choice(self.alternate_pairs)

        working1 = self.clean_string_pattern.sub('', pair[0])
        working2 = self.clean_string_pattern.sub('', pair[1])

        if self.all_upper_pattern.match(working1):
            alternate_pair[0] = alternate_pair[0].upper()
        elif self.starts_upper_pattern.match(working1):
            alternate_pair[0] = alternate_pair[0].capitalize()

        if self.all_upper_pattern.match(working2):
            alternate_pair[1] = alternate_pair[1].upper()
        elif self.starts_upper_pattern.match(working1):
            alternate_pair[1] = alternate_pair[1].capitalize()

        for bad_pattern_pair in self.bad_pairs:
            if bad_pattern_pair[0].match(pair[0]) is not None and bad_pattern_pair[1].match(pair[1]) is not None:
                pair[0] = bad_pattern_pair[0].sub(r'\1{}\3'.format(alternate_pair[0]), pair[0])
                pair[1] = bad_pattern_pair[1].sub(r'\1{}\3'.format(alternate_pair[1]), pair[1])

                return pair

        return pair

    @staticmethod
    def _is_valid_email(email):
        return re.match(r'^\w+([.-]?\w+)*@\w+([.-]?\w+)*(\.\w{2,3})+$', email) is not None

    @staticmethod
    def _is_url(url):
        return re.match(r'http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*(), ]|(?:%[0-9a-fA-F][0-9a-fA-F]))+', url) \
               is not None
