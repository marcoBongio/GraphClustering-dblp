
import csv
import sys
import os

# Convert comma-delimited CSV files to pipe-delimited files
# Usage: Drag-and-drop CSV file over script to convert it.

inputPath = input('Insert the name of the input file: ')
outputPath = input('Insert the name of the output file: ')

# https://stackoverflow.com/a/27553098/3357935
print("Converting CSV to tab-delimited file...")
with open(inputPath) as inputFile:
	with open(outputPath, 'w', newline='') as outputFile:
		reader = csv.DictReader(inputFile, delimiter=',')
		writer = csv.DictWriter(outputFile, reader.fieldnames, delimiter='|')
		writer.writeheader()
		writer.writerows(reader)
print("Conversion complete.")