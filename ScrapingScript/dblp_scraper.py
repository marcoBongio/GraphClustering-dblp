import urllib.request
from bs4 import BeautifulSoup
import json
import csv
import pandas as pd
from time import sleep 
import re
import os.path

#funzione per scraping informazioni base del ricercatore
def parse_author_entity(page_url,main_author, aff):
	with open('person.csv', 'a') as newFile:
		newFileWriter = csv.writer(newFile)
		try:
			print(aff.text)
			newFileWriter.writerow([main_author,page_url,aff.text])
		except:
			print(aff)
			newFileWriter.writerow([main_author,page_url])
	
#funzione per scraping informazioni base del venue
def parse_venue_entity2(page_url):
	user_agent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.47 Safari/537.36'
	request = urllib.request.Request(page_url,headers={'User-Agent': user_agent})
	html = urllib.request.urlopen(request).read()
	soup = BeautifulSoup(html,'html.parser')
	header = soup.find("header",class_="headline noline");
	venue = header.find("h1")
	with open('venue.csv', 'a') as newFile:
		newFileWriter = csv.writer(newFile)
		newFileWriter.writerow([venue.text,page_url])
        
def parse_venue_entity(page_url, title, article, id, year):
    check_venue = False
    a_venue = title.find_next_sibling("a")
    try:
        url_venue = a_venue.get('href')
        url_venue = url_venue.split('#', 1)[0]
        try:
            with open('publication.csv', 'a') as newFile:
                newFileWriter = csv.writer(newFile)
                newFileWriter.writerow([page_url, url_venue])
        except:
            pass
        if os.path.isfile("venue.csv"):
            csv_file = csv.reader(open('venue.csv', "r"), delimiter=",")
            #loop through csv list
            for row in csv_file:
                #if current rows 2nd value is equal to input not write
                if row:
                    if url_venue == row[1]:
                        check_venue = True
                        break
            #se non esiste già un venue con "url_venue"
            if check_venue is False:
                part_of = article.find("span",attrs={'itemprop':'isPartOf'})
                if part_of:
                    name = part_of.find("span",attrs={'itemprop':'name'})
                    to_write = name.text
                    volume = article.find("span", attrs={'itemprop':'volumeNumber'})
                    try:
                        to_write = to_write + ' ' + volume.text
                    except:
                        pass
                    try:
                        to_write = to_write + ' ' + year.text
                    except:
                        pass
                if part_of:
                    with open('venue.csv', 'a') as newFile:
                        newFileWriter = csv.writer(newFile)
                        newFileWriter.writerow([to_write, url_venue])
    except:
        return

#funzione per scraping delle pubblicazioni del ricercatore su "page_url"
def parse_publication_info(page_url, person):
	user_agent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.47 Safari/537.36'
	request = urllib.request.Request(page_url,headers={'User-Agent': user_agent})
	html = urllib.request.urlopen(request).read()
	soup = BeautifulSoup(html,'html.parser')
	sect = soup.find("div",attrs={'id':'info-section'})
	aff = sect.find("span", attrs={'itemprop':'name'})
	parse_author_entity(page_url, person, aff)
	#trova la sezione delle pubblicazioni
	main_table = soup.find("div",attrs={'id':'publ-section'})
	articles = main_table.find_all('li', {'class': re.compile(r'entry')})
	for article in articles:
		check = False
		#trova l'url dell'articolo
		li_ee = article.find('li', {'class': "ee"})
		if not li_ee:
			check = True
		if li_ee:
			a = li_ee.find('a')
			url = a.get('href')
			#se un articolo con lo stesso "url" esiste già nel file .csv non lo aggiunge
			if os.path.isfile("paper.csv"):
				csv_file = csv.reader(open('paper.csv', "r"), delimiter=",")
				#loop through csv list
				for row in csv_file:
					#if current rows 2nd value is equal to input not write
					if row:
						if url == row[1]:
							check = True
							print('Already scraped: %s'%url)
							break
		#se non esiste già un articolo con "url"
		if check is False:
			print('Extracting data %s'%url)
			#raccoglie le info sugli articoli
			nr = article.find("div", class_="nr")
			id = nr.get('id')
			id = id[0]
			authors = article.find_all("span",attrs={'itemprop':'author'})
			title = article.find("span", class_ = "title")
			year = article.find("span",attrs={'itemprop':'datePublished'})

			with open('author.csv', 'a') as newFile:
				newFileWriter = csv.writer(newFile)
				newFileWriter.writerow([url, page_url])
			for author in authors:
				a = author.find("a",attrs={'itemprop':'url'})
				if a:
					author_url = a.get('href')
					with open('author.csv', 'a') as newFile:
						newFileWriter = csv.writer(newFile)
						newFileWriter.writerow([url, author_url])
			#scrive i dati raccolti in una riga di un file .csv
			with open('paper.csv', 'a') as newFile:
				newFileWriter = csv.writer(newFile)
				newFileWriter.writerow([title.text,url,id,year.text])
			parse_venue_entity(url, title, article, id, year)

