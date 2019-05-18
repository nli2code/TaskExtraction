## TaskExtraction
This repository extracts tasks from StackOverflow posts written in Excel file.
### Setup
1. Clone the repository, build a local maven project in your Java IDE.
2. To specify the StackOverflow post file, modify `filePath` when call `parsePostsFromExcel` in `TaskExtractor.java`.
### Example
For a StackOverflow post `Apache POI converter, docx to pdf exception`
1. Extract 17 candidate verb phrases from text. The candidate verb phrases are as follows:
    ```
    need to add table to existing docx document and then convert it to Pdf file
    add table to existing docx document
    convert it to Pdf file
    Pdf file
    using Apache POI and Apache POI converter libs
    is my code
    receive a such exception: org.apache.poi.xwpf.converter.core.XWPFConverterException: java.lang.IllegalArgumentException
    be greater than zero
    write my edited docx document (w/o conversion) to the file using: it
    using
    shows me the well-formed table inside
    figure out why I receive "The number of columns in PdfPTable constructor must be greater than zero
    receive "The number of columns in PdfPTable constructor must be greater than zero
    be greater than zero
    has 3 rows and 3 columns
    create table in the wrong way
    suggest something to me
    ```
2. Calculate similarity matrix between candidate text phrases and code statements. The code statements and similarity matrix are as follows:
    ```
    public static void main(String[] args) throws Exception {
       FileInputStream fis = new FileInputStream("e:\\projects\\1.docx");
       XWPFDocument doc = new XWPFDocument(OPCPackage.open(fis));
       fis.close();
       XWPFTable table = doc.createTable();

    //added to satisfy poi docx->pdf converter and avoid future npe on getCTTbl().getTblGrid()...
       CTTblGrid ctg = table.getCTTbl().getTblGrid();
       table.getCTTbl().setTblGrid(ctg);

       fillTable(table);

       OutputStream pdfFile = new FileOutputStream(new File("e:\\projects\\1.pdf"));
       PdfOptions options= PdfOptions.create().fontEncoding("UTF-8");
       PdfConverter.getInstance().convert(doc, pdfFile, options);
    }
    ```
    ```
        1	2	3	4	5	6	7	8	9	10	11	12	13	14	15	16	17
    1	0.6	0.6							0.6			0.5					
    2	0.7	0.7							0.7								
    3																	
    4	0.3	0.3									0.3	0.5	0.5			0.3	
    5	0.3	0.3					0.4				0.3	0.4	0.4			0.3	
    6	0.3	0.3					0.3				0.3	0.3	0.3			0.3	1.0
    7	0.3	0.3									0.3	0.4	0.4			0.3	
    8	1.0		1.0	1.0					0.7			0.5					
    9	0.3		0.3	0.3												1.1	
    10	1.2		1.2	0.3	0.6		0.3					0.3	0.3				
    ```
3. Choose verb phrases with high similarity to code statements. In this post, we result in `need to add table to existing docx document and then convert it to Pdf file` and `convert it to Pdf file`.
