import urllib
from urllib.request import Request, urlopen
import os

class Scraper:
    path = ''
    url_list = []
    error_list = []

    def __init__(self, parent_dir, query_list):
        self.parent_dir = parent_dir
        self.query_list = query_list
        
        print('Initialized Scraper')
        self.start()
    
    def create_new_directory(self, directory_name):
        self.path = os.path.join(parent_dir, directory_name)
        try:
            os.mkdir(self.path)
        except OSError:
            print ("Creation of the directory %s failed" % self.path)
        else:
            print ("Successfully created the directory %s " % self.path)

    def copy_webpage(self, url, file_name):
        file_path = os.path.join(self.path, file_name)

        request = Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        infile = urllib.request.urlopen(request).read()

        with open(file_path, 'w', encoding="UTF-8") as f:
            f.write(infile.decode('UTF-8'))
    
    def get_urls(self, query):
        try:
            from googlesearch import search
        except ImportError:
            print("No module named 'google' found")
        
        num = 0
        for url in search(query, tld='co.in', lang='en', num=100, start=0, stop=None, pause=10.0):
            if not url.lower().find("youtube") >= 0:
                self.url_list.append(url)
                num += 1
                if num == 20:
                    break

    def test_query(self):
        for query in self.query_list:
            self.get_urls(query)

    def print_error_list(self):
        for query in self.error_list:
            print(query)
            debug_file = os.path.join(parent_dir, "debug_file.txt")
            with open(debug_file, "a") as f:
                f.write("\n")
                f.write(str(query))
                f.write("\n")

    def start(self):
        query_num = 0 # QUERY_NUM ----------------------------------------------------------------------------------
        for query in self.query_list:
            print("Creating folder for Query {}.".format(query_num))
            directory_name = 'query{}'.format(query_num)
            
            self.create_new_directory(directory_name)

            print("\tRetrieving urls for Query {}".format(query_num))
            self.get_urls(query)

            file_num = 1
            
            for url in self.url_list:
                print("\t\tCopying Webpage of URL: {} to folder, File #{}".format(url, file_num))
                file_name = "query{}_file{}.html".format(query_num, file_num)
                
                try:
                    self.copy_webpage(url, file_name)
                except:
                    print("Error on this webpage. File #{} does not work, must do manually.".format(file_num))
                    self.error_list.append({
                        "Query" : query,
                        "URL" : url,
                        "Query #" : query_num,
                        "File #" : file_num
                    })

                file_num += 1
            
            self.url_list = []
            file_num = 0
            query_num += 1

            self.print_error_list()

        self.print_error_list()


parent_dir = 'PATH_OF_DIR'

query_list = [
    "What is the most densely populated city?", # 1
    "What is the largest building in the world?", # 2
    "What is the net worth of the richest person?", # 3
    "What are the five great lakes called?", # 4
    "How many countries are there?", # 5
    "Who was the first person on the moon?", # 6
    "What countries won World War 1?", # 7
    "What is the largest organ in our body?", # 8
    "What is the largest desert?", # 9
    "How many bones are there in the human body?" # 10
]
### CHANGE QUERY NUM IF YOU BEGIN FROM DIFFERENT NUMBERED QUERY------------------------------------------------------------------------------------------

scraper = Scraper(parent_dir, query_list)

print("Finished")