#funzione per lo scraping del ricercatore in "page-url": relazioni con gli altri ricercatori e pubblicazioni
def parse_author_info(page_url, checked_list):
	#Adding a User-Agent String in the request to prevent getting blocked while scraping 
	user_agent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.47 Safari/537.36'
	request = urllib.request.Request(page_url,headers={'User-Agent': user_agent})
	html = urllib.request.urlopen(request).read()
	soup = BeautifulSoup(html,'html.parser')
	#trova la sezione dei co-autori
	main_table = soup.find("div",attrs={'id':'coauthor-section'})
	persons = main_table.find_all("div",class_="person")
	header = soup.find("header",class_="headline noline");
	main_author = header.get('data-name')
	print('+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ Extracting data from %s +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++'%page_url)
	#se una persona con lo stesso "url" esiste già nel file .csv non lo aggiunge
	check = False
	if os.path.isfile("person.csv"):
		csv_file = csv.reader(open('person.csv', "r"), delimiter=",")
		#loop through csv list
		for row in csv_file:
			#if current rows 2nd value is equal to input not write
			if row:
				if page_url == row[1]:
					check = True
					break
		#se non esiste già una persona con "url"
		if check is False:
			parse_publication_info(page_url, main_author)
	#Remove[:10] to scrape all links 
	#per ogni ricercatore nella co-author section del principale scrapa le sue info
	for person in persons:
		a = person.find('a')
		url = a.get('href')
		print('+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ Extracting data from %s +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++'%url)

		#se una persona con lo stesso "url" esiste già nel file .csv non lo aggiunge
		check = False
		if os.path.isfile("person.csv"):
			csv_file = csv.reader(open('person.csv', "r"), delimiter=",")
			#loop through csv list
			for row in csv_file:
				#if current rows 2nd value is equal to input not write
				if row:
					if url == row[1]:
						check = True
						break
		#se non esiste già una persona con "url"
		if check is False:
			parse_publication_info(url, person.text) 
			
	return checked_list

url = input('Insert the URL from which start scraping: ')
#Adding a User-Agent String in the request to prevent getting blocked while scraping 
user_agent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.47 Safari/537.36'
request = urllib.request.Request(url,headers={'User-Agent': user_agent})
html = urllib.request.urlopen(request).read()
soup = BeautifulSoup(html,'html.parser')
#First lets get the HTML of the table called site Table where all the links are displayed
main_table = soup.find("div",attrs={'id':'coauthor-section'})
persons = main_table.find_all("div",class_="person")
if not os.path.isfile("person.csv"):
    with open('person.csv', 'w', newline='') as outcsv:
        writer = csv.DictWriter(outcsv, fieldnames = ["FullName", "URL", "Affiliation"])
        writer.writeheader()
if not os.path.isfile("author.csv"):
    with open('author.csv', 'w', newline='') as outcsv:
        writer = csv.DictWriter(outcsv, fieldnames = ["URLPaper", "URLAuthor"])
        writer.writeheader()
if not os.path.isfile("paper.csv"):
    with open('paper.csv', 'w', newline='') as outcsv:
        writer = csv.DictWriter(outcsv, fieldnames = ["Title", "URL", "Type", "Year"])
        writer.writeheader()
if not os.path.isfile("venue.csv"):
    with open('venue.csv', 'w', newline='') as outcsv:
        writer = csv.DictWriter(outcsv, fieldnames = ["Name", "URL"])
        writer.writeheader()
if not os.path.isfile("publication.csv"):
    with open('publication.csv', 'w', newline='') as outcsv:
        writer = csv.DictWriter(outcsv, fieldnames = ["URLPaper", "URLVenue"])
        writer.writeheader()
checked_list = []
header = soup.find("header",class_="headline noline");
main_author = header.get('data-name')
check = False
print('======================================================================================== %s ========================================================================================' %main_author)
checked_list = parse_author_info(url, checked_list)
#Remove[:10] to scrape all links 
#for person in persons[:2]:
#	print('======================================================================================== %s ========================================================================================' %person.text)
#	a = person.find('a')
#	url = a.get('href')
#
#	checked_list = parse_author_info(url, checked_list)
