import urllib.request
from bs4 import BeautifulSoup
import json
import csv
import pandas as pd
from time import sleep 
import re
import os.path
if os.path.isfile("author.csv"):
    csv_file = csv.reader(open('author.csv', "r"), delimiter=",")
if os.path.isfile("person.csv"):
    csv_file2 = csv.reader(open('person.csv', "r"), delimiter=",")

check = False
author_list = []
person_list = []

for row in csv_file:
    if row and row != 'URLAuthor':
        author_list.append(row[1].replace(';',''))
list(set(author_list))

for row in csv_file2:
    person_list.append(row[1])

for author in author_list:
    if author not in person_list:
        url = (author)
        if url != 'URLAuthor':
            html = 'None'
            user_agent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.47 Safari/537.36'
            request = urllib.request.Request(url,headers={'User-Agent': user_agent})
            try:
                html = urllib.request.urlopen(request).read()
            except:
                print("URL Error: %s"%url)
                pass
            if html != 'None':
                soup = BeautifulSoup(html,'html.parser')
                header = soup.find("header",class_="headline noline");
                main_author = header.get('data-name')
                sect = soup.find("div",attrs={'id':'info-section'})
                aff = sect.find("span", attrs={'itemprop':'name'})
                try:
                    print("New Person = %s      %s      %s"%(main_author, author, aff.text))
                except:
                    print("New Person = %s      %s"%(main_author, author))
                with open('person.csv', 'a') as newFile:
                    newFileWriter = csv.writer(newFile)
                    try:
                        newFileWriter.writerow([main_author,url,aff.text])
                    except:
                        newFileWriter.writerow([main_author,url])
            person_list.append(author)
