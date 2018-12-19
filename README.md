# BDRC Etext Test 

This is a prototype for converting BDRC etexts into epubs.

Currently it only works with data kept locally on the file system. It creates markdown files which are then converted by [pandoc](http://pandoc.org/) into an epub.

## Required data

To run the prototype, you will need the relevant RDF .ttl files stored in a single directory on the file system. Namely, the files mentioned in the email describing how to get the details for texts.

For example:

    /path/to/data/:
    
        items/00/I00EGS1016699_I001.ttl
        etexts/00/UT1KG4522_I1KG4541_0000.ttl

    etc.
        
## Required applications

[Pandoc](http://pandoc.org/) v1.x is required to be installed. v2 will not work for now. 

## Running

`cd` to the directory where the repo has been cloned. Then enter this command, using the correct paths for your files:

    mvn compile exec:java -q -Dexec.args="-s /path/to/data/ -o /path/to/output/"

### Options

      * --sourceDir, -s
          The directory that contains the directories containing the .ttl files. 
          (required) 
          
        --outputDir, -o
          The directory where the generated files will be saved. Defaults to 
          ./output 
          
        --documentFiles, -df
          The directory that contains files used for the epub and docx. Defaults 
          to ./document_files
          
        --itemId, -id
          If supplied, only the item with this id will be processed.
          
        --epub, -e
          Only generate epub files
          Default: false
            
        --docx, -d
          Only generate docx files
          Default: false
          
        --help, -h
          Display the usage information.

Epub files will be generated in `/path/to/output/<datetime>/epub`.

docx files will be generated in `/path/to/output/<datetime>/docx`.

Markdown files will be generated in `/path/to/output/<datetime>/markdown`.

