# Lucifer file carver for Rm2k

A small forensic program i developed to recover some lost RPG Maker 2000 (RM2K) projects from a 15 year old HDD.
This software works with raw input data. You need to create a non-compressed sector-by-sector copy of your drive first.

All 4 file formats exclusive to RPG Maker 2000 are supported:
* LcfDataBase (.ldb)
* LcfMapTree (.lmt)
* LcfMapUnit (.lmu)
* LcfSaveData (.lsd)

Development is on hold right now because I was able to recover most of my files. If there are exceptions on encountering unknown field ids, you might need to tweak the data structures (refer to EasyRPG documentation or EasyRPG/liblcf) or just use the "-a" option.

# Usage
	java -jar ./lcf-carver.jar -i my_hdd_image.raw -o OUTPUT_PATH
	
##Additional options
	-s		start at specified position in file (decimal input)
	-m		maximum length of data read (decimal input)
	-l		specify an alternative path for log output (default: OUTPUT_PATH/carver.log)
	-d		path to custom data structure definitions
	-a		ignore unknown field ids (if the file is incomplete or fragmented, the application will likely carve out data until the maximum file size (16MB) is reached)
	
