from pychance import PyChance
from pychance.data import SimpleTable

from timmy import db_access


class TextGenerator:
    def __init__(self):
        self.pychance = PyChance()

    def _ensure_init(self):
        if len(self.pychance.tables) < 1:
            self._load_tables()

    def _setup_temp_tables(self, input_data: dict):
        self._ensure_init()

        for key, value in input_data.items():
            table_name = 'TEMP' + key

            if table_name not in self.pychance.tables:
                table = SimpleTable(table_name)
                self.pychance.add_table(table_name, table)

            self.pychance.tables[table_name].table_values = [value]

    def _cleanup_temp_tables(self, input_data: dict):
        self._ensure_init()

        for key, value in input_data.items():
            table_name = 'TEMP' + key

            if table_name in self.pychance.tables:
                del self.pychance.tables[table_name]

    def get_string(self, input_string, input_data: dict = None):
        self._ensure_init()

        if input_data is not None:
            self._setup_temp_tables(input_data)

        result = self.pychance.parser.parse(input_string)

        if input_data is not None:
            self._cleanup_temp_tables(input_data)

        return result

    def _load_tables(self):
        self.__load_simple_tables()

    def __load_simple_tables(self):
        db = db_access.connection_pool.get_connection()
        select_query = "SELECT `table_name`, `table_entry` FROM `pychance_basic_tables` `pbt` INNER JOIN " \
                       "`pychance_basic_table_entries` `pbte` ON (`pbt`.`uuid` = `pbte`.`pychance_basic_table_id`)"
        select_cursor = db.cursor(dictionary=True)
        select_cursor.execute(select_query)
        for row in select_cursor:
            if row['table_name'] not in self.pychance.tables:
                table = SimpleTable(row['table_name'], [row['table_entry']])
                self.pychance.add_table(row['table_name'], table)
            else:
                self.pychance.tables[row['table_name']].add_value(row['table_entry'])
