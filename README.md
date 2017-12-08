# BDRC Etext Test

This is a prototype for converting BDRC etexts into epubs.

Currently it only works with data kept locally on the file system. It creates a markdown file which is intended to be converted by [pandoc](http://pandoc.org/) into an epub.

## Required data

To run the prototype, you will need the relevant RDF .ttl files stored in a single directory on the file system. Namely, the files mentioned in the email describing how to get the details for texts.

For example:

    /path/to/data/:

        CP001.ttl
        I1CZ2485_E001.ttl
        I1KG12222_E001.ttl
        I4CZ5369_E001.ttl
        P7584.ttl
        UT1CZ2485_001_0000.ttl
        UT1CZ2485_001_0000.txt
        UT1KG12222_001_0001.ttl
        UT1KG12222_001_0001.txt
        UT1KG12222_001_0002.ttl
        UT1KG12222_001_0002.txt
        UT1KG12222_001_0003.ttl
        UT1KG12222_001_0003.txt
        UT1KG12222_001_0004.ttl
        UT1KG12222_001_0004.txt
        UT4CZ5369_I1KG9127_0000.ttl
        UT4CZ5369_I1KG9127_0000.txt
        W1CZ2485.ttl
        W1KG12222.ttl
        W1PD96682.ttl
        W4CZ5369.ttl

## Running

`cd` to the directory where the repo has been cloned. Then enter this command, using the correct paths for your files. TEXT-ID should be the bdrc text id e.g. `UT1CZ2485_001_0000`:

    ./gradlew -q run -PappArgs="/path/to/data/",TEXT-ID,"/path/to/output/"

Markdown files will be generated in `/path/to/output/`.

